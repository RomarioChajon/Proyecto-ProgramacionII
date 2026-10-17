package umg.sigelab.view;

import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/**
 * Controla la ventana principal de la aplicación y decide qué pantalla mostrar según el rol.
 *
 * <p>Hay UNA sola ventana ({@link JFrame}) y se le cambia el contenido: login -> panel del rol ->
 * login otra vez al cerrar sesión. Así ninguna pantalla necesita saber cómo se abre la siguiente.</p>
 */
public final class Navegador {

    private static JFrame ventana;
    private static LoginForm loginActual;

    private Navegador() {
        // Clase de utilidades: no se instancia.
    }

    /** Muestra la pantalla de acceso. Es lo primero que ve el usuario al abrir SIGELAB. */
    public static void mostrarLogin() {
        Sesion.cerrar();
        if (loginActual != null) {
            loginActual.cerrar(); // detiene su temporizador antes de crear otro login
        }
        loginActual = new LoginForm(Navegador::entrar, () -> ServicioFacialForm.abrir(ventana == null ? null : ventana.getContentPane()));
        mostrar("SIGELAB - Acceso", loginActual.getPanelPrincipal(), new Dimension(520, 640));
    }

    /** Abre la pantalla que corresponde al rol del usuario que acaba de ingresar. */
    public static void entrar(Usuario usuario) {
        Sesion.iniciar(usuario);
        switch (usuario.getRol()) {
            case ADMIN -> mostrar("SIGELAB - Panel principal",
                    new MenuPrincipalForm(usuario, Navegador::mostrarLogin).getPanelPrincipal(), new Dimension(900, 700));
            case ENCARGADO -> mostrar("SIGELAB - Encargado de laboratorio",
                    new PanelOperacionesForm(usuario, Navegador::mostrarLogin, "Cerrar sesión",
                            PanelOperacionesForm.ORDENES).getPanelPrincipal(), new Dimension(1100, 680));
            case DOCENTE, ESTUDIANTE -> mostrar("SIGELAB - Préstamo de equipos",
                    new SolicitudPrestamoForm(usuario, Navegador::mostrarLogin).getPanelPrincipal(), new Dimension(1100, 680));
        }
    }

    private static void mostrar(String titulo, JPanel panel, Dimension minimo) {
        if (ventana == null) {
            ventana = new JFrame();
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }
        ventana.setTitle(titulo);
        ventana.setContentPane(panel);
        ventana.setMinimumSize(minimo);
        ventana.pack();
        ventana.setSize(Math.max(ventana.getWidth(), minimo.width), Math.max(ventana.getHeight(), minimo.height));
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
        ventana.revalidate();
        ventana.repaint();
    }
}
