"""Punto de entrada del servicio facial empaquetado como facial-service.exe (PyInstaller).

- Sin argumentos: inicia la API, igual que ``python -m scripts.start``.
- Con ``-m <modulo> [args...]``: ejecuta ese módulo, igual que ``python -m``.

Lo segundo es necesario porque el servicio abre la cámara en un proceso hijo
(``scripts.interactive_face`` para el registro/verificación facial y ``scripts.qr_scan``
para leer QR) usando ``sys.executable -m ...``. Dentro del .exe, ``sys.executable`` es
este mismo programa, así que aquí se atiende ese ``-m``.

En desarrollo no hace falta este archivo: se sigue usando start-facial.ps1 / .bat.
"""

from __future__ import annotations

import importlib
import multiprocessing
import runpy
import sys


def _ejecutar_modulo(nombre: str, argumentos: list[str]) -> int:
    sys.argv = [nombre, *argumentos]
    modulo = importlib.import_module(nombre)
    principal = getattr(modulo, "main", None)
    if callable(principal):
        resultado = principal()
        return resultado if isinstance(resultado, int) else 0
    # Módulos sin main(): se ejecutan como lo haría "python -m".
    runpy.run_module(nombre, run_name="__main__", alter_sys=True)
    return 0


def _precargar_driver_odbc() -> None:
    """Carga el driver ODBC de SQL Server ANTES que ONNX Runtime y OpenCV.

    Dentro del .exe, si el driver se carga después del modelo facial, toma las DLL de
    Visual C++ que trajo ONNX Runtime y la conexión falla con
    "The driver did not supply an error!". Cargándolo primero usa las del sistema.
    """
    if sys.platform != "win32":
        return
    try:
        import ctypes
        import winreg

        from app.config import settings

        clave = rf"SOFTWARE\ODBC\ODBCINST.INI\{settings.db_driver}"
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, clave) as registro:
            ruta, _ = winreg.QueryValueEx(registro, "Driver")
        ctypes.WinDLL(ruta)
    except Exception:  # noqa: BLE001 - si no se puede, se sigue como antes
        pass


def main() -> int:
    _precargar_driver_odbc()
    if len(sys.argv) >= 3 and sys.argv[1] == "-m":
        return _ejecutar_modulo(sys.argv[2], sys.argv[3:])

    from scripts.start import main as iniciar_api

    iniciar_api()
    return 0


if __name__ == "__main__":
    multiprocessing.freeze_support()
    raise SystemExit(main())