package umg.sigelab.view;

import umg.sigelab.controller.ConsultaController;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Panel principal del ADMINISTRADOR: un botÃ³n por cada mÃ³dulo del sistema.
 *
 * <p>Todos los mÃ³dulos estÃ¡n conectados: usuarios, servicio facial, laboratorios, categorÃ­as, productos,
 * equipos, prÃ©stamos y despacho, devoluciones, mantenimiento, bloqueos, reportes y auditorÃ­a.</p>
 *
 * <p>Debajo de los botones se muestra el DASHBOARD (PDF 6.11): total de equipos, disponibles, prestados,
 * daÃ±ados, en mantenimiento, de baja, prÃ©stamos activos y prÃ©stamos vencidos, leÃ­dos de la base de datos.</p>
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
    private JButton btnDeteccionEmociones;
    private JButton btnCerrarSesion;

    /** Contenedor que se entrega a la ventana: el panel del formulario (.form) + el dashboard debajo. */
    private final JPanel raiz = new JPanel(new BorderLayout());
    /** Etiquetas de los totales del dashboard, en el orden en que se muestran. */
    private final Map<String, JLabel> valoresDashboard = new LinkedHashMap<>();
    private final ConsultaController consultas = new ConsultaController();
    private final Usuario usuario;
    private final Runnable alCerrarSesion;
    /** Ventana de usuarios: se reutiliza para no abrir varias copias. */
    private JFrame ventanaUsuarios;
    /** Ventanas de los mÃ³dulos de inventario, por tÃ­tulo (una sola copia de cada una). */
    private final Map<String, JFrame> ventanasModulos = new HashMap<>();

    /**
     * @param usuario        administrador con la sesiÃ³n abierta
     * @param alCerrarSesion quÃ© hacer al pulsar "Cerrar sesiÃ³n" (volver al login)
     */
    public MenuPrincipalForm(Usuario usuario, Runnable alCerrarSesion) {
        this.usuario = usuario;
        this.alCerrarSesion = alCerrarSesion;
        lblUsuarioActual.setText("SesiÃ³n: " + usuario.getNombreCompleto() + "  (" + usuario.getNombreUsuario()
                + ", ID " + usuario.getIdUsuario() + ")");
        configurarEventos();
        lblAyuda.setText("Elija un mÃ³dulo. Los totales de abajo se leen de la base de datos.");
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

    // ------------------------------------------------------------------
    // ConstrucciÃ³n visual Celeste y Blanco (fiel a mockup_menu_admin_celeste)
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

        btnCerrarSesion = new JButton("Cerrar sesiÃ³n");
        TemaCeleste.estilizarBotonEncabezado(btnCerrarSesion);

        panelSesion.add(iconoAvatar);
        panelSesion.add(lblUsuarioActual);
        panelSesion.add(btnCerrarSesion);

        barraSuperior.add(lblTitulo, BorderLayout.WEST);
        barraSuperior.add(panelSesion, BorderLayout.EAST);

        // 2. CuadrÃ­cula 3x4 de mÃ³dulos (tarjetas blancas redondeadas con Ã­cono celeste)
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

        TemaCeleste.estilizarBotonModuloAdmin(
                btnDeteccionEmociones,
                "DetecciÃ³n de emociones",
                TemaCeleste.TipoIcono.MONITOR_FACIAL
        );

        TemaCeleste.estilizarBotonModuloAdmin(btnUsuarios, "Usuarios", TemaCeleste.TipoIcono.USUARIOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnLaboratorios, "Laboratorios", TemaCeleste.TipoIcono.LABORATORIOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnCategorias, "CategorÃ­as", TemaCeleste.TipoIcono.CATEGORIAS);
        TemaCeleste.estilizarBotonModuloAdmin(btnProductos, "Productos", TemaCeleste.TipoIcono.PRODUCTOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnEquipos, "Equipos", TemaCeleste.TipoIcono.EQUIPOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnPrestamos, "PrÃ©stamos", TemaCeleste.TipoIcono.PRESTAMOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnDevoluciones, "Devoluciones", TemaCeleste.TipoIcono.DEVOLUCIONES);
        TemaCeleste.estilizarBotonModuloAdmin(btnMantenimiento, "Mantenimiento", TemaCeleste.TipoIcono.MANTENIMIENTO);
        TemaCeleste.estilizarBotonModuloAdmin(btnBloqueos, "Bloqueos", TemaCeleste.TipoIcono.BLOQUEOS);
        TemaCeleste.estilizarBotonModuloAdmin(btnReportes, "Reportes", TemaCeleste.TipoIcono.REPORTES);
        TemaCeleste.estilizarBotonModuloAdmin(btnAuditoria, "AuditorÃ­a", TemaCeleste.TipoIcono.AUDITORIA);
        TemaCeleste.estilizarBotonModuloAdmin(btnServicioFacial, "Servicio facial", TemaCeleste.TipoIcono.SERVICIO_FACIAL);

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
        String[] nombres = {"Total de equipos", "Disponibles", "Prestados", "DaÃ±ados", "En mantenimiento",
                "De baja", "PrÃ©stamos activos", "PrÃ©stamos vencidos"};
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
                    // Sin conexiÃ³n o error de consulta: se deja el guion y no se interrumpe al administrador.
                    valoresDashboard.values().forEach(etiqueta -> etiqueta.setText("-"));
                }
            }
        }.execute();
    }

    private void configurarEventos() {
        // MÃ³dulo terminado
        btnUsuarios.addActionListener(e -> abrirUsuarios());
        btnServicioFacial.addActionListener(e -> ServicioFacialForm.abrir(panelPrincipal));

        btnDeteccionEmociones.addActionListener(e ->
                abrirModulo("DetecciÃ³n de emociones", () ->
                        new DeteccionEmocionesForm().getPanelPrincipal()
                )
        );
        btnCerrarSesion.addActionListener(e -> {
            cerrarVentanasHijas();
            alCerrarSesion.run();
        });

        // MÃ³dulos de inventario (rama de Geovanni): laboratorios/sedes, categorÃ­as, productos y equipos con QR.
        btnLaboratorios.addActionListener(e -> abrirModulo("Laboratorios / sedes", () -> new LaboratorioForm().getPanelPrincipal()));
        btnCategorias.addActionListener(e -> abrirModulo("CategorÃ­as de equipo", () -> new TipoEquipoForm().getPanelPrincipal()));
        btnProductos.addActionListener(e -> abrirModulo("Productos", () -> new ProductoForm().getPanelPrincipal()));
        btnEquipos.addActionListener(e -> abrirModulo("Equipos (unidades)", () -> new EquipoForm().getPanelPrincipal()));

        // PrÃ©stamos, devoluciones, mantenimiento, bloqueos, reportes y auditorÃ­a.
        btnPrestamos.addActionListener(e -> abrirOperaciones("PrÃ©stamos y despacho", PanelOperacionesForm.ORDENES));
        btnDevoluciones.addActionListener(e -> abrirOperaciones("Devoluciones", PanelOperacionesForm.DEVOLUCIONES));
        btnMantenimiento.addActionListener(e -> abrirOperaciones("Mantenimiento", PanelOperacionesForm.MANTENIMIENTO));
        btnBloqueos.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.BLOQUEOS));
        btnReportes.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.DASHBOARD));
        btnAuditoria.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, null, ReportesForm.AUDITORIA));
    }

    /**
     * Abre el panel de operaciones (Ã³rdenes, inventario, mantenimiento, prÃ©stamo por QR, devoluciones)
     * en la secciÃ³n indicada. El administrador elige la sede en la barra lateral.
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

    /** Abre el CRUD de usuarios en su propia ventana (o la trae al frente si ya estÃ¡ abierta). */
    private void abrirUsuarios() {
        if (ventanaUsuarios != null && ventanaUsuarios.isDisplayable()) {
            ventanaUsuarios.toFront();
            return;
        }
        // Usuario conectado: sirve para no dejar que el administrador se desactive a sÃ­ mismo.
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
     * Abre un mÃ³dulo en su propia ventana (o la trae al frente si ya estÃ¡ abierta).
     *
     * @param titulo  nombre del mÃ³dulo; tambiÃ©n identifica la ventana para no abrir copias
     * @param fabrica crea el panel del mÃ³dulo (se invoca solo si hay que abrir la ventana)
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
                throw new IllegalStateException("El panel del mÃ³dulo no se creÃ³. Compile el proyecto desde IntelliJ "
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
            // LinkageError: falta alguna librerÃ­a o clase en tiempo de ejecuciÃ³n.
            Mensajes.error(panelPrincipal, titulo, "No se pudo abrir el mÃ³dulo \"" + titulo + "\".\n\nDetalle: "
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
