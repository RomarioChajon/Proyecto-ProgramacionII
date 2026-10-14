
package umg.sigelab.view;

import umg.sigelab.controller.ProductoController;
import umg.sigelab.controller.TipoEquipoController;
import umg.sigelab.model.Producto;
import umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductoForm {

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtDescripcion;

    private JComboBox<TipoEquipo> cmbTipoEquipo;
    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JButton buscarButton;
    private JButton btnBuscar;

    private JTable tblProducto;

    private final ProductoController productoController;
    private final TipoEquipoController tipoEquipoController;

    private final Map<Integer, String> nombresTipos = new HashMap<>();
    private final Map<Integer, Integer> tiposPorProducto = new HashMap<>();

    private DefaultTableModel modeloTabla;
    private boolean cargandoTabla = false;

    public ProductoForm() {
        productoController = new ProductoController();
        tipoEquipoController = new TipoEquipoController();

        if (!componentesDisponibles()) {
            crearInterfaz();
        }

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarTiposEquipo();
        cargarProductos();
        nuevo();
    }

    private boolean componentesDisponibles() {
        return panelPrincipal != null
                && txtId != null
                && txtCodigo != null
                && txtNombre != null
                && txtMarca != null
                && txtModelo != null
                && txtDescripcion != null
                && cmbTipoEquipo != null
                && cmbEstado != null
                && btnNuevo != null
                && btnGuardar != null
                && btnActualizar != null
                && btnDesactivar != null
                && btnSalir != null
                && (buscarButton != null || btnBuscar != null)
                && tblProducto != null;
    }

    private void crearInterfaz() {
        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titulo = new JLabel(
                "Gestión de Producto",
                SwingConstants.CENTER
        );
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        txtId = new JTextField();
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtMarca = new JTextField();
        txtModelo = new JTextField();
        txtDescripcion = new JTextField();

        cmbTipoEquipo = new JComboBox<>();
        cmbEstado = new JComboBox<>();

        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(campos, gbc, 0, "ID", txtId);
        agregarCampo(campos, gbc, 1, "Código", txtCodigo);
        agregarCampo(campos, gbc, 2, "Nombre", txtNombre);
        agregarCampo(campos, gbc, 3, "Tipo Equipo", cmbTipoEquipo);
        agregarCampo(campos, gbc, 4, "Marca", txtMarca);
        agregarCampo(campos, gbc, 5, "Modelo", txtModelo);
        agregarCampo(campos, gbc, 6, "Descripción", txtDescripcion);
        agregarCampo(campos, gbc, 7, "Estado", cmbEstado);

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnDesactivar = new JButton("Desactivar");
        buscarButton = new JButton("Buscar");
        btnSalir = new JButton("Salir");

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 8)
        );

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnDesactivar);
        botones.add(buscarButton);
        botones.add(btnSalir);

        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.add(titulo, BorderLayout.NORTH);
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tblProducto = new JTable();

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(
                new JScrollPane(tblProducto),
                BorderLayout.CENTER
        );

        panelPrincipal.setPreferredSize(
                new Dimension(1050, 680)
        );
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

        txtId.setFocusable(false);
        txtCodigo.setFocusable(false);

        txtCodigo.setToolTipText(
                "El código se genera automáticamente al guardar."
        );

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {
        modeloTabla = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código",
                        "Nombre",
                        "Tipo Equipo",
                        "Marca",
                        "Modelo",
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

        tblProducto.setModel(modeloTabla);
        tblProducto.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tblProducto.setAutoCreateRowSorter(true);
        tblProducto.setFillsViewportHeight(true);
        tblProducto.setRowHeight(25);
        tblProducto.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnSalir.addActionListener(e -> salir());

        if (buscarButton != null) {
            buscarButton.addActionListener(e -> buscar());
        }

        if (btnBuscar != null && btnBuscar != buscarButton) {
            btnBuscar.addActionListener(e -> buscar());
        }

        tblProducto.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoTabla) {
                cargarProductoSeleccionado();
            }
        });
    }

    private void cargarTiposEquipo() {
        try {
            List<TipoEquipo> tipos = tipoEquipoController.listar();

            nombresTipos.clear();
            cmbTipoEquipo.removeAllItems();

            for (TipoEquipo tipo : tipos) {
                nombresTipos.put(
                        tipo.getIdTipoEquipo(),
                        tipo.getNombre()
                );

                if (tipo.getEstado() != null
                        && "ACTIVO".equals(tipo.getEstado().name())) {
                    cmbTipoEquipo.addItem(tipo);
                }
            }

            cmbTipoEquipo.setSelectedIndex(-1);

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarProductos() {
        try {
            cargarTabla(productoController.listar());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los productos.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<Producto> productos) {
        cargandoTabla = true;

        try {
            tblProducto.clearSelection();
            modeloTabla.setRowCount(0);
            tiposPorProducto.clear();

            for (Producto producto : productos) {
                tiposPorProducto.put(
                        producto.getIdProducto(),
                        producto.getIdTipoEquipo()
                );

                modeloTabla.addRow(new Object[]{
                        producto.getIdProducto(),
                        producto.getCodigoProducto(),
                        producto.getNombre(),
                        nombresTipos.getOrDefault(
                                producto.getIdTipoEquipo(),
                                String.valueOf(producto.getIdTipoEquipo())
                        ),
                        producto.getMarca(),
                        producto.getModelo(),
                        producto.getDescripcion(),
                        producto.getEstado()
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
                    "Presione Nuevo antes de registrar otro producto."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        TipoEquipo tipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        try {
            Producto producto = productoController.guardar(
                    "",
                    txtNombre.getText().trim(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            cargarProductos();
            seleccionarFilaPorId(producto.getIdProducto());

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Producto guardado correctamente.\n\n"
                            + "ID: " + producto.getIdProducto()
                            + "\nCódigo: " + producto.getCodigoProducto()
                            + "\nNombre: " + producto.getNombre(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo guardar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione un producto de la tabla."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        TipoEquipo tipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean actualizado = productoController.actualizar(
                    id,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            if (actualizado) {
                cargarProductos();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                mostrarAdvertencia(
                        "No se pudo actualizar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo actualizar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void desactivar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione un producto de la tabla."
            );
            return;
        }

        if ("INACTIVO".equalsIgnoreCase(
                String.valueOf(cmbEstado.getSelectedItem())
        )) {
            mostrarAdvertencia(
                    "Este producto ya está inactivo."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este producto?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean desactivado = productoController.desactivar(id);

            if (desactivado) {
                cargarProductos();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                mostrarAdvertencia(
                        "No se pudo desactivar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo desactivar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {
        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Buscar por ID, código, nombre, tipo de equipo,\n"
                        + "marca, modelo, descripción o estado.\n\n"
                        + "Deje vacío para mostrar todos:",
                "Buscar producto",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        String criterio = entrada.trim();

        if (criterio.isEmpty()) {
            cargarProductos();
            nuevo();
            return;
        }

        try {
            List<Producto> exactos = new ArrayList<>();
            List<Producto> parciales = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (Producto producto : productoController.listar()) {
                String id = String.valueOf(producto.getIdProducto());
                String codigo = textoSeguro(producto.getCodigoProducto());
                String nombre = textoSeguro(producto.getNombre());
                String tipo = textoSeguro(
                        nombresTipos.get(producto.getIdTipoEquipo())
                );
                String marca = textoSeguro(producto.getMarca());
                String modelo = textoSeguro(producto.getModelo());
                String descripcion = textoSeguro(producto.getDescripcion());
                String estado = producto.getEstado() == null
                        ? ""
                        : producto.getEstado().toString();

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {
                    exactos.add(producto);

                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || tipo.toLowerCase(Locale.ROOT).contains(filtro)
                                || marca.toLowerCase(Locale.ROOT).contains(filtro)
                                || modelo.toLowerCase(Locale.ROOT).contains(filtro)
                                || descripcion.toLowerCase(Locale.ROOT).contains(filtro)
                                || estado.toLowerCase(Locale.ROOT).contains(filtro)
                ) {
                    parciales.add(producto);
                }
            }

            List<Producto> resultados =
                    exactos.isEmpty() ? exactos : exactos;

            if (exactos.isEmpty()) {
                resultados = parciales;
            }

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {
                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron productos con el criterio:\n"
                                + criterio,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else if (resultados.size() == 1) {
                seleccionarFilaPorId(
                        resultados.get(0).getIdProducto()
                );
            }

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    private void seleccionarFilaPorId(int idProducto) {
        for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {
            Object valor = modeloTabla.getValueAt(fila, 0);

            if (valor != null
                    && String.valueOf(valor).equals(
                    String.valueOf(idProducto)
            )) {

                int filaVista =
                        tblProducto.convertRowIndexToView(fila);

                if (filaVista >= 0) {
                    tblProducto.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblProducto.scrollRectToVisible(
                            tblProducto.getCellRect(filaVista, 0, true)
                    );

                    cargarProductoSeleccionado();
                }
                return;
            }
        }
    }

    private void cargarProductoSeleccionado() {
        if (cargandoTabla) {
            return;
        }

        int filaVista = tblProducto.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int fila = tblProducto.convertRowIndexToModel(filaVista);

        int idProducto = Integer.parseInt(
                valorTabla(fila, 0)
        );

        Integer idTipoEquipo = tiposPorProducto.get(idProducto);

        txtId.setText(String.valueOf(idProducto));
        txtCodigo.setText(valorTabla(fila, 1));
        txtNombre.setText(valorTabla(fila, 2));
        txtMarca.setText(valorTabla(fila, 4));
        txtModelo.setText(valorTabla(fila, 5));
        txtDescripcion.setText(valorTabla(fila, 6));

        seleccionarTipoEquipo(idTipoEquipo);

        String estado = valorTabla(fila, 7);
        cmbEstado.setSelectedItem(estado);

        cmbTipoEquipo.setEnabled(false);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(
                cmbTipoEquipo.getSelectedItem() != null
        );
        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private void seleccionarTipoEquipo(Integer idTipoEquipo) {
        cmbTipoEquipo.setSelectedIndex(-1);

        if (idTipoEquipo == null) {
            return;
        }

        for (int i = 0; i < cmbTipoEquipo.getItemCount(); i++) {
            TipoEquipo tipo = cmbTipoEquipo.getItemAt(i);

            if (tipo != null
                    && tipo.getIdTipoEquipo() == idTipoEquipo) {
                cmbTipoEquipo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String valorTabla(int fila, int columna) {
        Object valor = modeloTabla.getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean validarCampos() {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAdvertencia(
                    "Ingrese el nombre del producto."
            );
            txtNombre.requestFocusInWindow();
            return false;
        }

        if (nombre.length() > 100) {
            mostrarAdvertencia(
                    "El nombre no puede superar 100 caracteres."
            );
            return false;
        }

        if (cmbTipoEquipo.getSelectedItem() == null) {
            mostrarAdvertencia(
                    "Seleccione un tipo de equipo activo."
            );
            return false;
        }

        if (cmbEstado.getSelectedItem() == null) {
            mostrarAdvertencia(
                    "Seleccione el estado del producto."
            );
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtDescripcion.setText("");

        cmbTipoEquipo.setEnabled(true);

        if (cmbTipoEquipo.getItemCount() > 0) {
            cmbTipoEquipo.setSelectedIndex(0);
        } else {
            cmbTipoEquipo.setSelectedIndex(-1);
        }

        cmbEstado.setSelectedItem("ACTIVO");

        cargandoTabla = true;

        try {
            tblProducto.clearSelection();
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
        panelPrincipal.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(11, 9, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Gestion de Producto");
        panelPrincipal.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 9, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Id");
        panelPrincipal.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setToolTipText("Id");
        panelPrincipal.add(txtId, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Codigo");
        panelPrincipal.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtCodigo = new JTextField();
        txtCodigo.setToolTipText("Codigo");
        panelPrincipal.add(txtCodigo, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Nombre");
        panelPrincipal.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNombre = new JTextField();
        txtNombre.setToolTipText("Nombre");
        panelPrincipal.add(txtNombre, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Tipo Equipo");
        panelPrincipal.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbTipoEquipo = new JComboBox();
        panelPrincipal.add(cmbTipoEquipo, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Marca");
        panelPrincipal.add(label6, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtMarca = new JTextField();
        txtMarca.setToolTipText("Marca");
        panelPrincipal.add(txtMarca, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Mddelo");
        panelPrincipal.add(label7, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtModelo = new JTextField();
        txtModelo.setToolTipText("Modelo");
        panelPrincipal.add(txtModelo, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Descripcion");
        panelPrincipal.add(label8, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDescripcion = new JTextField();
        txtDescripcion.setToolTipText("Descripcion ");
        panelPrincipal.add(txtDescripcion, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Estado");
        panelPrincipal.add(label9, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEstado = new JComboBox();
        panelPrincipal.add(cmbEstado, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelPrincipal.add(btnNuevo, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelPrincipal.add(btnGuardar, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 8, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblProducto = new JTable();
        scrollPane1.setViewportView(tblProducto);
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelPrincipal.add(btnActualizar, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDesactivar = new JButton();
        btnDesactivar.setText("Desactivar");
        panelPrincipal.add(btnDesactivar, new com.intellij.uiDesigner.core.GridConstraints(9, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panelPrincipal.add(btnBuscar, new com.intellij.uiDesigner.core.GridConstraints(9, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnSalir = new JButton();
        btnSalir.setText("Salir");
        panelPrincipal.add(btnSalir, new com.intellij.uiDesigner.core.GridConstraints(9, 7, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }
}

