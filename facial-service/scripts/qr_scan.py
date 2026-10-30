"""Lector de QR por cámara. Se ejecuta como proceso hijo para que la ventana de OpenCV
use el hilo principal (obligatorio en macOS). Imprime UN objeto JSON por stdout."""
from __future__ import annotations

import argparse
import json
import logging
import sys
import time

import cv2
import numpy as np

from app.camera import open_camera
from app.config import settings
from app.errors import CaptureTimeoutError, FacialServiceError
from app.preview import CELESTE_MEDIO, CELESTE_PRINCIPAL, PreviewWindow
from app.qr import decode_qr


logging.basicConfig(stream=sys.stderr, level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger(__name__)

TITLE = "SIGELAB - Lector de codigo QR"


def _draw_qr_guide(frame: np.ndarray) -> np.ndarray:
    guided = frame.copy()
    h, w = guided.shape[:2]
    size = min(h, w) // 2
    x1 = (w - size) // 2
    y1 = max(60, (h - size) // 2 + 16)
    x2 = x1 + size
    y2 = y1 + size
    cv2.rectangle(guided, (x1, y1), (x2, y2), CELESTE_MEDIO, 2)
    corner = max(18, size // 6)
    cv2.line(guided, (x1, y1), (x1 + corner, y1), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x1, y1), (x1, y1 + corner), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x2, y1), (x2 - corner, y1), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x2, y1), (x2, y1 + corner), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x1, y2), (x1 + corner, y2), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x1, y2), (x1, y2 - corner), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x2, y2), (x2 - corner, y2), CELESTE_PRINCIPAL, 4)
    cv2.line(guided, (x2, y2), (x2, y2 - corner), CELESTE_PRINCIPAL, 4)
    return guided


def scan(timeout: int) -> dict[str, object]:
    window = PreviewWindow(TITLE)
    deadline = time.monotonic() + timeout
    try:
        with open_camera(settings) as capture:
            while time.monotonic() < deadline:
                ok, frame = capture.read()
                if not ok or frame is None:
                    time.sleep(0.05)
                    continue
                text = decode_qr(frame)
                if text:
                    logger.info("QR leido (%s caracteres)", len(text))
                    return {"success": True, "text": text}
                remaining = int(deadline - time.monotonic())
                guided_frame = _draw_qr_guide(frame)
                key = window.show(
                    guided_frame,
                    [],
                    [
                        "LECTOR QR - SIGELAB",
                        "Muestre el codigo QR del equipo dentro del recuadro",
                        f"Tiempo restante: {remaining} s",
                        "ESC - Cancelar",
                    ],
                )
                if key == 27:
                    return {"success": False, "code": "CAPTURA_CANCELADA",
                            "detail": "Lectura de QR cancelada.", "statusCode": 400}
        raise CaptureTimeoutError("No se detectó ningún código QR a tiempo.", "QR_TIMEOUT")
    finally:
        window.close()


def main() -> int:
    parser = argparse.ArgumentParser(description="Lee un código QR con la cámara.")
    parser.add_argument("--timeout", type=int, default=30)
    args = parser.parse_args()
    try:
        result = scan(max(5, min(args.timeout, 120)))
    except FacialServiceError as exc:
        result = {"success": False, "code": exc.code, "detail": exc.message, "statusCode": exc.status_code}
    except Exception:
        logger.exception("Falló la lectura de QR")
        result = {"success": False, "code": "QR_ERROR", "detail": "No fue posible leer el código QR.",
                  "statusCode": 500}
    print(json.dumps(result, ensure_ascii=False), flush=True)
    return 0 if result.get("success") is True else 1


if __name__ == "__main__":
    raise SystemExit(main())
