from __future__ import annotations

import cv2
import numpy as np


def decode_qr(frame: np.ndarray) -> str | None:
    """Lee un código QR de un cuadro de la cámara. Devuelve el texto o None si no hay QR legible."""
    detector = cv2.QRCodeDetector()
    candidates = [frame]
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) if frame.ndim == 3 else frame
    candidates.append(gray)
    # Segundo intento ampliado: ayuda con QR pequeños o lejanos.
    candidates.append(cv2.resize(gray, None, fx=1.6, fy=1.6, interpolation=cv2.INTER_CUBIC))
    for image in candidates:
        try:
            text, _, _ = detector.detectAndDecode(image)
        except cv2.error:
            continue
        if text:
            return text
    return None
