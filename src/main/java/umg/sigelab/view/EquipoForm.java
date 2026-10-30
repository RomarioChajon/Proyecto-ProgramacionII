package umg.sigelab.view;

import umg.sigelab.controller.EquipoController;
import umg.sigelab.controller.ProductoController;
import umg.sigelab.controller.LaboratorioAdminController;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.EstadoProducto;
import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.model.Equipo;
import umg.sigelab.model.Producto;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.qr.QrGenerator;

import com.google.zxing.WriterException;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EquipoForm {

    private static final Color CELESTE_PRINCIPAL = new Color(0x2B8FD6);
    private static final Color CELESTE_MEDIO = new Color(0x63B3ED);
    private static final Color CELESTE_SUAVE = new Color(0xBEE3F8);
    private static final Color BLANCO_HIELO = new Color(0xF0F8FF);
    private static final Color BLANCO_PURO = new Color(0xFFFFFF);
    private static final Color TEXTO_OSCURO = new Color(0x1A202C);

    private JPanel panelPrincipal;
    private JTextField txtId;
    private JTextField txtCodigoInventario;
    private JComboBox<Producto> cmbProducto;
    private JTextField txtNumeroSerie;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtEstado;
    private JTextField txtFechadeRegistro;
    private JTextArea observacionesTextArea;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnBuscar;
    private JButton btnMostrarQR;
    private JButton btnSalir;

    private JTable tblEquipo;

    private final EquipoController equipoController;
    private final ProductoController productoController;
    private final LaboratorioAdminController laboratorioController;

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Map<Integer, String> nombresProductos = new HashMap<>();
    private final Map<Integer, String> nombresLaboratorios = new HashMap<>();

    private DefaultTableModel modeloTabla;
    private boolean cargandoSeleccion = false;

    public EquipoForm() {
        equipoController = new EquipoController();
        productoController = new ProductoController();
        laboratorioController = new LaboratorioAdminController();

        if (!componentesDisponibles()) {
            crearInterfaz();
        }

        if (btnMostrarQR == null) {
            agregarBotonQRAlternativo();
        }

        configurarFormulario();
        configurarTabla();
        configurarEventos();
        configurarQR();
        aplicarTemaCeleste();

        cargarProductos();
        cargarLaboratorios();
        cargarEquipos();
        nuevo();
    }

    private boolean componentesDisponibles() {
        return panelPrincipal != null
                && txtId != null
                && txtCodigoInventario != null
                && cmbProducto != null
                && txtNumeroSerie != null
                && cmbLaboratorio != null
                && txtEstado != null
                && txtFechadeRegistro != null
                && observacionesTextArea != null
                && tblEquipo != null
                && btnNuevo != null
                && btnGuardar != null
                && btnActualizar != null
                && btnBuscar != null
                && btnSalir != null;
    }

    private void crearInterfaz() {
        panelPrincipal = new JPanel(new BorderLayout(12, 12));
        panelPrincipal.setBackground(BLANCO_HIELO);
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );

        JPanel campos = new JPanel(new GridLayout(0, 4, 8, 8));
        campos.setBackground(BLANCO_PURO);
        campos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_SUAVE, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

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

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnBuscar = new JButton("Buscar");
        btnMostrarQR = new JButton("Ver QR");
        btnSalir = new JButton("Salir");

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 6)
        );
        botones.setBackground(BLANCO_HIELO);

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnBuscar);
        botones.add(btnMostrarQR);
        botones.add(btnSalir);

        JPanel superior = new JPanel(new BorderLayout(8, 10));
        superior.setBackground(BLANCO_HIELO);

        JLabel titulo = new JLabel("Inventario de Equipos y Códigos QR", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(CELESTE_PRINCIPAL);

        superior.add(titulo, BorderLayout.NORTH);
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tblEquipo = new JTable();
        JScrollPane scroll = new JScrollPane(tblEquipo);
        scroll.getViewport().setBackground(BLANCO_PURO);
        scroll.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(scroll, BorderLayout.CENTER);

        panelPrincipal.setPreferredSize(new Dimension(1100, 640));
    }

    private void agregarBotonQRAlternativo() {
        btnMostrarQR = new JButton("Ver QR");

        JPanel barra = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        barra.setBackground(BLANCO_HIELO);
        barra.add(btnMostrarQR);

        JPanel original = panelPrincipal;
        panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(BLANCO_HIELO);
        panelPrincipal.add(original, BorderLayout.CENTER);
        panelPrincipal.add(barra, BorderLayout.SOUTH);
    }

    private void aplicarTemaCeleste() {
        panelPrincipal.setBackground(BLANCO_HIELO);
        panelPrincipal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, CELESTE_PRINCIPAL),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        estilizarContenedor(panelPrincipal);

        estilizarCampoTexto(txtId, false);
        estilizarCampoTexto(txtCodigoInventario, true);
        estilizarCampoTexto(txtNumeroSerie, true);
        estilizarCampoTexto(txtEstado, false);
        estilizarCampoTexto(txtFechadeRegistro, false);

        estilizarCombo(cmbProducto);
        estilizarCombo(cmbLaboratorio);

        if (observacionesTextArea != null) {
            observacionesTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            observacionesTextArea.setForeground(TEXTO_OSCURO);
            observacionesTextArea.setBackground(BLANCO_PURO);
            observacionesTextArea.setCaretColor(CELESTE_PRINCIPAL);
            observacionesTextArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }

        estilizarBoton(btnGuardar, CELESTE_PRINCIPAL, BLANCO_PURO);
        estilizarBoton(btnActualizar, CELESTE_PRINCIPAL, BLANCO_PURO);
        estilizarBoton(btnBuscar, CELESTE_PRINCIPAL, BLANCO_PURO);
        estilizarBoton(btnMostrarQR, CELESTE_PRINCIPAL, BLANCO_PURO);
        estilizarBoton(btnNuevo, CELESTE_MEDIO, TEXTO_OSCURO);
        estilizarBoton(btnSalir, BLANCO_PURO, TEXTO_OSCURO);

        estilizarTabla(tblEquipo);
    }

    private void estilizarContenedor(Component comp) {
        if (comp instanceof JLabel label) {
            String texto = label.getText() != null ? label.getText().trim() : "";
            if (texto.toLowerCase(Locale.ROOT).contains("gesti") || texto.toLowerCase(Locale.ROOT).contains("inventario")) {
                label.setFont(new Font("Segoe UI", Font.BOLD, 20));
                label.setForeground(CELESTE_PRINCIPAL);
            } else {
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setForeground(TEXTO_OSCURO);
            }
        } else if (comp instanceof JScrollPane scroll) {
            scroll.getViewport().setBackground(BLANCO_PURO);
            scroll.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
        }
        if (comp instanceof Container container) {
            for (Component hijo : container.getComponents()) {
                estilizarContenedor(hijo);
            }
        }
    }

    private void estilizarCampoTexto(JTextField campo, boolean editable) {
        if (campo == null) {
            return;
        }
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setForeground(TEXTO_OSCURO);
        campo.setBackground(editable ? BLANCO_PURO : BLANCO_HIELO);
        campo.setCaretColor(CELESTE_PRINCIPAL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void estilizarCombo(JComboBox<?> combo) {
        if (combo == null) {
            return;
        }
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXTO_OSCURO);
        combo.setBackground(BLANCO_PURO);
        combo.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto) {
        if (boton == null) {
            return;
        }
        boton.setUI(new BasicButtonUI());
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_PRINCIPAL, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        Color hoverFondo = fondo.equals(CELESTE_PRINCIPAL) ? CELESTE_MEDIO : CELESTE_SUAVE;
        Color hoverTexto = fondo.equals(CELESTE_PRINCIPAL) ? BLANCO_PURO : TEXTO_OSCURO;
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(hoverFondo);
                    boton.setForeground(hoverTexto);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
                boton.setForeground(texto);
            }
        });
    }

    private void estilizarTabla(JTable tabla) {
        if (tabla == null) {
            return;
        }
        tabla.setBackground(BLANCO_PURO);
        tabla.setForeground(TEXTO_OSCURO);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setGridColor(CELESTE_SUAVE);
        tabla.setSelectionBackground(CELESTE_SUAVE);
        tabla.setSelectionForeground(TEXTO_OSCURO);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(CELESTE_PRINCIPAL);
                lbl.setForeground(BLANCO_PURO);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, CELESTE_MEDIO),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        });
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        // Código editable: vacío al crear = se genera automáticamente (INV-0001...).
        txtCodigoInventario.setEditable(true);
        txtCodigoInventario.setToolTipText(
                "Puede escribirlo o dejarlo vacío para generarlo automáticamente. "
                        + "Si lo cambia, vuelva a generar el QR.");
        txtEstado.setEditable(false);
        txtFechadeRegistro.setEditable(false);

        txtCodigoInventario.setText("");
        txtEstado.setText(EstadoEquipo.DISPONIBLE.name());
        txtFechadeRegistro.setText(
                formatearFecha(LocalDateTime.now(ZONA_GUATEMALA))
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
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tblEquipo.setModel(modeloTabla);
        tblEquipo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEquipo.setRowHeight(26);
        tblEquipo.setAutoCreateRowSorter(true);
        tblEquipo.setFillsViewportHeight(true);
        tblEquipo.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnBuscar.addActionListener(e -> buscar());
        btnSalir.addActionListener(e -> salir());

        tblEquipo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoSeleccion) {
                seleccionarEquipo();
            }
        });
    }

    private void configurarQR() {
        btnMostrarQR.setText("Ver QR");
        btnMostrarQR.setEnabled(false);
        btnMostrarQR.setToolTipText(
                "Ver y guardar el QR con toda la información del equipo."
        );
        btnMostrarQR.addActionListener(e -> mostrarQRSeleccionado());
    }

    private void cargarProductos() {
        try {
            cmbProducto.removeAllItems();
            nombresProductos.clear();

            List<Producto> productos = productoController.listar();

            for (Producto producto : productos) {
                nombresProductos.put(
                        producto.getIdProducto(),
                        producto.getCodigoProducto() + " - " + producto.getNombre()
                );

                if (producto.getEstado() == EstadoProducto.ACTIVO) {
                    cmbProducto.addItem(producto);
                }
            }

            cmbProducto.setSelectedIndex(-1);

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los productos.\n" + ex.getMessage()
            );
        }
    }

    private void cargarLaboratorios() {
        try {
            cmbLaboratorio.removeAllItems();
            nombresLaboratorios.clear();

            List<Laboratorio> laboratorios = laboratorioController.listar();

            for (Laboratorio laboratorio : laboratorios) {
                nombresLaboratorios.put(
                        laboratorio.getIdLaboratorio(),
                        laboratorio.toString()
                );

                if (laboratorio.getEstado() == EstadoRegistro.ACTIVO) {
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
            cargarTabla(equipoController.listar());
        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los equipos.\n" + ex.getMessage()
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
                modeloTabla.addRow(new Object[]{
                        equipo.getIdEquipo(),
                        equipo.getCodigoInventario(),
                        obtenerNombreProducto(equipo.getIdProducto()),
                        equipo.getNumeroSerie(),
                        obtenerNombreLaboratorio(equipo.getIdLaboratorio()),
                        equipo.getEstado().name(),
                        equipo.getObservaciones(),
                        formatearFecha(equipo.getFechaRegistro())
                });
            }
        } finally {
            cargandoSeleccion = false;
        }
    }

    private String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtCodigoInventario.setText("");
        txtNumeroSerie.setText("");
        txtEstado.setText(EstadoEquipo.DISPONIBLE.name());
        txtFechadeRegistro.setText(
                formatearFecha(LocalDateTime.now(ZONA_GUATEMALA))
        );
        observacionesTextArea.setText("");

        cmbProducto.setEnabled(true);
        cmbLaboratorio.setEnabled(true);

        cmbProducto.setSelectedIndex(
                cmbProducto.getItemCount() > 0 ? 0 : -1
        );
        cmbLaboratorio.setSelectedIndex(
                cmbLaboratorio.getItemCount() > 0 ? 0 : -1
        );

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnMostrarQR.setEnabled(false);
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
            Equipo guardado = equipoController.guardar(equipo);

            txtId.setText(String.valueOf(guardado.getIdEquipo()));
            txtCodigoInventario.setText(guardado.getCodigoInventario());
            txtEstado.setText(guardado.getEstado().name());
            txtFechadeRegistro.setText(
                    formatearFecha(guardado.getFechaRegistro())
            );

            String avisoQR;

            try {
                Path archivo = generarArchivoQR(guardado);
                avisoQR = "\n\nQR generado correctamente:\n"
                        + archivo.toAbsolutePath();

            } catch (WriterException | IOException
                     | IllegalArgumentException exQR) {
                avisoQR = "\n\nEl equipo se guardó, pero no se pudo "
                        + "generar el QR:\n" + exQR.getMessage();
            }

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Equipo guardado correctamente.\n"
                            + "ID: " + guardado.getIdEquipo()
                            + "\nCódigo: " + guardado.getCodigoInventario()
                            + "\nEstado: " + guardado.getEstado()
                            + avisoQR,
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarEquipos();
            nuevo();

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo guardar el equipo.\n" + ex.getMessage()
            );
        }
    }

    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia("Seleccione un equipo de la tabla.");
            return;
        }

        try {
            int idEquipo = Integer.parseInt(txtId.getText().trim());

            Equipo existente = equipoController.buscarPorId(idEquipo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El equipo seleccionado no existe."
                    ));

            if (existente.getEstado() != EstadoEquipo.DISPONIBLE) {
                mostrarAdvertencia(
                        "Solo se pueden actualizar equipos DISPONIBLES."
                );
                return;
            }

            Equipo equipo = obtenerDatosFormulario();

            equipo.setIdEquipo(idEquipo);
            // El código sale del formulario (editable); si está vacío se conserva el actual.
            if (equipo.getCodigoInventario() == null) {
                equipo.setCodigoInventario(existente.getCodigoInventario());
            }
            equipo.setIdLaboratorio(
                    existente.getIdLaboratorio()
            );
            equipo.setEstado(existente.getEstado());

            boolean actualizado = equipoController.actualizar(equipo);

            if (actualizado) {
                String avisoQR = "";

                try {
                    Equipo actualizadoBD =
                            equipoController.buscarPorId(idEquipo)
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "No se pudo consultar el equipo actualizado."
                                            )
                                    );

                    generarArchivoQR(actualizadoBD);

                } catch (WriterException | IOException
                         | IllegalArgumentException exQR) {
                    avisoQR = "\nNo se pudo actualizar la imagen QR:\n"
                            + exQR.getMessage();
                }

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Equipo actualizado correctamente." + avisoQR,
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
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo actualizar el equipo.\n" + ex.getMessage()
            );
        }
    }

    private Equipo obtenerDatosFormulario() {
        Producto producto =
                (Producto) cmbProducto.getSelectedItem();

        Laboratorio laboratorio =
                (Laboratorio) cmbLaboratorio.getSelectedItem();

        String codigo = txtCodigoInventario.getText().trim().isEmpty()
                ? null
                : txtCodigoInventario.getText().trim();

        String serie = txtNumeroSerie.getText().trim();
        String observaciones = observacionesTextArea.getText().trim();

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
                "Buscar equipo por:\n"
                        + "ID, código de inventario, número de serie,\n"
                        + "producto, laboratorio u observaciones.\n\n"
                        + "Deje vacío para mostrar todos:",
                "Buscar equipo",
                JOptionPane.QUESTION_MESSAGE
        );

        if (texto == null) {
            return;
        }

        String criterio = texto.trim();

        try {
            if (criterio.isEmpty()) {
                nuevo();
                cargarEquipos();
                return;
            }

            List<Equipo> equipos = equipoController.listar();
            List<Equipo> exactos = new ArrayList<>();
            List<Equipo> parciales = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (Equipo equipo : equipos) {
                String id = String.valueOf(equipo.getIdEquipo());
                String codigo = textoSeguro(equipo.getCodigoInventario());
                String serie = textoSeguro(equipo.getNumeroSerie());
                String producto = textoSeguro(
                        obtenerNombreProducto(equipo.getIdProducto())
                );
                String laboratorio = textoSeguro(
                        obtenerNombreLaboratorio(equipo.getIdLaboratorio())
                );
                String observaciones = textoSeguro(
                        equipo.getObservaciones()
                );

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || serie.equalsIgnoreCase(criterio)
                        || producto.equalsIgnoreCase(criterio)
                        || laboratorio.equalsIgnoreCase(criterio)) {
                    exactos.add(equipo);

                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || serie.toLowerCase(Locale.ROOT).contains(filtro)
                                || producto.toLowerCase(Locale.ROOT).contains(filtro)
                                || laboratorio.toLowerCase(Locale.ROOT).contains(filtro)
                                || observaciones.toLowerCase(Locale.ROOT).contains(filtro)
                ) {
                    parciales.add(equipo);
                }
            }

            List<Equipo> resultados =
                    exactos.isEmpty() ? parciales : exactos;

            nuevo();
            cargarTabla(resultados);

            if (resultados.isEmpty()) {
                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron equipos con el criterio:\n"
                                + criterio,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else if (resultados.size() == 1) {
                int filaVista = tblEquipo.convertRowIndexToView(0);

                if (filaVista >= 0) {
                    tblEquipo.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );
                    tblEquipo.scrollRectToVisible(
                            tblEquipo.getCellRect(filaVista, 0, true)
                    );
                    seleccionarEquipo();
                }
            }

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo realizar la búsqueda.\n" + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void seleccionarEquipo() {
        if (cargandoSeleccion) {
            return;
        }

        int fila = tblEquipo.getSelectedRow();

        if (fila < 0) {
            return;
        }

        int filaModelo = tblEquipo.convertRowIndexToModel(fila);
        Object valorId = modeloTabla.getValueAt(filaModelo, 0);

        int idEquipo;

        try {
            idEquipo = Integer.parseInt(String.valueOf(valorId));

        } catch (NumberFormatException ex) {
            mostrarAdvertencia(
                    "El ID seleccionado no es válido."
            );
            return;
        }

        try {
            Equipo equipo = equipoController.buscarPorId(idEquipo)
                    .orElse(null);

            if (equipo == null) {
                mostrarAdvertencia(
                        "El equipo seleccionado ya no existe."
                );
                return;
            }

            txtId.setText(String.valueOf(equipo.getIdEquipo()));
            txtCodigoInventario.setText(equipo.getCodigoInventario());
            txtNumeroSerie.setText(equipo.getNumeroSerie());
            txtEstado.setText(equipo.getEstado().name());
            txtFechadeRegistro.setText(
                    formatearFecha(equipo.getFechaRegistro())
            );
            observacionesTextArea.setText(
                    equipo.getObservaciones() == null
                            ? ""
                            : equipo.getObservaciones()
            );

            seleccionarProducto(equipo.getIdProducto());
            seleccionarLaboratorio(equipo.getIdLaboratorio());

            cmbLaboratorio.setEnabled(false);

            boolean disponible =
                    equipo.getEstado() == EstadoEquipo.DISPONIBLE;

            btnGuardar.setEnabled(false);
            btnActualizar.setEnabled(disponible);
            btnMostrarQR.setEnabled(true);

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo consultar el equipo.\n" + ex.getMessage()
            );
        }
    }

    private void seleccionarProducto(int idProducto) {
        for (int i = 0; i < cmbProducto.getItemCount(); i++) {
            Producto producto = cmbProducto.getItemAt(i);

            if (producto != null
                    && producto.getIdProducto() == idProducto) {
                cmbProducto.setSelectedIndex(i);
                return;
            }
        }

        cmbProducto.setSelectedIndex(-1);
    }

    private void seleccionarLaboratorio(int idLaboratorio) {
        for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
            Laboratorio laboratorio = cmbLaboratorio.getItemAt(i);

            if (laboratorio != null
                    && laboratorio.getIdLaboratorio() == idLaboratorio) {
                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }

        cmbLaboratorio.setSelectedIndex(-1);
    }

    private String construirContenidoQR(Equipo equipo) {
        return "SIGELAB - INFORMACIÓN DEL EQUIPO\n"
                + "ID: " + equipo.getIdEquipo() + "\n"
                + "Código de inventario: "
                + textoSeguro(equipo.getCodigoInventario()) + "\n"
                + "Producto: "
                + obtenerNombreProducto(equipo.getIdProducto()) + "\n"
                + "Número de serie: "
                + textoSeguro(equipo.getNumeroSerie()) + "\n"
                + "Laboratorio: "
                + obtenerNombreLaboratorio(
                equipo.getIdLaboratorio()
        ) + "\n"
                + "Estado: " + equipo.getEstado() + "\n"
                + "Fecha de registro: "
                + formatearFecha(equipo.getFechaRegistro()) + "\n"
                + "Observaciones: "
                + textoSeguro(equipo.getObservaciones());
    }

    private String validarCodigoQR(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "El equipo no tiene código de inventario."
            );
        }

        String limpio = codigo.trim();

        if (!limpio.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El código de inventario contiene caracteres no permitidos."
            );
        }

        return limpio;
    }

    private Path generarArchivoQR(Equipo equipo)
            throws WriterException, IOException {

        String codigo = validarCodigoQR(
                equipo.getCodigoInventario()
        );

        String contenidoQR = construirContenidoQR(equipo);

        Path carpeta = Path.of("qr-equipos");
        Files.createDirectories(carpeta);

        Path destino = carpeta.resolve(codigo + ".png");

        QrGenerator.guardarQR(contenidoQR, destino);

        return destino.toAbsolutePath();
    }

    private void mostrarQRSeleccionado() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione primero un equipo registrado."
            );
            return;
        }

        try {
            int idEquipo = Integer.parseInt(
                    txtId.getText().trim()
            );

            Equipo equipo = equipoController.buscarPorId(idEquipo)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El equipo seleccionado ya no existe."
                            )
                    );

            String codigo = validarCodigoQR(
                    equipo.getCodigoInventario()
            );

            String contenidoQR = construirContenidoQR(equipo);

            BufferedImage imagen = QrGenerator.generarQR(
                    contenidoQR,
                    480,
                    480
            );

            JLabel imagenQR = new JLabel(
                    new ImageIcon(imagen),
                    SwingConstants.CENTER
            );

            JLabel etiquetaCodigo = new JLabel(
                    "Código de inventario: " + codigo,
                    SwingConstants.CENTER
            );
            etiquetaCodigo.setFont(new Font("Segoe UI", Font.BOLD, 14));
            etiquetaCodigo.setForeground(CELESTE_PRINCIPAL);

            JPanel tarjetaQr = new JPanel(new BorderLayout(8, 8));
            tarjetaQr.setBackground(BLANCO_PURO);
            tarjetaQr.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CELESTE_MEDIO, 1, true),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
            tarjetaQr.add(imagenQR, BorderLayout.CENTER);
            tarjetaQr.add(etiquetaCodigo, BorderLayout.SOUTH);

            JPanel contenido = new JPanel(new BorderLayout(8, 8));
            contenido.setBackground(BLANCO_HIELO);
            contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            contenido.add(tarjetaQr, BorderLayout.CENTER);

            Object[] opciones = {
                    "Guardar PNG...",
                    "Cerrar"
            };

            int opcion = JOptionPane.showOptionDialog(
                    panelPrincipal,
                    contenido,
                    "Código QR - " + codigo,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (opcion == 0) {
                guardarQRConSelector(equipo);
            }

        } catch (SQLException | WriterException
                 | IllegalArgumentException ex) {
            mostrarError(
                    "No se pudo mostrar el código QR.\n"
                            + ex.getMessage()
            );
        }
    }

    private void guardarQRConSelector(Equipo equipo) {
        String codigo;

        try {
            codigo = validarCodigoQR(
                    equipo.getCodigoInventario()
            );

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código QR");
        selector.setSelectedFile(
                new java.io.File(codigo + ".png")
        );

        int resultado = selector.showSaveDialog(panelPrincipal);

        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destino = selector.getSelectedFile().toPath();
        String nombre = destino.getFileName().toString();

        if (!nombre.toLowerCase(Locale.ROOT).endsWith(".png")) {
            destino = destino.resolveSibling(nombre + ".png");
        }

        if (Files.exists(destino)) {
            int respuesta = JOptionPane.showConfirmDialog(
                    panelPrincipal,
                    "El archivo ya existe.\n¿Desea reemplazarlo?",
                    "Confirmar reemplazo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            QrGenerator.guardarQR(
                    construirContenidoQR(equipo),
                    destino
            );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Código QR guardado correctamente en:\n"
                            + destino.toAbsolutePath(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (WriterException | IOException
                 | IllegalArgumentException ex) {
            mostrarError(
                    "No se pudo guardar el código QR.\n"
                            + ex.getMessage()
            );
        }
    }

    private void salir() {
        Window ventana = SwingUtilities.getWindowAncestor(
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
                "Error - SIGELAB",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}
