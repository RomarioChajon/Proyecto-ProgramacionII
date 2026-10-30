package umg.sigelab.view;

import umg.sigelab.controller.AuthController;
import umg.sigelab.integration.facial.FacialAuthClient;
import umg.sigelab.integration.facial.FacialClientException;
import umg.sigelab.integration.facial.FacialEnrollmentResult;
import umg.sigelab.integration.facial.FacialServiceLauncher;
import umg.sigelab.integration.facial.FacialVerificationResult;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.BusinessException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;


/**
 * Pantalla de acceso con tres pestañas (diseño Celeste y Blanco - Laboratorios de Computación):
 * <ol>
 *   <li><b>Ingresar</b>: usuario + contraseña + reconocimiento facial (los tres son obligatorios).</li>
 *   <li><b>Regístrate</b>: crea una cuenta de ESTUDIANTE y captura el rostro.</li>
 *   <li><b>Reiniciar contraseña</b>: usuario + correo + rostro; si el rostro coincide se habilita la contraseña nueva.</li>
 * </ol>
 */
public class LoginForm {

    private static final String TITULO = "SIGELAB";
    private static final Logger LOG = Logger.getLogger(LoginForm.class.getName());

    /** Mensaje que pide el enunciado para quienes no son estudiantes. */
    static final String AVISO_COLABORADOR =
            "Si usted es colaborador de la institución por favor contactarse con el administrador.";

    private static final int PESTANA_REINICIO = 2;

    // --- Componentes enlazados con LoginForm.form (los nombres deben coincidir exactamente) ---
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JTabbedPane tabs;
    // Pestaña "Ingresar"
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JLabel lblAyudaIngreso;
    // Pestaña "Regístrate"
    private JTextField txtRegNombre;
    private JTextField txtRegApellido;
    private JTextField txtRegUsuario;
    private JTextField txtRegCorreo;
    private JPasswordField txtRegPassword;
    private JPasswordField txtRegPassword2;
    private JLabel lblAvisoColaborador;
    private JButton btnRegistrar;
    // Pestaña "Reiniciar contraseña"
    private JTextField txtResUsuario;
    private JTextField txtResCorreo;
    private JButton btnVerificarRostro;
    private JLabel lblEstadoReinicio;
    private JPasswordField txtResNueva;
    private JPasswordField txtResNueva2;
    private JButton btnGuardarNueva;
    // Pie
    private JLabel lblEstadoServicio;
    private JButton btnServicioFacial;
    private JButton btnSalir;


    private final AuthController authController;
    private final FacialAuthClient facialClient;
    private final Consumer<Usuario> alIngresar;
    private final Runnable alAbrirServicioFacial;

    /** true mientras corre una operación larga (cámara, base de datos): bloquea los botones. */
    private boolean ocupado;
    /** Id del usuario cuyo rostro YA fue verificado en el reinicio de contraseña (0 = ninguno). */
    private int reinicioVerificadoId;
    private boolean comprobandoServicio;
    private Timer temporizadorServicio;

    public LoginForm(Consumer<Usuario> alIngresar, Runnable alAbrirServicioFacial) {
        this(new AuthController(), new FacialAuthClient(), alIngresar, alAbrirServicioFacial);
    }

