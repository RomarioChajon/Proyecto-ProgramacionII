package umg.sigelab.view;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Utilidades de tablas (JTable) y formato de fechas compartidas por las pantallas de préstamos. */
final class Tablas {

    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Tablas() {
        // Clase de utilidades: no se instancia.
    }

    /** Tabla de solo lectura con encabezados claros. */
    static JTable crear(String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        return tabla;
    }

    static DefaultTableModel modelo(JTable tabla) {
        return (DefaultTableModel) tabla.getModel();
    }

    static JScrollPane conScroll(JTable tabla) {
        return new JScrollPane(tabla);
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
}
