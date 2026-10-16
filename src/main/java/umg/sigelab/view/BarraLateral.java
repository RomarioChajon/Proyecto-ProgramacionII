package umg.sigelab.view;

import umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Barra lateral izquierda de las pantallas de docente, estudiante y encargado:
 * SIEMPRE muestra quién tiene la sesión (nombre, usuario e ID) y los botones de navegación.
 */
final class BarraLateral {

    static final int ANCHO = 230;

    private BarraLateral() {
        // Clase de utilidades: no se instancia.
    }

    static JPanel crear(Usuario usuario, String detalleExtra, JComponent... elementos) {
        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));
        barra.setBackground(new Color(0x1F3A5F));
        barra.setPreferredSize(new Dimension(ANCHO, 400));

        barra.add(etiqueta("SIGELAB", 20, true));
        barra.add(Box.createVerticalStrut(14));
        barra.add(etiqueta(usuario.getNombreCompleto(), 14, true));
        barra.add(etiqueta("Usuario: " + usuario.getNombreUsuario(), 12, false));
        barra.add(etiqueta("ID: " + usuario.getIdUsuario(), 12, false));
        barra.add(etiqueta("Rol: " + usuario.getRol().getEtiqueta(), 12, false));
        if (detalleExtra != null && !detalleExtra.isBlank()) {
            barra.add(etiqueta(detalleExtra, 12, false));
        }
        barra.add(Box.createVerticalStrut(20));

        for (JComponent elemento : elementos) {
            if (elemento == null) {
                barra.add(Box.createVerticalGlue());
                continue;
            }
            elemento.setAlignmentX(0f);
            elemento.setMaximumSize(new Dimension(Integer.MAX_VALUE, elemento instanceof JButton ? 36 : 60));
            barra.add(elemento);
            barra.add(Box.createVerticalStrut(8));
        }
        return barra;
    }

    private static JLabel etiqueta(String texto, int tamano, boolean negrita) {
        JLabel l = new JLabel("<html>" + texto.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(negrita ? Font.BOLD : Font.PLAIN, (float) tamano));
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }
}
