package umg.sigelab.view;

import umg.sigelab.controller.InventarioController;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.TipoMantenimiento;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Ver inventario de equipo": equipos de la sede con filtro por categoría y estado. El encargado puede
 * cambiar el estado (mantenimiento, dañado o baja) y consultar la ficha de un equipo leyendo su QR.
 */
class InventarioPanel extends PanelSede {

    private static final String TODAS = "Todas las categorías";
    private static final String TODOS = "Todos los estados";

    private final InventarioController controller = new InventarioController();
    private final JComboBox<Object> cmbCategoria = new JComboBox<>();
    private final JComboBox<Object> cmbEstado = new JComboBox<>();
    private final JTable tabla = Tablas.crear("Código", "Equipo", "Marca", "Modelo", "Categoría", "Serie", "Estado");
    private List<FilaInventario> filas = new ArrayList<>();
    private boolean cargando;
    private final QrCampo qr;

    InventarioPanel(Usuario operador, Supplier<Laboratorio> sede) {
        super(operador, sede);
        qr = new QrCampo("Equipo por QR / código:", this::mostrarFicha);
        Tablas.estilizarCombo(cmbCategoria);
        Tablas.estilizarCombo(cmbEstado);

        JLabel titulo = Tablas.titulo("Inventario de equipo");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtros.setBackground(Tablas.BLANCO_PURO);
        filtros.add(Tablas.etiqueta("Categoría:"));
        filtros.add(cmbCategoria);
        filtros.add(Tablas.etiqueta("Estado:"));
        filtros.add(cmbEstado);
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
        acciones.add(Tablas.etiqueta("Cambiar estado del equipo seleccionado a:"));
        JButton btnMant = new JButton("Mantenimiento");
        JButton btnDanado = new JButton("Dañado");
        JButton btnBaja = new JButton("De baja");
        Tablas.estilizarBotonPrimario(btnMant);
        Tablas.estilizarBotonSecundario(btnDanado);
        Tablas.estilizarBotonSuave(btnBaja);
        acciones.add(btnMant);
        acciones.add(btnDanado);
        acciones.add(btnBaja);
        add(acciones, BorderLayout.SOUTH);

        cmbEstado.addItem(TODOS);
        for (EstadoEquipo e : EstadoEquipo.values()) {
            cmbEstado.addItem(e);
        }
        btnActualizar.addActionListener(e -> refrescar());
        cmbCategoria.addActionListener(e -> {
            if (!cargando) {
                cargarTabla();
            }
        });
        cmbEstado.addActionListener(e -> {
            if (!cargando) {
                cargarTabla();
            }
        });
        btnMant.addActionListener(e -> cambiar(EstadoEquipo.MANTENIMIENTO));
        btnDanado.addActionListener(e -> cambiar(EstadoEquipo.DANADO));
        btnBaja.addActionListener(e -> cambiar(EstadoEquipo.BAJA));
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
            Mensajes.mostrarExcepcion(this, "Inventario", ex);
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
        EstadoEquipo estado = cmbEstado.getSelectedItem() instanceof EstadoEquipo e ? e : null;
        try {
            filas = controller.listar(operador, sede.getIdLaboratorio(), tipo, estado);
            for (FilaInventario f : filas) {
                modelo.addRow(new Object[]{f.codigoInventario(), f.producto(), Tablas.texto(f.marca()),
                        Tablas.texto(f.modelo()), f.categoria(), f.numeroSerie(), f.estado().getEtiqueta()});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Inventario", ex);
        }
    }

    /** Lectura de QR: muestra la ficha actual del equipo y lo selecciona en la tabla. */
    private void mostrarFicha(String codigo) {
        Laboratorio sede = sedeObligatoria();
        if (sede == null) {
            return;
        }
        try {
            String ficha = controller.ficha(operador, sede.getIdLaboratorio(), codigo);
            for (int i = 0; i < filas.size(); i++) {
                if (filas.get(i).codigoInventario().equalsIgnoreCase(codigo)) {
                    int vista = tabla.convertRowIndexToView(i);
                    tabla.getSelectionModel().setSelectionInterval(vista, vista);
                    tabla.scrollRectToVisible(tabla.getCellRect(vista, 0, true));
                    break;
                }
            }
            JTextArea texto = new JTextArea(ficha, 12, 42);
            texto.setEditable(false);
            texto.setLineWrap(true);
            texto.setWrapStyleWord(true);
            texto.setBackground(Tablas.BLANCO_PURO);
            texto.setForeground(Tablas.TEXTO_OSCURO);
            JOptionPane.showMessageDialog(this, texto, "Ficha del equipo " + codigo, JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Ficha del equipo", ex);
        }
    }

    private void cambiar(EstadoEquipo nuevo) {
        int fila = Tablas.filaModelo(tabla);
        if (fila < 0 || fila >= filas.size()) {
            Mensajes.advertir(this, "Inventario", "Seleccione un equipo de la tabla.");
            return;
        }
        FilaInventario equipo = filas.get(fila);
        if (!controller.transicionesDesde(equipo.estado()).contains(nuevo)) {
            Mensajes.advertir(this, "Inventario", "Un equipo " + equipo.estado().getEtiqueta().toLowerCase()
                    + " no puede pasar a " + nuevo.getEtiqueta().toLowerCase() + " desde aquí."
                    + (equipo.estado() == EstadoEquipo.MANTENIMIENTO
                    ? "\nCiérrelo desde la sección Mantenimiento." : "")
                    + (equipo.estado() == EstadoEquipo.PRESTADO
                    ? "\nUn equipo prestado cambia al registrar su devolución." : ""));
            return;
        }

        TipoMantenimiento tipo = null;
        String descripcion = null;
        String responsable = null;
        if (nuevo == EstadoEquipo.MANTENIMIENTO) {
            JComboBox<TipoMantenimiento> cmbTipo = new JComboBox<>(TipoMantenimiento.values());
            cmbTipo.setSelectedItem(equipo.estado() == EstadoEquipo.DANADO
                    ? TipoMantenimiento.CORRECTIVO : TipoMantenimiento.PREVENTIVO);
            JTextField txtDescripcion = new JTextField(28);
            JTextField txtResponsable = new JTextField(operador.getNombreCompleto(), 28);
            Tablas.estilizarCombo(cmbTipo);
            Tablas.estilizarCampo(txtDescripcion);
            Tablas.estilizarCampo(txtResponsable);
            JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
            formulario.add(Tablas.etiqueta("Tipo de mantenimiento:"));
            formulario.add(cmbTipo);
            formulario.add(Tablas.etiqueta("Motivo / descripción:"));
            formulario.add(txtDescripcion);
            formulario.add(Tablas.etiqueta("Responsable (técnico):"));
            formulario.add(txtResponsable);
            if (JOptionPane.showConfirmDialog(this, formulario, "Enviar " + equipo.codigoInventario()
                            + " a mantenimiento", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            tipo = (TipoMantenimiento) cmbTipo.getSelectedItem();
            descripcion = txtDescripcion.getText();
            responsable = txtResponsable.getText();
        } else {
            descripcion = JOptionPane.showInputDialog(this, "Motivo del cambio a " + nuevo.getEtiqueta()
                    + " (" + equipo.codigoInventario() + "):", "Cambiar estado", JOptionPane.QUESTION_MESSAGE);
            if (descripcion == null) {
                return;
            }
            if (nuevo == EstadoEquipo.BAJA && !Mensajes.confirmar(this, "Dar de baja",
                    "Un equipo de baja ya no se puede usar ni prestar. ¿Desea continuar?")) {
                return;
            }
        }
        try {
            controller.cambiarEstado(operador, equipo.idEquipo(), nuevo, tipo, descripcion, responsable);
            Mensajes.informar(this, "Inventario", "El equipo " + equipo.codigoInventario() + " ahora está: "
                    + nuevo.getEtiqueta() + ".");
            refrescar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Cambiar estado", ex);
        }
    }
}
