package umg.sigelab.view;

import umg.sigelab.controller.LaboratorioAdminController;
import umg.sigelab.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LaboratorioForm {

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;
    private JTextField txtFechaCreacion;

    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnBuscar;
    private JButton salirButton;

    private JTable tblLaborario;

    private final LaboratorioAdminController controller;

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private boolean cargandoTabla = false;

    public LaboratorioForm() {

        controller = new LaboratorioAdminController();

        crearInterfaz();
        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarLaboratorios();
        nuevo();
    }

    private void crearInterfaz() {

        panelPrincipal = new JPanel(new BorderLayout(10, 10));

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titulo = new JLabel(
                "SIGELAB - Gestión de Laboratorios",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        panelPrincipal.add(titulo, BorderLayout.NORTH);

        JPanel panelCampos = new JPanel(
                new GridLayout(7, 2, 10, 10)
        );

        txtId = new JTextField();
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtUbicacion = new JTextField();
        txtCapacidad = new JTextField();
        txtFechaCreacion = new JTextField();

        cmbEstado = new JComboBox<>();

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

        JPanel panelBotones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 8)
        );

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar);
        panelBotones.add(btnBuscar);
        panelBotones.add(salirButton);

        JPanel panelSuperior = new JPanel(
                new BorderLayout(10, 10)
        );

        panelSuperior.add(
                panelCampos,
                BorderLayout.CENTER
        );

        panelSuperior.add(
                panelBotones,
                BorderLayout.SOUTH
        );

        tblLaborario = new JTable();

        JScrollPane scrollTabla =
                new JScrollPane(tblLaborario);

        scrollTabla.setPreferredSize(
                new Dimension(850, 280)
        );

        JPanel contenido = new JPanel(
                new BorderLayout(10, 10)
        );

        contenido.add(
                panelSuperior,
                BorderLayout.NORTH
        );

        contenido.add(
                scrollTabla,
                BorderLayout.CENTER
        );

        panelPrincipal.add(
                contenido,
                BorderLayout.CENTER
        );

        panelPrincipal.setPreferredSize(
                new Dimension(1000, 650)
        );
    }

    private void mostrarFechaActual() {

        LocalDateTime fechaActual =
                LocalDateTime.now(ZONA_GUATEMALA);

        txtFechaCreacion.setText(
                fechaActual.format(FORMATO_FECHA)
        );
    }

    private String formatearFecha(LocalDateTime fecha) {

        if (fecha == null) {
            return "";
        }

        return fecha.format(FORMATO_FECHA);
    }

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtId.setFocusable(false);

        // El código es editable: si se deja vacío al crear, se genera automáticamente.
        txtCodigo.setEditable(true);
        txtCodigo.setFocusable(true);

        txtCodigo.setToolTipText(
                "Puede escribirlo o dejarlo vacío para que se genere automáticamente al guardar."
        );

        txtFechaCreacion.setEditable(false);
        txtFechaCreacion.setFocusable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        mostrarFechaActual();
    }

    private void configurarTabla() {

        DefaultTableModel modelo = new DefaultTableModel(
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
                    int fila,
                    int columna
            ) {
                return false;
            }
        };

        tblLaborario.setModel(modelo);

        tblLaborario.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblLaborario.setRowHeight(25);
        tblLaborario.setAutoCreateRowSorter(true);
        tblLaborario.setFillsViewportHeight(true);

        tblLaborario.getTableHeader()
                .setReorderingAllowed(false);
    }

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

    private void cargarTabla(List<Laboratorio> laboratorios) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        cargandoTabla = true;

        try {

            modelo.setRowCount(0);

            for (Laboratorio laboratorio : laboratorios) {

                modelo.addRow(new Object[]{
                        laboratorio.getIdLaboratorio(),
                        laboratorio.getCodigo(),
                        laboratorio.getNombre(),
                        laboratorio.getUbicacion(),
                        laboratorio.getCapacidad(),
                        laboratorio.getEstado().name(), // texto "ACTIVO"/"INACTIVO": el combo y los botones comparan con ese texto
                        formatearFecha(
                                laboratorio.getFechaCreacion()
                        )
                });
            }

            tblLaborario.clearSelection();

        } finally {

            cargandoTabla = false;
        }
    }

    private void nuevo() {

        limpiarCampos();
        mostrarFechaActual();

        txtNombre.requestFocusInWindow();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            int capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim()
            );

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            Laboratorio laboratorio = controller.guardar(
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtUbicacion.getText().trim(),
                    capacidad,
                    estado
            );

            String fechaGuardada = formatearFecha(
                    laboratorio.getFechaCreacion()
            );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Laboratorio guardado correctamente.\n"
                            + "Código: "
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

            int id = Integer.parseInt(
                    txtId.getText().trim()
            );

            int capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim()
            );

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            boolean actualizado = controller.actualizar(
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

    private void desactivar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertenciaGeneral(
                    "Seleccione un laboratorio de la tabla."
            );

            return;
        }

        String estadoActual = String.valueOf(
                cmbEstado.getSelectedItem()
        );

        if ("INACTIVO".equalsIgnoreCase(estadoActual)) {

            mostrarAdvertenciaGeneral(
                    "El laboratorio ya se encuentra inactivo."
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este laboratorio?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int id = Integer.parseInt(
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

    private void buscar() {

        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Ingrese ID, código, nombre o ubicación:\n"
                        + "(Deje vacío para mostrar todos)",
                "Buscar laboratorio",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        String criterio = entrada.trim();

        try {

            if (criterio.isEmpty()) {

                cargarTabla(controller.listar());
                limpiarCampos();

                return;
            }

            List<Laboratorio> laboratorios =
                    controller.listar();

            List<Laboratorio> exactos =
                    new ArrayList<>();

            List<Laboratorio> parciales =
                    new ArrayList<>();

            String filtro =
                    criterio.toLowerCase(Locale.ROOT);

            for (Laboratorio laboratorio : laboratorios) {

                String id = String.valueOf(
                        laboratorio.getIdLaboratorio()
                );

                String codigo =
                        textoSeguro(laboratorio.getCodigo());

                String nombre =
                        textoSeguro(laboratorio.getNombre());

                String ubicacion =
                        textoSeguro(laboratorio.getUbicacion());

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {

                    exactos.add(laboratorio);

                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || ubicacion.toLowerCase(Locale.ROOT).contains(filtro)
                ) {

                    parciales.add(laboratorio);
                }
            }

            List<Laboratorio> resultados =
                    exactos.isEmpty()
                            ? parciales
                            : exactos;

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron laboratorios con el criterio:\n"
                                + criterio,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else if (resultados.size() == 1) {

                int filaVista =
                        tblLaborario.convertRowIndexToView(0);

                if (filaVista >= 0) {

                    tblLaborario.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblLaborario.scrollRectToVisible(
                            tblLaborario.getCellRect(
                                    filaVista,
                                    0,
                                    true
                            )
                    );

                    cargarLaboratorioSeleccionado();
                }
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void cargarLaboratorioSeleccionado() {

        if (cargandoTabla) {
            return;
        }

        int filaVista = tblLaborario.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int filaModelo =
                tblLaborario.convertRowIndexToModel(filaVista);

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        txtId.setText(
                valorTabla(modelo, filaModelo, 0)
        );

        txtCodigo.setText(
                valorTabla(modelo, filaModelo, 1)
        );

        txtNombre.setText(
                valorTabla(modelo, filaModelo, 2)
        );

        txtUbicacion.setText(
                valorTabla(modelo, filaModelo, 3)
        );

        txtCapacidad.setText(
                valorTabla(modelo, filaModelo, 4)
        );

        String estado =
                valorTabla(modelo, filaModelo, 5);

        cmbEstado.setSelectedItem(estado);

        txtFechaCreacion.setText(
                valorTabla(modelo, filaModelo, 6)
        );

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private String valorTabla(
            DefaultTableModel modelo,
            int fila,
            int columna
    ) {

        Object valor =
                modelo.getValueAt(fila, columna);

        return valor == null ? "" : valor.toString();
    }

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

            int capacidad = Integer.parseInt(
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

    private void mostrarAdvertencia(
            String mensaje,
            JTextField campo
    ) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );

        campo.requestFocusInWindow();
    }

    private void mostrarAdvertenciaGeneral(String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarError(String mensaje) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "Error - SIGELAB",
                JOptionPane.ERROR_MESSAGE
        );
    }

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

    private void limpiarCamposSinTabla() {

        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        mostrarFechaActual();
    }

    private void salir() {

        Window ventana =
                SwingUtilities.getWindowAncestor(panelPrincipal);

        if (ventana != null) {
            ventana.dispose();
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(12, 4, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Gestion de Laboratorios");
        panelPrincipal.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Id");
        panelPrincipal.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setToolTipText("Id");
        panelPrincipal.add(txtId, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Codigo");
        panelPrincipal.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        txtCodigo.setToolTipText("Codigo");
        panelPrincipal.add(txtCodigo, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Nombre");
        panelPrincipal.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        txtNombre.setToolTipText("Nombre");
        panelPrincipal.add(txtNombre, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Ubicacion ");
        panelPrincipal.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtUbicacion = new JTextField();
        txtUbicacion.setToolTipText("Ubicacion ");
        panelPrincipal.add(txtUbicacion, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Capacidad");
        panelPrincipal.add(label6, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(64, 37), null, 0, false));
        txtCapacidad = new JTextField();
        txtCapacidad.setToolTipText("Capacidad");
        panelPrincipal.add(txtCapacidad, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, 37), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Estado");
        panelPrincipal.add(label7, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEstado = new JComboBox();
        panelPrincipal.add(cmbEstado, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Fecha de Creacion ");
        panelPrincipal.add(label8, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtFechaCreacion = new JTextField();
        panelPrincipal.add(txtFechaCreacion, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelPrincipal.add(btnNuevo, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelPrincipal.add(btnGuardar, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelPrincipal.add(btnActualizar, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDesactivar = new JButton();
        btnDesactivar.setText("Desactivar");
        panelPrincipal.add(btnDesactivar, new com.intellij.uiDesigner.core.GridConstraints(9, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        salirButton = new JButton();
        salirButton.setText("Salir");
        panelPrincipal.add(salirButton, new com.intellij.uiDesigner.core.GridConstraints(8, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panelPrincipal.add(btnBuscar, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JTextField textField1 = new JTextField();
        panelPrincipal.add(textField1, new com.intellij.uiDesigner.core.GridConstraints(10, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Buscar");
        panelPrincipal.add(label9, new com.intellij.uiDesigner.core.GridConstraints(10, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tblLaborario = new JTable();
        panelPrincipal.add(tblLaborario, new com.intellij.uiDesigner.core.GridConstraints(11, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }
}
