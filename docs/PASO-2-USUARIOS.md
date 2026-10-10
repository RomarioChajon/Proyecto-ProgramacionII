# Paso 2: config, enums, util y CRUD de Usuario

- `config/DatabaseConnection`: conexión JDBC (propiedad del sistema -> variable de entorno -> `db.properties`).
- `enums/`: estados; el valor en BD es `name()`, la etiqueta visible es `getEtiqueta()`.
- `util/`: BusinessException, DataAccessException, DateUtils, JdbcSupport, Validaciones, ImagenUtils.
  `DatosPrueba` no se usa: el script SQL 02 cumple ese papel.
- Usuario: una sola clase + enum `RolUsuario` (sin herencia).
- Nada se elimina: solo cambia `estado` (ACTIVO/INACTIVO).
- Foto: JPEG máx. 400 px guardado en `Usuario.foto` (VARBINARY(MAX)).
- Facial: botones "Registrar rostro" / "Eliminar rostro" usan `integration/facial/FacialAuthClient`
  (solo envía el id de usuario). Eliminar rostro requiere ADMIN con sesión (aún no hay login).
- Contraseña de todos los usuarios de prueba: `Sigelab2026*`.
