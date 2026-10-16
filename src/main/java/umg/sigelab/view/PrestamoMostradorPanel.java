package umg.sigelab.view;

import com.toedter.calendar.JDateChooser;
import umg.sigelab.controller.InventarioController;
import umg.sigelab.controller.PrestamoController;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.PrestamoService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * "Préstamo": el encargado crea un préstamo para un usuario (ID o nombre de usuario) y simula la entrega
 * física: cada equipo se escanea con su código QR y se agrega a la lista. Al registrar, los equipos quedan
 * PRESTADOS y el préstamo nace DESPACHADO con el encargado como quien lo entregó.
 */
class PrestamoMostradorPanel extends PanelSede {

    private final PrestamoController controller = new PrestamoController();
    private final InventarioController inventario = new InventarioController();
    private final JTextField txtUsuario = new JTextField(16);
    private final JTable tabla = Tablas.crear("Código", "Equipo", "Categoría", "Marca", "Modelo");
    private final JDateChooser dcInicio = calendario(LocalDate.now(), LocalDate.now());
    private final JDateChooser dcFin = calendario(LocalDate.now().plusDays(6), LocalDate.now());
    private final JLabel lblDias = new JLabel(" ");
    private final List<FilaInventario> equipos = new ArrayList<>();
    private final QrCampo qr;

    PrestamoMostradorPanel(Usuario operador, Supplier<Laboratorio> sede) {
        super(operador, sede);
        qr = new QrCampo("Escanear equipo (QR / código):", this::agregarEquipo);

        JLabel titulo = new JLabel("Crear préstamo para un usuario");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        JPanel filaUsuario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filaUsuario.add(new JLabel("ID o usuario del docente/estudiante:"));
        filaUsuario.add(txtUsuario);
        JPanel norte = new JPanel(new GridLayout(3, 1));
        norte.add(titulo);
        norte.add(filaUsuario);
        norte.add(qr);
        add(norte, BorderLayout.NORTH);
        add(Tablas.conScroll(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnQuitar = new JButton("Quitar seleccionado");
        acciones.add(btnQuitar);
        acciones.add(new JLabel("   Inicio:"));
        acciones.add(dcInicio);
        acciones.add(new JLabel("Fin:"));
        acciones.add(dcFin);
        lblDias.setFont(lblDias.getFont().deriveFont(Font.BOLD));
        acciones.add(lblDias);
        JButton btnRegistrar = new JButton("Registrar préstamo");
        btnRegistrar.setFont(btnRegistrar.getFont().deriveFont(Font.BOLD));
        acciones.add(btnRegistrar);
        add(acciones, BorderLayout.SOUTH);

        btnQuitar.addActionListener(e -> quitar());
        btnRegistrar.addActionListener(e -> registrar());
        java.beans.PropertyChangeListener alCambiar = e -> {
            if ("date".equals(e.getPropertyName())) {
                actualizarDias();
            }
        };
        dcInicio.addPropertyChangeListener(alCambiar);
        dcFin.addPropertyChangeListener(alCambiar);
        actualizarDias();
    }

    @Override
    void refrescar() {
        // Si cambia la sede, la lista de equipos escaneados ya no aplica.
        if (!equipos.isEmpty() && sede() != null && equipos.get(0).idLaboratorio() != sede().getIdLaboratorio()) {
            equipos.clear();
            pintar();
        }
    }

    private void agregarEquipo(String codigo) {
        Laboratorio sede = sedeObligatoria();
        if (sede == null) {
            return;
        }
        try {
            FilaInventario fila = inventario.buscarPorCodigo(operador, codigo)
                    .orElseThrow(() -> new umg.sigelab.util.BusinessException(
                            "No existe un equipo con el código " + codigo + "."));
            if (fila.idLaboratorio() != sede.getIdLaboratorio()) {
                Mensajes.advertir(this, "Equipo de otra sede", "El equipo " + codigo + " pertenece a "
                        + fila.laboratorio() + ". Un préstamo sale de una sola sede.");
                return;
            }
            if (fila.estado() != EstadoEquipo.DISPONIBLE) {
                Mensajes.advertir(this, "Equipo no disponible", inventario.ficha(operador, sede.getIdLaboratorio(), codigo)
                        + "\n\nNo se puede prestar (solo equipos DISPONIBLES).");
                return;
            }
            if (equipos.stream().anyMatch(e -> e.idEquipo() == fila.idEquipo())) {
                Mensajes.advertir(this, "Equipo repetido", "El equipo " + codigo + " ya está en la lista.");
                return;
            }
            if (equipos.stream().anyMatch(e -> e.idProducto() == fila.idProducto())) {
                Mensajes.advertir(this, "Mismo modelo", "Solo se puede prestar una unidad de cada modelo ("
                        + fila.producto() + ").");
                return;
            }
            equipos.add(fila);
            pintar();
            qr.limpiar();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Escanear equipo", ex);
        }
    }

    private void quitar() {
        int fila = Tablas.filaModelo(tabla);
        if (fila < 0) {
            Mensajes.advertir(this, "Préstamo", "Seleccione el equipo que desea quitar.");
            return;
        }
        equipos.remove(fila);
        pintar();
    }

    private void pintar() {
        DefaultTableModel modelo = Tablas.modelo(tabla);
        modelo.setRowCount(0);
        for (FilaInventario f : equipos) {
            modelo.addRow(new Object[]{f.codigoInventario(), f.producto(), f.categoria(), Tablas.texto(f.marca()),
                    Tablas.texto(f.modelo())});
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
        lblDias.setForeground(valido ? new Color(0x1B5E20) : new Color(0xB00020));
        lblDias.setText(valido ? dias + " día(s)" : "Máx. " + PrestamoService.DIAS_MAXIMOS + " días");
    }

    private void registrar() {
        Laboratorio sede = sedeObligatoria();
        if (sede == null) {
            return;
        }
        try {
            List<String> codigos = new ArrayList<>();
            for (FilaInventario f : equipos) {
                codigos.add(f.codigoInventario());
            }
            int id = controller.crearYDespachar(operador, sede.getIdLaboratorio(), txtUsuario.getText(), codigos,
                    aLocalDate(dcInicio.getDate()), aLocalDate(dcFin.getDate()));
            Mensajes.informar(this, "Préstamo registrado", "Préstamo #" + id + " entregado: " + codigos.size()
                    + " equipo(s) ahora están PRESTADOS.\nQuedó registrado que lo entregó "
                    + operador.getNombreUsuario() + ".");
            equipos.clear();
            pintar();
            txtUsuario.setText("");
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(this, "Registrar préstamo", ex);
        }
    }
}
