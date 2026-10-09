
package SIGELAB.view;

import SIGELAB.controller.EquipoController;
import SIGELAB.controller.ProductoController;
import SIGELAB.controller.LaboratorioController;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.enums.EstadoProducto;
import SIGELAB.enums.EstadoLaboratorio;

import SIGELAB.model.Equipo;
import SIGELAB.model.Producto;
import SIGELAB.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class EquipoForm {

    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigoInventario;
    private JComboBox<Producto> cmbProducto;
    private JTextField txtNumeroSerie;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtEstado;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnBuscar;
    private JButton salirButton;
    private JTable tblEquipo;
    private JTextField txtFechadeRegistro;
    private JTextArea observacionesTextArea;

    private final EquipoController equipoController;
    private final ProductoController productoController;
    private final LaboratorioController laboratorioController;

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Map<Integer, String> nombresProductos =
            new HashMap<>();

    private final Map<Integer, String> nombresLaboratorios =
            new HashMap<>();

    private DefaultTableModel modeloTabla;
    private boolean cargandoSeleccion = false;

    public EquipoForm() {

        equipoController = new EquipoController();
        productoController = new ProductoController();
        laboratorioController = new LaboratorioController();

        if (panelPrincipal == null || txtId == null ||
                txtCodigoInventario == null || cmbProducto == null ||
                txtNumeroSerie == null || cmbLaboratorio == null ||
                txtEstado == null || txtFechadeRegistro == null ||
                observacionesTextArea == null || tblEquipo == null ||
                btnNuevo == null || btnGuardar == null ||
                btnActualizar == null || btnBuscar == null ||
                salirButton == null) {

            crearInterfaz();
        }

        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarProductos();
        cargarLaboratorios();
        cargarEquipos();

        nuevo();
    }

    private void crearInterfaz() {

        panelPrincipal = new JPanel(new BorderLayout(10, 10));

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        JPanel campos = new JPanel(
                new GridLayout(0, 4, 8, 8)
        );

        txtId = new JTextField();
        txtCodigoInventario = new JTextField();
        cmbProducto = new JComboBox<>();
        txtNumeroSerie = new JTextField();
        cmbLaboratorio = new JComboBox<>();
        txtEstado = new JTextField();
        txtFechadeRegistro = new JTextField();
        observacionesTextArea = new JTextArea(3, 20);

        campos.add(new JLabel("ID:"));
        campos.add(txtId);

        campos.add(new JLabel("Código de inventario:"));
        campos.add(txtCodigoInventario);

        campos.add(new JLabel("Producto:"));
        campos.add(cmbProducto);

        campos.add(new JLabel("Número de serie:"));
        campos.add(txtNumeroSerie);

        campos.add(new JLabel("Laboratorio:"));
        campos.add(cmbLaboratorio);

        campos.add(new JLabel("Estado:"));
        campos.add(txtEstado);

        campos.add(new JLabel("Fecha de registro:"));
        campos.add(txtFechadeRegistro);

        campos.add(new JLabel("Observaciones:"));
        campos.add(new JScrollPane(observacionesTextArea));

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 5)
        );

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnBuscar = new JButton("Buscar");
        salirButton = new JButton("Salir");

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnBuscar);
        botones.add(salirButton);

        JPanel superior = new JPanel(
                new BorderLayout(5, 8)
        );

        superior.add(
                new JLabel(
                        "Gestión de Equipos",
                        SwingConstants.CENTER
                ),
                BorderLayout.NORTH
        );

        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tblEquipo = new JTable();

        panelPrincipal.add(superior, BorderLayout.NORTH);

        panelPrincipal.add(
                new JScrollPane(tblEquipo),
                BorderLayout.CENTER
        );

        panelPrincipal.setPreferredSize(
                new Dimension(1100, 620)
        );
    }

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtCodigoInventario.setEditable(false);
        txtEstado.setEditable(false);
        txtFechadeRegistro.setEditable(false);

        txtCodigoInventario.setText(
                "Automático al guardar"
        );

        txtEstado.setText(
                EstadoEquipo.DISPONIBLE.name()
        );

        txtFechadeRegistro.setText(
                formatearFecha(
                        LocalDateTime.now(ZONA_GUATEMALA)
                )
        );

        observacionesTextArea.setLineWrap(true);
        observacionesTextArea.setWrapStyleWord(true);
        observacionesTextArea.setRows(3);

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    private void configurarTabla() {

        modeloTabla = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código inventario",
                        "Producto",
                        "Número de serie",
                        "Laboratorio",
                        "Estado",
                        "Observaciones",
                        "Fecha registro"
                },
                0
        ) {

            @Override
            public boolean isCellEditable(
                    int row,
                    int column
            ) {
                return false;
            }
        };

        tblEquipo.setModel(modeloTabla);

        tblEquipo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblEquipo.setRowHeight(25);
        tblEquipo.setAutoCreateRowSorter(true);

        tblEquipo.getTableHeader()
                .setReorderingAllowed(false);
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

        btnBuscar.addActionListener(
                e -> buscar()
        );

        salirButton.addActionListener(
                e -> salir()
        );

        tblEquipo.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()
                            && !cargandoSeleccion) {

                        seleccionarEquipo();
                    }
                });
    }

    private void cargarProductos() {

        try {

            cmbProducto.removeAllItems();
            nombresProductos.clear();

            List<Producto> productos =
                    productoController.listar();

            for (Producto producto : productos) {

                nombresProductos.put(
                        producto.getIdProducto(),
                        producto.getCodigoProducto()
                                + " - "
                                + producto.getNombre()
                );

                if (producto.getEstado()
                        == EstadoProducto.ACTIVO) {

                    cmbProducto.addItem(producto);
                }
            }

            cmbProducto.setSelectedIndex(-1);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los productos.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarLaboratorios() {

        try {

            cmbLaboratorio.removeAllItems();
            nombresLaboratorios.clear();

            List<Laboratorio> laboratorios =
                    laboratorioController.listar();

            for (Laboratorio laboratorio : laboratorios) {

                nombresLaboratorios.put(
                        laboratorio.getIdLaboratorio(),
                        laboratorio.toString()
                );

                if (laboratorio.getEstado()
                        == EstadoLaboratorio.ACTIVO) {

                    cmbLaboratorio.addItem(laboratorio);
                }
            }

            cmbLaboratorio.setSelectedIndex(-1);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los laboratorios.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarEquipos() {

        try {

            List<Equipo> equipos =
                    equipoController.listar();

            cargarTabla(equipos);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los equipos.\n"
                            + ex.getMessage()
            );
        }
    }

    private String obtenerNombreProducto(int idProducto) {

        return nombresProductos.getOrDefault(
                idProducto,
                String.valueOf(idProducto)
        );
    }

    private String obtenerNombreLaboratorio(int idLaboratorio) {

        return nombresLaboratorios.getOrDefault(
                idLaboratorio,
                String.valueOf(idLaboratorio)
        );
    }

    private void cargarTabla(List<Equipo> equipos) {

        cargandoSeleccion = true;

        try {

            modeloTabla.setRowCount(0);

            for (Equipo equipo : equipos) {

                modeloTabla.addRow(
                        new Object[]{
                                equipo.getIdEquipo(),
                                equipo.getCodigoInventario(),

                                obtenerNombreProducto(
                                        equipo.getIdProducto()
                                ),

                                equipo.getNumeroSerie(),

                                obtenerNombreLaboratorio(
                                        equipo.getIdLaboratorio()
                                ),

                                equipo.getEstado(),
                                equipo.getObservaciones(),

                                formatearFecha(
                                        equipo.getFechaRegistro()
                                )
                        }
                );
            }

        } finally {

            cargandoSeleccion = false;
        }
    }

    private String formatearFecha(
            LocalDateTime fecha
    ) {

        if (fecha == null) {
            return "";
        }

        return fecha.format(FORMATO_FECHA);
    }

    private void limpiarCampos() {

        txtId.setText("");

        txtCodigoInventario.setText(
                "Automático al guardar"
        );

        txtNumeroSerie.setText("");

        txtEstado.setText(
                EstadoEquipo.DISPONIBLE.name()
        );

        txtFechadeRegistro.setText(
                formatearFecha(
                        LocalDateTime.now(ZONA_GUATEMALA)
                )
        );

        observacionesTextArea.setText("");

        cmbProducto.setEnabled(true);
        cmbLaboratorio.setEnabled(true);

        if (cmbProducto.getItemCount() > 0) {
            cmbProducto.setSelectedIndex(0);
        } else {
            cmbProducto.setSelectedIndex(-1);
        }

        if (cmbLaboratorio.getItemCount() > 0) {
            cmbLaboratorio.setSelectedIndex(0);
        } else {
            cmbLaboratorio.setSelectedIndex(-1);
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    private void nuevo() {

        cargandoSeleccion = true;

        try {

            tblEquipo.clearSelection();

        } finally {

            cargandoSeleccion = false;
        }

        limpiarCampos();

        txtNumeroSerie.requestFocusInWindow();
    }

    private void guardar() {

        if (!txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Presione Nuevo antes de guardar otro equipo."
            );

            return;
        }

        try {

            Equipo equipo = obtenerDatosFormulario();

            Equipo guardado =
                    equipoController.guardar(equipo);

            txtId.setText(
                    String.valueOf(
                            guardado.getIdEquipo()
                    )
            );

            txtCodigoInventario.setText(
                    guardado.getCodigoInventario()
            );

            txtEstado.setText(
                    guardado.getEstado().name()
            );

            txtFechadeRegistro.setText(
                    formatearFecha(
                            guardado.getFechaRegistro()
                    )
            );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Equipo guardado correctamente.\n"
                            + "ID: "
                            + guardado.getIdEquipo()
                            + "\nCódigo de inventario: "
                            + guardado.getCodigoInventario()
                            + "\nEstado: "
                            + guardado.getEstado().name()
                            + "\nFecha de registro: "
                            + formatearFecha(
                            guardado.getFechaRegistro()
                    ),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarEquipos();
            nuevo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Seleccione un equipo de la tabla."
            );

            return;
        }

        try {

            int idEquipo = Integer.parseInt(
                    txtId.getText().trim()
            );

            Equipo existente =
                    equipoController.buscarPorId(idEquipo)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El equipo seleccionado no existe."
                                    )
                            );

            if (existente.getEstado()
                    != EstadoEquipo.DISPONIBLE) {

                mostrarAdvertencia(
                        "Solo se pueden actualizar equipos DISPONIBLES."
                );

                return;
            }

            Equipo equipo = obtenerDatosFormulario();

            equipo.setIdEquipo(idEquipo);

            equipo.setCodigoInventario(
                    existente.getCodigoInventario()
            );

            equipo.setIdLaboratorio(
                    existente.getIdLaboratorio()
            );

            equipo.setEstado(
                    existente.getEstado()
            );

            boolean actualizado =
                    equipoController.actualizar(equipo);

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Equipo actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarEquipos();
                nuevo();

            } else {

                mostrarAdvertencia(
                        "No se pudo actualizar el equipo."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertencia(
                    ex.getMessage()
            );

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private Equipo obtenerDatosFormulario() {

        Producto producto =
                (Producto) cmbProducto.getSelectedItem();

        Laboratorio laboratorio =
                (Laboratorio) cmbLaboratorio.getSelectedItem();

        String codigo =
                txtId.getText().trim().isEmpty()
                        ? null
                        : txtCodigoInventario.getText().trim();

        String serie =
                txtNumeroSerie.getText().trim();

        String observaciones =
                observacionesTextArea.getText().trim();

        if (producto == null) {

            throw new IllegalArgumentException(
                    "Seleccione un producto activo."
            );
        }

        if (serie.isEmpty()) {

            throw new IllegalArgumentException(
                    "Ingrese el número de serie."
            );
        }

        if (serie.length() > 60) {

            throw new IllegalArgumentException(
                    "El número de serie no puede superar 60 caracteres."
            );
        }

        if (laboratorio == null) {

            throw new IllegalArgumentException(
                    "Seleccione un laboratorio activo."
            );
        }

        if (observaciones.length() > 300) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar 300 caracteres."
            );
        }

        return new Equipo(
                codigo,
                producto.getIdProducto(),
                serie,
                laboratorio.getIdLaboratorio(),
                EstadoEquipo.DISPONIBLE,
                observaciones
        );
    }

    private void buscar() {

        String texto = JOptionPane.showInputDialog(
                panelPrincipal,
                "Ingrese código, número de serie u observación:",
                "Buscar equipo",
                JOptionPane.QUESTION_MESSAGE
        );

        if (texto == null) {
            return;
        }

        try {

            List<Equipo> equipos =
                    equipoController.buscar(texto.trim());

            nuevo();
            cargarTabla(equipos);

            if (equipos.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron equipos.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    private void seleccionarEquipo() {

        int fila = tblEquipo.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo =
                tblEquipo.convertRowIndexToModel(fila);

        int idEquipo =
                (int) modeloTabla.getValueAt(filaModelo, 0);

        try {

            Equipo equipo =
                    equipoController.buscarPorId(idEquipo)
                            .orElse(null);

            if (equipo == null) {

                mostrarAdvertencia(
                        "El equipo seleccionado ya no existe."
                );

                return;
            }

            txtId.setText(
                    String.valueOf(
                            equipo.getIdEquipo()
                    )
            );

            txtCodigoInventario.setText(
                    equipo.getCodigoInventario()
            );

            txtNumeroSerie.setText(
                    equipo.getNumeroSerie()
            );

            txtEstado.setText(
                    equipo.getEstado().name()
            );

            txtFechadeRegistro.setText(
                    formatearFecha(
                            equipo.getFechaRegistro()
                    )
            );

            observacionesTextArea.setText(
                    equipo.getObservaciones() == null
                            ? ""
                            : equipo.getObservaciones()
            );

            seleccionarProducto(
                    equipo.getIdProducto()
            );

            seleccionarLaboratorio(
                    equipo.getIdLaboratorio()
            );

            cmbLaboratorio.setEnabled(false);

            boolean disponible =
                    equipo.getEstado()
                            == EstadoEquipo.DISPONIBLE;

            btnGuardar.setEnabled(false);
            btnActualizar.setEnabled(disponible);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo consultar el equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void seleccionarProducto(int idProducto) {

        for (int i = 0;
             i < cmbProducto.getItemCount();
             i++) {

            Producto producto =
                    cmbProducto.getItemAt(i);

            if (producto != null
                    && producto.getIdProducto() == idProducto) {

                cmbProducto.setSelectedIndex(i);
                return;
            }
        }

        cmbProducto.setSelectedIndex(-1);
    }

    private void seleccionarLaboratorio(int idLaboratorio) {

        for (int i = 0;
             i < cmbLaboratorio.getItemCount();
             i++) {

            Laboratorio laboratorio =
                    cmbLaboratorio.getItemAt(i);

            if (laboratorio != null
                    && laboratorio.getIdLaboratorio()
                    == idLaboratorio) {

                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }

        cmbLaboratorio.setSelectedIndex(-1);
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

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}

