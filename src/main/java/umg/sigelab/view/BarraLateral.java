package umg.sigelab.view;

import umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Barra lateral izquierda de las pantallas de docente, estudiante y encargado:
 * SIEMPRE muestra quién tiene la sesión (nombre, usuario e ID) y los botones de navegación.
 * Paleta Celeste y Blanco (SIGELAB).
 */
final class BarraLateral {

    static final int ANCHO = 240;

    private static final Color CELESTE_PRINCIPAL = new Color(0x2B8FD6);
    private static final Color CELESTE_MEDIO = new Color(0x63B3ED);
    private static final Color CELESTE_SUAVE = new Color(0xBEE3F8);
    private static final Color BLANCO_HIELO = new Color(0xF0F8FF);
    private static final Color BLANCO_PURO = new Color(0xFFFFFF);
    private static final Color TEXTO_OSCURO = new Color(0x1A202C);

    private BarraLateral() {
        // Clase de utilidades: no se instancia.
    }

    static JPanel crear(Usuario usuario, String detalleExtra, JComponent... elementos) {
        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 2, CELESTE_MEDIO),
                BorderFactory.createEmptyBorder(18, 14, 18, 14)
        ));
        barra.setBackground(CELESTE_PRINCIPAL);
        barra.setPreferredSize(new Dimension(ANCHO, 420));

        barra.add(etiqueta("SIGELAB", 22, true, BLANCO_PURO));
        barra.add(etiqueta("Gestión de Laboratorios", 11, false, CELESTE_SUAVE));
        barra.add(Box.createVerticalStrut(12));

        JPanel tarjetaSesion = new JPanel();
        tarjetaSesion.setLayout(new BoxLayout(tarjetaSesion, BoxLayout.Y_AXIS));
        tarjetaSesion.setBackground( new Color(0x2378B5));
        tarjetaSesion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        tarjetaSesion.setAlignmentX(0f);
        tarjetaSesion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        tarjetaSesion.add(etiqueta(usuario.getNombreCompleto(), 13, true, BLANCO_PURO));
        tarjetaSesion.add(etiqueta("Usuario: " + usuario.getNombreUsuario(), 11, false, BLANCO_HIELO));
        tarjetaSesion.add(etiqueta("ID: " + usuario.getIdUsuario(), 11, false, BLANCO_HIELO));
        tarjetaSesion.add(etiqueta("Rol: " + usuario.getRol().getEtiqueta(), 11, true, CELESTE_SUAVE));
        if (detalleExtra != null && !detalleExtra.isBlank()) {
            tarjetaSesion.add(etiqueta(detalleExtra, 11, false, BLANCO_HIELO));
        }
        barra.add(tarjetaSesion);
        barra.add(Box.createVerticalStrut(16));

        for (JComponent elemento : elementos) {
            if (elemento == null) {
                barra.add(Box.createVerticalGlue());
                continue;
            }
            if (elemento instanceof JButton boton) {
                estilizarBotonNavegacion(boton);
            }
            elemento.setAlignmentX(0f);
            elemento.setMaximumSize(new Dimension(Integer.MAX_VALUE, elemento instanceof JButton ? 38 : 64));
            barra.add(elemento);
            barra.add(Box.createVerticalStrut(8));
        }
        return barra;
    }

    private static void estilizarBotonNavegacion(JButton boton) {
        String texto = boton.getText() != null ? boton.getText().toLowerCase() : "";
        boolean esCerrar = texto.contains("cerrar") || texto.contains("volver") || texto.contains("salir");
        Color fondoBase = esCerrar ? CELESTE_SUAVE : BLANCO_PURO;
        Color fondoHover = esCerrar ? BLANCO_HIELO : CELESTE_SUAVE;

        boton.setUI(new BasicButtonUI());
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(fondoBase);
        boton.setForeground(TEXTO_OSCURO);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(fondoHover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondoBase);
            }
        });
    }

    private static JLabel etiqueta(String texto, int tamano, boolean negrita, Color color) {
        JLabel l = new JLabel("<html>" + texto.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        l.setForeground(color);
        l.setFont(new Font("Segoe UI", negrita ? Font.BOLD : Font.PLAIN, tamano));
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }
}
