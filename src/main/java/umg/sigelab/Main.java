package umg.sigelab;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.view.Mensajes;
import umg.sigelab.view.Navegador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de SIGELAB: comprueba la conexión con la base de datos y abre el login.
 * Desde ahí el {@link Navegador} lleva a cada rol a su pantalla.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            usarAparienciaDelSistema();

            // Si no hay conexión con la base de datos, avisamos con un mensaje claro y salimos.
            try {
                DatabaseConnection.verificar();
            } catch (RuntimeException e) {
                Mensajes.error(null, "SIGELAB", e.getMessage());
                System.exit(1);
            }


            Navegador.mostrarLogin();
        });
    }

    /** Usa el aspecto nativo de Windows/macOS/Linux en lugar del Swing genérico. */
    private static void usarAparienciaDelSistema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si falla, Swing usa su apariencia por defecto; no es grave.
        }
    }
}
