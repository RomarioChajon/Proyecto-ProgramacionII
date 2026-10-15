package umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import umg.sigelab.integration.facial.FacialServiceLauncher;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Ventana para instalar, iniciar y detener el servicio facial sin usar la línea de comandos.
 *
 * <p>Toda la lógica de procesos está en {@link FacialServiceLauncher}; aquí solo se muestran los botones
 * y el registro de mensajes. Las acciones largas corren en un {@link SwingWorker} para no congelar la ventana.</p>
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

    /**
     * Constructor con el lanzador inyectado (permite pruebas con uno falso).
     */
    public ServicioFacialForm(FacialServiceLauncher launcher) {
        this.launcher = launcher;

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

    /**
     * Abre la ventana (no modal: el login y el menú siguen usables) centrada sobre {@code padre}.
     */
    public static void abrir(Component padre) {
        ServicioFacialForm formulario = new ServicioFacialForm();
        Window propietario = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(propietario, TITULO);
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

    /**
     * Detiene el temporizador y deja de escuchar el registro.
     */
    public void liberar() {
        temporizador.stop();
        launcher.quitarOyente(oyenteLog);
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

    /**
     * Valida y recuerda la carpeta escrita. @return true si es válida
     */
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

    /**
     * Corre la acción en segundo plano bloqueando los botones hasta que termine.
     */
    private void ejecutar(Callable<?> accion) {
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
                } catch (ExecutionException e) {
                    Mensajes.error(panelPrincipal, TITULO, String.valueOf(e.getCause().getMessage()));
                }
                refrescarEstado();
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Estado y registro
    // ------------------------------------------------------------------

    /**
     * Consulta (fuera del hilo de Swing) si el servicio responde y si está instalado, y actualiza las etiquetas.
     */
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
                    lblServicio.setText(estado[0] ? "Servicio: ACTIVO" : "Servicio: detenido");
                    lblServicio.setForeground(estado[0] ? new Color(0x15803D) : new Color(0xB91C1C));
                    lblInstalacion.setText(estado[1] ? "Instalación: completa" : "Instalación: pendiente");
                    lblInstalacion.setForeground(estado[1] ? new Color(0x15803D) : new Color(0xB45309));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
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
        panelPrincipal.setLayout(new GridLayoutManager(6, 1, new Insets(12, 12, 12, 12), -1, -1));
        lblTitulo = new JLabel();
        Font lblTituloFont = this.$$$getFont$$$(null, Font.BOLD, 18, lblTitulo.getFont());
        if (lblTituloFont != null) lblTitulo.setFont(lblTituloFont);
        lblTitulo.setText("SERVICIO DE RECONOCIMIENTO FACIAL");
        panelPrincipal.add(lblTitulo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Carpeta facial-service");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtRuta = new JTextField();
        panel1.add(txtRuta, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnExaminar = new JButton();
        btnExaminar.setText("Examinar...");
        panel1.add(btnExaminar, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblInstalacion = new JLabel();
        lblInstalacion.setText("Instalación: -");
        panel2.add(lblInstalacion, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblServicio = new JLabel();
        lblServicio.setText("Servicio: -");
        panel2.add(lblServicio, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnInstalar = new JButton();
        btnInstalar.setText("1. Instalar");
        panel3.add(btnInstalar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnIniciar = new JButton();
        btnIniciar.setText("2. Iniciar");
        panel3.add(btnIniciar, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDetener = new JButton();
        btnDetener.setText("Detener");
        panel3.add(btnDetener, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chkTerminal = new JCheckBox();
        chkTerminal.setText("Usar Terminal (macOS)");
        panel3.add(chkTerminal, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(640, 260), null, 0, false));
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        scrollPane1.setViewportView(txtLog);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel4, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblAyuda = new JLabel();
        lblAyuda.setText("Deje el servicio activo mientras use el reconocimiento facial.");
        panel4.add(lblAyuda, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnCerrar = new JButton();
        btnCerrar.setText("CERRAR");
        panel4.add(btnCerrar, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
