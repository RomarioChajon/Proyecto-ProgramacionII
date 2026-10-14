
package umg.sigelab.view;

import umg.sigelab.controller.TipoEquipoController;
import umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TipoEquipoForm {

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JTextField textField1;
    private JTextField txtCotegoria;

    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JButton btnBuscar;

    private JTable tblTipoEquipo;

    private final TipoEquipoController controller;
    private boolean cargandoTabla = false;

    public TipoEquipoForm() {
        controller = new TipoEquipoController();

        if (!componentesDisponibles()) {
            crearInterfaz();
        }

        configurarFormulario();
        configurarTabla();
        configurarEventos();
        cargarTiposEquipo();
        nuevo();
    }

    private boolean componentesDisponibles() {
        return panelPrincipal != null
                && txtId != null
                && txtCodigo != null
                && txtNombre != null
                && txtDescripcion != null
                && cmbEstado != null
                && btnNuevo != null
                && btnGuardar != null
                && btnBuscar != null
                && btnActualizar != null
                && btnDesactivar != null
                && btnSalir != null
                && tblTipoEquipo != null;
    }

    private void crearInterfaz() {
        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titulo = new JLabel(
                "Gestión de Tipos de Equipo",
                SwingConstants.CENTER
        );
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        txtId = new JTextField();
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtDescripcion = new JTextField();
        cmbEstado = new JComboBox<>();

        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(campos, gbc, 0, "ID", txtId);
        agregarCampo(campos, gbc, 1, "Código", txtCodigo);
        agregarCampo(campos, gbc, 2, "Categoría", txtNombre);
        agregarCampo(campos, gbc, 3, "Descripción", txtDescripcion);
        agregarCampo(campos, gbc, 4, "Estado", cmbEstado);

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnBuscar = new JButton("Buscar");
        btnActualizar = new JButton("Actualizar");
        btnDesactivar = new JButton("Desactivar");
        btnSalir = new JButton("Salir");

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 8)
        );

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnBuscar);
        botones.add(btnActualizar);
        botones.add(btnDesactivar);
        botones.add(btnSalir);

        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.add(titulo, BorderLayout.NORTH);
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tblTipoEquipo = new JTable();

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(
                new JScrollPane(tblTipoEquipo),
                BorderLayout.CENTER
        );

        panelPrincipal.setPreferredSize(new Dimension(950, 620));
    }

    private void agregarCampo(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            String etiqueta,
            JComponent componente
    ) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(componente, gbc);
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        txtCodigo.setEditable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código",
                        "Categoría",
                        "Descripción",
                        "Estado"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tblTipoEquipo.setModel(modelo);
        tblTipoEquipo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tblTipoEquipo.setAutoCreateRowSorter(true);
        tblTipoEquipo.setFillsViewportHeight(true);
        tblTipoEquipo.setRowHeight(25);
        tblTipoEquipo.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnBuscar.addActionListener(e -> buscar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnSalir.addActionListener(e -> salir());

        tblTipoEquipo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoTabla) {
                cargarTipoSeleccionado();
            }
        });
    }

    private void cargarTiposEquipo() {
        try {
            cargarTabla(controller.listar());
        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<TipoEquipo> tipos) {
        cargandoTabla = true;

        try {
            tblTipoEquipo.clearSelection();

            DefaultTableModel modelo =
                    (DefaultTableModel) tblTipoEquipo.getModel();

            modelo.setRowCount(0);

            for (TipoEquipo tipo : tipos) {
                modelo.addRow(new Object[]{
                        tipo.getIdTipoEquipo(),
                        tipo.getCodigo(),
                        tipo.getNombre(),
                        tipo.getDescripcion(),
                        tipo.getEstado()
                });
            }
        } finally {
            cargandoTabla = false;
        }
    }

    private void nuevo() {
        limpiarCampos();
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        if (!txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Presione Nuevo antes de registrar otra categoría."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {
            TipoEquipo tipo = controller.guardar(
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            cargarTiposEquipo();
            seleccionarFilaPorId(tipo.getIdTipoEquipo());

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Tipo de equipo guardado correctamente.\n"
                            + "ID: " + tipo.getIdTipoEquipo()
                            + "\nCódigo: " + tipo.getCodigo()
                            + "\nCategoría: " + tipo.getNombre(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo guardar el tipo de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione una categoría de la tabla."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean actualizado = controller.actualizar(
                    id,
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            if (actualizado) {
                cargarTiposEquipo();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Tipo de equipo actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                mostrarAdvertencia(
                        "No se pudo actualizar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo actualizar el tipo de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void desactivar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione una categoría de la tabla."
            );
            return;
        }

        if ("INACTIVO".equals(
                String.valueOf(cmbEstado.getSelectedItem())
        )) {
            mostrarAdvertencia(
                    "Esta categoría ya está inactiva."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar esta categoría?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean desactivado = controller.desactivar(id);

            if (desactivado) {
                cargarTiposEquipo();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Categoría desactivada correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                mostrarAdvertencia(
                        "No se pudo desactivar la categoría."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo desactivar la categoría.\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {
        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Buscar por ID, código, categoría, descripción o estado.\n"
                        + "Deje vacío para mostrar todos:",
                "Buscar tipo de equipo",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        String criterio = entrada.trim();

        if (criterio.isEmpty()) {
            cargarTiposEquipo();
            nuevo();
            return;
        }

        try {
            List<TipoEquipo> exactos = new ArrayList<>();
            List<TipoEquipo> parciales = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (TipoEquipo tipo : controller.listar()) {
                String id = String.valueOf(tipo.getIdTipoEquipo());
                String codigo = textoSeguro(tipo.getCodigo());
                String nombre = textoSeguro(tipo.getNombre());
                String descripcion = textoSeguro(tipo.getDescripcion());
                String estado = tipo.getEstado() == null
                        ? ""
                        : tipo.getEstado().toString();

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {
                    exactos.add(tipo);
                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || descripcion.toLowerCase(Locale.ROOT).contains(filtro)
                                || estado.toLowerCase(Locale.ROOT).contains(filtro)
                ) {
                    parciales.add(tipo);
                }
            }

            List<TipoEquipo> resultados =
                    exactos.isEmpty() ? parciales : exactos;

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {
                mostrarAdvertencia(
                        "No se encontraron categorías con ese criterio."
                );
            } else if (resultados.size() == 1) {
                seleccionarFilaPorId(
                        resultados.get(0).getIdTipoEquipo()
                );
            }

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private void seleccionarFilaPorId(int id) {
        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            Object valor = modelo.getValueAt(fila, 0);

            if (valor != null
                    && String.valueOf(valor).equals(String.valueOf(id))) {

                int filaVista = tblTipoEquipo.convertRowIndexToView(fila);

                if (filaVista >= 0) {
                    tblTipoEquipo.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblTipoEquipo.scrollRectToVisible(
                            tblTipoEquipo.getCellRect(filaVista, 0, true)
                    );

                    cargarTipoSeleccionado();
                }
                return;
            }
        }
    }

    private void cargarTipoSeleccionado() {
        if (cargandoTabla) {
            return;
        }

        int filaVista = tblTipoEquipo.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int filaModelo =
                tblTipoEquipo.convertRowIndexToModel(filaVista);

        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        txtId.setText(valorTexto(modelo.getValueAt(filaModelo, 0)));
        txtCodigo.setText(valorTexto(modelo.getValueAt(filaModelo, 1)));
        txtNombre.setText(valorTexto(modelo.getValueAt(filaModelo, 2)));
        txtDescripcion.setText(valorTexto(modelo.getValueAt(filaModelo, 3)));

        String estado = valorTexto(
                modelo.getValueAt(filaModelo, 4)
        );

        cmbEstado.setSelectedItem(estado);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private boolean validarCampos() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAdvertencia(
                    "Ingrese el nombre de la categoría."
            );
            txtNombre.requestFocusInWindow();
            return false;
        }

        if (nombre.length() > 50) {
            mostrarAdvertencia(
                    "La categoría no puede superar 50 caracteres."
            );
            return false;
        }

        if (descripcion.length() > 200) {
            mostrarAdvertencia(
                    "La descripción no puede superar 200 caracteres."
            );
            return false;
        }

        if (cmbEstado.getSelectedItem() == null) {
            mostrarAdvertencia("Seleccione un estado.");
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtDescripcion.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        cargandoTabla = true;
        try {
            tblTipoEquipo.clearSelection();
        } finally {
            cargandoTabla = false;
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void mostrarAdvertencia(String mensaje) {
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
        panelPrincipal.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 5, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Gestion de Tipos de Equipo");
        panelPrincipal.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Id");
        panelPrincipal.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setText("");
        txtId.setToolTipText("Id");
        panelPrincipal.add(txtId, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Categoria");
        panelPrincipal.add(label3, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        txtNombre.setToolTipText("Nombre");
        panelPrincipal.add(txtNombre, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Descripcion ");
        panelPrincipal.add(label4, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDescripcion = new JTextField();
        panelPrincipal.add(txtDescripcion, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Estado");
        panelPrincipal.add(label5, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEstado = new JComboBox();
        panelPrincipal.add(cmbEstado, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelPrincipal.add(btnNuevo, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelPrincipal.add(btnGuardar, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelPrincipal.add(btnActualizar, new com.intellij.uiDesigner.core.GridConstraints(7, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDesactivar = new JButton();
        btnDesactivar.setText("Desactivar");
        panelPrincipal.add(btnDesactivar, new com.intellij.uiDesigner.core.GridConstraints(7, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnSalir = new JButton();
        btnSalir.setText("Salir");
        panelPrincipal.add(btnSalir, new com.intellij.uiDesigner.core.GridConstraints(6, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblTipoEquipo = new JTable();
        scrollPane1.setViewportView(tblTipoEquipo);
        final JLabel label6 = new JLabel();
        label6.setText("Codigo");
        panelPrincipal.add(label6, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        panelPrincipal.add(txtCodigo, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panelPrincipal.add(btnBuscar, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }
}

