package umg.sigelab.config;

import umg.sigelab.util.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto ÚNICO de acceso a la conexión con SQL Server.

 */
public final class DatabaseConnection {

    private static final String ARCHIVO = "/db.properties";
    private static final String URL_POR_DEFECTO =
            "jdbc:sqlserver://localhost:1433;databaseName=SIGELAB;encrypt=true;trustServerCertificate=true;";
    private static final String USUARIO_POR_DEFECTO = "sa";

    /** Contenido de db.properties, leído una sola vez. */
    private static Properties archivo;

    private DatabaseConnection() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Abre una conexión NUEVA. Quien la pida debe cerrarla (try-with-resources).
     *
     * @throws SQLException      si SQL Server rechaza la conexión
     * @throws DataAccessException si falta configurar la contraseña
     */
    public static Connection getConnection() throws SQLException {
        String url = valor("sigelab.db.url", "SIGELAB_DB_URL", "db.url", URL_POR_DEFECTO);
        String usuario = valor("sigelab.db.user", "SIGELAB_DB_USER", "db.user", USUARIO_POR_DEFECTO);
        String password = valor("sigelab.db.password", "SIGELAB_DB_PASSWORD", "db.password", "Dev2025!");

        if (password == null) {
            throw new DataAccessException(
                    "Falta configurar la contraseña de la base de datos. Copie db.properties.example a "
                            + "db.properties (en src/main/resources) y complete db.password.");
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    /**
     * Comprueba que se puede conectar. Se llama una vez al arrancar la aplicación
     * para avisar al usuario con un mensaje claro en lugar de fallar más adelante.
     *
     * @throws DataAccessException con mensaje entendible si no hay conexión
     */
    public static void verificar() {
        try (Connection c = getConnection()) {
            // isValid confirma que la conexión responde de verdad.
            if (!c.isValid(5)) {
                throw new SQLException("La conexión no respondió.");
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "No se pudo conectar con SQL Server. Verifique que el servidor esté encendido y que "
                            + "los datos de db.properties sean correctos.", e);
        }
    }

    // ------------------------------------------------------------------

    /** Busca un valor en propiedad JVM -> variable de entorno -> db.properties -> valor por defecto. */
    private static String valor(String propiedadJvm, String variableEntorno, String claveArchivo, String porDefecto) {
        String encontrado = System.getProperty(propiedadJvm);
        if (esVacio(encontrado)) {
            encontrado = System.getenv(variableEntorno);
        }
        if (esVacio(encontrado)) {
            encontrado = propiedadesDeArchivo().getProperty(claveArchivo);
        }
        return esVacio(encontrado) ? porDefecto : encontrado.trim();
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Carga db.properties desde el classpath la primera vez; si no existe devuelve un conjunto vacío. */
    private static synchronized Properties propiedadesDeArchivo() {
        if (archivo == null) {
            Properties cargadas = new Properties();
            try (InputStream entrada = DatabaseConnection.class.getResourceAsStream(ARCHIVO)) {
                if (entrada != null) {
                    cargadas.load(entrada);
                }
            } catch (IOException e) {
                throw new DataAccessException("No se pudo leer el archivo db.properties.", e);
            }
            archivo = cargadas;
        }
        return archivo;
    }
}
