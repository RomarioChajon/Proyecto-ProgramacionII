package umg.sigelab.view;

import umg.sigelab.controller.ConsultaController;
import umg.sigelab.controller.InventarioController;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.FilaAuditoria;
import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard, préstamos vencidos, bloqueos y auditoría (PDF 6.10 y 6.11), con los totales leídos de la
 * base de datos. El administrador ve todas las sedes (o filtra por una); el encargado, solo la suya.
 */
public class ReportesForm {

    public static final int DASHBOARD = 0;
    public static final int VENCIDOS = 1;
    public static final int BLOQUEOS = 2;
    public static final int AUDITORIA = 3;

    private static final String TODAS = "Todas las sedes";

    private final JPanel panelPrincipal = new JPanel(new BorderLayout(8, 8));
    private final Usuario usuario;
    private final ConsultaController controller = new ConsultaController();
    private final JComboBox<Object> cmbSede = new JComboBox<>();
    private final JTabbedPane pestanas = new JTabbedPane();

    private final JPanel panelTotales = new JPanel(new GridLayout(0, 5, 10, 10));
    private final JTable tblVencidos = Tablas.crear("N.º", "Solicitante", "Usuario", "Sede", "Entrega",
            "Devolución esperada", "Pendientes");
    private final JTable tblBloqueos = Tablas.crear("N.º", "Usuario", "Préstamo", "Motivo", "Estado", "Bloqueado",
            "Liberado");
    private final JTable tblAuditoria = Tablas.crear("Fecha y hora", "Usuario", "Sede", "Acción", "Entidad", "Registro",
            "Descripción");
    private List<FilaBloqueo> bloqueos = new ArrayList<>();
    private boolean cargando;

    private ReportesForm(Usuario usuario, Laboratorio sedeInicial) {
        this.usuario = usuario;
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Reportes, bloqueos y auditoría");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        norte.add(titulo);
        norte.add(new JLabel("Sede:"));
        norte.add(cmbSede);
        JButton btnActualizar = new JButton("Actualizar");
        norte.add(btnActualizar);
        panelPrincipal.add(norte, BorderLayout.NORTH);

        pestanas.addTab("Dashboard", panelTotales);
        pestanas.addTab("Préstamos vencidos", Tablas.conScroll(tblVencidos));
        JPanel panelBloqueos = new JPanel(new BorderLayout(4, 4));
        panelBloqueos.add(Tablas.conScroll(tblBloqueos), BorderLayout.CENTER);
        JButton btnLiberar = new JButton("Liberar bloqueo seleccionado");
        JPanel pieBloqueos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pieBloqueos.add(btnLiberar);
        panelBloqueos.add(pieBloqueos, BorderLayout.SOUTH);
        pestanas.addTab("Bloqueos de usuarios", panelBloqueos);
        pestanas.addTab("Auditoría", Tablas.conScroll(tblAuditoria));
        panelPrincipal.add(pestanas, BorderLayout.CENTER);
        panelPrincipal.setPreferredSize(new Dimension(1050, 620));

        configurarSedes(sedeInicial);
        btnActualizar.addActionListener(e -> cargar());
        cmbSede.addActionListener(e -> {
            if (!cargando) {
                cargar();
            }
        });
        pestanas.addChangeListener(e -> cargar());
        btnLiberar.addActionListener(e -> liberar());
        btnLiberar.setEnabled(usuario.getRol() == RolUsuario.ADMIN);
    }

    /** Abre los reportes en una ventana propia. */
    public static void abrir(Component padre, Usuario usuario, Laboratorio sede, int pestana) {
        try {
            ReportesForm form = new ReportesForm(usuario, sede);
            form.pestanas.setSelectedIndex(pestana);
            JFrame ventana = new JFrame("SIGELAB - Reportes y auditoría");
            ventana.setContentPane(form.panelPrincipal);
            ventana.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            ventana.pack();
            ventana.setLocationRelativeTo(padre);
            ventana.setVisible(true);
            form.cargar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(padre, "Reportes", ex);
        }
    }

