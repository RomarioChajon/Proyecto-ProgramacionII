package SIGELAB.view;

import SIGELAB.controller.EquipoController;
import SIGELAB.controller.ProductoController;
import SIGELAB.controller.LaboratorioController;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoProducto;
import SIGELAB.enums.EstadoLaboratorio;

import SIGELAB.model.Equipo;
import SIGELAB.model.Producto;
import SIGELAB.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EquipoForm {

    // COMPONENTES DEL GUI DESIGNER
    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigoInventario;
    private JComboBox<Producto> cmbProducto;
    private JTextField txtNumeroSerie;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtEstado;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnBuscar;
    private JButton salirButton;
    private JTable tblEquipo;
    private JTextField txtFechadeRegistro;
    private JTextArea observacionesTextArea;

    // CONTROLADORES
    private final EquipoController equipoController;
    private final ProductoController productoController;
    private final LaboratorioController laboratorioController;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DefaultTableModel modeloTabla;
    private boolean cargandoSeleccion = false;

    // CONSTRUCTOR
    public EquipoForm() {

        equipoController = new EquipoController();
        productoController = new ProductoController();
        laboratorioController = new LaboratorioController();

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarProductos();
        cargarLaboratorios();
        cargarEquipos();

        nuevo();
    }

    // CONFIGURAR FORMULARIO
    private void configurarFormulario() {

        txtId.setEditable(false);
        txtEstado.setEditable(false);
        txtFechadeRegistro.setEditable(false);

        txtEstado.setText(EstadoEquipo.DISPONIBLE.name());
        txtFechadeRegistro.setText("");

        observacionesTextArea.setLineWrap(true);
        observacionesTextArea.setWrapStyleWord(true);
        observacionesTextArea.setRows(3);

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    // CONFIGURAR TABLA
    private void configurarTabla() {

        modeloTabla = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código inventario",
                        "Producto",
                        "Número de serie",
                        "Laboratorio",
                        "Estado",
                        "Observaciones",
                        "Fecha registro"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblEquipo.setModel(modeloTabla);

        tblEquipo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblEquipo.setRowHeight(25);
        tblEquipo.setAutoCreateRowSorter(true);
        tblEquipo.getTableHeader().setReorderingAllowed(false);
    }

    // CONFIGURAR BOTONES
    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());

        btnGuardar.addActionListener(e -> guardar());

        btnActualizar.addActionListener(e -> actualizar());

        btnBuscar.addActionListener(e -> buscar());

        salirButton.addActionListener(e -> salir());

        tblEquipo.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()
                            && !cargandoSeleccion) {

                        seleccionarEquipo();
                    }
                });
    }

    // CARGAR PRODUCTOS ACTIVOS
    private void cargarProductos() {

        try {

            cmbProducto.removeAllItems();

            List<Producto> productos =
                    productoController.listar();

            for (Producto producto : productos) {

                if (producto.getEstado()
                        == EstadoProducto.ACTIVO) {

                    cmbProducto.addItem(producto);
                }
            }

            cmbProducto.setSelectedIndex(-1);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los productos.\n"
                            + ex.getMessage()
            );
        }
    }

    // CARGAR LABORATORIOS ACTIVOS
    private void cargarLaboratorios() {

        try {

            cmbLaboratorio.removeAllItems();

            List<Laboratorio> laboratorios =
                    laboratorioController.listar();

            for (Laboratorio laboratorio : laboratorios) {

                if (laboratorio.getEstado()
                        == EstadoLaboratorio.ACTIVO) {

                    cmbLaboratorio.addItem(laboratorio);
                }
            }

            cmbLaboratorio.setSelectedIndex(-1);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los laboratorios.\n"
                            + ex.getMessage()
            );
        }
    }

    // CARGAR EQUIPOS
    private void cargarEquipos() {

        try {

            List<Equipo> equipos =
                    equipoController.listar();

            cargarTabla(equipos);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los equipos.\n"
                            + ex.getMessage()
            );
        }
    }

    // LLENAR TABLA
    private void cargarTabla(List<Equipo> equipos) {

        cargandoSeleccion = true;

        try {

            modeloTabla.setRowCount(0);

            for (Equipo equipo : equipos) {

                modeloTabla.addRow(
                        new Object[]{
                                equipo.getIdEquipo(),
                                equipo.getCodigoInventario(),
                                equipo.getIdProducto(),
                                equipo.getNumeroSerie(),
                                equipo.getIdLaboratorio(),
                                equipo.getEstado(),
                                equipo.getObservaciones(),
                                formatearFecha(
                                        equipo.getFechaRegistro()
                                )
                        }
                );
            }

        } finally {
            cargandoSeleccion = false;
        }
    }

    // FORMATO DE FECHA
    private String formatearFecha(LocalDateTime fecha) {

        if (fecha == null) {
            return "";
        }

        return fecha.format(FORMATO_FECHA);
    }

    // LIMPIAR FORMULARIO
    private void limpiarCampos() {

        txtId.setText("");
        txtCodigoInventario.setText("");
        txtNumeroSerie.setText("");
        txtEstado.setText(EstadoEquipo.DISPONIBLE.name());
        txtFechadeRegistro.setText("");
        observacionesTextArea.setText("");

        cmbProducto.setEnabled(true);
        cmbLaboratorio.setEnabled(true);

        if (cmbProducto.getItemCount() > 0) {
            cmbProducto.setSelectedIndex(0);
        } else {
            cmbProducto.setSelectedIndex(-1);
        }

        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        } else {
            cmbLaboratorio.setSelectedIndex(-1);
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    // NUEVO EQUIPO
    private void nuevo() {

        cargandoSeleccion = true;

        try {
            tblEquipo.clearSelection();
        } finally {
            cargandoSeleccion = false;
        }

        limpiarCampos();
        txtCodigoInventario.requestFocusInWindow();
    }

    // GUARDAR EQUIPO
    private void guardar() {

        if (!txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Presione Nuevo antes de guardar otro equipo."
            );
            return;
        }

        try {

            Equipo equipo = obtenerDatosFormulario();

            Equipo guardado =
                    equipoController.guardar(equipo);

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Equipo guardado correctamente.\n"
                            + "ID: " + guardado.getIdEquipo()
                            + "\nFecha de registro: "
                            + formatearFecha(
                            guardado.getFechaRegistro()
                    ),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarEquipos();
            nuevo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    // ACTUALIZAR EQUIPO
    private void actualizar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Seleccione un equipo de la tabla."
            );
            return;
        }

        try {

            int idEquipo =
                    Integer.parseInt(txtId.getText().trim());

            Equipo existente =
                    equipoController.buscarPorId(idEquipo)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El equipo seleccionado no existe."
                                    )
                            );

            if (existente.getEstado()
                    != EstadoEquipo.DISPONIBLE) {

                mostrarAdvertencia(
                        "Solo se pueden actualizar equipos DISPONIBLES."
                );
                return;
            }

            Equipo equipo = obtenerDatosFormulario();

            equipo.setIdEquipo(idEquipo);

            // CONSERVAR EL LABORATORIO ORIGINAL
            equipo.setIdLaboratorio(
                    existente.getIdLaboratorio()
            );

            // CONSERVAR EL ESTADO REAL
            equipo.setEstado(
                    existente.getEstado()
            );

            boolean actualizado =
                    equipoController.actualizar(equipo);

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Equipo actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarEquipos();
                nuevo();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    // OBTENER DATOS DEL FORMULARIO
    private Equipo obtenerDatosFormulario() {

        Producto producto =
                (Producto) cmbProducto.getSelectedItem();

        Laboratorio laboratorio =
                (Laboratorio) cmbLaboratorio.getSelectedItem();

        String codigo =
                txtCodigoInventario.getText().trim();

        String serie =
                txtNumeroSerie.getText().trim();

        String observaciones =
                observacionesTextArea.getText().trim();

        if (codigo.isEmpty()) {

            throw new IllegalArgumentException(
                    "Ingrese el código de inventario."
            );
        }

        if (codigo.length() > 30) {

            throw new IllegalArgumentException(
                    "El código no puede superar 30 caracteres."
            );
        }

        if (producto == null) {

            throw new IllegalArgumentException(
                    "Seleccione un producto activo."
            );
        }

        if (serie.isEmpty()) {

            throw new IllegalArgumentException(
                    "Ingrese el número de serie."
            );
        }

        if (serie.length() > 60) {

            throw new IllegalArgumentException(
                    "El número de serie no puede superar 60 caracteres."
            );
        }

        if (laboratorio == null) {

            throw new IllegalArgumentException(
                    "Seleccione un laboratorio activo."
            );
        }

        if (observaciones.length() > 300) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar 300 caracteres."
            );
        }

        return new Equipo(
                codigo,
                producto.getIdProducto(),
                serie,
                laboratorio.getIdLaboratorio(),
                EstadoEquipo.DISPONIBLE,
                observaciones
        );
    }

    // BUSCAR EQUIPOS
    private void buscar() {

        String texto = JOptionPane.showInputDialog(
                panelPrincipal,
                "Ingrese código, número de serie u observación:",
                "Buscar equipo",
                JOptionPane.QUESTION_MESSAGE
        );

        if (texto == null) {
            return;
        }

        try {

            List<Equipo> equipos =
                    equipoController.buscar(texto.trim());

            // LIMPIAR LOS CAMPOS
            nuevo();

            // MOSTRAR LOS RESULTADOS
            cargarTabla(equipos);

            if (equipos.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron equipos.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    // SELECCIONAR EQUIPO
    private void seleccionarEquipo() {

        int fila = tblEquipo.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo =
                tblEquipo.convertRowIndexToModel(fila);

        int idEquipo =
                (int) modeloTabla.getValueAt(filaModelo, 0);

        try {

            Equipo equipo =
                    equipoController.buscarPorId(idEquipo)
                            .orElse(null);

            if (equipo == null) {

                mostrarAdvertencia(
                        "El equipo seleccionado ya no existe."
                );
                return;
            }

            txtId.setText(
                    String.valueOf(equipo.getIdEquipo())
            );

            txtCodigoInventario.setText(
                    equipo.getCodigoInventario()
            );

            txtNumeroSerie.setText(
                    equipo.getNumeroSerie()
            );

            txtEstado.setText(
                    equipo.getEstado().name()
            );

            txtFechadeRegistro.setText(
                    formatearFecha(
                            equipo.getFechaRegistro()
                    )
            );

            observacionesTextArea.setText(
                    equipo.getObservaciones() == null
                            ? ""
                            : equipo.getObservaciones()
            );

            seleccionarProducto(
                    equipo.getIdProducto()
            );

            seleccionarLaboratorio(
                    equipo.getIdLaboratorio()
            );

            // IMPEDIR CAMBIO DE LABORATORIO
            cmbLaboratorio.setEnabled(false);

            boolean disponible =
                    equipo.getEstado()
                            == EstadoEquipo.DISPONIBLE;

            btnGuardar.setEnabled(false);
            btnActualizar.setEnabled(disponible);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo consultar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    // SELECCIONAR PRODUCTO
    private void seleccionarProducto(int idProducto) {

        for (int i = 0; i < cmbProducto.getItemCount(); i++) {

            Producto producto =
                    cmbProducto.getItemAt(i);

            if (producto.getIdProducto() == idProducto) {

                cmbProducto.setSelectedIndex(i);
                return;
            }
        }

        cmbProducto.setSelectedIndex(-1);
    }

    // SELECCIONAR LABORATORIO
    private void seleccionarLaboratorio(int idLaboratorio) {

        for (int i = 0;
             i < cmbLaboratorio.getItemCount();
             i++) {

            Laboratorio laboratorio =
                    cmbLaboratorio.getItemAt(i);

            if (laboratorio.getIdLaboratorio()
                    == idLaboratorio) {

                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }

        cmbLaboratorio.setSelectedIndex(-1);
    }

    // SALIR
    private void salir() {

        Window ventana =
                SwingUtilities.getWindowAncestor(panelPrincipal);

        if (ventana != null) {
            ventana.dispose();
        }
    }

    // MOSTRAR ADVERTENCIA
    private void mostrarAdvertencia(String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );
    }

    // MOSTRAR ERROR
    private void mostrarError(String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // OBTENER PANEL PRINCIPAL
    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}


