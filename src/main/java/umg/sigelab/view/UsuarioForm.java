package umg.sigelab.view;

import umg.sigelab.controller.LaboratorioController;
import umg.sigelab.controller.UsuarioController;
import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.integration.facial.FacialAuthClient;
import umg.sigelab.integration.facial.FacialClientException;
import umg.sigelab.integration.facial.FacialEnrollmentResult;
import umg.sigelab.integration.facial.FacialStatus;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.DateUtils;
import umg.sigelab.util.ImagenUtils;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de administración de usuarios (CRUD) con diseño Celeste y Blanco
 * ("Laboratorios de Computación").
 *
 * <p>La vista NO decide reglas de negocio: valida que el usuario haya escrito/elegido lo básico
 * y delega todo lo demás al controlador. Las excepciones se muestran con {@link Mensajes}.</p>
 *
 * <p>Los usuarios no se eliminan: el botón Desactivar/Activar cambia su estado (baja lógica).</p>
 */
public class UsuarioForm {

    private static final String TITULO = "Usuarios";
    /** Lado del cuadro donde se dibuja la foto (el JLabel mide 180x180 con borde). */
    private static final int LADO_FOTO = 166;

    // Posición de cada columna en la tabla.
    private static final int COL_ID = 0;
    private static final int COL_ESTADO = 6;

    // ---- Componentes enlazados con UsuarioForm.form ----
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtId;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtCorreo;
    private JComboBox<RolUsuario> cmbRol;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JLabel lblEstado;
    private JLabel lblFoto;
    private JButton btnSeleccionarFoto;
    private JButton btnQuitarFoto;
    private JLabel lblEstadoRostro;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnCambiarEstado;
    private JButton btnRegistrarRostro;
    private JButton btnEliminarRostro;
    private JTextField txtBuscar;
    private JComboBox<FiltroEstado> cmbFiltroEstado;
    private JButton btnBuscar;
    private JTable tblUsuarios;
    private JButton btnSalir;
    private JScrollPane scrollTabla;

    // ---- Dependencias ----
    private final UsuarioController usuarioController;
    private final LaboratorioController laboratorioController;
    private final FacialAuthClient facialClient;
    /** Usuario con la sesión abierta (null si todavía no hay login). */
    private final Usuario usuarioAutenticado;

    // ---- Estado de la pantalla ----
    /** Usuario cargado en el formulario; null cuando se está creando uno nuevo. */
    private Usuario usuarioSeleccionado;
    /** Foto elegida en esta pantalla y todavía no guardada (JPEG ya reducido). */
    private byte[] fotoNueva;
    /** true si el usuario eligió o quitó la foto y falta guardar el cambio. */
    private boolean fotoModificada;
    /** true mientras se llena la tabla por código (evita que la selección dispare eventos). */
    private boolean cargandoTabla;
    private boolean perfilFacialRegistrado;

    /** Opciones del filtro por estado. */
    private enum FiltroEstado {
        TODOS("Todos los estados", null),
        ACTIVOS("Solo activos", EstadoRegistro.ACTIVO),
        INACTIVOS("Solo inactivos", EstadoRegistro.INACTIVO);

        private final String etiqueta;
        private final EstadoRegistro estado;

