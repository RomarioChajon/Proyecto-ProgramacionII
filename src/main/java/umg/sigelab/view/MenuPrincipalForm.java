package umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import umg.sigelab.controller.ConsultaController;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Panel principal del ADMINISTRADOR: un botón por cada módulo del sistema.
 *
 * <p>Todos los módulos están conectados: usuarios, servicio facial, laboratorios, categorías, productos,
 * equipos, préstamos y despacho, devoluciones, mantenimiento, bloqueos, reportes y auditoría.</p>
 *
 * <p>Debajo de los botones se muestra el DASHBOARD (PDF 6.11): total de equipos, disponibles, prestados,
 * dañados, en mantenimiento, de baja, préstamos activos y préstamos vencidos, leídos de la base de datos.</p>
 */
public class MenuPrincipalForm {

    // --- Componentes enlazados con MenuPrincipalForm.form ---
    private JPanel panelPrincipal;
    private JLabel lblTitulo;
    private JLabel lblUsuarioActual;
    private JLabel lblAyuda;
    private JButton btnUsuarios;
    private JButton btnLaboratorios;
    private JButton btnCategorias;
    private JButton btnProductos;
    private JButton btnEquipos;
    private JButton btnPrestamos;
    private JButton btnDevoluciones;
    private JButton btnMantenimiento;
    private JButton btnBloqueos;
    private JButton btnReportes;
    private JButton btnAuditoria;
    private JButton btnServicioFacial;
    private JButton btnCerrarSesion;

    /**
     * Contenedor que se entrega a la ventana: el panel del formulario (.form) + el dashboard debajo.
     */
    private final JPanel raiz = new JPanel(new BorderLayout());
    /**
     * Etiquetas de los totales del dashboard, en el orden en que se muestran.
     */
    private final Map<String, JLabel> valoresDashboard = new LinkedHashMap<>();
    private final ConsultaController consultas = new ConsultaController();
    private final Usuario usuario;
    private final Runnable alCerrarSesion;
    /**
     * Ventana de usuarios: se reutiliza para no abrir varias copias.
     */
    private JFrame ventanaUsuarios;
    /**
     * Ventanas de los módulos de inventario, por título (una sola copia de cada una).
     */
    private final Map<String, JFrame> ventanasModulos = new HashMap<>();

    /**
     * @param usuario        administrador con la sesión abierta
     * @param alCerrarSesion qué hacer al pulsar "Cerrar sesión" (volver al login)
     */
    public MenuPrincipalForm(Usuario usuario, Runnable alCerrarSesion) {
        this.usuario = usuario;
        this.alCerrarSesion = alCerrarSesion;
        lblUsuarioActual.setText("Sesión: " + usuario.getNombreCompleto() + "  (" + usuario.getNombreUsuario()
                + ", ID " + usuario.getIdUsuario() + ")");
        configurarEventos();
        lblAyuda.setText("Elija un módulo. Los totales de abajo se leen de la base de datos.");
        raiz.add(panelPrincipal, BorderLayout.CENTER);
        raiz.add(crearDashboard(), BorderLayout.SOUTH);
        actualizarDashboard();
        // Se refresca solo cada 30 segundos mientras la pantalla siga abierta.
        Timer reloj = new Timer(30_000, e -> {
            if (!raiz.isDisplayable()) {
                ((Timer) e.getSource()).stop();
            } else {
                actualizarDashboard();
            }
        });
        reloj.start();
    }

    public JPanel getPanelPrincipal() {
        return raiz;
    }

