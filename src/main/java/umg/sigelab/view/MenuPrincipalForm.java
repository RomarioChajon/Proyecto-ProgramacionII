package umg.sigelab.view;

import umg.sigelab.controller.ConsultaController;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Panel principal del ADMINISTRADOR (diseño Celeste y Blanco - Laboratorios de Computación):
 * tarjetas blancas redondeadas con íconos celestes para los 12 módulos del sistema y
 * el Dashboard de Estado en la parte inferior.
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
    private JButton btnDeteccionEmociones;

    /** Contenedor que se entrega a la ventana: encabezado + cuadrícula de módulos + dashboard. */
    private final JPanel raiz = TemaCeleste.crearPanelFondo(new BorderLayout(0, 8));
    /** Etiquetas de los totales del dashboard, en el orden en que se muestran. */
    private final Map<String, JLabel> valoresDashboard = new LinkedHashMap<>();
    private final ConsultaController consultas = new ConsultaController();
    private final Usuario usuario;
    private final Runnable alCerrarSesion;
    /** Ventana de usuarios: se reutiliza para no abrir varias copias. */
    private JFrame ventanaUsuarios;
    /** Ventanas de los módulos de inventario, por título (una sola copia de cada una). */
    private final Map<String, JFrame> ventanasModulos = new HashMap<>();

    /**
     * @param usuario        administrador con la sesión abierta
     * @param alCerrarSesion qué hacer al pulsar "Cerrar sesión" (volver al login)
     */
    public MenuPrincipalForm(Usuario usuario, Runnable alCerrarSesion) {
        this.usuario = usuario;
        this.alCerrarSesion = alCerrarSesion;

        construirInterfazCeleste();

        String rolTexto = usuario.getRol() != null ? usuario.getRol().getEtiqueta() : "Administrador";
        lblUsuarioActual.setText("Usuario: " + usuario.getNombreUsuario() + " (" + rolTexto + ")");
        lblAyuda.setText("Seleccione un módulo para administrarlo.");

        configurarEventos();
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

    // ------------------------------------------------------------------
    // Construcción visual Celeste y Blanco (fiel a mockup_menu_admin_celeste)
    // ------------------------------------------------------------------

    private void construirInterfazCeleste() {
        // 1. Barra superior azul (#2B8FD6)
        JPanel barraSuperior = TemaCeleste.crearEncabezadoAzul(new BorderLayout(16, 0));
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        lblTitulo = new JLabel("SIGELAB - Panel principal");
        lblTitulo.setFont(TemaCeleste.fuente(Font.BOLD, 22f));
        lblTitulo.setForeground(Color.WHITE);

        JPanel panelSesion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panelSesion.setOpaque(false);

        JLabel iconoAvatar = new JLabel(TemaCeleste.crearIconoCuadrado(
                TemaCeleste.TipoIcono.AVATAR_BLANCO, 30, null, Color.WHITE));
        lblUsuarioActual = new JLabel("Usuario: admin (Administrador)");
        lblUsuarioActual.setFont(TemaCeleste.fuente(Font.PLAIN, 14.5f));
        lblUsuarioActual.setForeground(Color.WHITE);

        btnCerrarSesion = new JButton("Cerrar sesión");
        TemaCeleste.estilizarBotonEncabezado(btnCerrarSesion);

        panelSesion.add(iconoAvatar);
        panelSesion.add(lblUsuarioActual);
        panelSesion.add(btnCerrarSesion);

        barraSuperior.add(lblTitulo, BorderLayout.WEST);
        barraSuperior.add(panelSesion, BorderLayout.EAST);

        // 2. Cuadrícula 3x4 de módulos (tarjetas blancas redondeadas con ícono celeste)
        btnUsuarios = new JButton();
        btnLaboratorios = new JButton();
        btnCategorias = new JButton();
        btnProductos = new JButton();
        btnEquipos = new JButton();
        btnPrestamos = new JButton();
        btnDevoluciones = new JButton();
        btnMantenimiento = new JButton();
        btnBloqueos = new JButton();
        btnReportes = new JButton();
        btnAuditoria = new JButton();
        btnServicioFacial = new JButton();
        btnDeteccionEmociones = new JButton();
        TemaCeleste.estilizarBotonModuloAdmin(btnUsuarios, "Usuarios", TemaCeleste.TipoIcono.USUARIOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnLaboratorios, "Laboratorios", TemaCeleste.TipoIcono.LABORATORIOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnCategorias, "Categorías", TemaCeleste.TipoIcono.CATEGORIAS);
        TemaCeleste.estilizarBotonModuloAdmin(btnProductos, "Productos", TemaCeleste.TipoIcono.PRODUCTOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnEquipos, "Equipos", TemaCeleste.TipoIcono.EQUIPOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnPrestamos, "Préstamos", TemaCeleste.TipoIcono.PRESTAMOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnDevoluciones, "Devoluciones", TemaCeleste.TipoIcono.DEVOLUCIONES);
        TemaCeleste.estilizarBotonModuloAdmin(btnMantenimiento, "Mantenimiento", TemaCeleste.TipoIcono.MANTENIMIENTO);
        TemaCeleste.estilizarBotonModuloAdmin(btnBloqueos, "Bloqueos", TemaCeleste.TipoIcono.BLOQUEOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnReportes, "Reportes", TemaCeleste.TipoIcono.REPORTES);
        TemaCeleste.estilizarBotonModuloAdmin(btnAuditoria, "Auditoría", TemaCeleste.TipoIcono.AUDITORIA);
        TemaCeleste.estilizarBotonModuloAdmin(btnServicioFacial, "Servicio facial", TemaCeleste.TipoIcono.SERVICIO_FACIAL);
        TemaCeleste.estilizarBotonModuloAdmin(
                btnDeteccionEmociones,
                "Detección de emociones",
                TemaCeleste.TipoIcono.MONITOR_FACIAL
        );


        JPanel grillaModulos = new JPanel(new GridLayout(3, 5, 16, 14));
        grillaModulos.setOpaque(false);
        grillaModulos.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
        grillaModulos.add(btnUsuarios);
        grillaModulos.add(btnLaboratorios);
        grillaModulos.add(btnCategorias);
        grillaModulos.add(btnProductos);
        grillaModulos.add(btnEquipos);
        grillaModulos.add(btnPrestamos);
        grillaModulos.add(btnDevoluciones);
        grillaModulos.add(btnMantenimiento);
        grillaModulos.add(btnBloqueos);
        grillaModulos.add(btnReportes);
        grillaModulos.add(btnAuditoria);
        grillaModulos.add(btnServicioFacial);
        grillaModulos.add(btnDeteccionEmociones);

        lblAyuda = new JLabel();
        lblAyuda.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        lblAyuda.setForeground(TemaCeleste.TEXTO_SECUNDARIO);
        lblAyuda.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));

        panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setOpaque(false);
        panelPrincipal.add(grillaModulos, BorderLayout.CENTER);

        raiz.setPreferredSize(new Dimension(1060, 660));
        raiz.add(barraSuperior, BorderLayout.NORTH);
        raiz.add(panelPrincipal, BorderLayout.CENTER);
        raiz.add(crearDashboard(), BorderLayout.SOUTH);
    }

    /** Franja de tarjetas con los totales (se llenan en {@link #actualizarDashboard()}). */
    private JPanel crearDashboard() {
        Object[][] configuracion = {
                {"Total de equipos", "Total equipos:", TemaCeleste.TipoIcono.EQUIPOS, TemaCeleste.AZUL_PRINCIPAL, TemaCeleste.AZUL_PRINCIPAL},
                {"Disponibles", "Disponibles:", TemaCeleste.TipoIcono.PRODUCTOS, TemaCeleste.VERDE_EXITO, TemaCeleste.VERDE_EXITO},
                {"Prestados", "Prestados:", TemaCeleste.TipoIcono.FLECHAS_PRESTAMO, TemaCeleste.VERDE_EXITO, TemaCeleste.VERDE_EXITO},
                {"Dañados", "Dañados:", TemaCeleste.TipoIcono.BICHO_DANADO, TemaCeleste.NARANJA_ALERTA, TemaCeleste.NARANJA_ALERTA},
                {"En mantenimiento", "En mantenimiento:", TemaCeleste.TipoIcono.MANTENIMIENTO, TemaCeleste.AZUL_PRINCIPAL, TemaCeleste.VERDE_EXITO},
                {"De baja", "De baja:", TemaCeleste.TipoIcono.CRUZ_BAJA, TemaCeleste.AZUL_PRINCIPAL, TemaCeleste.NARANJA_ALERTA},
                {"Préstamos activos", "Préstamos activos:", TemaCeleste.TipoIcono.FLECHAS_PRESTAMO, TemaCeleste.AZUL_PRINCIPAL, TemaCeleste.AZUL_BRILLANTE},
                {"Préstamos vencidos", "Préstamos vencidos:", TemaCeleste.TipoIcono.ALERTA_CIRCULO, TemaCeleste.NARANJA_ALERTA, TemaCeleste.NARANJA_ALERTA}
        };

        JPanel tarjetas = new JPanel(new GridLayout(2, 4, 16, 12));
        tarjetas.setOpaque(false);

        for (Object[] cfg : configuracion) {
            String clave = (String) cfg[0];
            String tituloVisible = (String) cfg[1];
            TemaCeleste.TipoIcono tipoIcono = (TemaCeleste.TipoIcono) cfg[2];
            Color colorIcono = (Color) cfg[3];
            Color colorPastilla = (Color) cfg[4];

            JLabel valor = new JLabel("0");
            valor.setFont(TemaCeleste.fuente(Font.BOLD, 24f));
            valor.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

            JLabel etiqueta = new JLabel(tituloVisible);
            etiqueta.setFont(TemaCeleste.fuente(Font.PLAIN, 13f));
            etiqueta.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

            // Pastilla superior derecha que refleja el valor actual
            JComponent pastilla = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    TemaCeleste.activarAntialiasing(g2);
                    String txt = valor.getText();
                    g2.setFont(TemaCeleste.fuente(Font.BOLD, 12f));
                    FontMetrics fm = g2.getFontMetrics();
                    int pw = Math.max(26, fm.stringWidth(txt) + 14);
                    int ph = 22;
                    int px = getWidth() - pw - 2;
                    int py = 2;
                    g2.setColor(colorPastilla);
                    g2.fill(new RoundRectangle2D.Float(px, py, pw, ph, 18, 18));
                    g2.setColor(Color.WHITE);
                    int tx = px + (pw - fm.stringWidth(txt)) / 2;
                    int ty = py + (ph - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(txt, tx, ty);
                    g2.dispose();
                }
            };
            pastilla.setPreferredSize(new Dimension(34, 24));
            valor.addPropertyChangeListener("text", e -> pastilla.repaint());

            JPanel tarjeta = TemaCeleste.crearTarjetaBlanca(new BorderLayout(10, 0), 18, new Insets(10, 12, 10, 10));
            JLabel lblIcono = new JLabel(TemaCeleste.crearIconoDashboard(tipoIcono, 44, colorIcono));

            JPanel centroTexto = new JPanel(new GridLayout(2, 1, 0, 2));
            centroTexto.setOpaque(false);
            centroTexto.add(etiqueta);
            centroTexto.add(valor);

            JPanel derPastilla = new JPanel(new BorderLayout());
            derPastilla.setOpaque(false);
            derPastilla.add(pastilla, BorderLayout.NORTH);

            tarjeta.add(lblIcono, BorderLayout.WEST);
            tarjeta.add(centroTexto, BorderLayout.CENTER);
            tarjeta.add(derPastilla, BorderLayout.EAST);

            tarjetas.add(tarjeta);
            valoresDashboard.put(clave, valor);
        }

        JLabel titulo = new JLabel("Dashboard de Estado");
        titulo.setFont(TemaCeleste.fuente(Font.BOLD, 18f));
        titulo.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        JButton btnActualizar = new JButton("Actualizar totales");
        TemaCeleste.estilizarBotonCelesteSuave(btnActualizar, 24, 12.5f);
        btnActualizar.setPreferredSize(new Dimension(150, 30));
        btnActualizar.addActionListener(e -> actualizarDashboard());

        JPanel izqEnc = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izqEnc.setOpaque(false);
        izqEnc.add(titulo);
        izqEnc.add(lblAyuda);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));
        encabezado.add(izqEnc, BorderLayout.WEST);
        encabezado.add(btnActualizar, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 24, 20, 24));
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(tarjetas, BorderLayout.CENTER);
        return panel;
    }

    /** Lee los totales en segundo plano (para no congelar la ventana) y los muestra. */
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
        btnDeteccionEmociones.addActionListener(e ->
                abrirModulo("Detección de emociones", () ->
                        new DeteccionEmocionesForm().getPanelPrincipal()
                )
        );
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
                        java.awt.Window ventana = javax.swing.SwingUtilities.getWindowAncestor(referencia[0]);
                        if (ventana != null) {
                            ventana.dispose();
                        }
                    },
                    "Cerrar ventana", seccion);
            referencia[0] = formulario.getPanelPrincipal();
            return referencia[0];
        });
    }

    /** Abre el CRUD de usuarios en su propia ventana (o la trae al frente si ya está abierta). */
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
        ventanaUsuarios.setMinimumSize(new Dimension(1080, 700));
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
}
