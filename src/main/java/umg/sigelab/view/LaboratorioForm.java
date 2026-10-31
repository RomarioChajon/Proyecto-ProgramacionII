// ============================================================================
// ARCHIVO: src/main/java/umg/sigelab/view/LaboratorioForm.java
// INSTRUCCIONES: Copia todo desde "package umg.sigelab.view;" hasta la última
// llave "}" y pégalo reemplazando todo el contenido de LaboratorioForm.java.
// (Nota: Requiere tener TemaCeleste.java en la misma carpeta umg/sigelab/view)
// ============================================================================

package umg.sigelab.view;

import umg.sigelab.controller.LaboratorioAdminController;
import umg.sigelab.model.Laboratorio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana de Gestión de Laboratorios / Sedes con diseño Celeste y Blanco
 * ("Laboratorios de Computación").
 */
public class LaboratorioForm {

    private static final int COL_ID = 0;
    private static final int COL_ESTADO = 5;

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;
    private JTextField txtFechaCreacion;
    private JTextField txtBuscar;

    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnBuscar;
    private JButton btnVerTodos;
    private JButton salirButton;

    private JTable tblLaborario;
    private JScrollPane scrollTabla;

    private final LaboratorioAdminController controller;

    private static final ZoneId ZONA_GUATEMALA =
            ZoneId.of("America/Guatemala");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private boolean cargandoTabla = false;

    public LaboratorioForm() {

        controller = new LaboratorioAdminController();

        crearInterfaz();
        configurarFormulario();
        configurarTabla();
        configurarEventos();

        cargarLaboratorios();
        nuevo();
    }

