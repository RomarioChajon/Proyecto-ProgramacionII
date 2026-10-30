package umg.sigelab;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.integration.facial.FacialServiceLauncher;
import umg.sigelab.view.Mensajes;
import umg.sigelab.view.Navegador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de SIGELAB: arranca el servicio facial en segundo plano, comprueba la conexión
 * con la base de datos y abre el login. Desde ahí el {@link Navegador} lleva a cada rol a su pantalla.
 *
 * <p>El servicio facial (cámara para rostro y QR) lo maneja {@link FacialServiceLauncher}:
 * desde IntelliJ usa {@code facial-service/start-facial}, y en la versión empaquetada
 * (SIGELAB.exe) usa el {@code facial-service.exe} incluido. Al cerrar SIGELAB se detiene solo.</p>
 */
public class Main {

    public static void main(String[] args) {
        iniciarServicioFacial();

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

    /**
     * Inicia el servicio facial en un hilo aparte para no retrasar el login: la primera vez
     * carga el modelo y puede tardar. Si falla, SIGELAB abre igual y el error queda en el
     * registro de la ventana "Servicio facial".
     */
    private static void iniciarServicioFacial() {
        Thread hilo = new Thread(() -> {
            try {
                FacialServiceLauncher launcher = FacialServiceLauncher.getInstance();
                if (!launcher.instalado()) {
                    launcher.instalar();
                }
                launcher.iniciar();
            } catch (RuntimeException e) {
                System.err.println("No se pudo iniciar el servicio facial: " + e.getMessage());
            }
        }, "sigelab-facial-inicio");
        hilo.setDaemon(true);
        hilo.start();
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
