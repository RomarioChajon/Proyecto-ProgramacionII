from __future__ import annotations

import logging
import os
import platform
import shutil
import subprocess
from contextlib import contextmanager
from pathlib import Path
import time
from typing import Iterator


def _configure_macos_odbc_runtime() -> None:
    """Expose Homebrew OpenSSL to Microsoft's macOS ODBC driver when needed."""
    if platform.system() != "Darwin" or shutil.which("brew") is None:
        return
    try:
        openssl_lib = subprocess.check_output(
            ["brew", "--prefix", "openssl@3"], text=True, stderr=subprocess.DEVNULL
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return
    lib_path = str(Path(openssl_lib) / "lib")
    existing = os.environ.get("DYLD_LIBRARY_PATH", "")
    if lib_path not in existing.split(":"):
        os.environ["DYLD_LIBRARY_PATH"] = ":".join(filter(None, (lib_path, existing)))


_configure_macos_odbc_runtime()
import pyodbc

from .config import Settings, settings


logger = logging.getLogger(__name__)


class Database:
    def __init__(self, configuration: Settings = settings):
        self.settings = configuration
        self._last_ping_ok: bool = False
        self._last_ping_time: float = 0.0

    def connect(self) -> pyodbc.Connection:
        return pyodbc.connect(self.settings.connection_string, timeout=30)

    def ping(self, cache_ttl: float = 60.0) -> bool:
        now = time.monotonic()
        ttl = cache_ttl if self._last_ping_ok else 10.0
        if self._last_ping_time > 0.0 and (now - self._last_ping_time) < ttl:
            return self._last_ping_ok
        try:
            with self.connect() as connection:
                connection.execute("SELECT 1").fetchone()
            self._last_ping_ok = True
        except pyodbc.Error:
            logger.exception("SQL Server no está disponible")
            self._last_ping_ok = False
        self._last_ping_time = time.monotonic()
        return self._last_ping_ok

    def facial_tables_ready(self) -> bool:
        query = """
            SELECT COUNT(*)
            FROM sys.tables
            WHERE name IN (N'PerfilFacial', N'AuditoriaFacial')
        """
        try:
            with self.connect() as connection:
                count = connection.execute(query).fetchone()[0]
            return int(count) == 2
        except pyodbc.Error:
            logger.exception("No fue posible comprobar las tablas faciales")
            return False

    @contextmanager
    def cursor(self) -> Iterator[pyodbc.Cursor]:
        with self.connect() as connection:
            yield connection.cursor()
            connection.commit()
