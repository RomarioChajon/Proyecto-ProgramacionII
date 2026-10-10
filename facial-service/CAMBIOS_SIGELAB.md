# Cambios respecto al proyecto de ejemplo
- `app/repository.py` `get_user`: consulta adaptada a `dbo.Usuario`/`dbo.Rol` de SIGELAB.
- `scripts/find_user.py`: usa `nombre_usuario` y `estado`.
- BD por defecto: `SIGELAB` (`.env.example`, `config.py`, README).
- `scripts/configure_from_java.py`: lee `src/main/resources/db.properties`.
- Copiar `.env.example` a `.env` con `DB_NAME=SIGELAB`.
- Registrar rostros con los ids: admin=1, enc.lab01=2, enc.lab02=3, enc.lab03=4, docente1=5, estudiante1=6, estudiante2=7.

## Control desde Java
La clase `umg.sigelab.integration.facial.FacialServiceLauncher` y la ventana `ServicioFacialForm`
ejecutan `setup-facial-*` y `start-facial.*` y muestran su salida. Al terminar bien la instalación se
crea el archivo `.setup-ok` (ignorado por git). Ver `docs/PASO-3-LOGIN.md`.
