package SIGELAB.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Usuario extends JFrame {

    private JTextField txtId;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JPasswordField txtPassword;
    private JLabel lblPassword;
    private JLabel lblRol;
    private JComboBox cmbRol;
    private JLabel lblId;
    private JLabel lblApellido;
    private JLabel lblNombre;
    private JLabel lblActivo;
    private JCheckBox chkActivo;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JPanel panelPrincipal;
    private JButton btnBuscar;
    private JTextField textField1;
    private JLabel lblBuscar;
    private JTable tblUsuario;

    private DefaultTableModel modeloTabla;

    public Usuario() {

        setTitle("SIGELAB - Gestión de Usuarios");
        setContentPane(panelPrincipal);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setSize(950, 600);
        setLocationRelativeTo(null);

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        nuevoUsuario();
    }

    private void configurarFormulario() {

        // El ID lo genera la base de datos.
        txtId.setEditable(false);

        // Roles disponibles en SIGELAB.
        cmbRol.removeAllItems();
        cmbRol.addItem("ADMIN");
        cmbRol.addItem("ENCARGADO");
        cmbRol.addItem("DOCENTE");
        cmbRol.addItem("ESTUDIANTE");

        // Usuario nuevo activo por defecto.
        chkActivo.setSelected(true);
    }

    private void configurarTabla() {

        modeloTabla = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Nombre",
                        "Apellido",
                        "Usuario",
                        "Rol",
                        "Activo"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblUsuario.setModel(modeloTabla);
        tblUsuario.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevoUsuario());

        btnGuardar.addActionListener(e -> guardarUsuario());

        btnBuscar.addActionListener(e -> buscarUsuario());

        btnActualizar.addActionListener(e -> actualizarUsuario());

        btnDesactivar.addActionListener(e -> desactivarUsuario());

        btnSalir.addActionListener(e -> dispose());

        tblUsuario.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 1) {
                    cargarUsuarioSeleccionado();
                }
            }
        });
    }

    private void nuevoUsuario() {

        txtId.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtPassword.setText("");
        textField1.setText("");

        if (cmbRol.getItemCount() > 0) {
            cmbRol.setSelectedIndex(0);
        }

        chkActivo.setSelected(true);

        txtNombre.requestFocus();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        tblUsuario.clearSelection();
    }

    private void guardarUsuario() {

        if (!validarFormulario()) {
            return;
        }

        /*
         * Aquí conectaremos UsuarioController.
         *
         * No agregamos todavía llamadas al controlador
         * hasta comprobar los métodos exactos que ya
         * tienes creados en UsuarioController.
         */

        JOptionPane.showMessageDialog(
                this,
                "Formulario validado correctamente.\n" +
                        "Falta conectar el botón Guardar con UsuarioController.",
                "SIGELAB",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void buscarUsuario() {

        String busqueda = textField1.getText().trim();

        if (busqueda.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese un dato para realizar la búsqueda.",
                    "SIGELAB",
                    JOptionPane.WARNING_MESSAGE
            );

            textField1.requestFocus();

            return;
        }

        /*
         * Aquí conectaremos la búsqueda con
         * UsuarioController.
         */

        JOptionPane.showMessageDialog(
                this,
                "Búsqueda preparada para: " + busqueda,
                "SIGELAB",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void actualizarUsuario() {

        if (txtId.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un usuario de la tabla.",
                    "SIGELAB",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (!validarFormulario()) {
            return;
        }

        /*
         * Aquí conectaremos UsuarioController
         * para actualizar el usuario.
         */

        JOptionPane.showMessageDialog(
                this,
                "Formulario preparado para actualizar el usuario.",
                "SIGELAB",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void desactivarUsuario() {

        if (txtId.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un usuario de la tabla.",
                    "SIGELAB",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea desactivar este usuario?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        /*
         * Aquí conectaremos UsuarioController
         * para realizar la desactivación.
         */

        chkActivo.setSelected(false);

        JOptionPane.showMessageDialog(
                this,
                "Usuario preparado para desactivación.",
                "SIGELAB",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private boolean validarFormulario() {

        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (nombre.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese el nombre.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );

            txtNombre.requestFocus();
            return false;
        }

        if (apellido.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese el apellido.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );

            txtApellido.requestFocus();
            return false;
        }

        if (password.isEmpty() && txtId.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ingrese una contraseña.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );

            txtPassword.requestFocus();
            return false;
        }

        if (cmbRol.getSelectedItem() == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione un rol.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );

            cmbRol.requestFocus();
            return false;
        }

        return true;
    }

    private void cargarUsuarioSeleccionado() {

        int fila = tblUsuario.getSelectedRow();

        if (fila == -1) {
            return;
        }

        txtId.setText(
                String.valueOf(
                        modeloTabla.getValueAt(fila, 0)
                )
        );

        txtNombre.setText(
                String.valueOf(
                        modeloTabla.getValueAt(fila, 1)
                )
        );

        txtApellido.setText(
                String.valueOf(
                        modeloTabla.getValueAt(fila, 2)
                )
        );

        /*
         * La contraseña nunca se muestra en la tabla.
         */
        txtPassword.setText("");

        Object rol = modeloTabla.getValueAt(fila, 4);

        if (rol != null) {
            cmbRol.setSelectedItem(
                    rol.toString()
            );
        }

        Object activo = modeloTabla.getValueAt(fila, 5);

        if (activo instanceof Boolean) {
            chkActivo.setSelected((Boolean) activo);
        } else {
            chkActivo.setSelected(
                    "true".equalsIgnoreCase(
                            String.valueOf(activo)
                    )
                            ||
                            "1".equals(
                                    String.valueOf(activo)
                            )
            );
        }

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnDesactivar.setEnabled(true);
    }

    public void agregarUsuarioTabla(
            int id,
            String nombre,
            String apellido,
            String usuario,
            String rol,
            boolean activo
    ) {

        modeloTabla.addRow(
                new Object[]{
                        id,
                        nombre,
                        apellido,
                        usuario,
                        rol,
                        activo
                }
        );
    }

    public void limpiarTabla() {

        modeloTabla.setRowCount(0);
    }

    private void createUIComponents() {
        // Componentes personalizados, si fueran necesarios.
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new Usuario().setVisible(true);
        });
    }
}
