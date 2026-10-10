package umg.sigelab.util;

/**
 * Se lanza cuando ocurre un error TÉCNICO al acceder a la base de datos
 * (conexión caída, SQL inválido, configuración faltante, etc.).
 *
 * <p>El mensaje es amigable para el usuario; el detalle técnico (la
 * {@code SQLException} original) viaja como "causa" y se escribe en el
 * registro (log) desde {@link JdbcSupport}, nunca se muestra en pantalla.</p>
 */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
