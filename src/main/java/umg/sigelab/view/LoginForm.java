package umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import umg.sigelab.controller.AuthController;
import umg.sigelab.integration.facial.*;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.BusinessException;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pantalla de acceso con tres pestañas:
 * <ol>
 *   <li><b>Ingresar</b>: usuario + contraseña + reconocimiento facial (los tres son obligatorios).</li>
 *   <li><b>Regístrate</b>: crea una cuenta de ESTUDIANTE y captura el rostro.</li>
 *   <li><b>Reiniciar contraseña</b>: usuario + correo + rostro; si el rostro coincide se habilita la contraseña nueva.</li>
 * </ol>
 *
 * <p>Esta clase solo arma la pantalla y coordina: las reglas están en {@code AuthService} y la cámara
 * en el servicio facial. Los componentes se dibujan en {@code LoginForm.form} (diseñador de IntelliJ).</p>
 */
public class LoginForm {

    private static final String TITULO = "SIGELAB";
    private static final Logger LOG = Logger.getLogger(LoginForm.class.getName());

    /**
     * Mensaje que pide el enunciado para quienes no son estudiantes.
     */
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
    //private JButton btnServicioFacial;
    private JButton btnSalir;
    private JLabel lblTelefono;
    private JTextField txtTelefono;

    private final AuthController authController;
    private final FacialAuthClient facialClient;
    private final Consumer<Usuario> alIngresar;
    private final Runnable alAbrirServicioFacial;

    /**
     * true mientras corre una operación larga (cámara, base de datos): bloquea los botones.
     */
    private boolean ocupado;
    /**
     * Id del usuario cuyo rostro YA fue verificado en el reinicio de contraseña (0 = ninguno).
     */
    private int reinicioVerificadoId;
    private boolean comprobandoServicio;
    private Timer temporizadorServicio;

    public LoginForm(Consumer<Usuario> alIngresar, Runnable alAbrirServicioFacial) {
        this(new AuthController(), new FacialAuthClient(), alIngresar, alAbrirServicioFacial);
    }

