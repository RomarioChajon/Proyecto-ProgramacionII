
from pathlib import Path

import cv2
import numpy as np
import onnxruntime as ort


class EmotionClassifier:
    """Estima la expresión facial visible usando el modelo FER+."""

    EMOTIONS = [
        "neutral",
        "felicidad",
        "sorpresa",
        "tristeza",
        "enojo",
        "disgusto",
        "miedo",
        "desprecio",
    ]

    def __init__(self):
        model_path = (
            Path.home()
            / ".insightface"
            / "models"
            / "emotion"
            / "emotion-ferplus-8.onnx"
        )

        if not model_path.is_file():
            raise FileNotFoundError(
                f"No se encontró el modelo de emociones: {model_path}"
            )

        self.session = ort.InferenceSession(
            str(model_path),
            providers=["CPUExecutionProvider"],
        )
        self.input_name = self.session.get_inputs()[0].name
        self.output_name = self.session.get_outputs()[0].name

    def predict(self, face_image: np.ndarray) -> dict:
        if face_image is None or face_image.size == 0:
            raise ValueError("La imagen del rostro está vacía.")

        gray = cv2.cvtColor(face_image, cv2.COLOR_BGR2GRAY)
        gray = cv2.resize(gray, (64, 64))
        gray = gray.astype(np.float32)
        tensor = gray[np.newaxis, np.newaxis, :, :]

        output = self.session.run(
            [self.output_name],
            {self.input_name: tensor},
        )[0][0]

        # Convierte los ocho valores de salida en probabilidades.
        output = output.astype(np.float64)
        exp_values = np.exp(output - np.max(output))
        probabilities = exp_values / np.sum(exp_values)

        index = int(np.argmax(probabilities))

        return {
            "emotion": self.EMOTIONS[index],
            "confidence": round(float(probabilities[index]), 4),
        }
