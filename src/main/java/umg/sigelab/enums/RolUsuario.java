package umg.sigelab.enums;

/**
 * Rol de un usuario (tabla Rol).
 *
 * <p>Los identificadores (1..4) son FIJOS y coinciden con los datos de la tabla Rol.
 * El CHECK de la tabla Usuario usa {@code id_rol = 2} para ENCARGADO, por eso
 * nunca deben cambiar.</p>
 *
 * <p>Se usa un enum con roles en lugar de herencia (Admin extends Usuario, etc.):
 * los 4 roles tienen los mismos datos y solo cambian los permisos, así que una
 * sola clase Usuario con un atributo {@code rol} es más simple y suficiente.</p>
 */
public enum RolUsuario {
    ADMIN(1, "Administrador"),
    ENCARGADO(2, "Encargado de laboratorio"),
    DOCENTE(3, "Docente"),
    ESTUDIANTE(4, "Estudiante");

    private final int id;
    private final String etiqueta;

    RolUsuario(int id, String etiqueta) {
        this.id = id;
        this.etiqueta = etiqueta;
    }

    /** Valor de la columna id_rol. */
    public int getId() {
        return id;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Solo el ENCARGADO está asignado a un laboratorio (sede). */
    public boolean requiereLaboratorio() {
        return this == ENCARGADO;
    }

    /** Convierte el id guardado en la base de datos al enum. */
    public static RolUsuario deId(int id) {
        for (RolUsuario rol : values()) {
            if (rol.id == id) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido en la base de datos: " + id);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
