package umg.sigelab.view;

import com.toedter.calendar.JDateChooser;
import umg.sigelab.controller.PrestamoController;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.PrestamoService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Ver órdenes pendientes de despacho": el encargado revisa las solicitudes de docentes y estudiantes,
 * asigna el tiempo con un calendario (inicio y fin, máximo 30 días) y las CONFIRMA (los equipos pasan a
 * PRESTADO y queda registrado quién confirmó) o las RECHAZA con un motivo.
 */
class OrdenesPendientesPanel extends PanelSede {

    private final PrestamoController controller = new PrestamoController();
    private final JTable tblOrdenes = Tablas.crear("N.º", "Solicitante", "Usuario", "Solicitado", "Días solic.", "Equipos");
    private final JTable tblLineas = Tablas.crear("Código", "Equipo", "Marca", "Modelo", "Categoría");
    private final JDateChooser dcInicio = calendario(LocalDate.now(), LocalDate.now());
    private final JDateChooser dcFin = calendario(LocalDate.now(), LocalDate.now());
    private final JLabel lblDias = new JLabel(" ");
    private List<PrestamoResumen> ordenes = new ArrayList<>();

    OrdenesPendientesPanel(Usuario operador, Supplier<Laboratorio> sede) {
        super(operador, sede);

        JLabel titulo = Tablas.titulo("Órdenes pendientes de despacho");
        JButton btnActualizar = new JButton("Actualizar");
        Tablas.estilizarBotonPrimario(btnActualizar);
        btnActualizar.addActionListener(e -> refrescar());
        JPanel norte = new JPanel(new BorderLayout());
        Tablas.estilizarTarjeta(norte);
        norte.add(titulo, BorderLayout.WEST);
        norte.add(btnActualizar, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setBackground(Tablas.BLANCO_HIELO);
        abajo.add(Tablas.etiqueta("Equipos de la orden seleccionada"), BorderLayout.NORTH);
        abajo.add(Tablas.conScroll(tblLineas), BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tablas.conScroll(tblOrdenes), abajo);
        division.setResizeWeight(0.55);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setBackground(Tablas.BLANCO_HIELO);
        add(division, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tablas.estilizarTarjeta(acciones);
        acciones.add(Tablas.etiqueta("Tiempo de préstamo  -  Inicio:"));
        acciones.add(dcInicio);
        acciones.add(Tablas.etiqueta("Fin:"));
        acciones.add(dcFin);
        lblDias.setFont(new Font("Segoe UI", Font.BOLD, 12));
        acciones.add(lblDias);
        JButton btnConfirmar = new JButton("Confirmar y despachar");
        JButton btnRechazar = new JButton("Rechazar...");
        Tablas.estilizarBotonPrimario(btnConfirmar);
        Tablas.estilizarBotonSuave(btnRechazar);
        acciones.add(btnConfirmar);
        acciones.add(btnRechazar);
        add(acciones, BorderLayout.SOUTH);

        btnConfirmar.addActionListener(e -> despachar());
        btnRechazar.addActionListener(e -> rechazar());
        java.beans.PropertyChangeListener alCambiarFecha = e -> {
            if ("date".equals(e.getPropertyName())) {
                actualizarDias();
            }
        };
        dcInicio.addPropertyChangeListener(alCambiarFecha);
        dcFin.addPropertyChangeListener(alCambiarFecha);
        tblOrdenes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionar();
            }
        });
    }

    @Override
    void refrescar() {
        DefaultTableModel modelo = Tablas.modelo(tblOrdenes);
        modelo.setRowCount(0);
        Tablas.modelo(tblLineas).setRowCount(0);
        ordenes = new ArrayList<>();
        Laboratorio sede = sede();
        if (sede == null) {
            return;
        }
        try {
            ordenes = controller.pendientesDeDespacho(operador, sede.getIdLaboratorio());
            for (PrestamoResumen p : ordenes) {
                modelo.addRow(new Object[]{p.idPrestamo(), p.solicitante(), p.usuario(),
                        Tablas.fechaHora(p.fechaPrestamo()), Tablas.texto(p.diasSolicitados()), p.total()});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Órdenes pendientes", ex);
        }
    }

    private PrestamoResumen seleccionada() {
        int fila = Tablas.filaModelo(tblOrdenes);
        return fila < 0 || fila >= ordenes.size() ? null : ordenes.get(fila);
    }

    private void alSeleccionar() {
        DefaultTableModel modelo = Tablas.modelo(tblLineas);
        modelo.setRowCount(0);
        PrestamoResumen orden = seleccionada();
        if (orden == null) {
            return;
        }
        try {
            for (LineaPrestamo l : controller.lineasOperador(operador, orden.idPrestamo())) {
                modelo.addRow(new Object[]{l.codigoInventario(), l.producto(), Tablas.texto(l.marca()),
                        Tablas.texto(l.modelo()), l.categoria()});
            }
            // Propone el plazo que pidió el usuario (el encargado puede cambiarlo).
            int dias = orden.diasSolicitados() == null ? 1 : orden.diasSolicitados();
            LocalDate inicio = LocalDate.now();
            dcInicio.setDate(aDate(inicio));
            dcFin.setDate(aDate(inicio.plusDays(dias - 1L)));
            actualizarDias();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Órdenes pendientes", ex);
        }
    }

    private void actualizarDias() {
        LocalDate inicio = aLocalDate(dcInicio.getDate());
        LocalDate fin = aLocalDate(dcFin.getDate());
        if (inicio == null || fin == null) {
            lblDias.setText(" ");
            return;
        }
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        boolean valido = dias >= 1 && dias <= PrestamoService.DIAS_MAXIMOS;
        lblDias.setForeground(valido ? Tablas.CELESTE_PRINCIPAL : new Color(0xB00020));
        lblDias.setText(valido ? dias + " día(s)" : "Máx. " + PrestamoService.DIAS_MAXIMOS + " días");
    }

    private void despachar() {
        PrestamoResumen orden = seleccionada();
        if (orden == null) {
            Mensajes.advertir(this, "Órdenes pendientes", "Seleccione una orden.");
            return;
        }
        LocalDate inicio = aLocalDate(dcInicio.getDate());
        LocalDate fin = aLocalDate(dcFin.getDate());
        boolean acepta = Mensajes.confirmar(this, "Confirmar préstamo",
                "¿Confirmar la salida de los " + orden.total() + " equipo(s) de la orden #" + orden.idPrestamo()
                        + " para " + orden.solicitante() + "?\nQuedará registrado que usted lo confirmó.");
        if (!acepta) {
            return;
        }
        try {
            controller.despachar(operador, orden.idPrestamo(), inicio, fin);
            Mensajes.informar(this, "Préstamo confirmado", "La orden #" + orden.idPrestamo()
                    + " fue despachachada. Los equipos ahora están PRESTADOS.");
            refrescar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Confirmar préstamo", ex);
            refrescar();
        }
    }

    private void rechazar() {
        PrestamoResumen orden = seleccionada();
        if (orden == null) {
            Mensajes.advertir(this, "Órdenes pendientes", "Seleccione una orden.");
            return;
        }
        String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo de la orden #" + orden.idPrestamo() + ":",
                "Rechazar solicitud", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        try {
            controller.rechazar(operador, orden.idPrestamo(), motivo);
            Mensajes.informar(this, "Solicitud rechazada", "La orden #" + orden.idPrestamo() + " fue rechazada.");
            refrescar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Rechazar solicitud", ex);
        }
    }
}
