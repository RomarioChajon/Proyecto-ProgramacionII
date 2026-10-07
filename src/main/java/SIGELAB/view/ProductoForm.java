package SIGELAB.view;

import SIGELAB.controller.ProductoController;
import SIGELAB.controller.TipoEquipoController;
import SIGELAB.model.Producto;
import SIGELAB.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

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

    // Nombre que actualmente tiene en el Designer
    private JButton buscarButton;

    // Campo adicional del Designer.
    // Por ahora no se utiliza.
    private JTextField textField1;

    private JTable tblProducto;

    private final ProductoController productoController;
    private final TipoEquipoController tipoEquipoController;

    public ProductoForm() {

        productoController = new ProductoController();
        tipoEquipoController = new TipoEquipoController();

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarTiposEquipo();
        cargarProductos();
    }

    private void configurarFormulario() {

        txtId.setEditable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {

        DefaultTableModel modelo = new DefaultTableModel(
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
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblProducto.setModel(modelo);

        tblProducto.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());

        btnGuardar.addActionListener(e -> guardar());

        btnActualizar.addActionListener(e -> actualizar());

        btnDesactivar.addActionListener(e -> desactivar());

        buscarButton.addActionListener(e -> buscar());

        btnSalir.addActionListener(e -> salir());

        tblProducto
                .getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        cargarProductoSeleccionado();
                    }
                });
    }

    private void cargarTiposEquipo() {

        try {

            List<TipoEquipo> tipos =
                    tipoEquipoController.listar();

            cmbTipoEquipo.removeAllItems();

            for (TipoEquipo tipo : tipos) {

                /*
                 * Solamente mostramos tipos de equipo activos.
                 */
                if (tipo.getEstado() != null
                        && "ACTIVO".equals(tipo.getEstado().name())) {

                    cmbTipoEquipo.addItem(tipo);
                }
            }

            if (cmbTipoEquipo.getItemCount() > 0) {
                cmbTipoEquipo.setSelectedIndex(0);
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarProductos() {

        try {

            List<Producto> productos =
                    productoController.listar();

            cargarTabla(productos);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los productos.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<Producto> productos) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblProducto.getModel();

        modelo.setRowCount(0);

        for (Producto producto : productos) {

            modelo.addRow(
                    new Object[]{
                            producto.getIdProducto(),
                            producto.getCodigoProducto(),
                            producto.getNombre(),
                            producto.getIdTipoEquipo(),
                            producto.getMarca(),
                            producto.getModelo(),
                            producto.getDescripcion(),
                            producto.getEstado()
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

            TipoEquipo tipoEquipo =
                    (TipoEquipo) cmbTipoEquipo.getSelectedItem();

            if (tipoEquipo == null) {

                mostrarAdvertencia(
                        "Debe seleccionar un tipo de equipo."
                );

                return;
            }

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            Producto producto =
                    productoController.guardar(
                            txtCodigo.getText().trim(),
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
                            + "ID generado: "
                            + producto.getIdProducto(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarCampos();
            cargarProductos();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(
                    ex.getMessage()
            );

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

        try {

            int idProducto =
                    Integer.parseInt(
                            txtId.getText().trim()
                    );

            TipoEquipo tipoEquipo =
                    (TipoEquipo) cmbTipoEquipo.getSelectedItem();

            if (tipoEquipo == null) {

                mostrarAdvertencia(
                        "Debe seleccionar un tipo de equipo."
                );

                return;
            }

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            boolean actualizado =
                    productoController.actualizar(
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
                        "Producto actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                cargarProductos();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(
                    ex.getMessage()
            );

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

        String estado =
                String.valueOf(
                        cmbEstado.getSelectedItem()
                );

        if ("INACTIVO".equals(estado)) {

            mostrarAdvertencia(
                    "El producto ya está inactivo."
            );

            return;
        }

        int respuesta =
                JOptionPane.showConfirmDialog(
                        panelPrincipal,
                        "¿Desea desactivar este producto?",
                        "Confirmar",
                        JOptionPane.YES_NO_OPTION
                );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int idProducto =
                    Integer.parseInt(
                            txtId.getText().trim()
                    );

            boolean desactivado =
                    productoController.desactivar(
                            idProducto
                    );

            if (desactivado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                cargarProductos();

            } else {

                mostrarAdvertencia(
                        "No se pudo desactivar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el producto.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {

        String texto =
                JOptionPane.showInputDialog(
                        panelPrincipal,
                        "Ingrese código, nombre, marca o modelo:",
                        "Buscar producto",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (texto == null) {
            return;
        }

        try {

            List<Producto> productos =
                    productoController.buscar(
                            texto.trim()
                    );

            cargarTabla(productos);

            limpiarCamposSinTabla();

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

        int fila =
                tblProducto.getSelectedRow();

        if (fila == -1) {
            return;
        }

        txtId.setText(
                String.valueOf(
                        tblProducto.getValueAt(fila, 0)
                )
        );

        txtCodigo.setText(
                valorTabla(fila, 1)
        );

        txtNombre.setText(
                valorTabla(fila, 2)
        );

        int idTipoEquipo =
                Integer.parseInt(
                        String.valueOf(
                                tblProducto.getValueAt(fila, 3)
                        )
                );

        seleccionarTipoEquipo(idTipoEquipo);

        txtMarca.setText(
                valorTabla(fila, 4)
        );

        txtModelo.setText(
                valorTabla(fila, 5)
        );

        txtDescripcion.setText(
                valorTabla(fila, 6)
        );

        String estado =
                valorTabla(fila, 7);

        cmbEstado.setSelectedItem(estado);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnDesactivar.setEnabled(
                "ACTIVO".equals(estado)
        );
    }

    private void seleccionarTipoEquipo(int idTipoEquipo) {

        for (int i = 0;
             i < cmbTipoEquipo.getItemCount();
             i++) {

            TipoEquipo tipo =
                    cmbTipoEquipo.getItemAt(i);

            if (tipo.getIdTipoEquipo() == idTipoEquipo) {

                cmbTipoEquipo.setSelectedIndex(i);

                break;
            }
        }
    }

    private String valorTabla(int fila, int columna) {

        Object valor =
                tblProducto.getValueAt(
                        fila,
                        columna
                );

        return valor == null
                ? ""
                : valor.toString();
    }

    private boolean validarCampos() {

        if (txtCodigo.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el código del producto."
            );

            txtCodigo.requestFocus();

            return false;
        }

        if (txtNombre.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del producto."
            );

            txtNombre.requestFocus();

            return false;
        }

        if (cmbTipoEquipo.getSelectedItem() == null) {

            mostrarAdvertencia(
                    "Debe seleccionar un tipo de equipo."
            );

            cmbTipoEquipo.requestFocus();

            return false;
        }

        return true;
    }

    private void limpiarCampos() {

        limpiarCamposSinTabla();

        tblProducto.clearSelection();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void limpiarCamposSinTabla() {

        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtDescripcion.setText("");

        if (cmbTipoEquipo.getItemCount() > 0) {
            cmbTipoEquipo.setSelectedIndex(0);
        }

        cmbEstado.setSelectedItem("ACTIVO");
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
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
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