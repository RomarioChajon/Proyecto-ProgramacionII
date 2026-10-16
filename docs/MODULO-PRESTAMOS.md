# Módulo de préstamos (docente/estudiante, encargado, administrador)

## Orden para ponerlo en marcha
1. Ejecutar `database/SIGELAB_07_modulo_prestamos.sql` (idempotente) en DBeaver con Option + X.
2. Reiniciar el servicio facial (agrega el endpoint `/qr/scan` para leer QR con la cámara).
3. IntelliJ: Build > Rebuild Project.

## Pantallas
- DOCENTE / ESTUDIANTE: `SolicitudPrestamoForm` (barra lateral con usuario e ID, sede, categoría, carrito, 1-30 días, historial).
- ENCARGADO: `PanelOperacionesForm` (órdenes pendientes, inventario, mantenimiento, préstamo en mostrador, devoluciones).
- ADMIN: botones del menú principal abren las mismas secciones (elige sede en la barra lateral) y `ReportesForm` (dashboard, vencidos, bloqueos, auditoría).

## Reglas en la capa Service
`PrestamoService`, `DevolucionService`, `InventarioService`, `ConsultaService`; todo con transacción y auditoría.
