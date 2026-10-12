package umg.sigelab;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.view.Mensajes;
import umg.sigelab.view.Navegador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Punto de entrada de SIGELAB.
 *
 * Comprueba la conexión con SQL Server y abre el login.
 * Desde el login, Navegador dirige a cada usuario
 * a la pantalla correspondiente a su rol.
 */
public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            usarAparienciaDelSistema();

            // Comprobar la conexión antes de abrir el login.
            try (Connection conexion = DatabaseConnection.getConnection()) {

                if (!conexion.isValid(5)) {
                    throw new SQLException(
                            "La conexión con SQL Server no es válida."
                    );
                }

                System.out.println(
                        "SIGELAB: conexión con SQL Server establecida correctamente."
                );

            } catch (SQLException e) {

                System.err.println(
                        "SIGELAB: error de conexión con SQL Server: "
                                + e.getMessage()
                );

                Mensajes.error(
                        null,
                        "SIGELAB",
                        "No se pudo conectar con SQL Server.\n\n"
                                + "Verifique que:\n"
                                + "- El contenedor sqlserver esté encendido.\n"
                                + "- SQL Server esté disponible en localhost:1433.\n"
                                + "- La base de datos SIGELAB_DB exista.\n"
                                + "- El usuario y la contraseña sean correctos.\n\n"
                                + "Detalle: " + e.getMessage()
                );

                return;
            }

            // Abrir el login únicamente si la conexión funciona.
            Navegador.mostrarLogin();

        });
    }

    /**
     * Aplica la apariencia nativa del sistema operativo.
     */
    private static void usarAparienciaDelSistema() {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );

        } catch (Exception e) {
            // Si falla, Swing utiliza su apariencia predeterminada.
        }
    }
}