        FiltroEstado(String etiqueta, EstadoRegistro estado) {
            this.etiqueta = etiqueta;
            this.estado = estado;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    public UsuarioForm() {
        this(null);
    }

    /** @param usuarioAutenticado quien tiene la sesión abierta (null si aún no hay login) */
    public UsuarioForm(Usuario usuarioAutenticado) {
        this(new UsuarioController(), new LaboratorioController(), new FacialAuthClient(), usuarioAutenticado);
    }

    /** Constructor completo: permite inyectar controladores falsos en pruebas. */
    public UsuarioForm(UsuarioController usuarioController, LaboratorioController laboratorioController,
                       FacialAuthClient facialClient, Usuario usuarioAutenticado) {
        this.usuarioController = usuarioController;
        this.laboratorioController = laboratorioController;
        this.facialClient = facialClient;
        this.usuarioAutenticado = usuarioAutenticado;

        construirInterfazCeleste();
        configurarFormulario();
        cargarLaboratorios();
        configurarEventos();
        cargarUsuarios();
        nuevo();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ------------------------------------------------------------------
    // Construcción visual Celeste y Blanco
    // ------------------------------------------------------------------

    private void construirInterfazCeleste() {
        // 1. Barra de encabezado azul (#2B8FD6)
        JPanel barraSuperior = TemaCeleste.crearEncabezadoAzul(new BorderLayout(16, 0));
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 24));

        JPanel izqHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izqHeader.setOpaque(false);
        JLabel iconoHeader = new JLabel(TemaCeleste.crearIconoDashboard(
                TemaCeleste.TipoIcono.USUARIOS, 36, new Color(255, 255, 255, 38)));
        lblTitulo = new JLabel("SIGELAB - Administración de Usuarios");
        lblTitulo.setFont(TemaCeleste.fuente(Font.BOLD, 20f));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        izqHeader.add(iconoHeader);
        izqHeader.add(lblTitulo);

        btnSalir = new JButton("Cerrar ventana");
        TemaCeleste.estilizarBotonEncabezado(btnSalir);

        barraSuperior.add(izqHeader, BorderLayout.WEST);
        barraSuperior.add(btnSalir, BorderLayout.EAST);

        // 2. Tarjeta superior: Formulario + Reconocimiento Facial + Fotografía
        JPanel tarjetaFormulario = TemaCeleste.crearTarjetaBlanca(new BorderLayout(18, 0), 18, new Insets(14, 20, 14, 20));

        // Campos del formulario (3 filas x 3 columnas)
        txtId = TemaCeleste.crearCampoTextoRedondeado();
        txtNombre = TemaCeleste.crearCampoTextoRedondeado();
        txtApellido = TemaCeleste.crearCampoTextoRedondeado();
        txtUsuario = TemaCeleste.crearCampoTextoRedondeado();
        txtPassword = TemaCeleste.crearCampoPasswordRedondeado();
        txtCorreo = TemaCeleste.crearCampoTextoRedondeado();

        cmbRol = new JComboBox<>();
        TemaCeleste.estilizarCombo(cmbRol);

        cmbLaboratorio = new JComboBox<>();
        TemaCeleste.estilizarCombo(cmbLaboratorio);

        lblEstado = new JLabel("Nuevo (Activo)", SwingConstants.CENTER);
        lblEstado.setFont(TemaCeleste.fuente(Font.BOLD, 13f));
        lblEstado.setForeground(TemaCeleste.AZUL_PRINCIPAL);
        JPanel cajaEstado = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(), 16, new Insets(4, 10, 4, 10));
        cajaEstado.add(lblEstado, BorderLayout.CENTER);

        JPanel grillaCampos = new JPanel(new GridLayout(3, 3, 14, 8));
        grillaCampos.setOpaque(false);
        grillaCampos.add(campoConEtiqueta("ID Usuario", txtId));
        grillaCampos.add(campoConEtiqueta("Nombre", txtNombre));
        grillaCampos.add(campoConEtiqueta("Apellido", txtApellido));
        grillaCampos.add(campoConEtiqueta("Nombre de usuario", txtUsuario));
        grillaCampos.add(campoConEtiqueta("Contraseña (vacía = conservar)", txtPassword));
        grillaCampos.add(campoConEtiqueta("Correo institucional", txtCorreo));
        grillaCampos.add(campoConEtiqueta("Rol en el sistema", cmbRol));
        grillaCampos.add(campoConEtiqueta("Laboratorio (solo encargado)", cmbLaboratorio));
        grillaCampos.add(campoConEtiqueta("Estado del registro", cajaEstado));