    /** Constructor completo: permite inyectar controlador y cliente falsos en pruebas. */
    public LoginForm(AuthController authController, FacialAuthClient facialClient,
                     Consumer<Usuario> alIngresar, Runnable alAbrirServicioFacial) {
        this.authController = authController;
        this.facialClient = facialClient;
        this.alIngresar = alIngresar;
        this.alAbrirServicioFacial = alAbrirServicioFacial;

        construirInterfazCeleste();
        lblAvisoColaborador.setToolTipText(AVISO_COLABORADOR);
        configurarEventos();
        refrescarControles();
        iniciarVigilanciaDelServicio();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    /** Detiene el temporizador que consulta el servicio facial. Llamar al salir de esta pantalla. */
    public void cerrar() {
        if (temporizadorServicio != null) {
            temporizadorServicio.stop();
        }




    }

    // ------------------------------------------------------------------
    // Construcción visual Celeste y Blanco (fiel a mockup_login_celeste)
    // ------------------------------------------------------------------

    private void construirInterfazCeleste() {
        // Campos redondeados con foco celeste
        txtUsuario = TemaCeleste.crearCampoTextoRedondeado();
        txtPassword = TemaCeleste.crearCampoPasswordRedondeado();
        txtRegNombre = TemaCeleste.crearCampoTextoRedondeado();
        txtRegApellido = TemaCeleste.crearCampoTextoRedondeado();
        txtRegUsuario = TemaCeleste.crearCampoTextoRedondeado();
        txtRegCorreo = TemaCeleste.crearCampoTextoRedondeado();
        txtRegPassword = TemaCeleste.crearCampoPasswordRedondeado();
        txtRegPassword2 = TemaCeleste.crearCampoPasswordRedondeado();
        txtResUsuario = TemaCeleste.crearCampoTextoRedondeado();
        txtResCorreo = TemaCeleste.crearCampoTextoRedondeado();
        txtResNueva = TemaCeleste.crearCampoPasswordRedondeado();
        txtResNueva2 = TemaCeleste.crearCampoPasswordRedondeado();

        // Botones principales y secundarios
        btnIngresar = new JButton("Ingresar");
        TemaCeleste.estilizarBotonPrimario(btnIngresar, 40, 15.5f);
        btnIngresar.setPreferredSize(new Dimension(320, 44));
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        btnRegistrar = new JButton("Crear cuenta y capturar rostro");
        TemaCeleste.estilizarBotonPrimario(btnRegistrar, 38, 14.5f);
        btnRegistrar.setPreferredSize(new Dimension(320, 42));

        btnVerificarRostro = new JButton("1. Verificar mi rostro");
        TemaCeleste.estilizarBotonPrimario(btnVerificarRostro, 34, 13.5f);
        btnVerificarRostro.setPreferredSize(new Dimension(320, 38));

        btnGuardarNueva = new JButton("2. Guardar nueva contraseña");
        TemaCeleste.estilizarBotonPrimario(btnGuardarNueva, 34, 13.5f);
        btnGuardarNueva.setPreferredSize(new Dimension(320, 38));

        btnServicioFacial = new JButton("Servicio facial...");
        TemaCeleste.estilizarBotonCelesteSuave(btnServicioFacial, 36, 14.5f);
        btnServicioFacial.setPreferredSize(new Dimension(320, 38));
        btnServicioFacial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        btnSalir = new JButton("Salir");
        TemaCeleste.estilizarBotonClaro(btnSalir, 34, 14f);
        btnSalir.setPreferredSize(new Dimension(320, 34));
        btnSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        // 1. Encabezado superior: Ícono Monitor Facial + SIGELAB
        JPanel panelEncabezado = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 6));
        panelEncabezado.setOpaque(false);
        panelEncabezado.setBorder(BorderFactory.createEmptyBorder(14, 24, 8, 24));

        JLabel lblIconoMonitor = new JLabel(TemaCeleste.crearIconoMonitorFacial(92, 80));
        panelEncabezado.add(lblIconoMonitor);

        JPanel textosEncabezado = new JPanel();
        textosEncabezado.setOpaque(false);
        textosEncabezado.setLayout(new BoxLayout(textosEncabezado, BoxLayout.Y_AXIS));

        lblTitulo = new JLabel("SIGELAB");
        lblTitulo.setFont(TemaCeleste.fuente(Font.BOLD, 32f));
        lblTitulo.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        lblSubtitulo = new JLabel("<html>Sistema de Gestión y Control<br>de Laboratorios</html>");
        lblSubtitulo.setFont(TemaCeleste.fuente(Font.PLAIN, 15f));
        lblSubtitulo.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        textosEncabezado.add(lblTitulo);
        textosEncabezado.add(Box.createVerticalStrut(2));
        textosEncabezado.add(lblSubtitulo);
        panelEncabezado.add(textosEncabezado);