    private void configurarSedes(Laboratorio sedeInicial) {
        cargando = true;
        try {
            if (usuario.getRol() == RolUsuario.ADMIN) {
                cmbSede.addItem(TODAS);
                for (Laboratorio l : new InventarioController().laboratorios()) {
                    cmbSede.addItem(l);
                }
                cmbSede.setSelectedItem(sedeInicial != null ? sedeInicial : TODAS);
                if (cmbSede.getSelectedItem() == null) {
                    cmbSede.setSelectedItem(TODAS);
                }
            } else if (usuario.getLaboratorio() != null) {
                cmbSede.addItem(usuario.getLaboratorio());
                cmbSede.setEnabled(false);
            }
        } finally {
            cargando = false;
        }
    }

    private Integer idSede() {
        return cmbSede.getSelectedItem() instanceof Laboratorio l ? l.getIdLaboratorio() : null;
    }

    private void cargar() {
        if (cargando) {
            return;
        }
        try {
            switch (pestanas.getSelectedIndex()) {
                case DASHBOARD -> cargarDashboard();
                case VENCIDOS -> cargarVencidos();
                case BLOQUEOS -> cargarBloqueos();
                default -> cargarAuditoria();
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Reportes", ex);
        }
    }

    private void cargarDashboard() {
        panelTotales.removeAll();
        for (Map.Entry<String, Integer> e : controller.dashboard(usuario, idSede()).entrySet()) {
            JPanel tarjeta = new JPanel(new GridLayout(2, 1));
            tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(java.awt.Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel valor = new JLabel(String.valueOf(e.getValue()));
            valor.setFont(valor.getFont().deriveFont(Font.BOLD, 28f));
            tarjeta.add(valor);
            tarjeta.add(new JLabel(e.getKey()));
            panelTotales.add(tarjeta);
        }
        panelTotales.revalidate();
        panelTotales.repaint();
    }

    private void cargarVencidos() {
        DefaultTableModel modelo = Tablas.modelo(tblVencidos);
        modelo.setRowCount(0);
        for (PrestamoResumen p : controller.vencidos(usuario, idSede())) {
            modelo.addRow(new Object[]{p.idPrestamo(), p.solicitante(), p.usuario(), p.laboratorio(),
                    Tablas.fecha(p.fechaDespacho()), Tablas.fecha(p.fechaEsperada()), p.pendientes()});
        }
    }

    private void cargarBloqueos() {
        DefaultTableModel modelo = Tablas.modelo(tblBloqueos);
        modelo.setRowCount(0);
        if (usuario.getRol() != RolUsuario.ADMIN) {
            return;
        }
        bloqueos = controller.bloqueos(usuario, false);
        for (FilaBloqueo b : bloqueos) {
            modelo.addRow(new Object[]{b.idBloqueo(), b.usuario(), "#" + b.idPrestamo(), b.motivo(), b.estado(),
                    Tablas.fechaHora(b.fechaBloqueo()), Tablas.fechaHora(b.fechaLiberacion())});
        }
    }

    private void cargarAuditoria() {
        DefaultTableModel modelo = Tablas.modelo(tblAuditoria);
        modelo.setRowCount(0);
        for (FilaAuditoria a : controller.auditoria(usuario, idSede())) {
            modelo.addRow(new Object[]{Tablas.fechaHora(a.fechaHora()), Tablas.texto(a.usuario()),
                    Tablas.texto(a.laboratorio()), a.accion(), a.entidad(), Tablas.texto(a.idRegistro()),
                    a.descripcion()});
        }
    }

    private void liberar() {
        int fila = Tablas.filaModelo(tblBloqueos);
        if (fila < 0 || fila >= bloqueos.size()) {
            Mensajes.advertir(panelPrincipal, "Bloqueos", "Seleccione un bloqueo.");
            return;
        }
        FilaBloqueo bloqueo = bloqueos.get(fila);
        if (!"ACTIVO".equals(bloqueo.estado())) {
            Mensajes.advertir(panelPrincipal, "Bloqueos", "Ese bloqueo ya está liberado.");
            return;
        }
        if (!Mensajes.confirmar(panelPrincipal, "Liberar bloqueo", "¿Liberar manualmente el bloqueo de "
                + bloqueo.usuario() + "?")) {
            return;
        }
        try {
            controller.liberarBloqueo(usuario, bloqueo);
            cargarBloqueos();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Bloqueos", ex);
        }
    }
}
