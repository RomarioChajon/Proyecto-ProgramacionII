package umg.sigelab.view;

import umg.sigelab.controller.AuthController;
import umg.sigelab.integration.facial.FacialAuthClient;
import umg.sigelab.integration.facial.FacialClientException;
import umg.sigelab.integration.facial.FacialEnrollmentResult;
import umg.sigelab.integration.facial.FacialVerificationResult;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.BusinessException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.Color;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;


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
    //private JButton btnServicioFacial;
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
}
