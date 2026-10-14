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

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de administración de usuarios (CRUD).
 *
 * <p>Diseño: el aspecto lo define {@code UsuarioForm.form} (diseñador de IntelliJ).
 * IntelliJ llena automáticamente los campos privados de abajo, uno por cada componente
 * "enlazado" (binding) en el .form. Esta clase solo contiene el COMPORTAMIENTO:
 * leer los campos, llamar al controlador y mostrar el resultado.</p>
 *
 * <p>La vista NO decide reglas de negocio: valida que el usuario haya escrito/elegido lo básico
 * y delega todo lo demás al controlador. Las excepciones se muestran con {@link Mensajes}.</p>
 *
 * <p>Los usuarios no se eliminan: el botón Desactivar/Activar cambia su estado (baja lógica).</p>
 */
public class UsuarioForm {

    private static final String TITULO = "Usuarios";
    /**
     * Lado del cuadro donde se dibuja la foto (el JLabel mide 180x180 con borde).
     */
    private static final int LADO_FOTO = 176;

    // Posición de cada columna en la tabla.
    private static final int COL_ID = 0;
    private static final int COL_ESTADO = 6;

    // ---- Componentes enlazados con UsuarioForm.form (los asigna IntelliJ) ----
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

    // ---- Dependencias ----
    private final UsuarioController usuarioController;
    private final LaboratorioController laboratorioController;
    private final FacialAuthClient facialClient;
    /**
     * Usuario con la sesión abierta (null si todavía no hay login).
     */
    private final Usuario usuarioAutenticado;

    // ---- Estado de la pantalla ----
    /**
     * Usuario cargado en el formulario; null cuando se está creando uno nuevo.
     */
    private Usuario usuarioSeleccionado;
    /**
     * Foto elegida en esta pantalla y todavía no guardada (JPEG ya reducido).
     */
    private byte[] fotoNueva;
    /**
     * true si el usuario eligió o quitó la foto y falta guardar el cambio.
     */
    private boolean fotoModificada;
    /**
     * true mientras se llena la tabla por código (evita que la selección dispare eventos).
     */
    private boolean cargandoTabla;
    private boolean perfilFacialRegistrado;

    /**
     * Opciones del filtro por estado.
     */
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

    /**
     * @param usuarioAutenticado quien tiene la sesión abierta (null si aún no hay login)
     */
    public UsuarioForm(Usuario usuarioAutenticado) {
        this(new UsuarioController(), new LaboratorioController(), new FacialAuthClient(), usuarioAutenticado);
    }

    /**
     * Constructor completo: permite inyectar controladores falsos en pruebas.
     */
    public UsuarioForm(UsuarioController usuarioController, LaboratorioController laboratorioController,
                       FacialAuthClient facialClient, Usuario usuarioAutenticado) {
        this.usuarioController = usuarioController;
        this.laboratorioController = laboratorioController;
        this.facialClient = facialClient;
        this.usuarioAutenticado = usuarioAutenticado;

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
    // Configuración inicial
    // ------------------------------------------------------------------

    private void configurarFormulario() {
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 16f));

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
        tblUsuarios.getColumnModel().getColumn(COL_ID).setMaxWidth(60);

        // Los usuarios INACTIVO se ven en gris para distinguirlos.
        DefaultTableCellRenderer renderizador = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                           boolean conFoco, int fila, int columna) {
                Component celda = super.getTableCellRendererComponent(tabla, valor, seleccionada, conFoco, fila, columna);
                int filaModelo = tabla.convertRowIndexToModel(fila);
                boolean inactivo = EstadoRegistro.INACTIVO.getEtiqueta()
                        .equals(tabla.getModel().getValueAt(filaModelo, COL_ESTADO));
                if (!seleccionada) {
                    celda.setForeground(inactivo ? Color.GRAY : tabla.getForeground());
                }
                return celda;
            }
        };
        renderizador.setHorizontalAlignment(JLabel.LEFT);
        tblUsuarios.setDefaultRenderer(Object.class, renderizador);
        tblUsuarios.setDefaultRenderer(Integer.class, renderizador);
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

    /**
     * Prepara el formulario para registrar un usuario nuevo.
     */
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

    /**
     * Desactiva o reactiva al usuario seleccionado. No se borra nada: se conserva el historial.
     */
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

    /**
     * Dibuja la foto en el cuadro, o el texto "Sin foto" si no hay.
     */
    private void mostrarFoto(byte[] foto) {
        ImageIcon icono = ImagenUtils.aIcono(foto, LADO_FOTO, LADO_FOTO);
        lblFoto.setIcon(icono);
        lblFoto.setText(icono == null ? "Sin foto" : "");
    }

    /**
     * Borde naranja = hay un cambio de foto pendiente de guardar; gris = foto tal como está guardada.
     */
    private void marcarFoto(boolean pendiente) {
        lblFoto.setBorder(BorderFactory.createLineBorder(pendiente ? new Color(0xE67E22) : Color.LIGHT_GRAY, 2));
        lblFoto.setToolTipText(pendiente ? "Cambio de foto pendiente: se guarda al presionar Guardar o Actualizar." : null);
    }

    // ------------------------------------------------------------------
    // Tabla y búsqueda
    // ------------------------------------------------------------------

    /**
     * Llena la tabla con los usuarios que cumplen el texto y el estado del filtro.
     */
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

    /**
     * Se ejecuta cuando el usuario hace clic en una fila: carga ese usuario completo (con foto).
     */
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

    /**
     * Muestra un usuario existente en el formulario y deja la pantalla en modo edición.
     */
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

    /**
     * Lee el formulario y arma un Usuario. NO valida reglas: de eso se encarga el Service.
     */
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

    /**
     * El combo de laboratorio solo se usa cuando el rol es ENCARGADO.
     */
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
    // La cámara se abre en el servicio Python; aquí solo se pide la operación.
    // Todo corre en un SwingWorker para no congelar la pantalla mientras se espera la cámara.
    // ------------------------------------------------------------------

    private void deshabilitarRostro(String mensaje) {
        lblEstadoRostro.setText(mensaje);
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        btnRegistrarRostro.setText("Registrar rostro");
    }

    /**
     * Consulta si el usuario ya tiene rostro registrado y ajusta los botones.
     */
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

    /**
     * Vuelve a consultar el estado facial si el usuario sigue siendo el seleccionado.
     */
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
