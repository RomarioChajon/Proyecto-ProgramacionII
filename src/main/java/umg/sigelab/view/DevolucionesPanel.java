package umg.sigelab.view;

import umg.sigelab.controller.PrestamoController;
import umg.sigelab.enums.CondicionDevolucion;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Devoluciones": el encargado recibe equipos de un préstamo activo, de forma PARCIAL (algunos equipos)
 * o TOTAL (todo lo pendiente), indicando la condición en que regresan (buen estado o dañado).
 * También puede identificar el equipo leyendo su QR.
 */
class DevolucionesPanel extends PanelSede {

    private final PrestamoController controller = new PrestamoController();
    private final JTable tblPrestamos = Tablas.crear("N.º", "Solicitante", "Usuario", "Entrega", "Devolución esperada",
            "Equipos", "Pendientes", "Estado");
    private final JTable tblLineas = Tablas.crear("Código", "Equipo", "Marca", "Modelo", "Categoría", "Estado actual",
            "Devolución");
    private final JComboBox<CondicionDevolucion> cmbCondicion = new JComboBox<>(CondicionDevolucion.values());
    private final JTextField txtObservacion = new JTextField(26);
    private List<PrestamoResumen> prestamos = new ArrayList<>();
    private List<LineaPrestamo> lineas = new ArrayList<>();
    private final QrCampo qr;
    private Integer detalleASeleccionar;