    /**
     * Constructor completo: permite inyectar controlador y cliente falsos en pruebas.
     */
    public LoginForm(AuthController authController, FacialAuthClient facialClient,
                     Consumer<Usuario> alIngresar, Runnable alAbrirServicioFacial) {
        this.authController = authController;
        this.facialClient = facialClient;
        this.alIngresar = alIngresar;
        this.alAbrirServicioFacial = alAbrirServicioFacial;


        lblAvisoColaborador.setToolTipText(AVISO_COLABORADOR);
        configurarEventos();
        refrescarControles();
        iniciarVigilanciaDelServicio();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    /**
     * Detiene el temporizador que consulta el servicio facial. Llamar al salir de esta pantalla.
     */
    public void cerrar() {
        if (temporizadorServicio != null) {
            temporizadorServicio.stop();
        }
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
        //btnServicioFacial.addActionListener(e -> alAbrirServicioFacial.run());
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
        datos.setTelefono(txtTelefono.getText());

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

    /**
     * Vuelve la pestaña de reinicio al paso 1 y borra todo lo escrito.
     */
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

    /**
     * Habilita o bloquea botones y campos según el estado actual (ocupado / paso del reinicio).
     */
    private void refrescarControles() {
        boolean libre = !ocupado;
        btnIngresar.setEnabled(libre);
        btnRegistrar.setEnabled(libre);
        // btnServicioFacial.setEnabled(libre);

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
        temporizadorServicio = new Timer(5000, e -> comprobarServicio());
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
                    lblEstadoServicio.setText(activo ? "Servicio facial: ACTIVO" : "Servicio facial: no disponible");
                    lblEstadoServicio.setForeground(activo ? new Color(0x15803D) : new Color(0xB91C1C));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblEstadoServicio.setText("Servicio facial: no disponible");
                }
            }
        }.execute();
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
        panelPrincipal.setLayout(new GridLayoutManager(4, 1, new Insets(14, 14, 14, 14), -1, -1));
        lblTitulo = new JLabel();
        Font lblTituloFont = this.$$$getFont$$$(null, Font.BOLD, 28, lblTitulo.getFont());
        if (lblTituloFont != null) lblTitulo.setFont(lblTituloFont);
        lblTitulo.setHorizontalAlignment(0);
        lblTitulo.setText("SIGELAB");
        panelPrincipal.add(lblTitulo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblSubtitulo = new JLabel();
        lblSubtitulo.setHorizontalAlignment(0);
        lblSubtitulo.setText("Sistema de Gestión y Control de Laboratorios");
        panelPrincipal.add(lblSubtitulo, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabs = new JTabbedPane();
        panelPrincipal.add(tabs, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(14, 14, 14, 14), -1, -1));
        tabs.addTab("Ingresar", panel1);
        final JLabel label1 = new JLabel();
        label1.setText("Usuario");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtUsuario = new JTextField();
        panel1.add(txtUsuario, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Contraseña");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtPassword = new JPasswordField();
        panel1.add(txtPassword, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnIngresar = new JButton();
        btnIngresar.setText("INGRESAR");
        panel1.add(btnIngresar, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblAyudaIngreso = new JLabel();
        lblAyudaIngreso.setHorizontalAlignment(0);
        lblAyudaIngreso.setText("<html><center>Se verificará su contraseña y después su rostro.<br>Se abrirá la cámara: siga las instrucciones.</center></html>");
        panel1.add(lblAyudaIngreso, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(10, 2, new Insets(14, 14, 14, 14), -1, -1));
        tabs.addTab("Regístrate", panel2);
        final JLabel label3 = new JLabel();
        label3.setText("Nombre *");
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegNombre = new JTextField();
        panel2.add(txtRegNombre, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Apellido *");
        panel2.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegApellido = new JTextField();
        panel2.add(txtRegApellido, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Usuario *");
        panel2.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegUsuario = new JTextField();
        panel2.add(txtRegUsuario, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Correo *");
        panel2.add(label6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegCorreo = new JTextField();
        panel2.add(txtRegCorreo, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Contraseña *");
        panel2.add(label7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegPassword = new JPasswordField();
        panel2.add(txtRegPassword, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Repetir contraseña *");
        panel2.add(label8, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRegPassword2 = new JPasswordField();
        panel2.add(txtRegPassword2, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        lblAvisoColaborador = new JLabel();
        lblAvisoColaborador.setForeground(new Color(-4959479));
        lblAvisoColaborador.setText("<html>Si usted es colaborador de la institución por favor contactarse con el administrador.<br>(Todas las cuentas nuevas se crean como estudiante.)</html>");
        panel2.add(lblAvisoColaborador, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(380, 44), null, 0, false));
        btnRegistrar = new JButton();
        btnRegistrar.setText("REGISTRARME Y CAPTURAR ROSTRO");
        panel2.add(btnRegistrar, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setHorizontalAlignment(0);
        label9.setText("<html><center>Se abrirá la cámara para registrar su rostro.</center></html>");
        panel2.add(label9, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblTelefono = new JLabel();
        lblTelefono.setText("Teléfono *");
        panel2.add(lblTelefono, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtTelefono = new JTextField();
        panel2.add(txtTelefono, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(8, 2, new Insets(14, 14, 14, 14), -1, -1));
        tabs.addTab("Reiniciar contraseña", panel3);
        final JLabel label10 = new JLabel();
        label10.setText("Usuario");
        panel3.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtResUsuario = new JTextField();
        panel3.add(txtResUsuario, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Correo");
        panel3.add(label11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtResCorreo = new JTextField();
        panel3.add(txtResCorreo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnVerificarRostro = new JButton();
        btnVerificarRostro.setText("VERIFICAR CON RECONOCIMIENTO FACIAL");
        panel3.add(btnVerificarRostro, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblEstadoReinicio = new JLabel();
        lblEstadoReinicio.setHorizontalAlignment(0);
        lblEstadoReinicio.setText("Paso 1: escriba su usuario y correo, y verifique su rostro.");
        panel3.add(lblEstadoReinicio, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Nueva contraseña");
        panel3.add(label12, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtResNueva = new JPasswordField();
        panel3.add(txtResNueva, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Repetir contraseña");
        panel3.add(label13, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtResNueva2 = new JPasswordField();
        panel3.add(txtResNueva2, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnGuardarNueva = new JButton();
        btnGuardarNueva.setText("GUARDAR NUEVA CONTRASEÑA");
        panel3.add(btnGuardarNueva, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setHorizontalAlignment(0);
        label14.setText("<html><center>La nueva contraseña se habilita solo si su rostro coincide.</center></html>");
        panel3.add(label14, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblEstadoServicio = new JLabel();
        lblEstadoServicio.setText("Servicio facial: comprobando...");
        panel4.add(lblEstadoServicio, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnSalir = new JButton();
        btnSalir.setText("SALIR");
        panel4.add(btnSalir, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panelPrincipal;
    }

}
