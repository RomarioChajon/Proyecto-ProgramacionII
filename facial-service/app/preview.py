from __future__ import annotations

import logging
import unicodedata
from dataclasses import dataclass

import cv2
import numpy as np

from .errors import CaptureCancelledError, PreviewUnavailableError


logger = logging.getLogger(__name__)

# Paleta Celeste y Blanco SIGELAB en formato BGR (OpenCV)
CELESTE_PRINCIPAL = (214, 143, 43)   # #2B8FD6
CELESTE_MEDIO = (237, 179, 99)       # #63B3ED
CELESTE_SUAVE = (248, 227, 190)      # #BEE3F8
BLANCO_HIELO = (255, 248, 240)       # #F0F8FF
BLANCO_PURO = (255, 255, 255)        # #FFFFFF
TEXTO_OSCURO = (44, 32, 26)          # #1A202C


@dataclass(frozen=True)
class FaceOverlay:
    face: object
    color: tuple[int, int, int]


class PreviewWindow:
    """Small HighGUI adapter owned by the runner's main thread (Tema Celeste y Blanco SIGELAB)."""

    def __init__(self, title: str):
        self.title = title
        self.initialized = False

    def _initialize(self) -> None:
        if self.initialized:
            return
        gui_line = next(
            (line.strip() for line in cv2.getBuildInformation().splitlines() if "GUI:" in line),
            "GUI: unknown",
        )
        if "NONE" in gui_line.upper():
            raise PreviewUnavailableError(
                "La instalación de OpenCV no incluye soporte GUI para mostrar la cámara."
            )
        try:
            cv2.namedWindow(self.title, cv2.WINDOW_NORMAL)
            cv2.resizeWindow(self.title, 960, 540)
            cv2.waitKey(1)
        except cv2.error as exc:
            raise PreviewUnavailableError(
                f"No se pudo inicializar la ventana de cámara ({gui_line}). "
                "Ejecute python -m scripts.preview_test para diagnosticarla."
            ) from exc
        self.initialized = True
        logger.info("Preview initialized title=%s", self.title)

    def show(self, frame: np.ndarray, overlays: list[FaceOverlay], lines: list[str]) -> int:
        self._initialize()
        preview = frame.copy()
        for overlay in overlays:
            bbox = np.asarray(getattr(overlay.face, "bbox", []), dtype=int)
            if bbox.size != 4:
                continue
            x1, y1, x2, y2 = bbox.tolist()
            cv2.rectangle(preview, (x1, y1), (x2, y2), overlay.color, 2)
            self._draw_corners(preview, x1, y1, x2, y2, CELESTE_PRINCIPAL)
        self._draw_overlay(preview, lines)
        try:
            cv2.imshow(self.title, preview)
            key = cv2.waitKey(1) & 0xFF
            if self._is_closed():
                raise CaptureCancelledError()
            return key
        except CaptureCancelledError:
            raise
        except cv2.error as exc:
            raise PreviewUnavailableError(
                "La ventana de cámara dejó de estar disponible; la captura fue detenida."
            ) from exc

    def _is_closed(self) -> bool:
        try:
            visible = cv2.getWindowProperty(self.title, cv2.WND_PROP_VISIBLE)
            return visible < 1
        except cv2.error as exc:
            raise PreviewUnavailableError("No se pudo comprobar la ventana de cámara.") from exc

    @staticmethod
    def _draw_corners(frame: np.ndarray, x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int]) -> None:
        length = max(14, min(28, (x2 - x1) // 5))
        thickness = 4
        cv2.line(frame, (x1, y1), (x1 + length, y1), color, thickness)
        cv2.line(frame, (x1, y1), (x1, y1 + length), color, thickness)
        cv2.line(frame, (x2, y1), (x2 - length, y1), color, thickness)
        cv2.line(frame, (x2, y1), (x2, y1 + length), color, thickness)
        cv2.line(frame, (x1, y2), (x1 + length, y2), color, thickness)
        cv2.line(frame, (x1, y2), (x1, y2 - length), color, thickness)
        cv2.line(frame, (x2, y2), (x2 - length, y2), color, thickness)
        cv2.line(frame, (x2, y2), (x2, y2 - length), color, thickness)

    @staticmethod
    def _ascii_text(text: str) -> str:
        clean = text.replace("✓", "[OK]")
        normalized = unicodedata.normalize("NFKD", clean)
        return "".join(ch for ch in normalized if not unicodedata.combining(ch))

    @classmethod
    def _draw_overlay(cls, frame: np.ndarray, lines: list[str]) -> None:
        if not lines:
            return
        h, w = frame.shape[:2]

        # Encabezado superior en Celeste Principal (#2B8FD6) con borde Celeste Medio (#63B3ED)
        header_h = 46
        cv2.rectangle(frame, (0, 0), (w, header_h), CELESTE_PRINCIPAL, -1)
        cv2.line(frame, (0, header_h), (w, header_h), CELESTE_MEDIO, 2)
        title_text = cls._ascii_text(lines[0])
        cv2.putText(
            frame,
            title_text,
            (18, 31),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.72,
            BLANCO_PURO,
            2,
            cv2.LINE_AA,
        )

        body_lines = lines[1:14]
        if not body_lines:
            return

        line_height = 27
        card_x1 = 14
        card_y1 = header_h + 12
        card_w = min(w - 28, 560)
        card_h = 16 + len(body_lines) * line_height
        card_x2 = card_x1 + card_w
        card_y2 = min(h - 10, card_y1 + card_h)

        # Tarjeta lateral Blanco Hielo (#F0F8FF) semitransparente con borde Celeste Suave/Medio
        roi = frame[card_y1:card_y2, card_x1:card_x2]
        if roi.size:
            ice_bg = np.full_like(roi, BLANCO_HIELO)
            cv2.addWeighted(ice_bg, 0.86, roi, 0.14, 0, roi)
        cv2.rectangle(frame, (card_x1, card_y1), (card_x2, card_y2), CELESTE_MEDIO, 1)
        cv2.rectangle(frame, (card_x1, card_y1), (card_x1 + 5, card_y2), CELESTE_PRINCIPAL, -1)

        for index, raw_line in enumerate(body_lines):
            text = cls._ascii_text(raw_line)
            y = card_y1 + 26 + index * line_height
            if y > card_y2 - 6:
                break
            is_highlight = (
                text.startswith("Accion:")
                or text.startswith("ACCION:")
                or text.startswith("Tiempo")
                or "[OK]" in text
                or "COMENZANDO" in text
            )
            color = CELESTE_PRINCIPAL if is_highlight else TEXTO_OSCURO
            thickness = 2 if is_highlight else 1
            cv2.putText(
                frame,
                text,
                (card_x1 + 16, y),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.58,
                color,
                thickness,
                cv2.LINE_AA,
            )

    def close(self) -> None:
        if not self.initialized:
            return
        try:
            cv2.destroyWindow(self.title)
            cv2.waitKey(1)
        except cv2.error:
            pass
        self.initialized = False


def preview_probe(frame: np.ndarray, title: str = "SIGELAB - Preview Test") -> bool:
    window = PreviewWindow(title)
    try:
        window.show(frame, [], ["PREVIEW TEST - SIGELAB", "Video en vivo detectado", "ESC - Cerrar"])
        return True
    finally:
        window.close()
