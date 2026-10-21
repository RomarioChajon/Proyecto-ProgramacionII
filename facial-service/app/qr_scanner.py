from __future__ import annotations

import logging
import time

import cv2

from .camera import open_camera
from .config import settings

logger = logging.getLogger(__name__)

WINDOW_NAME = "SIGELAB - Lector QR"


class QRScanError(Exception):
    """Error controlado durante la lectura del codigo QR."""


def scan_qr(timeout: int = 30) -> str:
    """
    Abre la camara, detecta un codigo QR y devuelve su texto.

    ENTER o ESC: cancelar.
    El tiempo maximo se indica en segundos.
    """
    if not 5 <= timeout <= 120:
        raise ValueError("El tiempo debe estar entre 5 y 120 segundos.")

    detector = cv2.QRCodeDetector()
    deadline = time.monotonic() + timeout

    logger.info("Iniciando lector QR SIGELAB, timeout=%s", timeout)

    try:
        with open_camera(settings) as capture:
            while time.monotonic() < deadline:
                ok, frame = capture.read()

                if not ok or frame is None:
                    raise QRScanError(
                        "No se pudo obtener una imagen de la camara."
                    )

                texto, _, _ = detector.detectAndDecode(frame)

                if texto and texto.strip():
                    resultado = texto.strip()
                    logger.info("Codigo QR detectado correctamente.")
                    return resultado

                restante = max(0, int(deadline - time.monotonic()))

                cv2.putText(
                    frame,
                    "SIGELAB - Coloque el QR frente a la camara",
                    (20, 40),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.65,
                    (0, 255, 0),
                    2,
                )

                cv2.putText(
                    frame,
                    f"Tiempo restante: {restante}s | ESC: cancelar",
                    (20, 75),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (255, 255, 255),
                    2,
                )

                cv2.imshow(WINDOW_NAME, frame)

                tecla = cv2.waitKey(1) & 0xFF

                if tecla in (27, 13):
                    raise QRScanError(
                        "Lectura QR cancelada por el usuario."
                    )

        raise QRScanError(
            "No se detecto ningun codigo QR dentro del tiempo establecido."
        )

    except cv2.error as exc:
        logger.exception("Error de OpenCV durante la lectura QR")
        raise QRScanError(
            "Ocurrio un error al utilizar la camara."
        ) from exc
