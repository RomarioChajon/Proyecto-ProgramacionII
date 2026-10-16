package umg.sigelab.view;

import umg.sigelab.controller.InventarioController;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel de operaciones del ENCARGADO DE LABORATORIO (y del administrador cuando abre sus opciones de préstamos):
 * órdenes pendientes de despacho, inventario, mantenimiento, préstamo con QR y devoluciones.
 *
 * <p>El encargado trabaja siempre sobre SU sede. El administrador elige la sede en la barra lateral.
 * Cada acción vuelve a validar el permiso en la capa Service (no depende de los botones visibles).</p>
 */
public class PanelOperacionesForm {

    public static final String ORDENES = "ordenes";
    public static final String INVENTARIO = "inventario";
    public static final String MANTENIMIENTO = "mantenimiento";
    public static final String PRESTAMO = "prestamo";
    public static final String DEVOLUCIONES = "devoluciones";

    private final JPanel panelPrincipal = new JPanel(new BorderLayout());
    private final Usuario usuario;
    private final InventarioController inventario = new InventarioController();

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final Map<String, PanelSede> secciones = new LinkedHashMap<>();
    private final JComboBox<Laboratorio> cmbSede = new JComboBox<>();
    private String seccionActual = ORDENES;
    private boolean cargando;

    /**
     * @param alCerrar acción del botón inferior (cerrar sesión o cerrar ventana)
     * @param textoCerrar texto del botón inferior
     * @param seccionInicial una de las constantes de esta clase
     */
    public PanelOperacionesForm(Usuario usuario, Runnable alCerrar, String textoCerrar, String seccionInicial) {
        this.usuario = usuario;

        secciones.put(ORDENES, new OrdenesPendientesPanel(usuario, this::sedeActual));
        secciones.put(INVENTARIO, new InventarioPanel(usuario, this::sedeActual));
        secciones.put(MANTENIMIENTO, new MantenimientoPanel(usuario, this::sedeActual));
        secciones.put(PRESTAMO, new PrestamoMostradorPanel(usuario, this::sedeActual));
        secciones.put(DEVOLUCIONES, new DevolucionesPanel(usuario, this::sedeActual));
        secciones.forEach((nombre, panel) -> contenido.add(panel, nombre));

        JButton btnOrdenes = boton("Órdenes pendientes de despacho", ORDENES);
        JButton btnInventario = boton("Inventario de equipo", INVENTARIO);
        JButton btnMantenimiento = boton("Mantenimiento", MANTENIMIENTO);
        JButton btnPrestamo = boton("Crear préstamo (QR)", PRESTAMO);
        JButton btnDevoluciones = boton("Devoluciones", DEVOLUCIONES);
        JButton btnReportes = new JButton("Reportes y auditoría");
        btnReportes.addActionListener(e -> ReportesForm.abrir(panelPrincipal, usuario, sedeActual(), ReportesForm.DASHBOARD));
        JButton btnCerrar = new JButton(textoCerrar);
        btnCerrar.addActionListener(e -> alCerrar.run());

        String detalle = null;
        JComponent selectorSede = null;
        if (usuario.getRol() == RolUsuario.ADMIN) {
            selectorSede = crearSelectorSede();
        } else if (usuario.getLaboratorio() != null) {
            detalle = "Sede: " + usuario.getLaboratorio().getCodigo() + " - " + usuario.getLaboratorio().getNombre();
        }

        panelPrincipal.add(BarraLateral.crear(usuario, detalle, selectorSede, btnOrdenes, btnInventario,
                btnMantenimiento, btnPrestamo, btnDevoluciones, btnReportes, null, btnCerrar), BorderLayout.WEST);
        panelPrincipal.add(contenido, BorderLayout.CENTER);
        panelPrincipal.setPreferredSize(new Dimension(1180, 700));

        if (usuario.getRol() == RolUsuario.ENCARGADO && usuario.getLaboratorio() == null) {
            Mensajes.advertir(panelPrincipal, "Encargado", "Su usuario no tiene un laboratorio asignado. "
                    + "Pida al administrador que lo asigne.");
        }
        mostrar(seccionInicial == null ? ORDENES : seccionInicial);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    /** Sede con la que trabajan las secciones. */
    private Laboratorio sedeActual() {
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return (Laboratorio) cmbSede.getSelectedItem();
        }
        return usuario.getLaboratorio();
    }

    public void mostrar(String seccion) {
        seccionActual = seccion;
        tarjetas.show(contenido, seccion);
        secciones.get(seccion).refrescar();
    }

    private JButton boton(String texto, String seccion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> mostrar(seccion));
        return boton;
    }

    private JComponent crearSelectorSede() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 2));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        JLabel etiqueta = new JLabel("Laboratorio / sede:");
        etiqueta.setForeground(Color.WHITE);
        panel.add(etiqueta);
        panel.add(cmbSede);
        try {
            List<Laboratorio> sedes = inventario.laboratorios();
            cargando = true;
            for (Laboratorio sede : sedes) {
                cmbSede.addItem(sede);
            }
            cargando = false;
            if (!sedes.isEmpty()) {
                cmbSede.setSelectedIndex(0);
            }
        } catch (RuntimeException ex) {
            cargando = false;
            Mensajes.mostrarExcepcion(panelPrincipal, "Laboratorios", ex);
        }
        cmbSede.addActionListener(e -> {
            if (!cargando) {
                secciones.get(seccionActual).refrescar();
            }
        });
        return panel;
    }
}
