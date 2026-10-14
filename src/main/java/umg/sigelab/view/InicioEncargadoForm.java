package umg.sigelab.view;

import umg.sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

/**
 * Pantalla de bienvenida para el ENCARGADO DE LABORATORIO. La vista real (aprobar préstamos, despacho,
 * inventario del laboratorio) la desarrollará otro integrante; el login ya queda listo.
 */
public class InicioEncargadoForm {

    // --- Componentes enlazados con InicioEncargadoForm.form ---
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblMensaje;
    private JLabel lblDetalle;
    private JButton btnCerrarSesion;

    public InicioEncargadoForm(Usuario usuario, Runnable alCerrarSesion) {
        configurar(usuario, alCerrarSesion);
    }

    /**
     * Escribe los textos y conecta el botón (separado del constructor para poder probarlo).
     */
    private void configurar(Usuario usuario, Runnable alCerrarSesion) {
        // Pedido del enunciado: "Bienvenido" + usuario + nombre + id.
        lblMensaje.setText("Bienvenido " + usuario.getNombreUsuario() + " - " + usuario.getNombreCompleto()
                + " - ID " + usuario.getIdUsuario());
        String laboratorio = usuario.getLaboratorio() == null ? "sin laboratorio"
                : usuario.getLaboratorio().getCodigo() + " - " + usuario.getLaboratorio().getNombre();
        lblDetalle.setText("<html><center>Rol: " + usuario.getRol().getEtiqueta() + " | Laboratorio: " + laboratorio
                + "<br><br>Esta pantalla será desarrollada por otro integrante del equipo.</center></html>");
        btnCerrarSesion.addActionListener(e -> alCerrarSesion.run());
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

}
