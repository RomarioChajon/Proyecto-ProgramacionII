package umg.sigelab.util;

/**
 * Se lanza cuando una operación incumple una REGLA DE NEGOCIO
 * (por ejemplo: "Ya existe un usuario con ese correo").
 *
 * <p>Su mensaje está escrito para el usuario final: la vista puede
 * mostrarlo tal cual en un cuadro de diálogo.</p>
 *
 * <p>Es una excepción no verificada (RuntimeException) para no obligar a
 * declarar {@code throws} en cada método de las capas Service/Controller.</p>
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