        // 2. Tarjeta central blanca redondeada con las 3 pestañas y botones inferiores
        JPanel tarjetaCentral = TemaCeleste.crearTarjetaBlanca(new BorderLayout(0, 6), 18, new Insets(4, 4, 18, 4));

        tabs = new JTabbedPane();
        tabs.addTab("Ingresar", crearPestanaIngresar());
        tabs.addTab("Regístrate", crearPestanaRegistrate());
        tabs.addTab("Reiniciar contraseña", crearPestanaReiniciar());
        TemaCeleste.estilizarPestanasLogin(tabs);

        JPanel zonaBotonesInferiores = new JPanel();
        zonaBotonesInferiores.setOpaque(false);
        zonaBotonesInferiores.setLayout(new BoxLayout(zonaBotonesInferiores, BoxLayout.Y_AXIS));
        zonaBotonesInferiores.setBorder(BorderFactory.createEmptyBorder(2, 34, 4, 34));

        btnServicioFacial.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSalir.setAlignmentX(Component.CENTER_ALIGNMENT);

        zonaBotonesInferiores.add(crearSeparadorDifuminado());
        zonaBotonesInferiores.add(Box.createVerticalStrut(8));
        zonaBotonesInferiores.add(btnServicioFacial);
        zonaBotonesInferiores.add(Box.createVerticalStrut(8));
        zonaBotonesInferiores.add(crearSeparadorDifuminado());
        zonaBotonesInferiores.add(Box.createVerticalStrut(8));
        zonaBotonesInferiores.add(btnSalir);

        tarjetaCentral.add(tabs, BorderLayout.CENTER);
        tarjetaCentral.add(zonaBotonesInferiores, BorderLayout.SOUTH);

        JPanel contenedorTarjeta = new JPanel(new BorderLayout());
        contenedorTarjeta.setOpaque(false);
        contenedorTarjeta.setBorder(BorderFactory.createEmptyBorder(4, 34, 14, 34));
        contenedorTarjeta.add(tarjetaCentral, BorderLayout.CENTER);

