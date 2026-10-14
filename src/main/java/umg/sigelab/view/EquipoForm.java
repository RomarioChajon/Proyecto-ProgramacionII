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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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
        panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        JPanel campos = new JPanel(new GridLayout(0, 4, 8, 8));

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
                new FlowLayout(FlowLayout.CENTER, 8, 5)
        );

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnBuscar);
        botones.add(btnMostrarQR);
        botones.add(btnSalir);

        JPanel superior = new JPanel(new BorderLayout(5, 8));

        superior.add(
                new JLabel("Gestión de Equipos", SwingConstants.CENTER),
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

        panelPrincipal.setPreferredSize(new Dimension(1100, 620));
    }

    private void agregarBotonQRAlternativo() {
        btnMostrarQR = new JButton("Ver QR");

        JPanel barra = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        barra.add(btnMostrarQR);

        JPanel original = panelPrincipal;
        panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.add(original, BorderLayout.CENTER);
        panelPrincipal.add(barra, BorderLayout.SOUTH);
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        txtCodigoInventario.setEditable(false);
        txtEstado.setEditable(false);
        txtFechadeRegistro.setEditable(false);

        txtCodigoInventario.setText("Automático al guardar");
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
        tblEquipo.setRowHeight(25);
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
        txtCodigoInventario.setText("Automático al guardar");
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
            equipo.setCodigoInventario(
                    existente.getCodigoInventario()
            );
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

        String codigo = txtId.getText().trim().isEmpty()
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
                    600,
                    600
            );

            JLabel imagenQR = new JLabel(
                    new ImageIcon(imagen),
                    SwingConstants.CENTER
            );

            JLabel etiquetaCodigo = new JLabel(
                    "Código de inventario: " + codigo,
                    SwingConstants.CENTER
            );

            JPanel contenido = new JPanel(
                    new BorderLayout(8, 8)
            );

            contenido.add(imagenQR, BorderLayout.CENTER);
            contenido.add(etiquetaCodigo, BorderLayout.SOUTH);

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
                new File(codigo + ".png")
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
        panelPrincipal.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(8, 6, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Gestion de Equipo ");
        panelPrincipal.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 6, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("ID Equipo");
        panelPrincipal.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtId = new JTextField();
        txtId.setToolTipText("IDEquipo");
        panelPrincipal.add(txtId, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Codigo de Inventario");
        panelPrincipal.add(label3, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Producto ");
        panelPrincipal.add(label4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbProducto = new JComboBox();
        panelPrincipal.add(cmbProducto, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Numero de Serie");
        panelPrincipal.add(label5, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Laboratorio");
        panelPrincipal.add(label6, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cmbLaboratorio = new JComboBox();
        panelPrincipal.add(cmbLaboratorio, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Estado");
        panelPrincipal.add(label7, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelPrincipal.add(btnNuevo, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelPrincipal.add(btnGuardar, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar ");
        panelPrincipal.add(btnActualizar, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Fecha de Registro");
        panelPrincipal.add(label8, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtFechadeRegistro = new JTextField();
        panelPrincipal.add(txtFechadeRegistro, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        observacionesTextArea = new JTextArea();
        panelPrincipal.add(observacionesTextArea, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panelPrincipal.add(btnBuscar, new com.intellij.uiDesigner.core.GridConstraints(6, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Observaciones");
        panelPrincipal.add(label9, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 6, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblEquipo = new JTable();
        scrollPane1.setViewportView(tblEquipo);
        btnMostrarQR = new JButton();
        btnMostrarQR.setText("Ver QR");
        panelPrincipal.add(btnMostrarQR, new com.intellij.uiDesigner.core.GridConstraints(6, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtNumeroSerie = new JTextField();
        panelPrincipal.add(txtNumeroSerie, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        txtCodigoInventario = new JTextField();
        txtCodigoInventario.setToolTipText("CodigoInventario ");
        panelPrincipal.add(txtCodigoInventario, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        txtEstado = new JTextField();
        panelPrincipal.add(txtEstado, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnSalir = new JButton();
        btnSalir.setText("Salir");
        panelPrincipal.add(btnSalir, new com.intellij.uiDesigner.core.GridConstraints(5, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }
}


