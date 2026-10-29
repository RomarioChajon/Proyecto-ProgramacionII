package umg.sigelab.view;

import umg.sigelab.integration.facial.FacialServiceLauncher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Ventana para instalar, iniciar y detener el servicio facial sin usar la línea de comandos
 * (diseño Celeste y Blanco - Laboratorios de Computación).
 */
public class ServicioFacialForm {

    private static final String TITULO = "Servicio facial";

    // --- Componentes enlazados con ServicioFacialForm.form ---
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JTextField txtRuta;
    private JButton btnExaminar;
    private JLabel lblInstalacion;
    private JLabel lblServicio;
    private JButton btnInstalar;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JCheckBox chkTerminal;
    private JTextArea txtLog;
    private JLabel lblAyuda;
    private JButton btnCerrar;

    private final FacialServiceLauncher launcher;
    private final Consumer<String> oyenteLog = linea -> SwingUtilities.invokeLater(() -> agregarLinea(linea));
    private Timer temporizador;
    private boolean ocupado;
    private boolean consultando;
    private boolean servicioActivo;

    public ServicioFacialForm() {
        this(FacialServiceLauncher.getInstance());
    }

    /** Constructor con el lanzador inyectado (permite pruebas con uno falso). */
    public ServicioFacialForm(FacialServiceLauncher launcher) {
        this.launcher = launcher;

        construirInterfazCeleste();

        txtRuta.setText(launcher.getCarpeta().toString());
        chkTerminal.setSelected(launcher.isUsarTerminal());
        chkTerminal.setVisible(FacialServiceLauncher.esMac()); // solo tiene sentido en macOS
        lblAyuda.setText("Deje el servicio activo mientras use el reconocimiento facial.");
        for (String linea : launcher.historial()) {
            agregarLinea(linea);
        }
        launcher.agregarOyente(oyenteLog);

        configurarEventos();
        refrescarEstado();
        temporizador = new Timer(2000, e -> refrescarEstado());
        temporizador.start();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    /** Abre la ventana (no modal: el login y el menú siguen usables) centrada sobre {@code padre}. */
    public static void abrir(Component padre) {
        ServicioFacialForm formulario = new ServicioFacialForm();
        Window propietario = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(propietario, "SIGELAB - Servicio facial");
        dialogo.setContentPane(formulario.getPanelPrincipal());
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                formulario.liberar();
            }
        });
        formulario.btnCerrar.addActionListener(e -> dialogo.dispose());
        dialogo.pack();
        dialogo.setLocationRelativeTo(padre);
        dialogo.setVisible(true);
    }

    /** Detiene el temporizador y deja de escuchar el registro. */
    public void liberar() {
        if (temporizador != null) {
            temporizador.stop();
        }
        launcher.quitarOyente(oyenteLog);
    }

    // ------------------------------------------------------------------
    // Construcción visual Celeste y Blanco
    // ------------------------------------------------------------------

    private void construirInterfazCeleste() {
        // 1. Encabezado superior azul (#2B8FD6)
        JPanel barraSuperior = TemaCeleste.crearEncabezadoAzul(new BorderLayout(12, 0));
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel izqHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izqHeader.setOpaque(false);
        JLabel iconoHeader = new JLabel(TemaCeleste.crearIconoDashboard(
                TemaCeleste.TipoIcono.SERVICIO_FACIAL, 38, new Color(255, 255, 255, 38)));
        lblTitulo = new JLabel("SIGELAB - Servicio de Reconocimiento Facial");
        lblTitulo.setFont(TemaCeleste.fuente(Font.BOLD, 19f));
        lblTitulo.setForeground(Color.WHITE);
        izqHeader.add(iconoHeader);
        izqHeader.add(lblTitulo);

        JLabel subHeader = new JLabel("Motor Biométrico y QR  ");
        subHeader.setFont(TemaCeleste.fuente(Font.PLAIN, 13f));
        subHeader.setForeground(new Color(0xE1F2FE));

        barraSuperior.add(izqHeader, BorderLayout.WEST);
        barraSuperior.add(subHeader, BorderLayout.EAST);

        // 2. Tarjeta superior: Ruta + Estado + Botones de control
        JPanel tarjetaControl = TemaCeleste.crearTarjetaBlanca(new BorderLayout(0, 14), 18, new Insets(16, 20, 16, 20));

        // Fila 1: Carpeta del servicio
        JPanel filaRuta = new JPanel(new BorderLayout(10, 0));
        filaRuta.setOpaque(false);
        JLabel lblCarpeta = new JLabel("Carpeta del servicio:");
        lblCarpeta.setFont(TemaCeleste.fuente(Font.BOLD, 13.5f));
        lblCarpeta.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        txtRuta = TemaCeleste.crearCampoTextoRedondeado();
        txtRuta.setPreferredSize(new Dimension(380, 38));

        btnExaminar = new JButton("Examinar...");
        TemaCeleste.estilizarBotonCelesteSuave(btnExaminar, 22, 13.5f);
        btnExaminar.setPreferredSize(new Dimension(125, 38));

        filaRuta.add(lblCarpeta, BorderLayout.WEST);
        filaRuta.add(txtRuta, BorderLayout.CENTER);
        filaRuta.add(btnExaminar, BorderLayout.EAST);

        // Fila 2: Indicadores de estado (Instalación y Servicio)
        JPanel filaEstados = new JPanel(new GridLayout(1, 2, 14, 0));
        filaEstados.setOpaque(false);

        JPanel badgeInstalacion = TemaCeleste.crearSubTarjetaCeleste(new FlowLayout(FlowLayout.LEFT, 10, 8), 14, new Insets(2, 8, 2, 8));
        lblInstalacion = new JLabel("Instalación: comprobando...");
        lblInstalacion.setFont(TemaCeleste.fuente(Font.BOLD, 13.5f));
        lblInstalacion.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        lblInstalacion.setIcon(TemaCeleste.crearPuntoEstado(TemaCeleste.NARANJA_ALERTA));
        lblInstalacion.setIconTextGap(8);
        badgeInstalacion.add(lblInstalacion);

        JPanel badgeServicio = TemaCeleste.crearSubTarjetaCeleste(new FlowLayout(FlowLayout.LEFT, 10, 8), 14, new Insets(2, 8, 2, 8));
        lblServicio = new JLabel("Servicio: comprobando...");
        lblServicio.setFont(TemaCeleste.fuente(Font.BOLD, 13.5f));
        lblServicio.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        lblServicio.setIcon(TemaCeleste.crearPuntoEstado(TemaCeleste.NARANJA_ALERTA));
        lblServicio.setIconTextGap(8);
        badgeServicio.add(lblServicio);

        filaEstados.add(badgeInstalacion);
        filaEstados.add(badgeServicio);

        // Fila 3: Botones de acción (Instalar, Iniciar, Detener)
        JPanel filaAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filaAcciones.setOpaque(false);

        btnInstalar = new JButton("1. Instalar dependencias");
        TemaCeleste.estilizarBotonCelesteSuave(btnInstalar, 28, 13.5f);
        btnInstalar.setPreferredSize(new Dimension(195, 38));

        btnIniciar = new JButton("2. Iniciar servicio");
        TemaCeleste.estilizarBotonPrimario(btnIniciar, 28, 14f);
        btnIniciar.setPreferredSize(new Dimension(170, 38));

        btnDetener = new JButton("Detener servicio");
        TemaCeleste.estilizarBotonPeligro(btnDetener, 28, 13.5f);
        btnDetener.setPreferredSize(new Dimension(155, 38));

        chkTerminal = new JCheckBox("Abrir en Terminal (macOS)");
        chkTerminal.setOpaque(false);
        chkTerminal.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        chkTerminal.setForeground(TemaCeleste.TEXTO_SECUNDARIO);

        filaAcciones.add(btnInstalar);
        filaAcciones.add(btnIniciar);
        filaAcciones.add(btnDetener);
        filaAcciones.add(chkTerminal);

        tarjetaControl.add(filaRuta, BorderLayout.NORTH);
        tarjetaControl.add(filaEstados, BorderLayout.CENTER);
        tarjetaControl.add(filaAcciones, BorderLayout.SOUTH);

        // 3. Tarjeta inferior: Registro de actividad (Consola)
        JPanel tarjetaLog = TemaCeleste.crearTarjetaBlanca(new BorderLayout(0, 8), 18, new Insets(14, 20, 16, 20));
        JLabel lblTituloLog = new JLabel("Registro de actividad del servicio");
        lblTituloLog.setFont(TemaCeleste.fuente(Font.BOLD, 14.5f));
        lblTituloLog.setForeground(TemaCeleste.AZUL_PRINCIPAL);

        txtLog = new JTextArea(11, 60);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(0xF7FBFF));
        txtLog.setForeground(new Color(0x1E293B));
        txtLog.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(new TemaCeleste.BordeRedondeadoSimple(TemaCeleste.CELESTE_SUAVE, 14, 1.2f));
        scrollLog.getViewport().setBackground(new Color(0xF7FBFF));

        tarjetaLog.add(lblTituloLog, BorderLayout.NORTH);
        tarjetaLog.add(scrollLog, BorderLayout.CENTER);

        // Contenedor central con ambas tarjetas
        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));
        cuerpo.add(tarjetaControl, BorderLayout.NORTH);
        cuerpo.add(tarjetaLog, BorderLayout.CENTER);

        // 4. Barra inferior de ayuda y botón Cerrar
        JPanel franjaPie = new JPanel(new BorderLayout(12, 0)) {
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
        franjaPie.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        lblAyuda = new JLabel("Deje el servicio activo mientras use el reconocimiento facial.");
        lblAyuda.setFont(TemaCeleste.fuente(Font.PLAIN, 13f));
        lblAyuda.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        btnCerrar = new JButton("Cerrar ventana");
        TemaCeleste.estilizarBotonClaro(btnCerrar, 26, 13.5f);
        btnCerrar.setPreferredSize(new Dimension(145, 34));

        franjaPie.add(lblAyuda, BorderLayout.CENTER);
        franjaPie.add(btnCerrar, BorderLayout.EAST);

        panelPrincipal = TemaCeleste.crearPanelFondo(new BorderLayout());
        panelPrincipal.setPreferredSize(new Dimension(760, 560));
        panelPrincipal.add(barraSuperior, BorderLayout.NORTH);
        panelPrincipal.add(cuerpo, BorderLayout.CENTER);
        panelPrincipal.add(franjaPie, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------

    private void configurarEventos() {
        btnExaminar.addActionListener(e -> examinar());
        btnInstalar.addActionListener(e -> instalar());
        btnIniciar.addActionListener(e -> iniciar());
        btnDetener.addActionListener(e -> detener());
        chkTerminal.addActionListener(e -> launcher.setUsarTerminal(chkTerminal.isSelected()));
    }

    private void examinar() {
        JFileChooser selector = new JFileChooser(txtRuta.getText());
        selector.setDialogTitle("Seleccione la carpeta facial-service");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (selector.showOpenDialog(panelPrincipal) == JFileChooser.APPROVE_OPTION) {
            txtRuta.setText(selector.getSelectedFile().getAbsolutePath());
            guardarRuta();
        }
    }

    /** Valida y recuerda la carpeta escrita. @return true si es válida */
    private boolean guardarRuta() {
        Path carpeta = Paths.get(txtRuta.getText().trim());
        String error = FacialServiceLauncher.validarCarpeta(carpeta);
        if (error != null) {
            Mensajes.advertir(panelPrincipal, TITULO, error);
            return false;
        }
        launcher.setCarpeta(carpeta);
        refrescarEstado();
        return true;
    }

    private void instalar() {
        if (ocupado || !guardarRuta()) {
            return;
        }
        if (!Mensajes.confirmar(panelPrincipal, TITULO,
                "La instalación descarga e instala Python, las librerías y el modelo facial.\n"
                        + "Necesita internet y puede tardar varios minutos. ¿Desea continuar?")) {
            return;
        }
        ejecutar(() -> {
            launcher.instalar();
            return null;
        });
    }

    private void iniciar() {
        if (ocupado || !guardarRuta()) {
            return;
        }
        ejecutar(launcher::iniciar);
    }

    private void detener() {
        if (ocupado) {
            return;
        }
        ejecutar(() -> {
            launcher.detener();
            return null;
        });
    }

    /** Corre la acción en segundo plano bloqueando los botones hasta que termine. */
    private void ejecutar(java.util.concurrent.Callable<?> accion) {
        ocupado = true;
        actualizarBotones();
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                return accion.call();
            }

            @Override
            protected void done() {
                ocupado = false;
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    Mensajes.error(panelPrincipal, TITULO, String.valueOf(e.getCause().getMessage()));
                }
                refrescarEstado();
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Estado y registro
    // ------------------------------------------------------------------

    /** Consulta (fuera del hilo de Swing) si el servicio responde y si está instalado, y actualiza las etiquetas. */
    private void refrescarEstado() {
        if (consultando) {
            return;
        }
        consultando = true;
        new SwingWorker<boolean[], Void>() {
            @Override
            protected boolean[] doInBackground() {
                return new boolean[]{launcher.servicioActivo(), launcher.instalado()};
            }

            @Override
            protected void done() {
                consultando = false;
                try {
                    boolean[] estado = get();
                    servicioActivo = estado[0];
                    lblServicio.setText(estado[0] ? "Servicio: ACTIVO (Puerto 8765)" : "Servicio: detenido");
                    lblServicio.setForeground(estado[0] ? new Color(0x15803D) : new Color(0xB91C1C));
                    lblServicio.setIcon(TemaCeleste.crearPuntoEstado(
                            estado[0] ? new Color(0x18E018) : TemaCeleste.ROJO_PELIGRO));

                    lblInstalacion.setText(estado[1] ? "Instalación: completa (.venv listo)" : "Instalación: pendiente");
                    lblInstalacion.setForeground(estado[1] ? new Color(0x15803D) : new Color(0xB45309));
                    lblInstalacion.setIcon(TemaCeleste.crearPuntoEstado(
                            estado[1] ? new Color(0x18E018) : TemaCeleste.NARANJA_ALERTA));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    lblServicio.setText("Servicio: desconocido");
                }
                actualizarBotones();
            }
        }.execute();
    }

    private void actualizarBotones() {
        btnInstalar.setEnabled(!ocupado);
        btnIniciar.setEnabled(!ocupado && !servicioActivo);
        btnDetener.setEnabled(!ocupado && servicioActivo);
        btnExaminar.setEnabled(!ocupado);
        txtRuta.setEnabled(!ocupado);
    }

    private void agregarLinea(String linea) {
        txtLog.append(linea + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength()); // siempre muestra lo último
    }
}
