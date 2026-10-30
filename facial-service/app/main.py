from __future__ import annotations

import logging
import threading
from contextlib import asynccontextmanager

import cv2
import numpy as np

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from .config import settings
from .db import Database
from .engine import FaceEngine
from .emotion import EmotionClassifier
from .errors import FacialServiceError
from .interactive import run_interactive_session, run_qr_scan
from .repository import FacialRepository
from .schemas import (
    DeleteProfileRequest,
    EnrollRequest,
    EnrollResponse,
    FaceStatusResponse,
    HealthResponse,
    UserRequest,
    VerifyResponse,
)
from .service import FaceService


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    database = Database(settings)
    repository = FacialRepository(database)
    try:
        engine = FaceEngine(settings)
    except FacialServiceError:
        logger.exception("El servicio iniciará en estado degradado: modelo no disponible")
        engine = None

    app.state.face_service = FaceService(repository, engine, settings)

    try:
        app.state.emotion_classifier = EmotionClassifier()
        logger.info("Modelo de expresiones faciales cargado correctamente")
    except Exception:
        logger.exception("No se pudo cargar el modelo de expresiones faciales")
        app.state.emotion_classifier = None

    yield



app = FastAPI(
    title="Universidad Asistencia - Facial Service",
    version="1.0.0",
    lifespan=lifespan,
)


def service() -> FaceService:
    return app.state.face_service


@app.exception_handler(FacialServiceError)
async def facial_error_handler(_, exc: FacialServiceError):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.message, "code": exc.code},
        headers={"X-Facial-Code": exc.code},
    )


@app.get("/health", response_model=HealthResponse, response_model_by_alias=True)
def health():
    return service().health()


@app.get("/face/status/{user_id}", response_model=FaceStatusResponse, response_model_by_alias=True)
def face_status(user_id: int):
    return service().status(user_id)


@app.post("/face/enroll", response_model=EnrollResponse, response_model_by_alias=True)
def face_enroll(request: EnrollRequest):
    logger.info("POST /face/enroll thread=%s user_id=%s", threading.current_thread().name, request.user_id)
    return run_interactive_session("enroll", request.user_id, request.replace, settings)


@app.post("/face/verify", response_model=VerifyResponse, response_model_by_alias=True)
def face_verify(request: UserRequest):
    logger.info("POST /face/verify thread=%s user_id=%s", threading.current_thread().name, request.user_id)
    return run_interactive_session("verify", request.user_id, False, settings)


@app.delete("/face/profile/{user_id}")
def face_delete(user_id: int, request: DeleteProfileRequest):
    return service().delete_profile(user_id, request.admin_user_id)


@app.post("/qr/scan")
def qr_scan(timeout: int = 30):
    """Abre la cámara, espera un código QR y devuelve su texto: {"success": true, "text": "..."}."""
    logger.info("POST /qr/scan thread=%s timeout=%s", threading.current_thread().name, timeout)
    return run_qr_scan(timeout)


@app.post("/face/detect-multiple")
async def detect_multiple_faces(request: Request):
    """Detecta varios rostros en una imagen enviada en el cuerpo de la solicitud."""
    face_service = service()

    if not face_service.model_loaded or face_service.engine is None:
        raise HTTPException(
            status_code=503,
            detail="El modelo facial no está disponible.",
        )

    image_bytes = await request.body()

    if not image_bytes:
        raise HTTPException(
            status_code=400,
            detail="No se recibió ninguna imagen.",
        )

    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    frame = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

    if frame is None:
        raise HTTPException(
            status_code=400,
            detail="El archivo recibido no es una imagen válida.",
        )

    faces = face_service.engine.detect(frame)
    detected_faces = []

    for index, face in enumerate(faces, start=1):
        bbox = getattr(face, "bbox", None)

        if bbox is None:
            continue

        x1, y1, x2, y2 = [int(value) for value in bbox]

        detected_faces.append({
            "faceNumber": index,
            "boundingBox": {
                "x1": x1,
                "y1": y1,
                "x2": x2,
                "y2": y2,
            },
        })

    return {
        "success": True,
        "count": len(detected_faces),
        "faces": detected_faces,
        "message": (
            "Rostros detectados correctamente."
            if detected_faces
            else "No se detectaron rostros."
        ),
    }

@app.post("/face/detect-emotions")
async def detect_face_emotions(request: Request):
    """Detecta rostros y estima la expresión visible de cada uno."""
    face_service = service()
    classifier = getattr(app.state, "emotion_classifier", None)

    if not face_service.model_loaded or face_service.engine is None:
        raise HTTPException(
            status_code=503,
            detail="El modelo de detección facial no está disponible.",
        )

    if classifier is None:
        raise HTTPException(
            status_code=503,
            detail="El modelo de expresiones faciales no está disponible.",
        )

    image_bytes = await request.body()

    if not image_bytes:
        raise HTTPException(
            status_code=400,
            detail="No se recibió ninguna imagen.",
        )

    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    frame = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

    if frame is None:
        raise HTTPException(
            status_code=400,
            detail="El archivo recibido no es una imagen válida.",
        )

    faces = face_service.engine.detect(frame)
    detected_faces = []

    height, width = frame.shape[:2]

    for index, face in enumerate(faces, start=1):
        bbox = getattr(face, "bbox", None)

        if bbox is None:
            continue

        x1, y1, x2, y2 = [int(value) for value in bbox]

        # Limitar las coordenadas al tamaño de la imagen.
        x1 = max(0, min(x1, width))
        x2 = max(0, min(x2, width))
        y1 = max(0, min(y1, height))
        y2 = max(0, min(y2, height))

        if x2 <= x1 or y2 <= y1:
            continue

        face_crop = frame[y1:y2, x1:x2]

        try:
            prediction = classifier.predict(face_crop)
        except Exception:
            logger.exception(
                "Error al estimar la expresión del rostro %s", index
            )
            continue

        detected_faces.append({
            "faceNumber": index,
            "boundingBox": {
                "x1": x1,
                "y1": y1,
                "x2": x2,
                "y2": y2,
            },
            "expression": prediction["emotion"],
            "confidence": prediction["confidence"],
        })

    return {
        "success": True,
        "count": len(detected_faces),
        "faces": detected_faces,
        "message": (
            "Rostros y expresiones estimados correctamente."
            if detected_faces
            else "No se pudieron obtener rostros con expresiones."
        ),
        "note": (
            "La expresión es una estimación visual, no una "
            "determinación del estado emocional real."
        ),
    }
