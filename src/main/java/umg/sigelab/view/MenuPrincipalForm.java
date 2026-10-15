
package umg.sigelab.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class MenuPrincipalForm {

    // COMPONENTES DEL FORMULARIO ORIGINAL

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

    private final Usuario usuario;
    private final Runnable alCerrarSesion;

    private JFrame ventanaUsuarios;

    private final Map<String, JFrame> ventanasModulos =
            new HashMap<>();

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public MenuPrincipalForm(
            Usuario usuario,
            Runnable alCerrarSesion
    ) {

        this.usuario = usuario;
        this.alCerrarSesion = alCerrarSesion;

        if (panelPrincipal == null ||
                lblUsuarioActual == null) {

            throw new IllegalStateException(
                    "No se inicializaron los componentes de " +
                            "MenuPrincipalForm. Revise el archivo .form."
            );
        }

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "No se recibió el usuario de la sesión."
            );
        }

        lblUsuarioActual.setText(
                "Sesión: " + usuario.getNombreCompleto()
                        + " (" + usuario.getNombreUsuario()
                        + ", ID " + usuario.getIdUsuario() + ")"
        );

        configurarEventos();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ==========================================
    // EVENTOS DE TODOS LOS BOTONES
    // ==========================================

    private void configurarEventos() {

        // USUARIOS
        btnUsuarios.addActionListener(
                e -> abrirUsuarios()
        );

        // SERVICIO FACIAL
        btnServicioFacial.addActionListener(
                e -> ServicioFacialForm.abrir(panelPrincipal)
        );

        // CERRAR SESIÓN
        btnCerrarSesion.addActionListener(e -> {

            cerrarVentanasHijas();

            if (alCerrarSesion != null) {
                alCerrarSesion.run();
            }
        });

        // LABORATORIOS
        btnLaboratorios.addActionListener(e ->
                abrirModulo(
                        "Laboratorios / sedes",
                        () -> new LaboratorioForm()
                                .getPanelPrincipal()
                )
        );

        // CATEGORÍAS
        btnCategorias.addActionListener(e ->
                abrirModulo(
                        "Categorías de equipo",
                        () -> new TipoEquipoForm()
                                .getPanelPrincipal()
                )
        );

        // PRODUCTOS
        btnProductos.addActionListener(e ->
                abrirModulo(
                        "Productos",
                        () -> new ProductoForm()
                                .getPanelPrincipal()
                )
        );

        // EQUIPOS
        btnEquipos.addActionListener(e ->
                abrirModulo(
                        "Equipos (unidades)",
                        () -> new EquipoForm()
                                .getPanelPrincipal()
                )
        );

        // ==========================================
        // PRÉSTAMOS Y DESPACHO
        // ==========================================

        btnPrestamos.addActionListener(e ->
                abrirModulo(
                        "Préstamos y despacho",
                        this::crearMenuPrestamos
                )
        );

        // ==========================================
        // DEVOLUCIONES
        // ==========================================

        btnDevoluciones.addActionListener(e ->
                abrirModulo(
                        "Devoluciones",
                        this::crearPanelDevoluciones
                )
        );

        // MANTENIMIENTO
        btnMantenimiento.addActionListener(e ->
                enDesarrollo("Mantenimiento")
        );

        // BLOQUEOS
        btnBloqueos.addActionListener(e ->
                enDesarrollo("Bloqueos de usuarios")
        );

        // REPORTES
        btnReportes.addActionListener(e ->
                enDesarrollo("Reportes y consultas")
        );

        // AUDITORÍA
        btnAuditoria.addActionListener(e ->
                enDesarrollo("Auditoría")
        );
    }

    // ==========================================
    // MENÚ INTERMEDIO DE PRÉSTAMOS
    // ==========================================


    private JPanel crearMenuPrestamos() {

        JPanel panel = new JPanel(
                new BorderLayout(15, 20)
        );

        panel.setBorder(
                new EmptyBorder(30, 30, 30, 30)
        );

        JLabel titulo = new JLabel(
                "Módulo de Préstamos y Despacho",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 23)
        );

        panel.add(titulo, BorderLayout.NORTH);

        // OPCIONES DE PRÉSTAMOS
        JPanel opciones = new JPanel(
                new GridLayout(1, 2, 20, 0)
        );

        JButton btnSolicitud = crearTarjeta(
                "Solicitud de Préstamos",
                "DOCENTE / ESTUDIANTE",
                "Solicitar equipos disponibles"
        );

        JButton btnGestion = crearTarjeta(
                "Gestión de Préstamos",
                "ADMIN / ENCARGADO",
                "Aprobar, rechazar y despachar"
        );

        btnSolicitud.addActionListener(
                e -> abrirSolicitudPrestamo()
        );

        btnGestion.addActionListener(
                e -> abrirGestionPrestamos()
        );

        opciones.add(btnSolicitud);
        opciones.add(btnGestion);

        panel.add(opciones, BorderLayout.CENTER);

        // PANEL INFERIOR
        JPanel panelInferior = new JPanel(
                new BorderLayout(10, 10)
        );

        JLabel ayuda = new JLabel(
                "Seleccione una opción para continuar.",
                SwingConstants.CENTER
        );

        panelInferior.add(ayuda, BorderLayout.NORTH);

        // BOTÓN REGRESAR
        JButton btnRegresar = new JButton(
                "← Regresar al panel principal"
        );

        btnRegresar.setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );

        btnRegresar.setFocusPainted(false);

        btnRegresar.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        btnRegresar.addActionListener(e -> {

            JFrame ventana = (JFrame)
                    SwingUtilities.getWindowAncestor(panel);

            if (ventana != null) {
                ventana.dispose();
            }

            ventanasModulos.remove("Préstamos y despacho");
        });

        JPanel panelBoton = new JPanel(
                new FlowLayout(FlowLayout.CENTER)
        );

        panelBoton.add(btnRegresar);

        panelInferior.add(
                panelBoton,
                BorderLayout.CENTER
        );

        panel.add(
                panelInferior,
                BorderLayout.SOUTH
        );

        return panel;
    }


    // ==========================================
    // TARJETAS DEL MENÚ DE PRÉSTAMOS
    // ==========================================

    private JButton crearTarjeta(
            String titulo,
            String rol,
            String descripcion
    ) {

        String texto =
                "<html><center>" +
                        "<h2>" + titulo + "</h2>" +
                        "<p>" + rol + "</p>" +
                        "<p>" + descripcion + "</p>" +
                        "</center></html>";

        JButton boton = new JButton(texto);

        boton.setFont(
                new Font("SansSerif", Font.PLAIN, 14)
        );

        boton.setFocusPainted(false);

        boton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        boton.setPreferredSize(
                new Dimension(320, 220)
        );

        return boton;
    }

    // ==========================================
    // ABRIR SOLICITUD DE PRÉSTAMOS
    // ==========================================

    private void abrirSolicitudPrestamo() {

        abrirModulo(
                "Solicitud de préstamos",
                () -> new SolicitudPrestamoForm()
                        .getPanelPrincipal()
        );
    }

    // ==========================================
    // ABRIR GESTIÓN DE PRÉSTAMOS
    // ==========================================

    private void abrirGestionPrestamos() {

        final String titulo = "Gestión de préstamos";

        JFrame abierta = ventanasModulos.get(titulo);

        if (abierta != null && abierta.isDisplayable()) {
            abierta.toFront();
            abierta.requestFocus();
            return;
        }

        try {

            Object formulario = new GestionPrestamos();

            // Compatible con GestionPrestamos si es JFrame.
            if (formulario instanceof JFrame) {

                JFrame ventana = (JFrame) formulario;

                ventana.setDefaultCloseOperation(
                        WindowConstants.DISPOSE_ON_CLOSE
                );

                if (ventana.getWidth() < 900 ||
                        ventana.getHeight() < 650) {

                    ventana.setSize(
                            Math.max(900, ventana.getWidth()),
                            Math.max(650, ventana.getHeight())
                    );
                }

                ventana.setLocationRelativeTo(panelPrincipal);

                ventanasModulos.put(titulo, ventana);

                ventana.setVisible(true);

                return;
            }

            // Compatible con GestionPrestamos si ofrece
            // el método getPanelPrincipal().
            Method metodo = formulario.getClass()
                    .getMethod("getPanelPrincipal");

            Object resultado = metodo.invoke(formulario);

            if (!(resultado instanceof JPanel)) {
                throw new IllegalStateException(
                        "GestionPrestamos no devolvió un JPanel."
                );
            }

            JPanel panel = (JPanel) resultado;

            JFrame ventana = new JFrame(
                    "SIGELAB - " + titulo
            );

            ventana.setContentPane(panel);

            ventana.setDefaultCloseOperation(
                    WindowConstants.DISPOSE_ON_CLOSE
            );

            ventana.pack();

            ventana.setSize(
                    Math.max(900, ventana.getWidth()),
                    Math.max(650, ventana.getHeight())
            );

            ventana.setLocationRelativeTo(panelPrincipal);

            ventanasModulos.put(titulo, ventana);

            ventana.setVisible(true);

        } catch (Exception | LinkageError ex) {

            Mensajes.error(
                    panelPrincipal,
                    titulo,
                    "No se pudo abrir Gestión de préstamos.\n\n" +
                            "Detalle: " +
                            (
                                    ex.getMessage() == null
                                            ? ex.getClass().getSimpleName()
                                            : ex.getMessage()
                            )
            );
        }
    }

    // ==========================================
    // PANTALLA PROVISIONAL DE DEVOLUCIONES
    // ==========================================

    private JPanel crearPanelDevoluciones() {

        JPanel panel = new JPanel(
                new BorderLayout(15, 20)
        );

        panel.setBorder(
                new EmptyBorder(30, 30, 30, 30)
        );

        JLabel titulo = new JLabel(
                "Módulo de Devoluciones",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 23)
        );

        panel.add(titulo, BorderLayout.NORTH);

        JLabel mensaje = new JLabel(
                "<html><center>" +
                        "<h2>Gestión de Devoluciones</h2>" +
                        "<p>Registro de devolución de equipos</p>" +
                        "<p>Devoluciones totales y parciales</p>" +
                        "<p>Formulario pendiente de implementar</p>" +
                        "</center></html>",
                SwingConstants.CENTER
        );

        panel.add(mensaje, BorderLayout.CENTER);

        return panel;
    }

    // ==========================================
    // AVISO DE OPCIONES EN DESARROLLO
    // ==========================================

    private void enDesarrollo(String opcion) {

        Mensajes.informar(
                panelPrincipal,
                opcion,
                "Opción aún en desarrollo."
        );
    }

    // ==========================================
    // ABRIR USUARIOS
    // ==========================================

    private void abrirUsuarios() {

        if (ventanaUsuarios != null &&
                ventanaUsuarios.isDisplayable()) {

            ventanaUsuarios.toFront();
            ventanaUsuarios.requestFocus();
            return;
        }

        Usuario usuarioActual =
                Sesion.getUsuario() != null
                        ? Sesion.getUsuario()
                        : usuario;

        UsuarioForm formulario =
                new UsuarioForm(usuarioActual);

        ventanaUsuarios =
                new JFrame("SIGELAB - Usuarios");

        ventanaUsuarios.setContentPane(
                formulario.getPanelPrincipal()
        );

        ventanaUsuarios.setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE
        );

        ventanaUsuarios.setMinimumSize(
                new Dimension(1050, 680)
        );

        ventanaUsuarios.pack();

        ventanaUsuarios.setLocationRelativeTo(
                panelPrincipal
        );

        ventanaUsuarios.setVisible(true);
    }

    // ==========================================
    // ABRIR MÓDULOS
    // ==========================================

    private void abrirModulo(
            String titulo,
            Supplier<JPanel> fabrica
    ) {

        JFrame abierta = ventanasModulos.get(titulo);

        if (abierta != null &&
                abierta.isDisplayable()) {

            abierta.toFront();
            abierta.requestFocus();
            return;
        }

        try {

            JPanel panel = fabrica.get();

            if (panel == null) {

                throw new IllegalStateException(
                        "El panel del módulo no se creó. " +
                                "Verifique su archivo .form."
                );
            }

            JFrame ventana =
                    new JFrame("SIGELAB - " + titulo);

            ventana.setContentPane(panel);

            ventana.setDefaultCloseOperation(
                    WindowConstants.DISPOSE_ON_CLOSE
            );

            ventana.pack();

            if (ventana.getWidth() < 900 ||
                    ventana.getHeight() < 650) {

                ventana.setSize(
                        Math.max(900, ventana.getWidth()),
                        Math.max(650, ventana.getHeight())
                );
            }

            ventana.setLocationRelativeTo(
                    panelPrincipal
            );

            ventanasModulos.put(titulo, ventana);

            ventana.setVisible(true);

        } catch (Exception | LinkageError ex) {

            Mensajes.error(
                    panelPrincipal,
                    titulo,
                    "No se pudo abrir el módulo \"" +
                            titulo + "\".\n\nDetalle: " +
                            (
                                    ex.getMessage() == null
                                            ? ex.getClass().getSimpleName()
                                            : ex.getMessage()
                            )
            );
        }
    }

    // ==========================================
    // CERRAR VENTANAS HIJAS
    // ==========================================

    private void cerrarVentanasHijas() {

        if (ventanaUsuarios != null) {

            ventanaUsuarios.dispose();
            ventanaUsuarios = null;
        }

        for (JFrame ventana : ventanasModulos.values()) {

            if (ventana != null) {
                ventana.dispose();
            }
        }

        ventanasModulos.clear();
    }

    // ==========================================
    // CÓDIGO GENERADO POR INTELLIJ GUI DESIGNER
    // ==========================================

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
        lblUsuarioActual = new JLabel();
        lblUsuarioActual.setText("Sesión: -");
        panelPrincipal.add(lblUsuarioActual, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    // ==========================================
    // RESTRICCIONES PARA BOTONES DEL FORMULARIO
    // ==========================================

    private GridConstraints crearRestriccionBoton(
            int fila,
            int columna
    ) {

        return new GridConstraints(
                fila,
                columna,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK |
                        GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
        );
    }

}