        // 3. Franja inferior de estado del servicio facial
        JPanel franjaPie = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0xD8EEFE), getWidth(), 0, new Color(0xCBE7FE)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(TemaCeleste.CELESTE_SUAVE);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        franjaPie.setOpaque(false);

        lblEstadoServicio = new JLabel("Servicio facial: iniciando...");
        lblEstadoServicio.setFont(TemaCeleste.fuente(Font.PLAIN, 14.5f));
        lblEstadoServicio.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        lblEstadoServicio.setIcon(TemaCeleste.crearPuntoEstado(TemaCeleste.NARANJA_ALERTA));
        lblEstadoServicio.setIconTextGap(10);
        franjaPie.add(lblEstadoServicio);

        // Panel raíz
        panelPrincipal = TemaCeleste.crearPanelFondo(new BorderLayout());
        panelPrincipal.setPreferredSize(new Dimension(520, 665));
        panelPrincipal.add(panelEncabezado, BorderLayout.NORTH);
        panelPrincipal.add(contenedorTarjeta, BorderLayout.CENTER);
        panelPrincipal.add(franjaPie, BorderLayout.SOUTH);
    }

    private JPanel crearPestanaIngresar() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(18, 32, 6, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblUsu = TemaCeleste.crearEtiquetaCampo("Usuario");
        lblUsu.setFont(TemaCeleste.fuente(Font.PLAIN, 15f));
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 2, 6, 2);
        p.add(lblUsu, gbc);

        txtUsuario.setPreferredSize(new Dimension(320, 40));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        p.add(txtUsuario, gbc);

        JLabel lblPass = TemaCeleste.crearEtiquetaCampo("Contraseña");
        lblPass.setFont(TemaCeleste.fuente(Font.PLAIN, 15f));
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 2, 6, 2);
        p.add(lblPass, gbc);

        txtPassword.setPreferredSize(new Dimension(320, 40));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        p.add(txtPassword, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 10, 0);
        p.add(btnIngresar, gbc);

        lblAyudaIngreso = new JLabel(
                "<html><center>Se verificará su contraseña y después su rostro con la cámara.</center></html>",
                SwingConstants.CENTER);
        lblAyudaIngreso.setFont(TemaCeleste.fuente(Font.PLAIN, 12f));
        lblAyudaIngreso.setForeground(TemaCeleste.TEXTO_SECUNDARIO);
        gbc.gridy = 5;
        gbc.insets = new Insets(2, 0, 4, 0);
        p.add(lblAyudaIngreso, gbc);

        return p;
    }

    private JPanel crearPestanaRegistrate() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 24, 6, 24));

        lblAvisoColaborador = new JLabel(
                "Registro para ESTUDIANTES (colaboradores: contactar al admin).",
                SwingConstants.CENTER);
        lblAvisoColaborador.setFont(TemaCeleste.fuente(Font.PLAIN, 12f));
        lblAvisoColaborador.setForeground(new Color(0x1E5A8A));
        JPanel bannerAviso = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(), 12, new Insets(6, 10, 6, 10));
        bannerAviso.add(lblAvisoColaborador, BorderLayout.CENTER);

        JPanel grilla = new JPanel(new GridLayout(3, 2, 12, 8));
        grilla.setOpaque(false);
        grilla.add(campoConEtiqueta("Nombre", txtRegNombre));
        grilla.add(campoConEtiqueta("Apellido", txtRegApellido));
        grilla.add(campoConEtiqueta("Usuario", txtRegUsuario));
        grilla.add(campoConEtiqueta("Correo institucional", txtRegCorreo));
        grilla.add(campoConEtiqueta("Contraseña", txtRegPassword));
        grilla.add(campoConEtiqueta("Confirmar contraseña", txtRegPassword2));

        JPanel pieReg = new JPanel(new BorderLayout());
        pieReg.setOpaque(false);
        pieReg.setBorder(BorderFactory.createEmptyBorder(4, 10, 2, 10));
        pieReg.add(btnRegistrar, BorderLayout.CENTER);

        p.add(bannerAviso, BorderLayout.NORTH);
        p.add(grilla, BorderLayout.CENTER);
        p.add(pieReg, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearPestanaReiniciar() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 24, 6, 24));

        lblEstadoReinicio = new JLabel(
                "Paso 1: escriba su usuario y correo, y verifique su rostro.",
                SwingConstants.CENTER);
        lblEstadoReinicio.setFont(TemaCeleste.fuente(Font.BOLD, 12.5f));
        lblEstadoReinicio.setForeground(TemaCeleste.AZUL_PRINCIPAL);
        JPanel bannerPaso = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(), 12, new Insets(6, 10, 6, 10));
        bannerPaso.add(lblEstadoReinicio, BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel filaPaso1 = new JPanel(new GridLayout(1, 2, 10, 0));
        filaPaso1.setOpaque(false);
        filaPaso1.add(campoConEtiqueta("Usuario", txtResUsuario));
        filaPaso1.add(campoConEtiqueta("Correo registrado", txtResCorreo));

        gbc.gridy = 0;
        gbc.insets = new Insets(2, 0, 6, 0);
        cuerpo.add(filaPaso1, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 10, 10, 10);
        cuerpo.add(btnVerificarRostro, gbc);

        JPanel filaPaso2 = new JPanel(new GridLayout(1, 2, 10, 0));
        filaPaso2.setOpaque(false);
        filaPaso2.add(campoConEtiqueta("Nueva contraseña", txtResNueva));
        filaPaso2.add(campoConEtiqueta("Confirmar nueva", txtResNueva2));

        gbc.gridy = 2;
        gbc.insets = new Insets(2, 0, 6, 0);
        cuerpo.add(filaPaso2, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 10, 2, 10);
        cuerpo.add(btnGuardarNueva, gbc);

        p.add(bannerPaso, BorderLayout.NORTH);
        p.add(cuerpo, BorderLayout.CENTER);
        return p;
    }

    private JPanel campoConEtiqueta(String texto, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(texto);
        l.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        l.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        campo.setPreferredSize(new Dimension(140, 35));
        p.add(l, BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private JComponent crearSeparadorDifuminado() {
        JComponent sep = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int m = w / 2;
                g2.setPaint(new GradientPaint(10, 0, new Color(190, 227, 248, 0),
                        m, 0, new Color(160, 210, 245, 190)));
                g2.drawLine(10, 1, m, 1);
                g2.setPaint(new GradientPaint(m, 0, new Color(160, 210, 245, 190),
                        w - 10, 0, new Color(190, 227, 248, 0)));
                g2.drawLine(m, 1, w - 10, 1);
                g2.dispose();
            }
        };
        sep.setPreferredSize(new Dimension(280, 3));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        return sep;
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------

    private void configurarEventos() {
        btnIngresar.addActionListener(e -> ingresar());
        txtPassword.addActionListener(e -> ingresar());      // Enter en la contraseña
        txtUsuario.addActionListener(e -> txtPassword.requestFocusInWindow());
        btnRegistrar.addActionListener(e -> registrar());
        btnVerificarRostro.addActionListener(e -> verificarRostroParaReinicio());
        btnGuardarNueva.addActionListener(e -> guardarNuevaPassword());
        if (btnServicioFacial != null) {
            btnServicioFacial.addActionListener(e -> alAbrirServicioFacial.run());
        }
        btnSalir.addActionListener(e -> System.exit(0));


        // Salir de la pestaña de reinicio cancela el proceso: nunca queda "desbloqueado" en segundo plano.
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() != PESTANA_REINICIO) {
                cancelarReinicio();
            }
        });
    }

    // ------------------------------------------------------------------
    // 1) Ingresar
    // ------------------------------------------------------------------

    /**
     * Ingreso en DOS pasos obligatorios para TODOS los roles (administrador, encargado, docente y estudiante):
     * 1) usuario y contraseña correctos, 2) el rostro coincide con ese usuario. Sin los dos no se entra.
     */
    private void ingresar() {
        if (ocupado) {
            return;
        }
        final String usuario = txtUsuario.getText();
        final String password = new String(txtPassword.getPassword());
        ejecutar(() -> {
            // Paso 1: contraseña. Si falla NO se abre la cámara.
            Usuario autenticado = authController.autenticar(usuario, password);
            // Paso 2: rostro. Java solo envía el id; la cámara y la comparación las hace el servicio Python.
            FacialVerificationResult resultado = facialClient.verify(autenticado.getIdUsuario());
            if (!resultado.matched()
                    || (resultado.userId() != 0 && resultado.userId() != autenticado.getIdUsuario())) {
                String motivo = mensajeNoCoincide(resultado);
                authController.registrarIngresoFallidoFacial(autenticado, motivo);   // RN-18
                throw new BusinessException(motivo);
            }
            authController.registrarIngresoExitoso(autenticado);                      // RN-18
            return autenticado;
        }, this::entrar);
    }

    private void entrar(Usuario usuario) {
        limpiarContrasenas();
        cerrar();
        alIngresar.accept(usuario);
    }

    // ------------------------------------------------------------------
    // 2) Regístrate
    // ------------------------------------------------------------------

    private void registrar() {
        if (ocupado) {
            return;
        }
        String clave = new String(txtRegPassword.getPassword());
        if (!clave.equals(new String(txtRegPassword2.getPassword()))) {
            Mensajes.advertir(panelPrincipal, TITULO, "Las contraseñas no coinciden.");
            txtRegPassword2.requestFocusInWindow();
            return;
        }
        if (!Mensajes.confirmar(panelPrincipal, "Registro facial",
                "Se creará su cuenta de estudiante y se abrirá la cámara para registrar su rostro.\n"
                        + "Siga las instrucciones en pantalla. ¿Desea continuar?")) {
            return;
        }

        final Usuario datos = new Usuario();
        datos.setNombre(txtRegNombre.getText());
        datos.setApellido(txtRegApellido.getText());
        datos.setNombreUsuario(txtRegUsuario.getText());
        datos.setCorreo(txtRegCorreo.getText());

        ejecutar(() -> {
            // Se comprueba ANTES de crear nada: si no hay cámara/servicio no dejamos cuentas a medias.
            if (!facialClient.disponible()) {
                throw new FacialClientException("El servicio facial no está activo.", 0);
            }
            Usuario creado = authController.registrarEstudiante(datos, clave);
            try {
                FacialEnrollmentResult r = facialClient.enroll(creado.getIdUsuario(), false);
                if (!r.success()) {
                    throw new BusinessException(r.message() == null ? "No se pudo registrar el rostro." : r.message());
                }
            } catch (RuntimeException falla) {
                // Sin rostro no hay cuenta: se deshace para que pueda volver a intentarlo con los mismos datos.
                try {
                    authController.deshacerRegistro(creado.getIdUsuario());
                } catch (RuntimeException ignorada) {
                    LOG.log(Level.WARNING, "No se pudo deshacer el registro del usuario " + creado.getIdUsuario(), ignorada);
                }
                throw falla;
            }
            return creado;
        }, creado -> {
            Mensajes.informar(panelPrincipal, "Registro completado",
                    "¡Listo, " + creado.getNombre() + "! Su cuenta de estudiante fue creada y su rostro registrado.\n\n"
                            + AVISO_COLABORADOR);
            limpiarRegistro();
            txtUsuario.setText(creado.getNombreUsuario());
            tabs.setSelectedIndex(0);
            txtPassword.requestFocusInWindow();
        });
    }

    // ------------------------------------------------------------------
    // 3) Reiniciar contraseña (usuario + correo + rostro)
    // ------------------------------------------------------------------

    private void verificarRostroParaReinicio() {
        if (ocupado || reinicioVerificadoId != 0) {
            return;
        }
        final String usuario = txtResUsuario.getText();
        final String correo = txtResCorreo.getText();
        ejecutar(() -> {
            Usuario cuenta = authController.verificarDatosParaReinicio(usuario, correo);
            FacialVerificationResult resultado = facialClient.verify(cuenta.getIdUsuario());
            if (!resultado.matched() || (resultado.userId() != 0 && resultado.userId() != cuenta.getIdUsuario())) {
                throw new BusinessException(mensajeNoCoincide(resultado));
            }
            return cuenta;
        }, cuenta -> {
            reinicioVerificadoId = cuenta.getIdUsuario();
            lblEstadoReinicio.setText("Rostro verificado. Paso 2: escriba su nueva contraseña.");
            refrescarControles();
            txtResNueva.requestFocusInWindow();
        });
    }

    private void guardarNuevaPassword() {
        if (ocupado || reinicioVerificadoId == 0) {
            return;
        }
        final String nueva = new String(txtResNueva.getPassword());
        if (!nueva.equals(new String(txtResNueva2.getPassword()))) {
            Mensajes.advertir(panelPrincipal, TITULO, "Las contraseñas no coinciden.");
            txtResNueva2.requestFocusInWindow();
            return;
        }
        final int id = reinicioVerificadoId;
        ejecutar(() -> {
            authController.reiniciarPassword(id, nueva);
            return Boolean.TRUE;
        }, ok -> {
            Mensajes.informar(panelPrincipal, "Contraseña actualizada",
                    "Su contraseña fue cambiada. Ya puede ingresar con la nueva.");
            String usuario = txtResUsuario.getText().trim();
            cancelarReinicio();
            txtUsuario.setText(usuario);
            tabs.setSelectedIndex(0);
            txtPassword.requestFocusInWindow();
        });
    }

    /** Vuelve la pestaña de reinicio al paso 1 y borra todo lo escrito. */
    private void cancelarReinicio() {
        reinicioVerificadoId = 0;
        txtResUsuario.setText("");
        txtResCorreo.setText("");
        txtResNueva.setText("");
        txtResNueva2.setText("");
        lblEstadoReinicio.setText("Paso 1: escriba su usuario y correo, y verifique su rostro.");
        refrescarControles();
    }

    // ------------------------------------------------------------------
    // Utilidades de pantalla
    // ------------------------------------------------------------------

    /**
     * Ejecuta una tarea larga fuera del hilo de Swing (la cámara puede tardar decenas de segundos)
     * y devuelve el resultado a la pantalla. Mientras corre, los botones quedan bloqueados.
     */
    private <T> void ejecutar(Callable<T> tarea, Consumer<T> alTerminar) {
        ocupado = true;
        refrescarControles();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                ocupado = false;
                refrescarControles();
                try {
                    alTerminar.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    mostrarError(e.getCause());
                }
            }
        }.execute();
    }

    private void mostrarError(Throwable causa) {
        if (causa instanceof FacialClientException facial) {
            String mensaje = facial.getMessage() == null ? "No se pudo completar la verificación facial." : facial.getMessage();
            if (facial.esSinConexion()) {
                mensaje += "\n\nInicie el servicio con el botón \"Servicio facial...\" de esta pantalla.";
            }
            Mensajes.error(panelPrincipal, TITULO, mensaje);
        } else {
            Mensajes.mostrarExcepcion(panelPrincipal, TITULO, causa);
        }
    }

    private static String mensajeNoCoincide(FacialVerificationResult r) {
        return r.message() == null || r.message().isBlank() ? "El rostro no coincide con el usuario." : r.message();
    }

    /** Habilita o bloquea botones y campos según el estado actual (ocupado / paso del reinicio). */
    private void refrescarControles() {
        boolean libre = !ocupado;
        btnIngresar.setEnabled(libre);
        btnRegistrar.setEnabled(libre);
        if (btnServicioFacial != null) {
            btnServicioFacial.setEnabled(libre);
        }

        boolean paso1 = libre && reinicioVerificadoId == 0;
        txtResUsuario.setEnabled(paso1);
        txtResCorreo.setEnabled(paso1);
        btnVerificarRostro.setEnabled(paso1);

        boolean paso2 = libre && reinicioVerificadoId != 0;
        txtResNueva.setEnabled(paso2);
        txtResNueva2.setEnabled(paso2);
        btnGuardarNueva.setEnabled(paso2);
    }

    private void limpiarContrasenas() {
        txtPassword.setText("");
        txtRegPassword.setText("");
        txtRegPassword2.setText("");
        cancelarReinicio();
    }

    private void limpiarRegistro() {
        txtRegNombre.setText("");
        txtRegApellido.setText("");
        txtRegUsuario.setText("");
        txtRegCorreo.setText("");
        txtRegPassword.setText("");
        txtRegPassword2.setText("");
    }

    // ------------------------------------------------------------------
    // Estado del servicio facial (se consulta cada pocos segundos)
    // ------------------------------------------------------------------

    private void iniciarVigilanciaDelServicio() {
        comprobarServicio();
        temporizadorServicio = new Timer(2500, e -> comprobarServicio());
        temporizadorServicio.setRepeats(true);
        temporizadorServicio.start();
    }

    private void comprobarServicio() {
        if (comprobandoServicio) {
            return;
        }
        comprobandoServicio = true;
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return facialClient.disponible();
            }

            @Override
            protected void done() {
                comprobandoServicio = false;
                try {
                    boolean activo = get();
                    boolean enArranque = FacialServiceLauncher.getInstance().isIniciando()
                            || FacialServiceLauncher.getInstance().isInstalando();
                    lblEstadoServicio.setText(activo ? "Servicio facial: activo"
                            : (enArranque ? "Servicio facial: iniciando..." : "Servicio facial: no disponible"));
                    lblEstadoServicio.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
                    lblEstadoServicio.setIcon(TemaCeleste.crearPuntoEstado(
                            activo ? new Color(0x18E018)
                                    : (enArranque ? TemaCeleste.NARANJA_ALERTA : TemaCeleste.ROJO_PELIGRO)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblEstadoServicio.setText("Servicio facial: no disponible");
                    lblEstadoServicio.setIcon(TemaCeleste.crearPuntoEstado(TemaCeleste.ROJO_PELIGRO));
                }
            }
        }.execute();
    }
}
