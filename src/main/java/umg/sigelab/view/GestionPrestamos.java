package umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GestionPrestamos extends JFrame {

    private JTextField txtBuscarSolicitud;
    private JComboBox<String> cmbEstado;
    private JButton btnBuscar;
    private JButton btnActualizar;

    private JTable tblPrestamos;
    private JPanel panelSolicitudes;
    private JScrollPane scrollSolicitudes;
    private JLabel lblSolicitudes;

    private JTextField txtSolicitante;
    private JTextField txtDiasPrestamo;

    private JTable tblDetalleEquipos;
    private JScrollPane scrollDetalleEquipos;

    private JTextArea txtObservaciones;
    private JScrollPane scrollObservaciones;

    private JButton btnAprobar;
    private JButton btnRechazar;
    private JButton btnDespachar;
    private JButton btnSalir;

    private DefaultTableModel modeloPrestamos;
    private DefaultTableModel modeloDetalle;

    private final List<SolicitudDemo> solicitudes = new ArrayList<>();

    public GestionPrestamos() {
        setTitle("SIGELAB - Gestión de Préstamos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 750);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);

        construirInterfaz();
        configurarTablas();
        cargarDatosDemostrativos();
        configurarEventos();
        cargarSolicitudes();
    }

    private void construirInterfaz() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        setContentPane(panelPrincipal);

        JLabel lblTitulo = new JLabel("Gestión de Préstamos");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));

        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(
                new BoxLayout(panelContenido, BoxLayout.Y_AXIS)
        );

        JScrollPane scrollPrincipal = new JScrollPane(panelContenido);
        scrollPrincipal.setBorder(null);

        panelPrincipal.add(scrollPrincipal, BorderLayout.CENTER);

        // ======================================
        // BUSCADOR Y FILTROS
        // ======================================

        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBorder(
                BorderFactory.createTitledBorder("Buscar solicitudes")
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblBuscar = new JLabel("Buscar solicitud");
        JLabel lblEstado = new JLabel("Estado");

        txtBuscarSolicitud = new JTextField(18);

        cmbEstado = new JComboBox<>(new String[]{
                "TODOS",
                "PENDIENTE",
                "EN_TRAMITE",
                "DESPACHADO",
                "FINALIZADO",
                "RECHAZADO",
                "CANCELADO"
        });

        btnBuscar = new JButton("Buscar");
        btnActualizar = new JButton("Actualizar");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        panelFiltros.add(lblBuscar, gbc);

        gbc.gridx = 1;
        panelFiltros.add(lblEstado, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panelFiltros.add(txtBuscarSolicitud, gbc);

        gbc.gridx = 1;
        panelFiltros.add(cmbEstado, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panelFiltros.add(btnBuscar, gbc);

        gbc.gridx = 3;
        panelFiltros.add(btnActualizar, gbc);

        panelFiltros.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 110)
        );

        panelContenido.add(panelFiltros);
        panelContenido.add(Box.createVerticalStrut(10));

        // ======================================
        // SOLICITUDES REGISTRADAS
        // ======================================

        panelSolicitudes = new JPanel(new BorderLayout(5, 5));
        panelSolicitudes.setBorder(
                BorderFactory.createTitledBorder("Solicitudes registradas")
        );

        lblSolicitudes = new JLabel(
                "Seleccione una solicitud para consultar sus detalles"
        );

        panelSolicitudes.add(lblSolicitudes, BorderLayout.NORTH);

        tblPrestamos = new JTable();
        scrollSolicitudes = new JScrollPane(tblPrestamos);

        panelSolicitudes.add(
                scrollSolicitudes,
                BorderLayout.CENTER
        );

        panelSolicitudes.setPreferredSize(new Dimension(900, 230));
        panelSolicitudes.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 230)
        );

        panelContenido.add(panelSolicitudes);
        panelContenido.add(Box.createVerticalStrut(10));

        // ======================================
        // DETALLE DEL PRÉSTAMO
        // ======================================

        JPanel panelDetalle = new JPanel(new BorderLayout(10, 10));

        panelDetalle.setBorder(
                BorderFactory.createTitledBorder(
                        "Detalle del préstamo seleccionado"
                )
        );

        JPanel panelDatos = new JPanel(new GridLayout(2, 2, 10, 5));

        JLabel lblSolicitante = new JLabel("Solicitante");
        JLabel lblDias = new JLabel("Días solicitados");

        txtSolicitante = new JTextField();
        txtDiasPrestamo = new JTextField();

        txtSolicitante.setEditable(false);
        txtDiasPrestamo.setEditable(false);

        panelDatos.add(lblSolicitante);
        panelDatos.add(lblDias);
        panelDatos.add(txtSolicitante);
        panelDatos.add(txtDiasPrestamo);

        panelDetalle.add(panelDatos, BorderLayout.NORTH);

        JPanel panelEquipos = new JPanel(new BorderLayout(5, 5));

        JLabel lblEquipos = new JLabel("Equipos solicitados");

        tblDetalleEquipos = new JTable();
        scrollDetalleEquipos = new JScrollPane(tblDetalleEquipos);

        panelEquipos.add(lblEquipos, BorderLayout.NORTH);
        panelEquipos.add(scrollDetalleEquipos, BorderLayout.CENTER);

        panelDetalle.add(panelEquipos, BorderLayout.CENTER);

        JPanel panelObservaciones = new JPanel(new BorderLayout(5, 5));

        JLabel lblObservaciones = new JLabel(
                "Motivo de rechazo / Observaciones"
        );

        txtObservaciones = new JTextArea(3, 20);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        scrollObservaciones = new JScrollPane(txtObservaciones);

        panelObservaciones.add(
                lblObservaciones,
                BorderLayout.NORTH
        );

        panelObservaciones.add(
                scrollObservaciones,
                BorderLayout.CENTER
        );

        panelDetalle.add(panelObservaciones, BorderLayout.SOUTH);

        panelDetalle.setPreferredSize(new Dimension(900, 350));
        panelDetalle.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 350)
        );

        panelContenido.add(panelDetalle);
        panelContenido.add(Box.createVerticalStrut(10));

        // ======================================
        // BOTONES DE GESTIÓN
        // ======================================

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnAprobar = new JButton("Aprobar");
        btnRechazar = new JButton("Rechazar");
        btnDespachar = new JButton("Despachar");
        btnSalir = new JButton("Salir");

        panelBotones.add(btnAprobar);
        panelBotones.add(btnRechazar);
        panelBotones.add(btnDespachar);
        panelBotones.add(btnSalir);

        panelBotones.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 60)
        );

        panelContenido.add(panelBotones);
    }

    private void configurarTablas() {
        modeloPrestamos = new DefaultTableModel(
                new Object[]{
                        "N.º",
                        "Solicitante",
                        "Laboratorio",
                        "Estado"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblPrestamos.setModel(modeloPrestamos);
        tblPrestamos.setRowHeight(28);
        tblPrestamos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        modeloDetalle = new DefaultTableModel(
                new Object[]{
                        "Código",
                        "Equipo",
                        "Estado"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblDetalleEquipos.setModel(modeloDetalle);
        tblDetalleEquipos.setRowHeight(28);
    }

    private void cargarDatosDemostrativos() {
        solicitudes.add(new SolicitudDemo(
                1,
                "Estudiante",
                "Redes",
                5,
                "PENDIENTE",
                "INV-0001",
                "Router",
                "DISPONIBLE"
        ));

        solicitudes.add(new SolicitudDemo(
                2,
                "Docente",
                "Computación",
                3,
                "EN_TRAMITE",
                "INV-0002",
                "Computadora",
                "DISPONIBLE"
        ));
    }

    private void cargarSolicitudes() {
        modeloPrestamos.setRowCount(0);

        String busqueda = txtBuscarSolicitud.getText()
                .trim()
                .toLowerCase();

        String filtroEstado = String.valueOf(
                cmbEstado.getSelectedItem()
        );

        for (SolicitudDemo solicitud : solicitudes) {

            boolean coincideBusqueda =
                    String.valueOf(solicitud.id).contains(busqueda)
                            || solicitud.solicitante
                            .toLowerCase()
                            .contains(busqueda);

            boolean coincideEstado =
                    "TODOS".equals(filtroEstado)
                            || solicitud.estado.equals(filtroEstado);

            if (coincideBusqueda && coincideEstado) {
                modeloPrestamos.addRow(new Object[]{
                        String.format("%03d", solicitud.id),
                        solicitud.solicitante,
                        solicitud.laboratorio,
                        solicitud.estado
                });
            }
        }

        limpiarDetalle();
    }

    private void configurarEventos() {
        btnBuscar.addActionListener(e -> cargarSolicitudes());

        btnActualizar.addActionListener(e -> {
            txtBuscarSolicitud.setText("");
            cmbEstado.setSelectedItem("TODOS");
            cargarSolicitudes();
        });

        tblPrestamos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalleSeleccionado();
            }
        });

        btnAprobar.addActionListener(e ->
                gestionarAccion("APROBAR")
        );

        btnRechazar.addActionListener(e ->
                gestionarAccion("RECHAZAR")
        );

        btnDespachar.addActionListener(e ->
                gestionarAccion("DESPACHAR")
        );

        btnSalir.addActionListener(e -> dispose());
    }

    private void mostrarDetalleSeleccionado() {
        int fila = tblPrestamos.getSelectedRow();

        if (fila < 0) {
            limpiarDetalle();
            return;
        }

        int filaModelo = tblPrestamos.convertRowIndexToModel(fila);

        String numero = String.valueOf(
                modeloPrestamos.getValueAt(filaModelo, 0)
        );

        int id = Integer.parseInt(numero);

        for (SolicitudDemo solicitud : solicitudes) {
            if (solicitud.id == id) {

                txtSolicitante.setText(solicitud.solicitante);
                txtDiasPrestamo.setText(
                        String.valueOf(solicitud.dias)
                );

                modeloDetalle.setRowCount(0);

                modeloDetalle.addRow(new Object[]{
                        solicitud.codigoEquipo,
                        solicitud.nombreEquipo,
                        solicitud.estadoEquipo
                });

                txtObservaciones.setText("");
                return;
            }
        }

        limpiarDetalle();
    }

    private void limpiarDetalle() {
        txtSolicitante.setText("");
        txtDiasPrestamo.setText("");
        txtObservaciones.setText("");
        modeloDetalle.setRowCount(0);
    }

    private void gestionarAccion(String accion) {
        int fila = tblPrestamos.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Seleccione primero una solicitud.",
                    "SIGELAB",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String numero = String.valueOf(
                tblPrestamos.getValueAt(fila, 0)
        );

        JOptionPane.showMessageDialog(
                this,
                "Solicitud: " + numero
                        + "\nAcción seleccionada: " + accion
                        + "\n\nEsta es una demostración visual."
                        + "\nTodavía no se han modificado datos en SQL Server.",
                "SIGELAB - Gestión de Préstamos",
                JOptionPane.INFORMATION_MESSAGE
        );
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(13, 7, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Gestion de Prestamos ");
        panel1.add(label1, new GridConstraints(0, 0, 1, 7, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Buscar Solicitud");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtBuscarSolicitud = new JTextField();
        panel1.add(txtBuscarSolicitud, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panel1.add(btnBuscar, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbEstado = new JComboBox();
        panel1.add(cmbEstado, new GridConstraints(2, 1, 1, 5, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panel1.add(btnActualizar, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Estado");
        panel1.add(label3, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panelSolicitudes = new JPanel();
        panelSolicitudes.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panelSolicitudes, new GridConstraints(5, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollSolicitudes = new JScrollPane();
        panelSolicitudes.add(scrollSolicitudes, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblPrestamos = new JTable();
        scrollSolicitudes.setViewportView(tblPrestamos);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelSolicitudes.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Detalle del Prestamo");
        panel2.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(4, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label5 = new JLabel();
        label5.setText("Solicitudes Registradas");
        panel3.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Solicitante");
        panel1.add(label6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtSolicitante = new JTextField();
        panel1.add(txtSolicitante, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Dias ");
        panel1.add(label7, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtDiasPrestamo = new JTextField();
        panel1.add(txtDiasPrestamo, new GridConstraints(7, 3, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Equipos Solicitados ");
        panel1.add(label8, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollDetalleEquipos = new JScrollPane();
        panel1.add(scrollDetalleEquipos, new GridConstraints(9, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblDetalleEquipos = new JTable();
        scrollDetalleEquipos.setViewportView(tblDetalleEquipos);
        final JLabel label9 = new JLabel();
        label9.setText("Observaciones ");
        panel1.add(label9, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollObservaciones = new JScrollPane();
        panel1.add(scrollObservaciones, new GridConstraints(11, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        txtObservaciones = new JTextArea();
        scrollObservaciones.setViewportView(txtObservaciones);
        btnAprobar = new JButton();
        btnAprobar.setText("Aprobar ");
        panel1.add(btnAprobar, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRechazar = new JButton();
        btnRechazar.setText("Rechazar");
        panel1.add(btnRechazar, new GridConstraints(12, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDespachar = new JButton();
        btnDespachar.setText("Despachar");
        panel1.add(btnDespachar, new GridConstraints(12, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnSalir = new JButton();
        btnSalir.setText("Salir");
        panel1.add(btnSalir, new GridConstraints(12, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private static class SolicitudDemo {
        private final int id;
        private final String solicitante;
        private final String laboratorio;
        private final int dias;
        private final String estado;
        private final String codigoEquipo;
        private final String nombreEquipo;
        private final String estadoEquipo;

        private SolicitudDemo(
                int id,
                String solicitante,
                String laboratorio,
                int dias,
                String estado,
                String codigoEquipo,
                String nombreEquipo,
                String estadoEquipo
        ) {
            this.id = id;
            this.solicitante = solicitante;
            this.laboratorio = laboratorio;
            this.dias = dias;
            this.estado = estado;
            this.codigoEquipo = codigoEquipo;
            this.nombreEquipo = nombreEquipo;
            this.estadoEquipo = estadoEquipo;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GestionPrestamos ventana = new GestionPrestamos();
            ventana.setVisible(true);
        });
    }
}