
package SIGELAB.view;

import SIGELAB.controller.TipoEquipoController;
import SIGELAB.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TipoEquipoForm {

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JPanel panelPrincipal;
    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton salirButton;
    private JButton btnBuscar;
    private JTextField textField1;
    private JTable tblTipoEquipo;

    private final TipoEquipoController controller;

    private boolean cargandoTabla = false;

    public TipoEquipoForm() {

        controller = new TipoEquipoController();

        verificarComponentes();
        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarTiposEquipo();
        nuevo();
    }

    private void verificarComponentes() {

        if (panelPrincipal == null ||
                txtId == null ||
                txtCodigo == null ||
                txtNombre == null ||
                txtDescripcion == null ||
                cmbEstado == null ||
                btnNuevo == null ||
                btnGuardar == null ||
                btnActualizar == null ||
                btnDesactivar == null ||
                salirButton == null ||
                btnBuscar == null ||
                tblTipoEquipo == null) {

            throw new IllegalStateException(
                    "Verifique que los componentes estén vinculados " +
                            "correctamente en TipoEquipoForm.form, " +
                            "especialmente el campo txtCodigo."
            );
        }
    }

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtId.setFocusable(false);

        txtCodigo.setEditable(false);
        txtCodigo.setFocusable(false);
        txtCodigo.setToolTipText(
                "Código generado automáticamente al guardar la categoría."
        );

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        if (textField1 != null) {
            textField1.setToolTipText(
                    "Buscar por ID, código, categoría, descripción o estado"
            );
        }
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
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblTipoEquipo.setModel(modelo);

        tblTipoEquipo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblTipoEquipo.setAutoCreateRowSorter(true);

        tblTipoEquipo.getTableHeader()
                .setReorderingAllowed(false);

        tblTipoEquipo.setFillsViewportHeight(true);
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        salirButton.addActionListener(e -> salir());
        btnBuscar.addActionListener(e -> buscar());

        if (textField1 != null) {
            textField1.addActionListener(e -> buscar());
        }

        tblTipoEquipo.getSelectionModel()
                .addListSelectionListener(e -> {

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
                    "No se pudieron cargar los tipos de equipo.\n\n" +
                            ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<TipoEquipo> tipos) {

        cargandoTabla = true;

        try {

            tblTipoEquipo.clearSelection();

            if (tblTipoEquipo.getRowSorter() != null) {
                tblTipoEquipo.getRowSorter().setSortKeys(
                        new ArrayList<>()
                );
            }

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

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        txtNombre.requestFocusInWindow();
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            TipoEquipo tipo = controller.guardar(
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    estado
            );

            cargarTiposEquipo();

            limpiarCampos();

            txtId.setText(
                    String.valueOf(tipo.getIdTipoEquipo())
            );

            txtCodigo.setText(
                    tipo.getCodigo() == null ? "" : tipo.getCodigo()
            );

            txtNombre.setText(
                    tipo.getNombre() == null ? "" : tipo.getNombre()
            );

            txtDescripcion.setText(
                    tipo.getDescripcion() == null
                            ? "" : tipo.getDescripcion()
            );

            cmbEstado.setSelectedItem(
                    tipo.getEstado().name()
            );

            btnGuardar.setEnabled(false);
            btnActualizar.setEnabled(true);
            btnDesactivar.setEnabled(
                    "ACTIVO".equals(tipo.getEstado().name())
            );

            seleccionarFilaPorId(tipo.getIdTipoEquipo());

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Tipo de equipo guardado correctamente.\n\n" +
                            "ID generado: " + tipo.getIdTipoEquipo() +
                            "\nCódigo generado: " + tipo.getCodigo() +
                            "\nCategoría: " + tipo.getNombre(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el tipo de equipo.\n\n" +
                            ex.getMessage()
            );
        }
    }

    private void seleccionarFilaPorId(int idTipoEquipo) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        for (int fila = 0; fila < modelo.getRowCount(); fila++) {

            Object valor = modelo.getValueAt(fila, 0);

            if (valor instanceof Number &&
                    ((Number) valor).intValue() == idTipoEquipo) {

                int filaVista =
                        tblTipoEquipo.convertRowIndexToView(fila);

                if (filaVista >= 0) {
                    cargandoTabla = true;

                    try {
                        tblTipoEquipo.setRowSelectionInterval(
                                filaVista,
                                filaVista
                        );

                        tblTipoEquipo.scrollRectToVisible(
                                tblTipoEquipo.getCellRect(
                                        filaVista,
                                        0,
                                        true
                                )
                        );

                    } finally {
                        cargandoTabla = false;
                    }
                }

                return;
            }
        }
    }

    private void actualizar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Seleccione un tipo de equipo de la tabla."
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

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            boolean actualizado = controller.actualizar(
                    id,
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    estado
            );

            if (actualizado) {

                String codigoActual = txtCodigo.getText();

                cargarTiposEquipo();
                limpiarCampos();

                txtCodigo.setText(codigoActual);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Tipo de equipo actualizado correctamente.\n\n" +
                                "Código conservado: " + codigoActual,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                seleccionarFilaPorId(id);
                cargarTipoSeleccionado();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el tipo de equipo.\n\n" +
                            ex.getMessage()
            );
        }
    }

    private void desactivar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Seleccione un tipo de equipo de la tabla."
            );

            return;
        }

        String estado = String.valueOf(
                cmbEstado.getSelectedItem()
        );

        if ("INACTIVO".equalsIgnoreCase(estado)) {

            mostrarAdvertencia(
                    "El tipo de equipo ya está inactivo."
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este tipo de equipo?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int id = Integer.parseInt(
                    txtId.getText().trim()
            );

            boolean desactivado = controller.desactivar(id);

            if (desactivado) {

                cargarTiposEquipo();
                limpiarCampos();

                seleccionarFilaPorId(id);
                cargarTipoSeleccionado();

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Tipo de equipo desactivado correctamente.\n\n" +
                                "Su código se conserva.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {

                mostrarAdvertencia(
                        "No se pudo desactivar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el tipo de equipo.\n\n" +
                            ex.getMessage()
            );
        }
    }

    private void buscar() {

        String criterio = textField1 == null
                ? ""
                : textField1.getText().trim();

        if (criterio.isEmpty()) {

            cargarTiposEquipo();
            nuevo();
            return;
        }

        try {

            List<TipoEquipo> todos = controller.listar();
            List<TipoEquipo> resultados = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (TipoEquipo tipo : todos) {

                String id = String.valueOf(
                        tipo.getIdTipoEquipo()
                );

                String codigo = tipo.getCodigo() == null
                        ? ""
                        : tipo.getCodigo();

                String nombre = tipo.getNombre() == null
                        ? ""
                        : tipo.getNombre();

                String descripcion = tipo.getDescripcion() == null
                        ? ""
                        : tipo.getDescripcion();

                String estado = tipo.getEstado() == null
                        ? ""
                        : tipo.getEstado().toString();

                if (id.contains(filtro)
                        || codigo.toLowerCase(Locale.ROOT).contains(filtro)
                        || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                        || descripcion.toLowerCase(Locale.ROOT).contains(filtro)
                        || estado.toLowerCase(Locale.ROOT).contains(filtro)) {

                    resultados.add(tipo);
                }
            }

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {

                mostrarAdvertencia(
                        "No se encontraron tipos de equipo " +
                                "con ese criterio."
                );
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n\n" +
                            ex.getMessage()
            );
        }
    }

    private void cargarTipoSeleccionado() {

        if (cargandoTabla) {
            return;
        }

        int filaVista = tblTipoEquipo.getSelectedRow();

        if (filaVista < 0 ||
                filaVista >= tblTipoEquipo.getRowCount()) {
            return;
        }

        int filaModelo;

        try {

            filaModelo =
                    tblTipoEquipo.convertRowIndexToModel(filaVista);

        } catch (IndexOutOfBoundsException ex) {
            return;
        }

        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        if (filaModelo < 0 ||
                filaModelo >= modelo.getRowCount()) {
            return;
        }

        Object id = modelo.getValueAt(filaModelo, 0);
        Object codigo = modelo.getValueAt(filaModelo, 1);
        Object nombre = modelo.getValueAt(filaModelo, 2);
        Object descripcion = modelo.getValueAt(filaModelo, 3);
        Object estado = modelo.getValueAt(filaModelo, 4);

        txtId.setText(
                id == null ? "" : id.toString()
        );

        txtCodigo.setText(
                codigo == null ? "" : codigo.toString()
        );

        txtNombre.setText(
                nombre == null ? "" : nombre.toString()
        );

        txtDescripcion.setText(
                descripcion == null ? "" : descripcion.toString()
        );

        String estadoTexto =
                estado == null ? "" : estado.toString();

        cmbEstado.setSelectedItem(estadoTexto);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estadoTexto)
        );
    }

    private boolean validarCampos() {

        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del tipo de equipo."
            );

            txtNombre.requestFocusInWindow();
            return false;
        }

        if (nombre.length() > 50) {

            mostrarAdvertencia(
                    "El nombre no puede superar los 50 caracteres."
            );

            txtNombre.requestFocusInWindow();
            return false;
        }

        if (descripcion.length() > 200) {

            mostrarAdvertencia(
                    "La descripción no puede superar los 200 caracteres."
            );

            txtDescripcion.requestFocusInWindow();
            return false;
        }

        if (cmbEstado.getSelectedItem() == null) {

            mostrarAdvertencia(
                    "Seleccione un estado."
            );

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
                "Error",
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


