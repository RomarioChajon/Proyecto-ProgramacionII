package umg.sigelab.view;

import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Panel principal del ADMINISTRADOR: un botón por cada módulo del sistema.
 *
 * <p>Todos los módulos están conectados: usuarios, servicio facial, laboratorios, categorías, productos,
 * equipos, préstamos y despacho, devoluciones, mantenimiento, bloqueos, reportes y auditoría.</p>
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
        lblUsuarioActual.setText("Sesión: " + usuario.getNombreCompleto() + "  (" + usuario.getNombreUsuario()
                + ", ID " + usuario.getIdUsuario() + ")");
        configurarEventos();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
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
}
