package umg.sigelab.view;

import umg.sigelab.model.Usuario;
import umg.sigelab.util.Sesion;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Dimension;

/**
 * Panel principal del ADMINISTRADOR: un botón por cada módulo del sistema.
 *
 * <p>Por ahora solo "Crear usuario" y "Servicio facial" están terminados. El resto muestra el aviso
 * "Opción aún en desarrollo"; cada vez que se termine un módulo basta con cambiar la línea de su botón
 * en {@link #configurarEventos()} para abrir la pantalla nueva.</p>
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

        // Módulos pendientes: cuando existan, reemplazar enDesarrollo(...) por la apertura de su pantalla.
        btnLaboratorios.addActionListener(e -> enDesarrollo("Laboratorios / sedes"));
        btnCategorias.addActionListener(e -> enDesarrollo("Crear categoría de equipo"));
        btnProductos.addActionListener(e -> enDesarrollo("Productos"));
        btnEquipos.addActionListener(e -> enDesarrollo("Equipos (unidades)"));
        btnPrestamos.addActionListener(e -> enDesarrollo("Préstamos y despacho"));
        btnDevoluciones.addActionListener(e -> enDesarrollo("Devoluciones"));
        btnMantenimiento.addActionListener(e -> enDesarrollo("Mantenimiento"));
        btnBloqueos.addActionListener(e -> enDesarrollo("Bloqueos de usuarios"));
        btnReportes.addActionListener(e -> enDesarrollo("Reportes y consultas"));
        btnAuditoria.addActionListener(e -> enDesarrollo("Auditoría"));
    }

    private void enDesarrollo(String opcion) {
        Mensajes.informar(panelPrincipal, opcion, "Opción aún en desarrollo.");
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

    private void cerrarVentanasHijas() {
        if (ventanaUsuarios != null) {
            ventanaUsuarios.dispose();
            ventanaUsuarios = null;
        }
    }
}