    DevolucionesPanel(Usuario operador, Supplier<Laboratorio> sede) {
        super(operador, sede);
        qr = new QrCampo("Equipo a devolver (QR / código):", this::buscarPorCodigo);
        tblLineas.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Tablas.estilizarCombo(cmbCondicion);
        Tablas.estilizarCampo(txtObservacion);

        JLabel titulo = Tablas.titulo("Recibir devoluciones");
        JButton btnActualizar = new JButton("Actualizar");
        Tablas.estilizarBotonPrimario(btnActualizar);
        JPanel linea1 = new JPanel(new BorderLayout());
        linea1.setBackground(Tablas.BLANCO_PURO);
        linea1.add(titulo, BorderLayout.WEST);
        linea1.add(btnActualizar, BorderLayout.EAST);
        JPanel norte = new JPanel(new GridLayout(2, 1, 0, 2));
        Tablas.estilizarTarjeta(norte);
        norte.add(linea1);
        norte.add(qr);
        add(norte, BorderLayout.NORTH);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setBackground(Tablas.BLANCO_HIELO);
        abajo.add(Tablas.etiqueta("Equipos del préstamo (seleccione uno o varios para devolver)"), BorderLayout.NORTH);
        abajo.add(Tablas.conScroll(tblLineas), BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tablas.conScroll(tblPrestamos), abajo);
        division.setResizeWeight(0.45);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setBackground(Tablas.BLANCO_HIELO);
        add(division, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tablas.estilizarTarjeta(acciones);
        acciones.add(Tablas.etiqueta("Condición:"));
        acciones.add(cmbCondicion);
        acciones.add(Tablas.etiqueta("Observación:"));
        acciones.add(txtObservacion);
        JButton btnParcial = new JButton("Devolver seleccionados");
        JButton btnTotal = new JButton("Devolver todo lo pendiente");
        Tablas.estilizarBotonSecundario(btnParcial);
        Tablas.estilizarBotonPrimario(btnTotal);
        acciones.add(btnParcial);
        acciones.add(btnTotal);
        add(acciones, BorderLayout.SOUTH);

        btnActualizar.addActionListener(e -> refrescar());
        btnParcial.addActionListener(e -> devolver(false));
        btnTotal.addActionListener(e -> devolver(true));
        tblPrestamos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarLineas();
            }
        });
    }

    @Override
    void refrescar() {
        DefaultTableModel modelo = Tablas.modelo(tblPrestamos);
        modelo.setRowCount(0);
        Tablas.modelo(tblLineas).setRowCount(0);
        prestamos = new ArrayList<>();
        lineas = new ArrayList<>();
        Laboratorio sede = sede();
        if (sede == null) {
            return;
        }
        try {
            prestamos = controller.prestamosActivos(operador, sede.getIdLaboratorio());
            for (PrestamoResumen p : prestamos) {
                modelo.addRow(new Object[]{p.idPrestamo(), p.solicitante(), p.usuario(), Tablas.fecha(p.fechaDespacho()),
                        Tablas.fecha(p.fechaEsperada()), p.total(), p.pendientes(), p.estadoVisible()});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Devoluciones", ex);
        }
    }

    private void cargarLineas() {
        DefaultTableModel modelo = Tablas.modelo(tblLineas);
        modelo.setRowCount(0);
        lineas = new ArrayList<>();
        int fila = Tablas.filaModelo(tblPrestamos);
        if (fila < 0 || fila >= prestamos.size()) {
            return;
        }
        try {
            lineas = controller.lineasOperador(operador, prestamos.get(fila).idPrestamo());
            for (LineaPrestamo l : lineas) {
                modelo.addRow(new Object[]{l.codigoInventario(), l.producto(), Tablas.texto(l.marca()),
                        Tablas.texto(l.modelo()), l.categoria(), l.estadoEquipo().getEtiqueta(),
                        l.devuelto() ? "Devuelto (" + l.condicion().getEtiqueta() + ")" : "Pendiente"});
            }
            if (detalleASeleccionar != null) {
                for (int i = 0; i < lineas.size(); i++) {
                    if (lineas.get(i).idDetalle() == detalleASeleccionar) {
                        int vista = tblLineas.convertRowIndexToView(i);
                        tblLineas.getSelectionModel().setSelectionInterval(vista, vista);
                    }
                }
                detalleASeleccionar = null;
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Devoluciones", ex);
        }
    }

    /** Lectura de QR: localiza el préstamo donde el equipo está pendiente y lo deja seleccionado. */
    private void buscarPorCodigo(String codigo) {
        Laboratorio sede = sedeObligatoria();
        if (sede == null) {
            return;
        }
        try {
            int[] encontrado = controller.buscarPendientePorCodigo(operador, sede.getIdLaboratorio(), codigo);
            if (prestamos.stream().noneMatch(p -> p.idPrestamo() == encontrado[1])) {
                refrescar();
            }
            for (int i = 0; i < prestamos.size(); i++) {
                if (prestamos.get(i).idPrestamo() == encontrado[1]) {
                    detalleASeleccionar = encontrado[0];
                    int vista = tblPrestamos.convertRowIndexToView(i);
                    if (tblPrestamos.getSelectedRow() == vista) {
                        cargarLineas();
                    } else {
                        tblPrestamos.getSelectionModel().setSelectionInterval(vista, vista);
                    }
                    return;
                }
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Devoluciones", ex);
        }
    }

    private void devolver(boolean todo) {
        int filaPrestamo = Tablas.filaModelo(tblPrestamos);
        if (filaPrestamo < 0 || lineas.isEmpty()) {
            Mensajes.advertir(this, "Devoluciones", "Seleccione un préstamo.");
            return;
        }
        List<Integer> detalles = new ArrayList<>();
        if (todo) {
            for (LineaPrestamo l : lineas) {
                if (!l.devuelto()) {
                    detalles.add(l.idDetalle());
                }
            }
        } else {
            for (int vista : tblLineas.getSelectedRows()) {
                LineaPrestamo l = lineas.get(tblLineas.convertRowIndexToModel(vista));
                if (l.devuelto()) {
                    Mensajes.advertir(this, "Devoluciones", "El equipo " + l.codigoInventario()
                            + " ya fue devuelto; no puede devolverse dos veces.");
                    return;
                }
                detalles.add(l.idDetalle());
            }
        }
        if (detalles.isEmpty()) {
            Mensajes.advertir(this, "Devoluciones", todo ? "No hay equipos pendientes en este préstamo."
                    : "Seleccione al menos un equipo pendiente.");
            return;
        }
        CondicionDevolucion condicion = (CondicionDevolucion) cmbCondicion.getSelectedItem();
        boolean acepta = Mensajes.confirmar(this, "Registrar devolución", "¿Registrar la devolución de "
                + detalles.size() + " equipo(s) en condición \"" + condicion.getEtiqueta() + "\"?");
        if (!acepta) {
            return;
        }
        try {
            String mensaje = controller.devolver(operador, detalles, condicion, txtObservacion.getText());
            Mensajes.informar(this, "Devolución", mensaje);
            txtObservacion.setText("");
            int idPrestamo = prestamos.get(filaPrestamo).idPrestamo();
            refrescar();
            for (int i = 0; i < prestamos.size(); i++) {
                if (prestamos.get(i).idPrestamo() == idPrestamo) {
                    int vista = tblPrestamos.convertRowIndexToView(i);
                    tblPrestamos.getSelectionModel().setSelectionInterval(vista, vista);
                }
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Registrar devolución", ex);
            refrescar();
        }
    }
}
