package umg.sigelab.view;

import umg.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

/**
 * Pantalla de bienvenida para ESTUDIANTE y DOCENTE. La vista real (solicitar préstamos, etc.)
 * la desarrollará otro integrante del equipo; basta con reemplazar el contenido de esta clase y su .form.
 */
public class InicioEstudianteForm {

    // --- Componentes enlazados con InicioEstudianteForm.form ---
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblMensaje;
    private JLabel lblDetalle;
    private JButton btnCerrarSesion;

    public InicioEstudianteForm(Usuario usuario, Runnable alCerrarSesion) {
        configurar(usuario, alCerrarSesion);
    }

    /**
     * Escribe los textos y conecta el botón (separado del constructor para poder probarlo).
     */
    private void configurar(Usuario usuario, Runnable alCerrarSesion) {
        // Estudiante y docente comparten esta pantalla de bienvenida (por ahora).
        lblMensaje.setText("Bienvenido " + usuario.getNombreCompleto() + " (" + usuario.getNombreUsuario() + ")");
        lblDetalle.setText("<html><center>Rol: " + usuario.getRol().getEtiqueta()
                + "<br><br>Esta pantalla será desarrollada por otro integrante del equipo.</center></html>");
        btnCerrarSesion.addActionListener(e -> alCerrarSesion.run());
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

}
