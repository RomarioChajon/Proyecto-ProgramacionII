package SIGELAB.view;

import SIGELAB.controller.LaboratorioController;
import SIGELAB.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LaboratorioForm {

    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;
    private JComboBox<String> cmbEstado;
    private JTextField txtFechaCreacion;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton salirButton;
    private JButton btnBuscar;

    private JTable tblLaborario;

    private final LaboratorioController controller;

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private boolean cargandoTabla = false;

    public LaboratorioForm() {

        controller = new LaboratorioController();

        crearInterfaz();
        configurarFormulario();
        configurarTabla();
        configurarEventos();
        cargarLaboratorios();

        nuevo();
    }

    // CREAR INTERFAZ

    private void crearInterfaz() {

        panelPrincipal = new JPanel(new BorderLayout(10, 10));

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel lblTitulo = new JLabel(
                "Gestión de Laboratorios",
                SwingConstants.CENTER
        );

        lblTitulo.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelCampos =
                new JPanel(new GridLayout(7, 2, 10, 10));

        txtId = new JTextField();
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtUbicacion = new JTextField();
        txtCapacidad = new JTextField();

        cmbEstado = new JComboBox<>();

        txtFechaCreacion = new JTextField();

        panelCampos.add(new JLabel("ID:"));
        panelCampos.add(txtId);

        panelCampos.add(new JLabel("Código:"));
        panelCampos.add(txtCodigo);

        panelCampos.add(new JLabel("Nombre:"));
        panelCampos.add(txtNombre);

        panelCampos.add(new JLabel("Ubicación:"));
        panelCampos.add(txtUbicacion);

        panelCampos.add(new JLabel("Capacidad:"));
        panelCampos.add(txtCapacidad);

        panelCampos.add(new JLabel("Estado:"));
        panelCampos.add(cmbEstado);

        panelCampos.add(new JLabel("Fecha de creación:"));
        panelCampos.add(txtFechaCreacion);

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnDesactivar = new JButton("Desactivar");
        btnBuscar = new JButton("Buscar");
        salirButton = new JButton("Salir");

        JPanel panelBotones =
                new JPanel(new FlowLayout(FlowLayout.CENTER));

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnBuscar);
        panelBotones.add(salirButton);

        JPanel panelSuperior =
                new JPanel(new BorderLayout(10, 10));

        panelSuperior.add(panelCampos, BorderLayout.CENTER);
        panelSuperior.add(panelBotones, BorderLayout.SOUTH);

        tblLaborario = new JTable();

        JScrollPane scrollTabla =
                new JScrollPane(tblLaborario);

        scrollTabla.setPreferredSize(
                new Dimension(850, 280)
        );

        JPanel contenido =
                new JPanel(new BorderLayout(10, 10));

        contenido.add(panelSuperior, BorderLayout.NORTH);
        contenido.add(scrollTabla, BorderLayout.CENTER);

        panelPrincipal.add(contenido, BorderLayout.CENTER);
    }

    // FECHA ACTUAL DE GUATEMALA

    private void mostrarFechaActual() {

        LocalDateTime fechaActual =
                LocalDateTime.now(ZONA_GUATEMALA);

        txtFechaCreacion.setText(
                fechaActual.format(formatoFecha)
        );
    }

    // FORMATEAR FECHA

    private String formatearFecha(LocalDateTime fecha) {

        if (fecha == null) {
            return "";
        }

        return fecha.format(formatoFecha);
    }

    // CONFIGURAR FORMULARIO

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtCodigo.setEditable(false);
        txtCodigo.setFocusable(false);
        txtCodigo.setToolTipText("Se genera automáticamente al guardar el laboratorio.");
        txtFechaCreacion.setEditable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        mostrarFechaActual();
    }

    // CONFIGURAR TABLA

    private void configurarTabla() {

        DefaultTableModel modelo =
                new DefaultTableModel(
                        new Object[]{
                                "ID",
                                "Código",
                                "Nombre",
                                "Ubicación",
                                "Capacidad",
                                "Estado",
                                "Fecha creación"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {
                        return false;
                    }
                };

        tblLaborario.setModel(modelo);

        tblLaborario.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblLaborario.setRowHeight(25);
        tblLaborario.setAutoCreateRowSorter(true);

        tblLaborario.getTableHeader()
                .setReorderingAllowed(false);
    }

    // CONFIGURAR EVENTOS

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());

        btnGuardar.addActionListener(e -> guardar());

        btnActualizar.addActionListener(e -> actualizar());

        btnDesactivar.addActionListener(e -> desactivar());

        btnBuscar.addActionListener(e -> buscar());

        salirButton.addActionListener(e -> salir());

        tblLaborario.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()
                            && !cargandoTabla) {

                        cargarLaboratorioSeleccionado();
                    }
                });
    }

    // CARGAR LABORATORIOS

    private void cargarLaboratorios() {

        try {

            List<Laboratorio> laboratorios =
                    controller.listar();

            cargarTabla(laboratorios);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los laboratorios.\n\n"
                            + ex.getMessage()
            );
        }
    }

    // LLENAR TABLA

    private void cargarTabla(List<Laboratorio> laboratorios) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        cargandoTabla = true;

        try {

            modelo.setRowCount(0);

            for (Laboratorio laboratorio : laboratorios) {

                modelo.addRow(
                        new Object[]{
                                laboratorio.getIdLaboratorio(),
                                laboratorio.getCodigo(),
                                laboratorio.getNombre(),
                                laboratorio.getUbicacion(),
                                laboratorio.getCapacidad(),
                                laboratorio.getEstado(),
                                formatearFecha(
                                        laboratorio.getFechaCreacion()
                                )
                        }
                );
            }

            tblLaborario.clearSelection();

        } finally {

            cargandoTabla = false;
        }
    }

    // NUEVO LABORATORIO

    private void nuevo() {

        limpiarCampos();

        mostrarFechaActual();

        txtNombre.requestFocusInWindow();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    // GUARDAR LABORATORIO

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            int capacidad =
                    Integer.parseInt(
                            txtCapacidad.getText().trim()
                    );

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            Laboratorio laboratorio =
                    controller.guardar(
                            null,
                            txtNombre.getText().trim(),
                            txtUbicacion.getText().trim(),
                            capacidad,
                            estado
                    );

            // La fecha definitiva proviene de SQL Server.
            String fechaGuardada =
                    formatearFecha(
                            laboratorio.getFechaCreacion()
                    );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Laboratorio guardado correctamente.\n"
                            + "Código generado: "
                            + laboratorio.getCodigo()
                            + "\nID generado: "
                            + laboratorio.getIdLaboratorio()
                            + "\nFecha de creación: "
                            + (fechaGuardada.isEmpty()
                            ? "No disponible"
                            : fechaGuardada),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarLaboratorios();
            nuevo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    // ACTUALIZAR LABORATORIO

    private void actualizar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertenciaGeneral(
                    "Seleccione un laboratorio de la tabla."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {

            int id =
                    Integer.parseInt(
                            txtId.getText().trim()
                    );

            int capacidad =
                    Integer.parseInt(
                            txtCapacidad.getText().trim()
                    );

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            boolean actualizado =
                    controller.actualizar(
                            id,
                            null,
                            txtNombre.getText().trim(),
                            txtUbicacion.getText().trim(),
                            capacidad,
                            estado
                    );

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Laboratorio actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // La fecha original se conserva en SQL Server.
                cargarLaboratorios();
                nuevo();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo actualizar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    // DESACTIVAR LABORATORIO

    private void desactivar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertenciaGeneral(
                    "Seleccione un laboratorio de la tabla."
            );
            return;
        }

        String estadoActual =
                String.valueOf(
                        cmbEstado.getSelectedItem()
                );

        if ("INACTIVO".equals(estadoActual)) {

            mostrarAdvertenciaGeneral(
                    "El laboratorio ya se encuentra inactivo."
            );
            return;
        }

        int respuesta =
                JOptionPane.showConfirmDialog(
                        panelPrincipal,
                        "¿Desea desactivar este laboratorio?",
                        "Confirmar",
                        JOptionPane.YES_NO_OPTION
                );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int id =
                    Integer.parseInt(
                            txtId.getText().trim()
                    );

            boolean desactivado =
                    controller.desactivar(id);

            if (desactivado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Laboratorio desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarLaboratorios();
                nuevo();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo desactivar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    // BUSCAR LABORATORIOS

    private void buscar() {

        String busqueda =
                JOptionPane.showInputDialog(
                        panelPrincipal,
                        "Ingrese código, nombre o ubicación:",
                        "Buscar laboratorio",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (busqueda == null) {
            return;
        }

        try {

            List<Laboratorio> resultados =
                    controller.buscar(busqueda.trim());

            cargarTabla(resultados);

            limpiarCamposSinTabla();

            btnGuardar.setEnabled(true);
            btnActualizar.setEnabled(false);
            btnDesactivar.setEnabled(false);

            if (resultados.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron laboratorios.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n\n"
                            + ex.getMessage()
            );
        }
    }

    // SELECCIONAR LABORATORIO

    private void cargarLaboratorioSeleccionado() {

        int fila = tblLaborario.getSelectedRow();

        if (fila == -1) {
            return;
        }

        int filaModelo =
                tblLaborario.convertRowIndexToModel(fila);

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        txtId.setText(
                String.valueOf(modelo.getValueAt(filaModelo, 0))
        );

        txtCodigo.setText(
                String.valueOf(modelo.getValueAt(filaModelo, 1))
        );

        txtNombre.setText(
                String.valueOf(modelo.getValueAt(filaModelo, 2))
        );

        txtUbicacion.setText(
                String.valueOf(modelo.getValueAt(filaModelo, 3))
        );

        txtCapacidad.setText(
                String.valueOf(modelo.getValueAt(filaModelo, 4))
        );

        cmbEstado.setSelectedItem(
                String.valueOf(modelo.getValueAt(filaModelo, 5))
        );

        // Mostrar fecha original de creación.
        Object fecha = modelo.getValueAt(filaModelo, 6);

        txtFechaCreacion.setText(
                fecha == null ? "" : fecha.toString()
        );

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        String estado =
                String.valueOf(modelo.getValueAt(filaModelo, 5));

        btnDesactivar.setEnabled(
                "ACTIVO".equals(estado)
        );
    }

    // VALIDAR CAMPOS

    private boolean validarCampos() {

        if (txtNombre.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del laboratorio.",
                    txtNombre
            );
            return false;
        }

        if (txtUbicacion.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese la ubicación del laboratorio.",
                    txtUbicacion
            );
            return false;
        }

        if (txtCapacidad.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese la capacidad del laboratorio.",
                    txtCapacidad
            );
            return false;
        }

        try {

            int capacidad =
                    Integer.parseInt(
                            txtCapacidad.getText().trim()
                    );

            if (capacidad <= 0) {

                mostrarAdvertencia(
                        "La capacidad debe ser mayor que cero.",
                        txtCapacidad
                );
                return false;
            }

        } catch (NumberFormatException ex) {

            mostrarAdvertencia(
                    "La capacidad debe ser un número entero.",
                    txtCapacidad
            );
            return false;
        }

        return true;
    }

    // MOSTRAR ADVERTENCIA

    private void mostrarAdvertencia(
            String mensaje,
            JTextField campo) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );

        campo.requestFocusInWindow();
    }

    // MOSTRAR ADVERTENCIA GENERAL

    private void mostrarAdvertenciaGeneral(String mensaje) {

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

    // LIMPIAR CAMPOS

    private void limpiarCampos() {

        limpiarCamposSinTabla();

        cargandoTabla = true;

        try {

            tblLaborario.clearSelection();

        } finally {

            cargandoTabla = false;
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    // LIMPIAR CAMPOS SIN MODIFICAR TABLA

    private void limpiarCamposSinTabla() {

        txtId.setText("");
        txtCodigo.setText("Automático al guardar");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        // Fecha y hora actuales de Guatemala.
        mostrarFechaActual();
    }

    // SALIR

    private void salir() {

        Window ventana =
                SwingUtilities.getWindowAncestor(panelPrincipal);

        if (ventana != null) {
            ventana.dispose();
        }
    }

    // OBTENER PANEL PRINCIPAL

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}
