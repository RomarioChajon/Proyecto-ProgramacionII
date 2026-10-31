package umg.sigelab.view;

import umg.sigelab.model.TablaReporte;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Utilidades de tablas (JTable), formato de fechas y estilos Celeste y Blanco compartidos por las pantallas de préstamos y reportes. */
final class Tablas {

    static final Color CELESTE_PRINCIPAL = new Color(0x2B8FD6);
    static final Color CELESTE_MEDIO = new Color(0x63B3ED);
    static final Color CELESTE_SUAVE = new Color(0xBEE3F8);
    static final Color BLANCO_HIELO = new Color(0xF0F8FF);
    static final Color BLANCO_PURO = new Color(0xFFFFFF);
    static final Color TEXTO_OSCURO = new Color(0x1A202C);

    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Tablas() {
        // Clase de utilidades: no se instancia.
    }

    /** Tabla de solo lectura con encabezados celestes y selección suave. */
    static JTable crear(String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setBackground(BLANCO_PURO);
        tabla.setForeground(TEXTO_OSCURO);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setGridColor(CELESTE_SUAVE);
        tabla.setSelectionBackground(CELESTE_SUAVE);
        tabla.setSelectionForeground(TEXTO_OSCURO);
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(CELESTE_PRINCIPAL);
                lbl.setForeground(BLANCO_PURO);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, CELESTE_MEDIO),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        });
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        return tabla;
    }

    /** Muestra un resultado de reporte: cambia las columnas de la tabla y carga todas sus filas. */
    static void cargar(JTable tabla, TablaReporte datos) {
        DefaultTableModel modelo = modelo(tabla);
        modelo.setColumnIdentifiers(datos.columnas());
        modelo.setRowCount(0);
        for (Object[] fila : datos.filas()) {
            modelo.addRow(fila);
        }
    }

    static DefaultTableModel modelo(JTable tabla) {
        return (DefaultTableModel) tabla.getModel();
    }

    static JScrollPane conScroll(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(BLANCO_PURO);
        scroll.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
        return scroll;
    }

    /** Índice de la fila seleccionada en el MODELO (o -1), aunque la tabla esté ordenada. */
    static int filaModelo(JTable tabla) {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? -1 : tabla.convertRowIndexToModel(fila);
    }

    static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }

    static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        l.setForeground(CELESTE_PRINCIPAL);
        return l;
    }

    static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXTO_OSCURO);
        return l;
    }

    static void estilizarBotonPrimario(JButton boton) {
        estilizarBoton(boton, CELESTE_PRINCIPAL, BLANCO_PURO, CELESTE_MEDIO, BLANCO_PURO);
    }

    static void estilizarBotonSecundario(JButton boton) {
        estilizarBoton(boton, CELESTE_MEDIO, TEXTO_OSCURO, CELESTE_SUAVE, TEXTO_OSCURO);
    }

    static void estilizarBotonSuave(JButton boton) {
        estilizarBoton(boton, CELESTE_SUAVE, TEXTO_OSCURO, BLANCO_PURO, TEXTO_OSCURO);
    }

    private static void estilizarBoton(JButton boton, Color fondo, Color texto, Color hoverFondo, Color hoverTexto) {
        if (boton == null) {
            return;
        }
        boton.setUI(new BasicButtonUI());
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_PRINCIPAL, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(hoverFondo);
                    boton.setForeground(hoverTexto);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
                boton.setForeground(texto);
            }
        });
    }

    static void estilizarCampo(JTextField campo) {
        if (campo == null) {
            return;
        }
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setForeground(TEXTO_OSCURO);
        campo.setBackground(BLANCO_PURO);
        campo.setCaretColor(CELESTE_PRINCIPAL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    static void estilizarCombo(JComboBox<?> combo) {
        if (combo == null) {
            return;
        }
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXTO_OSCURO);
        combo.setBackground(BLANCO_PURO);
        combo.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
    }

    static void estilizarTarjeta(JPanel panel) {
        panel.setBackground(BLANCO_PURO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_SUAVE, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }
}
