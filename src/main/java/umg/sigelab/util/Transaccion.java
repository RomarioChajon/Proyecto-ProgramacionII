package umg.sigelab.util;

import umg.sigelab.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ayuda para ejecutar varias operaciones SQL como UNA sola transacción (todo o nada).
 *
 * <p>Los Service la usan cuando una acción modifica varias tablas (por ejemplo, despachar un préstamo:
 * actualizar el préstamo, cambiar el estado de cada equipo y registrar la auditoría). Si algo falla,
 * se hace rollback y no queda información a medias.</p>
 */
public final class Transaccion {

    /** Trabajo que se ejecuta con una conexión ya abierta. */
    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection cn) throws SQLException;
    }

    private Transaccion() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Ejecuta el trabajo dentro de una transacción: commit si termina bien, rollback si lanza error.
     *
     * @param operacion verbo + objeto para el mensaje de error ("registrar el préstamo")
     */
    public static <T> T ejecutar(String operacion, Trabajo<T> trabajo) {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(cn);
                cn.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcSupport.error(operacion, e);
        }
    }

    /** Ejecuta una consulta de solo lectura (sin transacción explícita). */
    public static <T> T consultar(String operacion, Trabajo<T> trabajo) {
        try (Connection cn = DatabaseConnection.getConnection()) {
            return trabajo.ejecutar(cn);
        } catch (SQLException e) {
            throw JdbcSupport.error(operacion, e);
        }
    }
}
