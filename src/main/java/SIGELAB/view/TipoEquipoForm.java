package SIGELAB.view;

import SIGELAB.controller.TipoEquipoController;
import SIGELAB.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class TipoEquipoForm {

    private JTextField txtId;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JPanel panelPrincipal;
    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;

    // Estos nombres vienen del Designer
    private JButton salirButton;
    private JButton guardarButton;
    private JTextField textField1;

    private JTable tblTipoEquipo;

    private final TipoEquipoController controller;

    public TipoEquipoForm() {

        controller = new TipoEquipoController();

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarTiposEquipo();
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
                        "Nombre",
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
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());

        btnGuardar.addActionListener(e -> guardar());

        btnActualizar.addActionListener(e -> actualizar());

        btnDesactivar.addActionListener(e -> desactivar());

        salirButton.addActionListener(e -> salir());

        tblTipoEquipo
                .getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        cargarTipoSeleccionado();
                    }
                });
    }

    private void cargarTiposEquipo() {

        try {

            List<TipoEquipo> tipos =
                    controller.listar();

            cargarTabla(tipos);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<TipoEquipo> tipos) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        modelo.setRowCount(0);

        for (TipoEquipo tipo : tipos) {

            modelo.addRow(
                    new Object[]{
                            tipo.getIdTipoEquipo(),
                            tipo.getNombre(),
                            tipo.getDescripcion(),
                            tipo.getEstado()
                    }
            );
        }
    }

    private void nuevo() {

        limpiarCampos();

        txtNombre.requestFocus();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            TipoEquipo tipo =
                    controller.guardar(
                            txtNombre.getText().trim(),
                            txtDescripcion.getText().trim(),
                            estado
                    );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Tipo de equipo guardado correctamente.\n\n"
                            + "ID generado: "
                            + tipo.getIdTipoEquipo(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            limpiarCampos();
            cargarTiposEquipo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el tipo de equipo.\n\n"
                            + ex.getMessage()
            );
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

            int id =
                    Integer.parseInt(
                            txtId.getText().trim()
                    );

            String estado =
                    String.valueOf(
                            cmbEstado.getSelectedItem()
                    );

            boolean actualizado =
                    controller.actualizar(
                            id,
                            txtNombre.getText().trim(),
                            txtDescripcion.getText().trim(),
                            estado
                    );

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Tipo de equipo actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                cargarTiposEquipo();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el tipo de equipo.\n\n"
                            + ex.getMessage()
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

        String estado =
                String.valueOf(
                        cmbEstado.getSelectedItem()
                );

        if ("INACTIVO".equals(estado)) {

            mostrarAdvertencia(
                    "El tipo de equipo ya está inactivo."
            );

            return;
        }

        int respuesta =
                JOptionPane.showConfirmDialog(
                        panelPrincipal,
                        "¿Desea desactivar este tipo de equipo?",
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
                        "Tipo de equipo desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                limpiarCampos();
                cargarTiposEquipo();

            } else {

                mostrarAdvertencia(
                        "No se pudo desactivar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el tipo de equipo.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTipoSeleccionado() {

        int fila =
                tblTipoEquipo.getSelectedRow();

        if (fila == -1) {
            return;
        }

        txtId.setText(
                String.valueOf(
                        tblTipoEquipo.getValueAt(fila, 0)
                )
        );

        txtNombre.setText(
                String.valueOf(
                        tblTipoEquipo.getValueAt(fila, 1)
                )
        );

        Object descripcion =
                tblTipoEquipo.getValueAt(fila, 2);

        txtDescripcion.setText(
                descripcion == null
                        ? ""
                        : descripcion.toString()
        );

        cmbEstado.setSelectedItem(
                String.valueOf(
                        tblTipoEquipo.getValueAt(fila, 3)
                )
        );

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        String estado =
                String.valueOf(
                        tblTipoEquipo.getValueAt(fila, 3)
                );

        btnDesactivar.setEnabled(
                "ACTIVO".equals(estado)
        );
    }

    private boolean validarCampos() {

        if (txtNombre.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del tipo de equipo."
            );

            txtNombre.requestFocus();

            return false;
        }

        if (txtNombre.getText().trim().length() > 50) {

            mostrarAdvertencia(
                    "El nombre no puede superar los 50 caracteres."
            );

            txtNombre.requestFocus();

            return false;
        }

        if (txtDescripcion.getText().trim().length() > 200) {

            mostrarAdvertencia(
                    "La descripción no puede superar los 200 caracteres."
            );

            txtDescripcion.requestFocus();

            return false;
        }

        return true;
    }

    private void limpiarCampos() {

        txtId.setText("");
        txtNombre.setText("");
        txtDescripcion.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        tblTipoEquipo.clearSelection();

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