    /**
     * Franja de tarjetas con los totales (se llenan en {@link #actualizarDashboard()}).
     */
    private JPanel crearDashboard() {
        String[] nombres = {"Total de equipos", "Disponibles", "Prestados", "Dañados", "En mantenimiento",
                "De baja", "Préstamos activos", "Préstamos vencidos"};
        JPanel tarjetas = new JPanel(new GridLayout(2, 4, 8, 8));
        for (String nombre : nombres) {
            JLabel valor = new JLabel("-", JLabel.CENTER);
            valor.setFont(valor.getFont().deriveFont(Font.BOLD, 24f));
            JLabel etiqueta = new JLabel(nombre, JLabel.CENTER);
            JPanel tarjeta = new JPanel(new GridLayout(2, 1));
            tarjeta.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            tarjeta.add(valor);
            tarjeta.add(etiqueta);
            tarjetas.add(tarjeta);
            valoresDashboard.put(nombre, valor);
        }
        JButton btnActualizar = new JButton("Actualizar totales");
        btnActualizar.addActionListener(e -> actualizarDashboard());
        JLabel titulo = new JLabel("Resumen general");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        JPanel encabezado = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        encabezado.add(titulo);
        encabezado.add(btnActualizar);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(tarjetas, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Lee los totales en segundo plano (para no congelar la ventana) y los muestra.
     */
    private void actualizarDashboard() {
        new SwingWorker<Map<String, Integer>, Void>() {
            @Override
            protected Map<String, Integer> doInBackground() {
                return consultas.dashboard(usuario, null);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Integer> datos = get();
                    valoresDashboard.forEach((nombre, etiqueta) ->
                            etiqueta.setText(String.valueOf(datos.getOrDefault(nombre, 0))));
                } catch (Exception ex) {
                    // Sin conexión o error de consulta: se deja el guion y no se interrumpe al administrador.
                    valoresDashboard.values().forEach(etiqueta -> etiqueta.setText("-"));
                }
            }
        }.execute();
    }

    private void configurarEventos() {
        // Módulo terminado
        btnUsuarios.addActionListener(e -> abrirUsuarios());
        btnServicioFacial.addActionListener(e -> ServicioFacialForm.abrir(panelPrincipal));
        btnCerrarSesion.addActionListener(e -> {
            cerrarVentanasHijas();
            alCerrarSesion.run();
        });

        // Módulos de inventario (rama de Geovanni): laboratorios/sedes, categorías, productos y equipos con QR.
        btnLaboratorios.addActionListener(e -> abrirModulo("Laboratorios / sedes", () -> new LaboratorioForm().getPanelPrincipal()));
        btnCategorias.addActionListener(e -> abrirModulo("Categorías de equipo", () -> new TipoEquipoForm().getPanelPrincipal()));
        btnProductos.addActionListener(e -> abrirModulo("Productos", () -> new ProductoForm().getPanelPrincipal()));
        btnEquipos.addActionListener(e -> abrirModulo("Equipos (unidades)", () -> new EquipoForm().getPanelPrincipal()));

        // Préstamos, devoluciones, mantenimiento, bloqueos, reportes y auditoría.
        btnPrestamos.addActionListener(e -> abrirOperaciones("Préstamos y despacho", PanelOperacionesForm.ORDENES));
        btnDevoluciones.addActionListener(e -> abrirOperaciones("Devoluciones", PanelOperacionesForm.DEVOLUCIONES));
        btnMantenimiento.addActionListener(e -> abrirOperaciones("Mantenimiento", PanelOperacionesForm.MANTENIMIENTO));
        btnBloqueos.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.BLOQUEOS));
        btnReportes.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.DASHBOARD));
        btnAuditoria.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.AUDITORIA));
    }

    /**
     * Abre el panel de operaciones (órdenes, inventario, mantenimiento, préstamo por QR, devoluciones)
     * en la sección indicada. El administrador elige la sede en la barra lateral.
     */
    private void abrirOperaciones(String titulo, String seccion) {
        abrirModulo(titulo, () -> {
            JPanel[] referencia = new JPanel[1];
            PanelOperacionesForm formulario = new PanelOperacionesForm(
                    Sesion.getUsuario() != null ? Sesion.getUsuario() : usuario,
                    () -> {
                        Window ventana = SwingUtilities.getWindowAncestor(referencia[0]);
                        if (ventana != null) {
                            ventana.dispose();
                        }
                    },
                    "Cerrar ventana", seccion);
            referencia[0] = formulario.getPanelPrincipal();
            return referencia[0];
        });
    }

    /**
     * Abre el CRUD de usuarios en su propia ventana (o la trae al frente si ya está abierta).
     */
    private void abrirUsuarios() {
        if (ventanaUsuarios != null && ventanaUsuarios.isDisplayable()) {
            ventanaUsuarios.toFront();
            return;
        }
        // Usuario conectado: sirve para no dejar que el administrador se desactive a sí mismo.
        UsuarioForm formulario = new UsuarioForm(Sesion.getUsuario() != null ? Sesion.getUsuario() : usuario);
        ventanaUsuarios = new JFrame("SIGELAB - Usuarios");
        ventanaUsuarios.setContentPane(formulario.getPanelPrincipal());
        ventanaUsuarios.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        ventanaUsuarios.setMinimumSize(new Dimension(1050, 680));
        ventanaUsuarios.pack();
        ventanaUsuarios.setLocationRelativeTo(panelPrincipal);
        ventanaUsuarios.setVisible(true);
    }

    /**
     * Abre un módulo en su propia ventana (o la trae al frente si ya está abierta).
     *
     * @param titulo  nombre del módulo; también identifica la ventana para no abrir copias
     * @param fabrica crea el panel del módulo (se invoca solo si hay que abrir la ventana)
     */
    private void abrirModulo(String titulo, Supplier<JPanel> fabrica) {
        JFrame abierta = ventanasModulos.get(titulo);
        if (abierta != null && abierta.isDisplayable()) {
            abierta.toFront();
            return;
        }
        try {
            JPanel panel = fabrica.get();
            if (panel == null) {
                throw new IllegalStateException("El panel del módulo no se creó. Compile el proyecto desde IntelliJ "
                        + "(Build > Rebuild Project) para que se generen las pantallas de los archivos .form.");
            }
            JFrame ventana = new JFrame("SIGELAB - " + titulo);
            ventana.setContentPane(panel);
            ventana.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            ventana.pack();
            ventana.setLocationRelativeTo(panelPrincipal);
            ventana.setVisible(true);
            ventanasModulos.put(titulo, ventana);
        } catch (Exception | LinkageError ex) {
            // LinkageError: falta alguna librería o clase en tiempo de ejecución.
            Mensajes.error(panelPrincipal, titulo, "No se pudo abrir el módulo \"" + titulo + "\".\n\nDetalle: "
                    + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    private void cerrarVentanasHijas() {
        if (ventanaUsuarios != null) {
            ventanaUsuarios.dispose();
            ventanaUsuarios = null;
        }
        for (JFrame ventana : ventanasModulos.values()) {
            ventana.dispose();
        }
        ventanasModulos.clear();
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
        Font lblTituloFont = this.$$$getFont$$$(null, Font.BOLD, 22, lblTitulo.getFont());
        if (lblTituloFont != null) lblTitulo.setFont(lblTituloFont);
        lblTitulo.setText("PANEL PRINCIPAL - ADMINISTRADOR");
        panelPrincipal.add(lblTitulo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnUsuarios = new JButton();
        btnUsuarios.setText("Crear usuario");
        panel1.add(btnUsuarios, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnLaboratorios = new JButton();
        btnLaboratorios.setText("Laboratorios / sedes");
        panel1.add(btnLaboratorios, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnCategorias = new JButton();
        btnCategorias.setText("Crear categoría de equipo");
        panel1.add(btnCategorias, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnProductos = new JButton();
        btnProductos.setText("Productos");
        panel1.add(btnProductos, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnEquipos = new JButton();
        btnEquipos.setText("Equipos (unidades)");
        panel1.add(btnEquipos, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnPrestamos = new JButton();
        btnPrestamos.setText("Préstamos y despacho");
        panel1.add(btnPrestamos, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnDevoluciones = new JButton();
        btnDevoluciones.setText("Devoluciones");
        panel1.add(btnDevoluciones, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnMantenimiento = new JButton();
        btnMantenimiento.setText("Mantenimiento");
        panel1.add(btnMantenimiento, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnBloqueos = new JButton();
        btnBloqueos.setText("Bloqueos de usuarios");
        panel1.add(btnBloqueos, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnReportes = new JButton();
        btnReportes.setText("Reportes y consultas");
        panel1.add(btnReportes, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnAuditoria = new JButton();
        btnAuditoria.setText("Auditoría");
        panel1.add(btnAuditoria, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnServicioFacial = new JButton();
        btnServicioFacial.setText("Servicio facial");
        panel1.add(btnServicioFacial, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panelPrincipal.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblAyuda = new JLabel();
        lblAyuda.setText("Las opciones sin terminar mostrarán el aviso \"Opción aún en desarrollo\".");
        panel2.add(lblAyuda, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnCerrarSesion = new JButton();
        btnCerrarSesion.setText("CERRAR SESIÓN");
        panel2.add(btnCerrarSesion, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lblUsuarioActual = new JLabel();
        lblUsuarioActual.setText("Sesión: -");
        panelPrincipal.add(lblUsuarioActual, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
