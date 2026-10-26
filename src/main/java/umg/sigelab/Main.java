package umg.sigelab;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.view.Mensajes;
import umg.sigelab.view.Navegador;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;



/**
 * Punto de entrada de SIGELAB: comprueba la conexión con la base de datos y abre el login.
 * Desde ahí el {@link Navegador} lleva a cada rol a su pantalla.
 */
public class Main {

    private static Process proceso;

    public static void main(String[] args) throws IOException, InterruptedException{

       String ruta = System.getProperty("user.dir")+ File.separator+"facial-service";
        File script = new File(ruta, "start-facial.ps1");

        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-File", script.getAbsolutePath()
        );

        pb.directory(new File(ruta));
        pb.inheritIO();

         proceso = pb.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            detenerServicioFacial();
        }));

        //int codigo = proceso.waitFor();

        //System.out.println("Código de salida: " + codigo);
















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
    private static void detenerServicioFacial() {
        if (proceso == null || !proceso.isAlive()) {
            return;
        }

        try {
            Process detener = new ProcessBuilder(
                    "taskkill",
                    "/PID", String.valueOf(proceso.pid()),
                    "/T",
                    "/F"
            ).inheritIO().start();

            int codigo = detener.waitFor();

            System.out.println("Resultado al detener el servicio: " + codigo);

        } catch (IOException e) {
            System.err.println("Error al detener el servicio facial: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Se interrumpió el cierre del servicio facial.");
        }
    }





    }






