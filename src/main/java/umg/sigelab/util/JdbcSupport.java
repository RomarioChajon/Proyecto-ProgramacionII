package umg.sigelab.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidades compartidas por todos los repositorios para que el manejo de
 * errores y la lectura de columnas sea SIEMPRE igual.
 */
public final class JdbcSupport {

    private static final Logger LOG = Logger.getLogger(JdbcSupport.class.getName());

    /** Códigos de error de SQL Server que sabemos traducir a un mensaje de negocio. */
    private static final int ERROR_CLAVE_DUPLICADA_CONSTRAINT = 2627; // UNIQUE / PRIMARY KEY
    private static final int ERROR_CLAVE_DUPLICADA_INDICE = 2601;     // índice único
    private static final int ERROR_RESTRICCION = 547;                 // FOREIGN KEY o CHECK

    private JdbcSupport() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Traduce una {@link SQLException} a una excepción entendible.
     *
     * <ul>
     *   <li>Valor duplicado (UNIQUE) -> {@link BusinessException}.</li>
     *   <li>Violación de FK/CHECK    -> {@link BusinessException}.</li>
     *   <li>Cualquier otro error     -> {@link DataAccessException}.</li>
     * </ul>
     * En todos los casos el detalle técnico queda en el log.
     *
     * <p>Uso típico: {@code throw JdbcSupport.error("guardar el usuario", e);}</p>
     *
     * @param operacion verbo + objeto, en infinitivo: "guardar el usuario"
     */
    public static RuntimeException error(String operacion, SQLException causa) {
        LOG.log(Level.SEVERE, "Error SQL al " + operacion + " (código=" + causa.getErrorCode()
                + ", sqlState=" + causa.getSQLState() + ")", causa);

        int codigo = causa.getErrorCode();
        if (codigo == ERROR_CLAVE_DUPLICADA_CONSTRAINT || codigo == ERROR_CLAVE_DUPLICADA_INDICE) {
            return new BusinessException(
                    "No se pudo " + operacion + ": ya existe un registro con esos datos únicos.", causa);
        }
        if (codigo == ERROR_RESTRICCION) {
            return new BusinessException(
                    "No se pudo " + operacion + ": los datos no cumplen las reglas de la base de datos "
                            + "o el registro está relacionado con otra información.", causa);
        }
        return new DataAccessException(
                "No se pudo " + operacion + ". Verifique la conexión con la base de datos e intente de nuevo.",
                causa);
    }

    // ------------------------------------------------------------------
    // Lectura de columnas
    // ------------------------------------------------------------------

    public static LocalDate localDate(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDate.class);
    }

    public static LocalDateTime localDateTime(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDateTime.class);
    }

    /** Lee un INT que puede ser NULL (getInt devolvería 0 y perdería el NULL). */
    public static Integer integerNullable(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    // ------------------------------------------------------------------
    // Escritura de parámetros
    // ------------------------------------------------------------------

    /** Asigna un INT que puede ser null (en ese caso envía NULL a la base). */
    public static void setIntegerNullable(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }

    /** Asigna un VARBINARY que puede ser null (fotos, archivos). */
    public static void setBytesNullable(PreparedStatement ps, int indice, byte[] valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.VARBINARY);
        } else {
            ps.setBytes(indice, valor);
        }
    }

    /**
     * Escapa los comodines de LIKE de SQL Server (% _ [) para que el texto que
     * escribe el usuario en un buscador se interprete literalmente.
     */
    public static String escaparLike(String texto) {
        return texto.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }
}
