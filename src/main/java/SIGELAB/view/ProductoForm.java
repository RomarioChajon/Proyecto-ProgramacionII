
package SIGELAB.view;

import SIGELAB.controller.ProductoController;
import SIGELAB.controller.TipoEquipoController;
import SIGELAB.model.Producto;
import SIGELAB.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductoForm {

    private JTextField txtId;
    private JPanel panelPrincipal;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JComboBox<TipoEquipo> cmbTipoEquipo;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtDescripcion;
    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JButton buscarButton;

    private JTextField textField1;
    private JTable tblProducto;

    private final ProductoController productoController;
    private final TipoEquipoController tipoEquipoController;

    private final Map<Integer, String> codigosTipos = new HashMap<>();
    private final Map<Integer, Integer> tiposPorProducto = new HashMap<>();

    private boolean cargandoTabla = false;

    public ProductoForm() {

        productoController = new ProductoController();
        tipoEquipoController = new TipoEquipoController();

        verificarComponentes();
        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarTiposEquipo();
        cargarProductos();
        nuevo();
    }

    private void verificarComponentes() {

        StringBuilder faltantes = new StringBuilder();

        if (panelPrincipal == null) faltantes.append("panelPrincipal\n");
        if (txtId == null) faltantes.append("txtId\n");
        if (txtCodigo == null) faltantes.append("txtCodigo\n");
        if (txtNombre == null) faltantes.append("txtNombre\n");
        if (cmbTipoEquipo == null) faltantes.append("cmbTipoEquipo\n");
        if (txtMarca == null) faltantes.append("txtMarca\n");
        if (txtModelo == null) faltantes.append("txtModelo\n");
        if (txtDescripcion == null) faltantes.append("txtDescripcion\n");
        if (cmbEstado == null) faltantes.append("cmbEstado\n");
        if (btnNuevo == null) faltantes.append("btnNuevo\n");
        if (btnGuardar == null) faltantes.append("btnGuardar\n");
        if (btnActualizar == null) faltantes.append("btnActualizar\n");
        if (btnDesactivar == null) faltantes.append("btnDesactivar\n");
        if (btnSalir == null) faltantes.append("btnSalir\n");
        if (buscarButton == null) faltantes.append("buscarButton\n");
        if (textField1 == null) faltantes.append("textField1\n");
        if (tblProducto == null) faltantes.append("tblProducto\n");

        if (faltantes.length() > 0) {
            throw new IllegalStateException(
                    "Faltan componentes vinculados en ProductoForm.form:\n\n"
                            + faltantes
            );
        }
    }

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtId.setFocusable(false);

        txtCodigo.setEditable(false);
        txtCodigo.setFocusable(false);
        txtCodigo.setToolTipText(
                "El código del producto se genera automáticamente al guardar."
        );

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        cmbTipoEquipo.setToolTipText(
                "Seleccione la categoría del producto."
        );

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {

        tblProducto.setModel(crearModeloTabla());

        tblProducto.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblProducto.setAutoCreateRowSorter(true);
        tblProducto.setFillsViewportHeight(true);

        tblProducto.getTableHeader().setReorderingAllowed(false);
    }

    private DefaultTableModel crearModeloTabla() {

        return new DefaultTableModel(
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
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnSalir.addActionListener(e -> salir());
        buscarButton.addActionListener(e -> buscar());
        textField1.addActionListener(e -> buscar());

        tblProducto.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting() && !cargandoTabla) {
                        cargarProductoSeleccionado();
                    }
                });
    }

    private void cargarTiposEquipo() {

        try {

            List<TipoEquipo> tipos = tipoEquipoController.listar();

            codigosTipos.clear();
            cmbTipoEquipo.removeAllItems();

            for (TipoEquipo tipo : tipos) {

                codigosTipos.put(
                        tipo.getIdTipoEquipo(),
                        tipo.getCodigo()
                );

                if (tipo.getEstado() != null
                        && "ACTIVO".equals(tipo.getEstado().name())) {

                    cmbTipoEquipo.addItem(tipo);
                }
            }

            if (cmbTipoEquipo.getItemCount() > 0) {
                cmbTipoEquipo.setSelectedIndex(0);
            } else {
                cmbTipoEquipo.setSelectedIndex(-1);
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private String obtenerCodigoTipoEquipo(int idTipoEquipo) {

        String codigo = codigosTipos.get(idTipoEquipo);

        if (codigo == null || codigo.isBlank()) {
            return "SIN CÓDIGO";
        }

        return codigo;
    }

    private void cargarProductos() {

        try {

            cargarTabla(productoController.listar());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los productos.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<Producto> productos) {

        cargandoTabla = true;

        try {

            DefaultTableModel modelo = crearModeloTabla();

            tiposPorProducto.clear();

            for (Producto producto : productos) {

                tiposPorProducto.put(
                        producto.getIdProducto(),
                        producto.getIdTipoEquipo()
                );

                modelo.addRow(new Object[]{
                        producto.getIdProducto(),
                        producto.getCodigoProducto(),
                        producto.getNombre(),
                        obtenerCodigoTipoEquipo(
                                producto.getIdTipoEquipo()
                        ),
                        producto.getMarca(),
                        producto.getModelo(),
                        producto.getDescripcion(),
                        producto.getEstado()
                });
            }

            tblProducto.setModel(modelo);

        } finally {

            cargandoTabla = false;
        }
    }

    private void nuevo() {

        limpiarCampos();
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        TipoEquipo tipoEquipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        if (tipoEquipo == null) {
            mostrarAdvertencia(
                    "Debe seleccionar un tipo de equipo."
            );
            return;
        }

        try {

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            Producto producto = productoController.guardar(
                    "",
                    txtNombre.getText().trim(),
                    tipoEquipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    estado
            );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Producto guardado correctamente.\n\n"
                            + "ID: " + producto.getIdProducto()
                            + "\nCódigo del producto: "
                            + producto.getCodigoProducto()
                            + "\nCódigo de categoría: "
                            + obtenerCodigoTipoEquipo(
                            tipoEquipo.getIdTipoEquipo()
                    ),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarProductos();
            nuevo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el producto.\n\n"
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

        TipoEquipo tipoEquipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        if (tipoEquipo == null) {
            mostrarAdvertencia(
                    "El tipo de equipo del producto no está disponible."
            );
            return;
        }

        try {

            int idProducto = Integer.parseInt(
                    txtId.getText().trim()
            );

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            boolean actualizado = productoController.actualizar(
                    idProducto,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    tipoEquipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    estado
            );

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto actualizado correctamente.\n\n"
                                + "Código conservado: "
                                + txtCodigo.getText(),
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarProductos();
                nuevo();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el producto.\n\n"
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

        String estado = String.valueOf(
                cmbEstado.getSelectedItem()
        );

        if ("INACTIVO".equals(estado)) {
            mostrarAdvertencia(
                    "El producto ya está inactivo."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este producto?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int idProducto = Integer.parseInt(
                    txtId.getText().trim()
            );

            boolean desactivado =
                    productoController.desactivar(idProducto);

            if (desactivado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarProductos();
                nuevo();

            } else {

                mostrarAdvertencia(
                        "No se pudo desactivar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el producto.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {

        String texto = textField1.getText().trim();

        if (texto.isEmpty()) {
            cargarProductos();
            nuevo();
            return;
        }

        try {

            List<Producto> productos =
                    productoController.buscar(texto);

            cargarTabla(productos);
            limpiarCampos();

            if (productos.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron productos.",
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

    private void cargarProductoSeleccionado() {

        if (cargandoTabla) {
            return;
        }

        int filaVista = tblProducto.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int fila = tblProducto.convertRowIndexToModel(filaVista);

        DefaultTableModel modelo =
                (DefaultTableModel) tblProducto.getModel();

        if (fila < 0 || fila >= modelo.getRowCount()) {
            return;
        }

        try {

            int idProducto = Integer.parseInt(
                    valorTabla(modelo, fila, 0)
            );

            Integer idTipoEquipo =
                    tiposPorProducto.get(idProducto);

            if (idTipoEquipo == null) {
                mostrarAdvertencia(
                        "No se encontró el tipo de equipo del producto."
                );
                return;
            }

            txtId.setText(String.valueOf(idProducto));

            txtCodigo.setText(
                    valorTabla(modelo, fila, 1)
            );

            txtNombre.setText(
                    valorTabla(modelo, fila, 2)
            );

            seleccionarTipoEquipo(idTipoEquipo);

            txtMarca.setText(
                    valorTabla(modelo, fila, 4)
            );

            txtModelo.setText(
                    valorTabla(modelo, fila, 5)
            );

            txtDescripcion.setText(
                    valorTabla(modelo, fila, 6)
            );

            String estado = valorTabla(modelo, fila, 7);

            cmbEstado.setSelectedItem(estado);

            cmbTipoEquipo.setEnabled(false);

            btnGuardar.setEnabled(false);

            btnActualizar.setEnabled(
                    cmbTipoEquipo.getSelectedItem() != null
            );

            btnDesactivar.setEnabled(
                    "ACTIVO".equals(estado)
            );

        } catch (NumberFormatException ex) {

            mostrarAdvertencia(
                    "El ID del producto seleccionado no es válido."
            );
        }
    }

    private void seleccionarTipoEquipo(int idTipoEquipo) {

        cmbTipoEquipo.setSelectedIndex(-1);

        for (int i = 0; i < cmbTipoEquipo.getItemCount(); i++) {

            TipoEquipo tipo = cmbTipoEquipo.getItemAt(i);

            if (tipo != null
                    && tipo.getIdTipoEquipo() == idTipoEquipo) {

                cmbTipoEquipo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String valorTabla(
            DefaultTableModel modelo,
            int fila,
            int columna
    ) {

        Object valor = modelo.getValueAt(fila, columna);

        return valor == null ? "" : valor.toString();
    }

    private boolean validarCampos() {

        if (txtNombre.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del producto."
            );

            txtNombre.requestFocusInWindow();
            return false;
        }

        if (cmbTipoEquipo.getSelectedItem() == null) {

            mostrarAdvertencia(
                    "Debe seleccionar un tipo de equipo activo."
            );

            cmbTipoEquipo.requestFocusInWindow();
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
}
