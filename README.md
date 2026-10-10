# Proyecto-ProgramacionII
Sistema de Gestión y Control de Laboratorios Universitarios (SIGELAB)

## Configuración rápida
1. Ejecutar en SQL Server `SIGELAB_01_estructura.sql`, `SIGELAB_02_datos_prueba.sql` y `SIGELAB_03_foto_usuario.sql`.
2. Copiar `src/main/resources/db.properties.example` a `db.properties` y poner su contraseña (no se sube a git).
3. Ejecutar `umg.sigelab.Main` desde IntelliJ (los `.form` se compilan en el IDE). Abre el login.
4. Reconocimiento facial (OBLIGATORIO para ingresar; registre los rostros antes): ver `docs/PASO-3-LOGIN.md`, `facial-service/README.md` y `facial-service/CAMBIOS_SIGELAB.md`.

Guías: `docs/PASO-2-USUARIOS.md` (usuarios) y `docs/PASO-3-LOGIN.md` (login, menú y servicio facial).
