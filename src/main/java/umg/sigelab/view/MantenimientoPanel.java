package umg.sigelab.view;

import umg.sigelab.controller.InventarioController;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.ResultadoMantenimiento;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Mantenimiento": equipos de la sede que están en mantenimiento, con filtro por categoría.
 * Al cerrar el mantenimiento el equipo queda DISPONIBLE (reparado) o de BAJA (no reparable) — RN-15.
 */
class MantenimientoPanel extends PanelSede {

    private static final String TODAS = "Todas las categorías";

    private final InventarioController controller = new InventarioController();
    private final JComboBox<Object> cmbCategoria = new JComboBox<>();
    private final JTable tabla = Tablas.crear("Código", "Equipo", "Marca", "Modelo", "Categoría", "Serie", "Observaciones");
    private List<FilaInventario> filas = new ArrayList<>();
    private boolean cargando;
    private final QrCampo qr;

    MantenimientoPanel(Usuario operador, Supplier<Laboratorio> sede) {
        super(operador, sede);
        qr = new QrCampo("Equipo por QR / código:", this::seleccionarPorCodigo);
        Tablas.estilizarCombo(cmbCategoria);

        JLabel titulo = Tablas.titulo("Equipos en mantenimiento");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtros.setBackground(Tablas.BLANCO_PURO);
        filtros.add(Tablas.etiqueta("Categoría:"));
        filtros.add(cmbCategoria);
        JButton btnActualizar = new JButton("Actualizar");
        Tablas.estilizarBotonPrimario(btnActualizar);
        filtros.add(btnActualizar);
        JPanel norte = new JPanel(new GridLayout(3, 1, 0, 2));
        Tablas.estilizarTarjeta(norte);
        norte.add(titulo);
        norte.add(filtros);
        norte.add(qr);
        add(norte, BorderLayout.NORTH);
        add(Tablas.conScroll(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        Tablas.estilizarTarjeta(acciones);
        JButton btnReparado = new JButton("Finalizar: reparado (vuelve a Disponible)");
        JButton btnBaja = new JButton("Finalizar: no reparable (pasa a Baja)");
        Tablas.estilizarBotonPrimario(btnReparado);
        Tablas.estilizarBotonSuave(btnBaja);
        acciones.add(btnReparado);
        acciones.add(btnBaja);
        add(acciones, BorderLayout.SOUTH);

        btnActualizar.addActionListener(e -> refrescar());
        cmbCategoria.addActionListener(e -> {
            if (!cargando) {
                cargarTabla();
            }
        });
        btnReparado.addActionListener(e -> finalizar(ResultadoMantenimiento.REPARADO));
        btnBaja.addActionListener(e -> finalizar(ResultadoMantenimiento.NO_REPARABLE));
    }

    @Override
    void refrescar() {
        Laboratorio sede = sede();
        cargando = true;
        try {
            Object previa = cmbCategoria.getSelectedItem();
            cmbCategoria.removeAllItems();
            cmbCategoria.addItem(TODAS);
            if (sede != null) {
                for (CategoriaStock c : controller.categorias(operador, sede.getIdLaboratorio())) {
                    cmbCategoria.addItem(c);
                }
            }
            cmbCategoria.setSelectedItem(previa != null ? previa : TODAS);
            if (cmbCategoria.getSelectedItem() == null) {
                cmbCategoria.setSelectedIndex(0);
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Mantenimiento", ex);
        } finally {
            cargando = false;
        }
        cargarTabla();
    }

    private void cargarTabla() {
        DefaultTableModel modelo = Tablas.modelo(tabla);
        modelo.setRowCount(0);
        filas = new ArrayList<>();
        Laboratorio sede = sede();
        if (sede == null) {
            return;
        }
        Integer tipo = cmbCategoria.getSelectedItem() instanceof CategoriaStock c ? c.idTipoEquipo() : null;
        try {
            filas = controller.listar(operador, sede.getIdLaboratorio(), tipo, EstadoEquipo.MANTENIMIENTO);
            for (FilaInventario f : filas) {
                modelo.addRow(new Object[]{f.codigoInventario(), f.producto(), Tablas.texto(f.marca()),
                        Tablas.texto(f.modelo()), f.categoria(), f.numeroSerie(), Tablas.texto(f.observaciones())});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Mantenimiento", ex);
        }
    }

    private void seleccionarPorCodigo(String codigo) {
        Laboratorio sede = sedeObligatoria();
        if (sede == null) {
            return;
        }
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).codigoInventario().equalsIgnoreCase(codigo)) {
                int vista = tabla.convertRowIndexToView(i);
                tabla.getSelectionModel().setSelectionInterval(vista, vista);
                tabla.scrollRectToVisible(tabla.getCellRect(vista, 0, true));
                return;
            }
        }
        try {
            // No está en la lista: se muestra su situación real para explicar por qué.
            Mensajes.informar(this, "Equipo " + codigo, "El equipo no está en mantenimiento.\n\n"
                    + controller.ficha(operador, sede.getIdLaboratorio(), codigo));
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Mantenimiento", ex);
        }
    }

    private void finalizar(ResultadoMantenimiento resultado) {
        int fila = Tablas.filaModelo(tabla);
        if (fila < 0 || fila >= filas.size()) {
            Mensajes.advertir(this, "Mantenimiento", "Seleccione un equipo de la tabla.");
            return;
        }
        FilaInventario equipo = filas.get(fila);
        String observacion = JOptionPane.showInputDialog(this, "Observaciones del cierre de " + equipo.codigoInventario()
                + " (" + resultado.getEtiqueta() + "):", "Finalizar mantenimiento", JOptionPane.QUESTION_MESSAGE);
        if (observacion == null) {
            return;
        }
        try {
            controller.finalizarMantenimiento(operador, equipo.idEquipo(), resultado, observacion);
            Mensajes.informar(this, "Mantenimiento", "El equipo " + equipo.codigoInventario() + " ahora está "
                    + (resultado == ResultadoMantenimiento.REPARADO ? "DISPONIBLE." : "de BAJA."));
            refrescar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Mantenimiento", ex);
        }
    }
}
