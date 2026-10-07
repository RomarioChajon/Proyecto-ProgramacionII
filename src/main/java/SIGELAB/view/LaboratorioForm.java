package SIGELAB.view;

import SIGELAB.controller.LaboratorioController;
import SIGELAB.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
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

    public LaboratorioForm() {

        controller = new LaboratorioController();

        crearInterfaz();
        configurarFormulario();
        configurarTabla();
        configurarEventos();
        cargarLaboratorios();
    }

    /*
     * Crea todos los componentes Swing.
     * De esta manera no dependemos del archivo .form
     * para inicializar los componentes.
     */
    private void crearInterfaz() {

        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel lblTitulo =
                new JLabel("Gestión de Laboratorios", SwingConstants.CENTER);

        lblTitulo.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // -------------------------------------------------
        // CAMPOS
        // -------------------------------------------------

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

        // -------------------------------------------------
        // BOTONES
        // -------------------------------------------------

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

        // -------------------------------------------------
        // TABLA
        // -------------------------------------------------

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

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtFechaCreacion.setEditable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

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
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(
                e -> nuevo()
        );

        btnGuardar.addActionListener(
                e -> guardar()
        );

        btnActualizar.addActionListener(
                e -> actualizar()
        );

        btnDesactivar.addActionListener(
                e -> desactivar()
        );

        btnBuscar.addActionListener(
                e -> buscar()
        );

        salirButton.addActionListener(
                e -> salir()
        );

        tblLaborario
                .getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        cargarLaboratorioSeleccionado();
                    }
                });
    }

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

    private void cargarTabla(
            List<Laboratorio> laboratorios) {

        DefaultTableModel modelo =
                (DefaultTableModel)
                        tblLaborario.getModel();

        modelo.setRowCount(0);

        for (Laboratorio laboratorio : laboratorios) {

            String fecha = "";

            if (laboratorio.getFechaCreacion() != null) {

                fecha = laboratorio
                        .getFechaCreacion()
                        .format(formatoFecha);
            }

            modelo.addRow(
                    new Object[]{
                            laboratorio.getIdLaboratorio(),
                            laboratorio.getCodigo(),
                            laboratorio.getNombre(),
                            laboratorio.getUbicacion(),
                            laboratorio.getCapacidad(),
                            laboratorio.getEstado(),
                            fecha
                    }
            );
        }
    }

    private void nuevo() {

        limpiarCampos();

        txtCodigo.requestFocus();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            int capacidad =
                    Integer.parseInt(
                            txtCapacidad
                                    .getText()
                                    .trim()
                    );

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            Laboratorio laboratorio =
                    controller.guardar(
                            txtCodigo.getText().trim(),
                            txtNombre.getText().trim(),
                            txtUbicacion.getText().trim(),
                            capacidad,
                            estado
                    );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Laboratorio guardado correctamente.\n"
                            + "ID generado: "
                            + laboratorio.getIdLaboratorio(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarCampos();
            cargarLaboratorios();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

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
                            txtCodigo.getText().trim(),
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

                limpiarCampos();
                cargarLaboratorios();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo actualizar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

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

                limpiarCampos();
                cargarLaboratorios();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo desactivar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

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
                    controller.buscar(
                            busqueda.trim()
                    );

            cargarTabla(resultados);
            limpiarCamposSinTabla();

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

    private void cargarLaboratorioSeleccionado() {

        int fila =
                tblLaborario.getSelectedRow();

        if (fila == -1) {
            return;
        }

        txtId.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 0)
                )
        );

        txtCodigo.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 1)
                )
        );

        txtNombre.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 2)
                )
        );

        txtUbicacion.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 3)
                )
        );

        txtCapacidad.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 4)
                )
        );

        cmbEstado.setSelectedItem(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 5)
                )
        );

        txtFechaCreacion.setText(
                String.valueOf(
                        tblLaborario.getValueAt(fila, 6)
                )
        );

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        String estado =
                String.valueOf(
                        tblLaborario.getValueAt(fila, 5)
                );

        btnDesactivar.setEnabled(
                "ACTIVO".equals(estado)
        );
    }

    private boolean validarCampos() {

        if (txtCodigo.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el código del laboratorio.",
                    txtCodigo
            );

            return false;
        }

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
                            txtCapacidad
                                    .getText()
                                    .trim()
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

    private void mostrarAdvertencia(
            String mensaje,
            JTextField campo) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );

        campo.requestFocus();
    }

    private void mostrarAdvertenciaGeneral(
            String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarError(
            String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void limpiarCampos() {

        limpiarCamposSinTabla();

        tblLaborario.clearSelection();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void limpiarCamposSinTabla() {

        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");
        cmbEstado.setSelectedItem("ACTIVO");
        txtFechaCreacion.setText("");
    }

    private void salir() {

        Window ventana =
                SwingUtilities.getWindowAncestor(
                        panelPrincipal
                );

        if (ventana != null) {
            ventana.dispose();
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}