        // Franja de reconocimiento facial
        JPanel franjaRostro = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(12, 0), 14, new Insets(8, 14, 8, 14));
        JPanel izqRostro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izqRostro.setOpaque(false);
        JLabel lblTitRostro = new JLabel("Reconocimiento facial:");
        lblTitRostro.setFont(TemaCeleste.fuente(Font.BOLD, 13f));
        lblTitRostro.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        lblEstadoRostro = new JLabel("Guarde el usuario para registrar su rostro.");
        lblEstadoRostro.setFont(TemaCeleste.fuente(Font.PLAIN, 13f));
        lblEstadoRostro.setForeground(TemaCeleste.AZUL_PRINCIPAL);
        lblEstadoRostro.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));

        izqRostro.add(lblTitRostro);
        izqRostro.add(lblEstadoRostro);

        JPanel derRostro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derRostro.setOpaque(false);
        btnRegistrarRostro = new JButton("Registrar rostro");
        TemaCeleste.estilizarBotonCelesteSuave(btnRegistrarRostro, 24, 13f);
        btnRegistrarRostro.setPreferredSize(new Dimension(150, 32));

        btnEliminarRostro = new JButton("Eliminar rostro");
        TemaCeleste.estilizarBotonPeligro(btnEliminarRostro, 24, 13f);
        btnEliminarRostro.setPreferredSize(new Dimension(140, 32));

        derRostro.add(btnRegistrarRostro);
        derRostro.add(btnEliminarRostro);

        franjaRostro.add(izqRostro, BorderLayout.WEST);
        franjaRostro.add(derRostro, BorderLayout.EAST);

        // Barra de botones de acción CRUD
        JPanel barraCrud = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barraCrud.setOpaque(false);

        btnNuevo = new JButton("Nuevo / Limpiar");
        TemaCeleste.estilizarBotonClaro(btnNuevo, 28, 13.5f);
        btnNuevo.setPreferredSize(new Dimension(150, 36));

        btnGuardar = new JButton("Guardar nuevo");
        TemaCeleste.estilizarBotonPrimario(btnGuardar, 28, 14f);
        btnGuardar.setPreferredSize(new Dimension(160, 36));

        btnActualizar = new JButton("Actualizar datos");
        TemaCeleste.estilizarBotonPrimario(btnActualizar, 28, 14f);
        btnActualizar.setPreferredSize(new Dimension(160, 36));

        btnCambiarEstado = new JButton("Desactivar");
        TemaCeleste.estilizarBotonPeligro(btnCambiarEstado, 28, 13.5f);
        btnCambiarEstado.setPreferredSize(new Dimension(135, 36));

        barraCrud.add(btnNuevo);
        barraCrud.add(btnGuardar);
        barraCrud.add(btnActualizar);
        barraCrud.add(btnCambiarEstado);

        JPanel bloqueSurForm = new JPanel(new BorderLayout(0, 8));
        bloqueSurForm.setOpaque(false);
        bloqueSurForm.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        bloqueSurForm.add(franjaRostro, BorderLayout.NORTH);
        bloqueSurForm.add(barraCrud, BorderLayout.SOUTH);

        JPanel columnaIzquierda = new JPanel(new BorderLayout(0, 4));
        columnaIzquierda.setOpaque(false);
        columnaIzquierda.add(grillaCampos, BorderLayout.CENTER);
        columnaIzquierda.add(bloqueSurForm, BorderLayout.SOUTH);

        // Columna derecha: Fotografía del usuario
        JPanel tarjetaFoto = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(0, 8), 16, new Insets(10, 12, 10, 12));
        JLabel lblTitFoto = new JLabel("Fotografía de perfil", SwingConstants.CENTER);
        lblTitFoto.setFont(TemaCeleste.fuente(Font.BOLD, 13f));
        lblTitFoto.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        lblFoto = new JLabel("Sin foto", SwingConstants.CENTER);
        lblFoto.setFont(TemaCeleste.fuente(Font.PLAIN, 13f));
        lblFoto.setForeground(TemaCeleste.TEXTO_SECUNDARIO);
        lblFoto.setOpaque(true);
        lblFoto.setBackground(Color.WHITE);
        lblFoto.setPreferredSize(new Dimension(172, 172));

        JPanel botonesFoto = new JPanel(new GridLayout(1, 2, 6, 0));
        botonesFoto.setOpaque(false);
        btnSeleccionarFoto = new JButton("Elegir...");
        TemaCeleste.estilizarBotonCelesteSuave(btnSeleccionarFoto, 20, 12.5f);
        btnSeleccionarFoto.setPreferredSize(new Dimension(85, 30));

        btnQuitarFoto = new JButton("Quitar");
        TemaCeleste.estilizarBotonClaro(btnQuitarFoto, 20, 12.5f);
        btnQuitarFoto.setPreferredSize(new Dimension(75, 30));

        botonesFoto.add(btnSeleccionarFoto);
        botonesFoto.add(btnQuitarFoto);

        tarjetaFoto.add(lblTitFoto, BorderLayout.NORTH);
        tarjetaFoto.add(lblFoto, BorderLayout.CENTER);
        tarjetaFoto.add(botonesFoto, BorderLayout.SOUTH);

        tarjetaFormulario.add(columnaIzquierda, BorderLayout.CENTER);
        tarjetaFormulario.add(tarjetaFoto, BorderLayout.EAST);

        // 3. Tarjeta inferior: Buscador + Tabla de usuarios (estilo mockup_prestamos_celeste)
        JPanel tarjetaTabla = TemaCeleste.crearTarjetaBlanca(new BorderLayout(0, 10), 18, new Insets(14, 20, 16, 20));

        JPanel barraBusqueda = new JPanel(new BorderLayout(12, 0));
        barraBusqueda.setOpaque(false);

        JLabel lblListado = new JLabel("Usuarios Registrados");
        lblListado.setFont(TemaCeleste.fuente(Font.BOLD, 18f));
        lblListado.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        JPanel controlesFiltro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlesFiltro.setOpaque(false);

        JLabel lblBuscarTxt = new JLabel("Buscar:");
        lblBuscarTxt.setFont(TemaCeleste.fuente(Font.PLAIN, 13.5f));
        lblBuscarTxt.setForeground(TemaCeleste.TEXTO_SECUNDARIO);

        txtBuscar = TemaCeleste.crearCampoTextoRedondeado();
        txtBuscar.setPreferredSize(new Dimension(230, 36));
        txtBuscar.setToolTipText("Buscar por nombre, apellido, usuario o correo");

        cmbFiltroEstado = new JComboBox<>();
        TemaCeleste.estilizarCombo(cmbFiltroEstado);
        cmbFiltroEstado.setPreferredSize(new Dimension(185, 36));

        btnBuscar = new JButton("Buscar");
        TemaCeleste.estilizarBotonPrimario(btnBuscar, 24, 13.5f);
        btnBuscar.setPreferredSize(new Dimension(105, 36));

        controlesFiltro.add(lblBuscarTxt);
        controlesFiltro.add(txtBuscar);
        controlesFiltro.add(cmbFiltroEstado);
        controlesFiltro.add(btnBuscar);

        barraBusqueda.add(lblListado, BorderLayout.WEST);
        barraBusqueda.add(controlesFiltro, BorderLayout.EAST);

        tblUsuarios = new JTable();
        scrollTabla = new JScrollPane(tblUsuarios);
        scrollTabla.setPreferredSize(new Dimension(980, 230));

        tarjetaTabla.add(barraBusqueda, BorderLayout.NORTH);
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);

        // Cuerpo principal con ambas tarjetas
        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        cuerpo.add(tarjetaFormulario, BorderLayout.NORTH);
        cuerpo.add(tarjetaTabla, BorderLayout.CENTER);

        panelPrincipal = TemaCeleste.crearPanelFondo(new BorderLayout());
        panelPrincipal.setPreferredSize(new Dimension(1120, 710));
        panelPrincipal.add(barraSuperior, BorderLayout.NORTH);
        panelPrincipal.add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel campoConEtiqueta(String titulo, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(titulo);
        l.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        l.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        componente.setPreferredSize(new Dimension(160, 36));
        p.add(l, BorderLayout.NORTH);
        p.add(componente, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------
    // Configuración inicial
    // ------------------------------------------------------------------

    private void configurarFormulario() {
        // El id lo genera la base de datos: solo lectura.
        txtId.setEditable(false);

        // Rol: placeholder + los 4 roles.
        ComboBoxUtils.llenarConPlaceholder(cmbRol, Arrays.asList(RolUsuario.values()), "-- Seleccione --");

        // Filtro de estado de la lista.
        cmbFiltroEstado.removeAllItems();
        for (FiltroEstado filtro : FiltroEstado.values()) {
            cmbFiltroEstado.addItem(filtro);
        }

        // Foto: cuadro con borde fijo.
        lblFoto.setHorizontalAlignment(JLabel.CENTER);
        marcarFoto(false);

        configurarTabla();
    }

    private void configurarTabla() {
        tblUsuarios.setModel(new DefaultTableModel(
                new Object[]{"ID", "Usuario", "Nombre completo", "Correo", "Rol", "Laboratorio", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false; // la tabla solo muestra; se edita en el formulario
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                // El ID es numérico: así al ordenar por ID queda 2 antes que 10 (y no al revés).
                return columna == COL_ID ? Integer.class : Object.class;
            }
        });
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUsuarios.setAutoCreateRowSorter(true); // clic en el encabezado ordena
        tblUsuarios.getColumnModel().getColumn(COL_ID).setMaxWidth(65);
        tblUsuarios.getColumnModel().getColumn(COL_ESTADO).setPreferredWidth(115);

        TemaCeleste.estilizarTabla(tblUsuarios, scrollTabla, COL_ESTADO);
    }

    private void cargarLaboratorios() {
        try {
            ComboBoxUtils.llenarConPlaceholder(cmbLaboratorio, laboratorioController.listarActivos(), "-- Seleccione --");
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnCambiarEstado.addActionListener(e -> cambiarEstado());
        btnSeleccionarFoto.addActionListener(e -> seleccionarFoto());
        btnQuitarFoto.addActionListener(e -> quitarFoto());
        btnRegistrarRostro.addActionListener(e -> registrarRostro());
        btnEliminarRostro.addActionListener(e -> eliminarRostro());
        btnBuscar.addActionListener(e -> cargarUsuarios());
        txtBuscar.addActionListener(e -> cargarUsuarios());          // Enter en el buscador
        cmbFiltroEstado.addActionListener(e -> cargarUsuarios());    // cambiar el filtro recarga
        cmbRol.addActionListener(e -> actualizarCampoLaboratorio());
        btnSalir.addActionListener(e -> salir());

        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoTabla) {
                seleccionarDesdeTabla();
            }
        });
    }

    // ------------------------------------------------------------------
    // Acciones de los botones
    // ------------------------------------------------------------------

    /** Prepara el formulario para registrar un usuario nuevo. */
    private void nuevo() {
        usuarioSeleccionado = null;
        perfilFacialRegistrado = false;

        txtId.setText("");
        txtNombre.setText("");
        txtApellido.setText("");
        txtUsuario.setText("");
        txtPassword.setText("");
        txtCorreo.setText("");
        cmbRol.setSelectedItem(null);
        cmbLaboratorio.setSelectedItem(null);
        lblEstado.setText("Nuevo (se creará como Activo)");
        mostrarFoto(null);
        fotoNueva = null;
        fotoModificada = false;
        marcarFoto(false);

        tblUsuarios.clearSelection();
        actualizarCampoLaboratorio();

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnCambiarEstado.setEnabled(false);
        btnCambiarEstado.setText("Desactivar");
        deshabilitarRostro("Guarde el usuario para registrar su rostro.");
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        try {
            Usuario creado = usuarioController.crear(construirUsuario(), new String(txtPassword.getPassword()));
            Mensajes.informar(panelPrincipal, TITULO, "Usuario \"" + creado.getNombreUsuario() + "\" creado correctamente.");
            cargarUsuarios();
            seleccionarFilaPorId(creado.getIdUsuario());
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    private void actualizar() {
        if (usuarioSeleccionado == null) {
            Mensajes.advertir(panelPrincipal, TITULO, "Seleccione un usuario de la lista.");
            return;
        }
        if (!Mensajes.confirmar(panelPrincipal, TITULO, "¿Desea actualizar los datos de este usuario?")) {
            return;
        }
        try {
            Usuario datos = construirUsuario();
            datos.setIdUsuario(usuarioSeleccionado.getIdUsuario());
            Usuario guardado = usuarioController.actualizar(datos, new String(txtPassword.getPassword()), fotoModificada);
            Mensajes.informar(panelPrincipal, TITULO, "Usuario actualizado correctamente.");
            cargarUsuarios();
            seleccionarFilaPorId(guardado.getIdUsuario());
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    /** Desactiva o reactiva al usuario seleccionado. No se borra nada: se conserva el historial. */
    private void cambiarEstado() {
        if (usuarioSeleccionado == null) {
            Mensajes.advertir(panelPrincipal, TITULO, "Seleccione un usuario de la lista.");
            return;
        }
        EstadoRegistro nuevoEstado = usuarioSeleccionado.isActivo() ? EstadoRegistro.INACTIVO : EstadoRegistro.ACTIVO;
        String accion = nuevoEstado == EstadoRegistro.INACTIVO ? "desactivar" : "activar";
        if (!Mensajes.confirmar(panelPrincipal, TITULO,
                "¿Desea " + accion + " al usuario \"" + usuarioSeleccionado.getNombreUsuario() + "\"?")) {
            return;
        }
        try {
            int id = usuarioSeleccionado.getIdUsuario();
            usuarioController.cambiarEstado(id, nuevoEstado, idUsuarioActual());
            cargarUsuarios();
            seleccionarFilaPorId(id);
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    private void salir() {
        Window ventana = SwingUtilities.getWindowAncestor(panelPrincipal);
        if (ventana != null) {
            ventana.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Foto
    // ------------------------------------------------------------------

    private void seleccionarFoto() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Seleccione la foto del usuario");
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (selector.showOpenDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            fotoNueva = ImagenUtils.prepararFoto(selector.getSelectedFile()); // valida y reduce
            fotoModificada = true;
            mostrarFoto(fotoNueva);
            marcarFoto(true);
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    private void quitarFoto() {
        fotoNueva = null;
        fotoModificada = true;
        mostrarFoto(null);
        marcarFoto(true);
    }

    /** Dibuja la foto en el cuadro, o el texto "Sin foto" si no hay. */
    private void mostrarFoto(byte[] foto) {
        ImageIcon icono = ImagenUtils.aIcono(foto, LADO_FOTO, LADO_FOTO);
        lblFoto.setIcon(icono);
        lblFoto.setText(icono == null ? "Sin foto" : "");
    }

    /** Borde naranja = hay un cambio de foto pendiente de guardar; celeste = foto tal como está guardada. */
    private void marcarFoto(boolean pendiente) {
        lblFoto.setBorder(new TemaCeleste.BordeRedondeadoSimple(
                pendiente ? TemaCeleste.NARANJA_ALERTA : TemaCeleste.CELESTE_SUAVE, 14, pendiente ? 2.2f : 1.4f));
        lblFoto.setToolTipText(pendiente ? "Cambio de foto pendiente: se guarda al presionar Guardar o Actualizar." : null);
    }

    // ------------------------------------------------------------------
    // Tabla y búsqueda
    // ------------------------------------------------------------------

    /** Llena la tabla con los usuarios que cumplen el texto y el estado del filtro. */
    private void cargarUsuarios() {
        try {
            FiltroEstado filtro = (FiltroEstado) cmbFiltroEstado.getSelectedItem();
            var usuarios = usuarioController.listar(txtBuscar.getText(), filtro == null ? null : filtro.estado);

            cargandoTabla = true;
            DefaultTableModel modelo = (DefaultTableModel) tblUsuarios.getModel();
            modelo.setRowCount(0);
            for (Usuario u : usuarios) {
                modelo.addRow(new Object[]{
                        u.getIdUsuario(),
                        u.getNombreUsuario(),
                        u.getNombreCompleto(),
                        u.getCorreo(),
                        u.getRol().getEtiqueta(),
                        u.getLaboratorio() == null ? "" : u.getLaboratorio().getCodigo(),
                        u.getEstado().getEtiqueta()});
            }
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        } finally {
            cargandoTabla = false;
        }
    }

    /** Se ejecuta cuando el usuario hace clic en una fila: carga ese usuario completo (con foto). */
    private void seleccionarDesdeTabla() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int id = (int) tblUsuarios.getModel().getValueAt(tblUsuarios.convertRowIndexToModel(fila), COL_ID);
        try {
            usuarioController.buscarPorId(id).ifPresentOrElse(this::mostrarUsuario, () -> {
                Mensajes.advertir(panelPrincipal, TITULO, "El usuario ya no existe. Se actualizará la lista.");
                cargarUsuarios();
            });
        } catch (RuntimeException e) {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, e);
        }
    }

    /**
     * Marca en la tabla la fila de ese usuario (si es visible con el filtro actual) y lo carga en el formulario.
     * Siempre vuelve a leerlo de la base de datos, así el formulario refleja lo que realmente quedó guardado.
     */
    private void seleccionarFilaPorId(int id) {
        DefaultTableModel modelo = (DefaultTableModel) tblUsuarios.getModel();
        cargandoTabla = true; // marcar la fila no debe disparar una segunda carga
        try {
            for (int filaModelo = 0; filaModelo < modelo.getRowCount(); filaModelo++) {
                if ((int) modelo.getValueAt(filaModelo, COL_ID) == id) {
                    int filaVista = tblUsuarios.convertRowIndexToView(filaModelo);
                    tblUsuarios.setRowSelectionInterval(filaVista, filaVista);
                    tblUsuarios.scrollRectToVisible(tblUsuarios.getCellRect(filaVista, 0, true));
                    break;
                }
            }
        } finally {
            cargandoTabla = false;
        }
        // Si no aparece en la tabla (p. ej. se desactivó y se ven solo activos), igual se muestra en el formulario.
        usuarioController.buscarPorId(id).ifPresent(this::mostrarUsuario);
    }

    // ------------------------------------------------------------------
    // Pasar datos entre formulario y modelo
    // ------------------------------------------------------------------

    /** Muestra un usuario existente en el formulario y deja la pantalla en modo edición. */
    private void mostrarUsuario(Usuario u) {
        usuarioSeleccionado = u;

        txtId.setText(String.valueOf(u.getIdUsuario()));
        txtNombre.setText(u.getNombre());
        txtApellido.setText(u.getApellido());
        txtUsuario.setText(u.getNombreUsuario());
        txtPassword.setText(""); // la contraseña nunca se muestra; vacía = conservar la actual
        txtCorreo.setText(u.getCorreo());
        cmbRol.setSelectedItem(u.getRol());
        seleccionarLaboratorio(u.getLaboratorio());
        actualizarCampoLaboratorio();
        lblEstado.setText(u.getEstado().getEtiqueta() + "  (creado " + DateUtils.formatear(u.getFechaCreacion()) + ")");

        fotoNueva = null;
        fotoModificada = false;
        mostrarFoto(u.getFoto());
        marcarFoto(false);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnCambiarEstado.setText(u.isActivo() ? "Desactivar" : "Activar");
        // No se permite desactivar al usuario con el que se inició sesión.
        btnCambiarEstado.setEnabled(!(u.isActivo() && u.getIdUsuario() == idUsuarioActual()));

        consultarEstadoRostro(u);
    }

    /** Lee el formulario y arma un Usuario. NO valida reglas: de eso se encarga el Service. */
    private Usuario construirUsuario() {
        Usuario u = new Usuario();
        u.setNombre(txtNombre.getText());
        u.setApellido(txtApellido.getText());
        u.setNombreUsuario(txtUsuario.getText());
        u.setCorreo(txtCorreo.getText());
        u.setRol((RolUsuario) cmbRol.getSelectedItem());
        u.setLaboratorio((Laboratorio) cmbLaboratorio.getSelectedItem());
        u.setFoto(fotoModificada ? fotoNueva : null);
        return u;
    }

    /** El combo de laboratorio solo se usa cuando el rol es ENCARGADO. */
    private void actualizarCampoLaboratorio() {
        RolUsuario rol = (RolUsuario) cmbRol.getSelectedItem();
        boolean requiere = rol != null && rol.requiereLaboratorio();
        cmbLaboratorio.setEnabled(requiere);
        if (!requiere) {
            cmbLaboratorio.setSelectedItem(null);
        }
    }

    private void seleccionarLaboratorio(Laboratorio buscado) {
        cmbLaboratorio.setSelectedItem(null);
        if (buscado == null) {
            return;
        }
        for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
            Laboratorio actual = cmbLaboratorio.getItemAt(i);
            if (actual != null && actual.getIdLaboratorio() == buscado.getIdLaboratorio()) {
                cmbLaboratorio.setSelectedIndex(i);
                return;
            }
        }
        // El laboratorio del usuario está inactivo y no aparece en el combo: lo agregamos para no perder el dato.
        cmbLaboratorio.addItem(buscado);
        cmbLaboratorio.setSelectedItem(buscado);
    }

    private int idUsuarioActual() {
        return usuarioAutenticado == null ? 0 : usuarioAutenticado.getIdUsuario();
    }

    // ------------------------------------------------------------------
    // Reconocimiento facial (servicio Python local)
    // ------------------------------------------------------------------

    private void deshabilitarRostro(String mensaje) {
        lblEstadoRostro.setText(mensaje);
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        btnRegistrarRostro.setText("Registrar rostro");
    }

    /** Consulta si el usuario ya tiene rostro registrado y ajusta los botones. */
    private void consultarEstadoRostro(Usuario usuario) {
        if (!usuario.isActivo()) {
            deshabilitarRostro("Usuario inactivo: actívelo para registrar su rostro.");
            return;
        }
        deshabilitarRostro("Consultando perfil facial...");
        int idConsultado = usuario.getIdUsuario();

        new SwingWorker<FacialStatus, Void>() {
            @Override
            protected FacialStatus doInBackground() {
                return facialClient.status(idConsultado);
            }

            @Override
            protected void done() {
                // Si mientras tanto el usuario eligió otra fila, se descarta esta respuesta.
                if (usuarioSeleccionado == null || usuarioSeleccionado.getIdUsuario() != idConsultado) {
                    return;
                }
                try {
                    FacialStatus estado = get();
                    perfilFacialRegistrado = estado.enrolled();
                    lblEstadoRostro.setText(estado.enrolled()
                            ? "Registrado (" + estado.model() + ", " + estado.samples() + " muestras)"
                            : "No registrado");
                    btnRegistrarRostro.setText(estado.enrolled() ? "Actualizar rostro" : "Registrar rostro");
                    btnRegistrarRostro.setEnabled(true);
                    btnEliminarRostro.setEnabled(estado.enrolled() && esAdministradorAutenticado());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Sin diálogo: este chequeo ocurre al seleccionar cada fila y sería molesto.
                    perfilFacialRegistrado = false;
                    deshabilitarRostro("Servicio facial no disponible.");
                }
            }
        }.execute();
    }

    private void registrarRostro() {
        if (usuarioSeleccionado == null) {
            return;
        }
        final int id = usuarioSeleccionado.getIdUsuario();
        final boolean reemplazar = perfilFacialRegistrado;
        String accion = reemplazar ? "actualizar" : "registrar";
        if (!Mensajes.confirmar(panelPrincipal, "Registro facial",
                "¿Desea " + accion + " el rostro de \"" + usuarioSeleccionado.getNombreUsuario() + "\"?\n"
                        + "Se abrirá la cámara y deberá seguir las instrucciones.")) {
            return;
        }
        deshabilitarRostro("Cámara activa. Siga las instrucciones...");

        new SwingWorker<FacialEnrollmentResult, Void>() {
            @Override
            protected FacialEnrollmentResult doInBackground() {
                return facialClient.enroll(id, reemplazar);
            }

            @Override
            protected void done() {
                try {
                    Mensajes.informar(panelPrincipal, "Registro facial", get().message());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Mensajes.error(panelPrincipal, "Registro facial",
                            mensajeFacial(e.getCause(), "No fue posible registrar el rostro."));
                }
                refrescarEstadoRostro(id);
            }
        }.execute();
    }

    private void eliminarRostro() {
        if (usuarioSeleccionado == null) {
            return;
        }
        if (!esAdministradorAutenticado()) {
            Mensajes.advertir(panelPrincipal, "Perfil facial", "Solo un administrador puede eliminar perfiles faciales.");
            return;
        }
        final int id = usuarioSeleccionado.getIdUsuario();
        if (!Mensajes.confirmar(panelPrincipal, "Eliminar perfil facial",
                "¿Desea eliminar el perfil facial de \"" + usuarioSeleccionado.getNombreUsuario() + "\"?")) {
            return;
        }
        deshabilitarRostro("Eliminando perfil facial...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                facialClient.deleteProfile(id, usuarioAutenticado.getIdUsuario());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Mensajes.informar(panelPrincipal, "Perfil facial", "Perfil facial eliminado correctamente.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Mensajes.error(panelPrincipal, "Perfil facial",
                            mensajeFacial(e.getCause(), "No fue posible eliminar el perfil facial."));
                }
                refrescarEstadoRostro(id);
            }
        }.execute();
    }

    /** Vuelve a consultar el estado facial si el usuario sigue siendo el seleccionado. */
    private void refrescarEstadoRostro(int id) {
        if (usuarioSeleccionado != null && usuarioSeleccionado.getIdUsuario() == id) {
            consultarEstadoRostro(usuarioSeleccionado);
        }
    }

    private boolean esAdministradorAutenticado() {
        return usuarioAutenticado != null && usuarioAutenticado.getRol() == RolUsuario.ADMIN;
    }

    private String mensajeFacial(Throwable error, String mensajePorDefecto) {
        if (error instanceof FacialClientException && error.getMessage() != null) {
            return error.getMessage();
        }
        return mensajePorDefecto;
    }
}