    private void crearInterfaz() {
        // 1. Encabezado superior azul (#2B8FD6)
        JPanel barraSuperior = TemaCeleste.crearEncabezadoAzul(new BorderLayout(16, 0));
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 24));

        JPanel izqHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izqHeader.setOpaque(false);
        JLabel iconoHeader = new JLabel(TemaCeleste.crearIconoDashboard(
                TemaCeleste.TipoIcono.LABORATORIOS, 36, new Color(255, 255, 255, 38)));
        JLabel titulo = new JLabel("SIGELAB - Gestión de Laboratorios / Sedes");
        titulo.setFont(TemaCeleste.fuente(Font.BOLD, 20f));
        titulo.setForeground(Color.WHITE);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        izqHeader.add(iconoHeader);
        izqHeader.add(titulo);

        salirButton = new JButton("Cerrar ventana");
        TemaCeleste.estilizarBotonEncabezado(salirButton);

        barraSuperior.add(izqHeader, BorderLayout.WEST);
        barraSuperior.add(salirButton, BorderLayout.EAST);

        // 2. Tarjeta superior: Formulario de laboratorio / sede
        JPanel tarjetaFormulario = TemaCeleste.crearTarjetaBlanca(
                new BorderLayout(0, 14), 18, new Insets(16, 22, 16, 22));

        txtId = TemaCeleste.crearCampoTextoRedondeado();
        txtCodigo = TemaCeleste.crearCampoTextoRedondeado();
        txtNombre = TemaCeleste.crearCampoTextoRedondeado();
        txtUbicacion = TemaCeleste.crearCampoTextoRedondeado();
        txtCapacidad = TemaCeleste.crearCampoTextoRedondeado();
        txtFechaCreacion = TemaCeleste.crearCampoTextoRedondeado();

        cmbEstado = new JComboBox<>();
        TemaCeleste.estilizarCombo(cmbEstado);

        JPanel grillaCampos = new JPanel(new GridLayout(2, 4, 16, 12));
        grillaCampos.setOpaque(false);

        grillaCampos.add(campoConEtiqueta("ID Laboratorio", txtId));
        grillaCampos.add(campoConEtiqueta("Código (ej. LMRomero, vacío = automático)", txtCodigo));
        grillaCampos.add(campoConEtiqueta("Nombre completo del laboratorio / sede", txtNombre));
        grillaCampos.add(campoConEtiqueta("Ubicación / salón", txtUbicacion));

        grillaCampos.add(campoConEtiqueta("Capacidad de estudiantes", txtCapacidad));
        grillaCampos.add(campoConEtiqueta("Estado del laboratorio", cmbEstado));
        grillaCampos.add(campoConEtiqueta("Fecha de creación", txtFechaCreacion));

        JPanel notaAuto = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(), 16, new Insets(6, 12, 6, 12));
        JLabel lblNota = new JLabel("<html><div style='text-align:center;'>Código autogenerado<br><span style='color:#4A5568;font-size:10px;'>si se deja en blanco al guardar</span></div></html>", SwingConstants.CENTER);
        lblNota.setFont(TemaCeleste.fuente(Font.BOLD, 12f));
        lblNota.setForeground(TemaCeleste.AZUL_PRINCIPAL);
        notaAuto.add(lblNota, BorderLayout.CENTER);
        grillaCampos.add(campoConEtiqueta("Generación de código", notaAuto));

        // Botones de acción CRUD
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelBotones.setOpaque(false);

        btnNuevo = new JButton("Nuevo / Limpiar");
        TemaCeleste.estilizarBotonClaro(btnNuevo, 28, 13.5f);
        btnNuevo.setPreferredSize(new Dimension(150, 36));

        btnGuardar = new JButton("Guardar laboratorio");
        TemaCeleste.estilizarBotonPrimario(btnGuardar, 28, 14f);
        btnGuardar.setPreferredSize(new Dimension(175, 36));

        btnActualizar = new JButton("Actualizar datos");
        TemaCeleste.estilizarBotonPrimario(btnActualizar, 28, 14f);
        btnActualizar.setPreferredSize(new Dimension(160, 36));

        btnDesactivar = new JButton("Desactivar");
        TemaCeleste.estilizarBotonPeligro(btnDesactivar, 28, 13.5f);
        btnDesactivar.setPreferredSize(new Dimension(135, 36));

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDesactivar);

        tarjetaFormulario.add(grillaCampos, BorderLayout.CENTER);
        tarjetaFormulario.add(panelBotones, BorderLayout.SOUTH);

        // 3. Tarjeta inferior: Buscador + Tabla de laboratorios
        JPanel tarjetaTabla = TemaCeleste.crearTarjetaBlanca(
                new BorderLayout(0, 12), 18, new Insets(16, 22, 18, 22));

        JPanel barraBusqueda = new JPanel(new BorderLayout(12, 0));
        barraBusqueda.setOpaque(false);

        JLabel lblListado = new JLabel("Laboratorios y Sedes Registrados");
        lblListado.setFont(TemaCeleste.fuente(Font.BOLD, 18f));
        lblListado.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        JPanel controlesFiltro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlesFiltro.setOpaque(false);

        JLabel lblBuscarTxt = new JLabel("Buscar:");
        lblBuscarTxt.setFont(TemaCeleste.fuente(Font.PLAIN, 13.5f));
        lblBuscarTxt.setForeground(TemaCeleste.TEXTO_SECUNDARIO);

        txtBuscar = TemaCeleste.crearCampoTextoRedondeado();
        txtBuscar.setPreferredSize(new Dimension(240, 36));
        txtBuscar.setToolTipText("Ingrese ID, código, nombre o ubicación y presione Enter o Buscar");

        btnBuscar = new JButton("Buscar");
        TemaCeleste.estilizarBotonPrimario(btnBuscar, 24, 13.5f);
        btnBuscar.setPreferredSize(new Dimension(105, 36));

        btnVerTodos = new JButton("Ver todos");
        TemaCeleste.estilizarBotonCelesteSuave(btnVerTodos, 24, 13f);
        btnVerTodos.setPreferredSize(new Dimension(105, 36));

        controlesFiltro.add(lblBuscarTxt);
        controlesFiltro.add(txtBuscar);
        controlesFiltro.add(btnBuscar);
        controlesFiltro.add(btnVerTodos);

        barraBusqueda.add(lblListado, BorderLayout.WEST);
        barraBusqueda.add(controlesFiltro, BorderLayout.EAST);

        tblLaborario = new JTable();
        scrollTabla = new JScrollPane(tblLaborario);
        scrollTabla.setPreferredSize(new Dimension(980, 270));

        tarjetaTabla.add(barraBusqueda, BorderLayout.NORTH);
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);

        // Cuerpo principal
        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        contenido.add(tarjetaFormulario, BorderLayout.NORTH);
        contenido.add(tarjetaTabla, BorderLayout.CENTER);

        panelPrincipal = TemaCeleste.crearPanelFondo(new BorderLayout());
        panelPrincipal.setPreferredSize(new Dimension(1080, 680));
        panelPrincipal.add(barraSuperior, BorderLayout.NORTH);
        panelPrincipal.add(contenido, BorderLayout.CENTER);
    }

    private JPanel campoConEtiqueta(String titulo, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(titulo);
        l.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        l.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        componente.setPreferredSize(new Dimension(160, 38));
        p.add(l, BorderLayout.NORTH);
        p.add(componente, BorderLayout.CENTER);
        return p;
    }

    private void mostrarFechaActual() {

        LocalDateTime fechaActual =
                LocalDateTime.now(ZONA_GUATEMALA);

        txtFechaCreacion.setText(
                fechaActual.format(FORMATO_FECHA)
        );
    }

    private String formatearFecha(LocalDateTime fecha) {

        if (fecha == null) {
            return "";
        }

        return fecha.format(FORMATO_FECHA);
    }

    private void configurarFormulario() {

        txtId.setEditable(false);
        txtId.setFocusable(false);

        // El código es editable: si se deja vacío al crear, se genera automáticamente.
        txtCodigo.setEditable(true);
        txtCodigo.setFocusable(true);

        txtCodigo.setToolTipText(
                "Puede escribirlo o dejarlo vacío para que se genere automáticamente al guardar."
        );

        txtFechaCreacion.setEditable(false);
        txtFechaCreacion.setFocusable(false);

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);

        mostrarFechaActual();
    }

    private void configurarTabla() {

        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código",
                        "Nombre",
                        "Ubicación",
                        "Capacidad",
                        "Estado",
                        "Fecha creación"
                },
                0
        ) {

            @Override
            public boolean isCellEditable(
                    int fila,
                    int columna
            ) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                return columna == COL_ID ? Integer.class : Object.class;
            }
        };

        tblLaborario.setModel(modelo);

        tblLaborario.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tblLaborario.setAutoCreateRowSorter(true);
        tblLaborario.getColumnModel().getColumn(COL_ID).setMaxWidth(65);
        tblLaborario.getColumnModel().getColumn(COL_ESTADO).setPreferredWidth(115);

        TemaCeleste.estilizarTabla(tblLaborario, scrollTabla, COL_ESTADO);
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e -> nuevo());

        btnGuardar.addActionListener(e -> guardar());

        btnActualizar.addActionListener(e -> actualizar());

        btnDesactivar.addActionListener(e -> desactivar());

        btnBuscar.addActionListener(e -> buscar());

        if (txtBuscar != null) {
            txtBuscar.addActionListener(e -> ejecutarBusqueda(txtBuscar.getText().trim()));
        }

        if (btnVerTodos != null) {
            btnVerTodos.addActionListener(e -> {
                if (txtBuscar != null) {
                    txtBuscar.setText("");
                }
                cargarLaboratorios();
                nuevo();
            });
        }

        salirButton.addActionListener(e -> salir());

        tblLaborario.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()
                            && !cargandoTabla) {

                        cargarLaboratorioSeleccionado();
                    }
                });
    }

    private void cargarLaboratorios() {

        try {

            List<Laboratorio> laboratorios =
                    controller.listar();

            cargarTabla(laboratorios);

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudieron cargar los laboratorios.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<Laboratorio> laboratorios) {

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        cargandoTabla = true;

        try {

            modelo.setRowCount(0);

            for (Laboratorio laboratorio : laboratorios) {

                modelo.addRow(new Object[]{
                        laboratorio.getIdLaboratorio(),
                        laboratorio.getCodigo(),
                        laboratorio.getNombre(),
                        laboratorio.getUbicacion(),
                        laboratorio.getCapacidad(),
                        laboratorio.getEstado().name(), // texto "ACTIVO"/"INACTIVO": el combo y los botones comparan con ese texto
                        formatearFecha(
                                laboratorio.getFechaCreacion()
                        )
                });
            }

            tblLaborario.clearSelection();

        } finally {

            cargandoTabla = false;
        }
    }

    private void nuevo() {

        limpiarCampos();
        mostrarFechaActual();

        txtNombre.requestFocusInWindow();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void guardar() {

        if (!validarCampos()) {
            return;
        }

        try {

            int capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim()
            );

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            Laboratorio laboratorio = controller.guardar(
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtUbicacion.getText().trim(),
                    capacidad,
                    estado
            );

            String fechaGuardada = formatearFecha(
                    laboratorio.getFechaCreacion()
            );

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Laboratorio guardado correctamente.\n"
                            + "Código: "
                            + laboratorio.getCodigo()
                            + "\nID generado: "
                            + laboratorio.getIdLaboratorio()
                            + "\nFecha de creación: "
                            + (fechaGuardada.isEmpty()
                            ? "No disponible"
                            : fechaGuardada),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cargarLaboratorios();
            nuevo();

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo guardar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertenciaGeneral(
                    "Seleccione un laboratorio de la tabla."
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

            int capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim()
            );

            String estado = String.valueOf(
                    cmbEstado.getSelectedItem()
            );

            boolean actualizado = controller.actualizar(
                    id,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtUbicacion.getText().trim(),
                    capacidad,
                    estado
            );

            if (actualizado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Laboratorio actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarLaboratorios();
                nuevo();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo actualizar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo actualizar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void desactivar() {

        if (txtId.getText().trim().isEmpty()) {

            mostrarAdvertenciaGeneral(
                    "Seleccione un laboratorio de la tabla."
            );

            return;
        }

        String estadoActual = String.valueOf(
                cmbEstado.getSelectedItem()
        );

        if ("INACTIVO".equalsIgnoreCase(estadoActual)) {

            mostrarAdvertenciaGeneral(
                    "El laboratorio ya se encuentra inactivo."
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este laboratorio?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int id = Integer.parseInt(
                    txtId.getText().trim()
            );

            boolean desactivado =
                    controller.desactivar(id);

            if (desactivado) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Laboratorio desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

                cargarLaboratorios();
                nuevo();

            } else {

                mostrarAdvertenciaGeneral(
                        "No se pudo desactivar el laboratorio."
                );
            }

        } catch (IllegalArgumentException ex) {

            mostrarAdvertenciaGeneral(ex.getMessage());

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo desactivar el laboratorio.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {
        String textoBarra = txtBuscar == null ? "" : txtBuscar.getText().trim();
        if (!textoBarra.isEmpty()) {
            ejecutarBusqueda(textoBarra);
            return;
        }

        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Ingrese ID, código, nombre o ubicación:\n"
                        + "(Deje vacío para mostrar todos)",
                "Buscar laboratorio",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        ejecutarBusqueda(entrada.trim());
    }

    private void ejecutarBusqueda(String criterio) {
        try {

            if (criterio.isEmpty()) {

                cargarTabla(controller.listar());
                limpiarCampos();

                return;
            }

            List<Laboratorio> laboratorios =
                    controller.listar();

            List<Laboratorio> exactos =
                    new ArrayList<>();

            List<Laboratorio> parciales =
                    new ArrayList<>();

            String filtro =
                    criterio.toLowerCase(Locale.ROOT);

            for (Laboratorio laboratorio : laboratorios) {

                String id = String.valueOf(
                        laboratorio.getIdLaboratorio()
                );

                String codigo =
                        textoSeguro(laboratorio.getCodigo());

                String nombre =
                        textoSeguro(laboratorio.getNombre());

                String ubicacion =
                        textoSeguro(laboratorio.getUbicacion());

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {

                    exactos.add(laboratorio);

                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || ubicacion.toLowerCase(Locale.ROOT).contains(filtro)
                ) {

                    parciales.add(laboratorio);
                }
            }

            List<Laboratorio> resultados =
                    exactos.isEmpty()
                            ? parciales
                            : exactos;

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron laboratorios con el criterio:\n"
                                + criterio,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else if (resultados.size() == 1) {

                int filaVista =
                        tblLaborario.convertRowIndexToView(0);

                if (filaVista >= 0) {

                    tblLaborario.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblLaborario.scrollRectToVisible(
                            tblLaborario.getCellRect(
                                    filaVista,
                                    0,
                                    true
                            )
                    );

                    cargarLaboratorioSeleccionado();
                }
            }

        } catch (SQLException ex) {

            mostrarError(
                    "No se pudo realizar la búsqueda.\n\n"
                            + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void cargarLaboratorioSeleccionado() {

        if (cargandoTabla) {
            return;
        }

        int filaVista = tblLaborario.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int filaModelo =
                tblLaborario.convertRowIndexToModel(filaVista);

        DefaultTableModel modelo =
                (DefaultTableModel) tblLaborario.getModel();

        txtId.setText(
                valorTabla(modelo, filaModelo, 0)
        );

        txtCodigo.setText(
                valorTabla(modelo, filaModelo, 1)
        );

        txtNombre.setText(
                valorTabla(modelo, filaModelo, 2)
        );

        txtUbicacion.setText(
                valorTabla(modelo, filaModelo, 3)
        );

        txtCapacidad.setText(
                valorTabla(modelo, filaModelo, 4)
        );

        String estado =
                valorTabla(modelo, filaModelo, 5);

        cmbEstado.setSelectedItem(estado);

        txtFechaCreacion.setText(
                valorTabla(modelo, filaModelo, 6)
        );

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);

        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private String valorTabla(
            DefaultTableModel modelo,
            int fila,
            int columna
    ) {

        Object valor =
                modelo.getValueAt(fila, columna);

        return valor == null ? "" : valor.toString();
    }

    private boolean validarCampos() {

        if (txtNombre.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese el nombre del laboratorio.",
                    txtNombre
            );

            return false;
        }

        if (txtUbicacion.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese la ubicación del laboratorio.",
                    txtUbicacion
            );

            return false;
        }

        if (txtCapacidad.getText().trim().isEmpty()) {

            mostrarAdvertencia(
                    "Ingrese la capacidad del laboratorio.",
                    txtCapacidad
            );

            return false;
        }

        try {

            int capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim()
            );

            if (capacidad <= 0) {

                mostrarAdvertencia(
                        "La capacidad debe ser mayor que cero.",
                        txtCapacidad
                );

                return false;
            }

        } catch (NumberFormatException ex) {

            mostrarAdvertencia(
                    "La capacidad debe ser un número entero.",
                    txtCapacidad
            );

            return false;
        }

        return true;
    }

    private void mostrarAdvertencia(
            String mensaje,
            JTextField campo
    ) {

        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );

        campo.requestFocusInWindow();
    }

    private void mostrarAdvertenciaGeneral(String mensaje) {

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

    private void limpiarCampos() {

        limpiarCamposSinTabla();

        cargandoTabla = true;

        try {

            tblLaborario.clearSelection();

        } finally {

            cargandoTabla = false;
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void limpiarCamposSinTabla() {

        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtUbicacion.setText("");
        txtCapacidad.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        mostrarFechaActual();
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
