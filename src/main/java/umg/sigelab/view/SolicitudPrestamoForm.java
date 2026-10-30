package umg.sigelab.view;

import umg.sigelab.controller.PrestamoController;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.ModeloDisponible;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.PrestamoService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del DOCENTE y del ESTUDIANTE: pedir un préstamo y ver su historial.
 *
 * <p>Flujo: elegir sede (laboratorio) -> elegir categoría -> agregar modelos al carrito (una sola unidad de
 * cada modelo; varias categorías permitidas) -> elegir los días (1 a 30) -> confirmar. El préstamo queda
 * PENDIENTE hasta que el encargado de la sede lo valide. Las reglas las aplica {@link PrestamoService};
 * esta clase solo muestra y recoge datos.</p>
 */
public class SolicitudPrestamoForm {

    private static final String TARJETA_NUEVO = "nuevo";
    private static final String TARJETA_HISTORIAL = "historial";

    private final JPanel panelPrincipal = new JPanel(new BorderLayout());
    private final Usuario usuario;
    private final PrestamoController controller = new PrestamoController();

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);

    // --- Nuevo préstamo ---
    private final JComboBox<Laboratorio> cmbSede = new JComboBox<>();
    private final JComboBox<CategoriaStock> cmbCategoria = new JComboBox<>();
    private final JSpinner spnDias = new JSpinner(new SpinnerNumberModel(7, 1, PrestamoService.DIAS_MAXIMOS, 1)); //se pone por defecto 7 pero se puede editar
    private final JLabel lblAviso = new JLabel(" ");
    private final JTable tblCatalogo = Tablas.crear("Equipo (modelo)", "Marca", "Modelo", "Disponibles");
    private final JTable tblCarrito = Tablas.crear("Categoría", "Equipo (modelo)", "Marca", "Modelo");
    private final JButton btnAgregar = new JButton("Agregar al carrito");
    private final JButton btnQuitar = new JButton("Quitar seleccionado");
    private final JButton btnVaciar = new JButton("Vaciar carrito");
    private final JButton btnConfirmar = new JButton("Confirmar préstamo");
    private final JLabel lblResumen = new JLabel("Carrito vacío");

    private List<ModeloDisponible> catalogo = new ArrayList<>();
    private final List<ModeloDisponible> carrito = new ArrayList<>();
    private Laboratorio sedeActual;
    private boolean cargando;
    private boolean bloqueado;

    // --- Historial ---
    private final JTable tblHistorial = Tablas.crear("N.º", "Sede", "Solicitado", "Estado", "Días solic.",
            "Días asignados", "Entrega", "Devolución esperada", "Equipos", "Pendientes");
    private final JTable tblLineas = Tablas.crear("Código", "Equipo", "Marca", "Modelo", "Categoría",
            "Devolución", "Fecha devolución");
    private List<PrestamoResumen> historial = new ArrayList<>();

    public SolicitudPrestamoForm(Usuario usuario, Runnable alCerrarSesion) {
        this.usuario = usuario;
        panelPrincipal.setBackground(Tablas.BLANCO_HIELO);
        contenido.setBackground(Tablas.BLANCO_HIELO);

        JButton btnNuevo = new JButton("Nuevo préstamo");
        JButton btnHistorial = new JButton("Mi historial de préstamos");
        JButton btnCerrar = new JButton("Cerrar sesión");
        btnNuevo.addActionListener(e -> mostrarNuevo());
        btnHistorial.addActionListener(e -> mostrarHistorial());
        btnCerrar.addActionListener(e -> alCerrarSesion.run());

        panelPrincipal.add(BarraLateral.crear(usuario, null, btnNuevo, btnHistorial, null, btnCerrar),
                BorderLayout.WEST);
        contenido.add(crearPanelNuevo(), TARJETA_NUEVO);
        contenido.add(crearPanelHistorial(), TARJETA_HISTORIAL);
        panelPrincipal.add(contenido, BorderLayout.CENTER);

        conectarEventos();
        mostrarNuevo();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ------------------------------------------------------------------
    // Construcción de paneles
    // ------------------------------------------------------------------

    private JPanel crearPanelNuevo() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Tablas.BLANCO_HIELO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        Tablas.estilizarCombo(cmbSede);
        Tablas.estilizarCombo(cmbCategoria);
        spnDias.setFont(new Font("Segoe UI", Font.BOLD, 13));
        spnDias.setBorder(BorderFactory.createLineBorder(Tablas.CELESTE_MEDIO, 1));

        Tablas.estilizarBotonPrimario(btnAgregar);
        Tablas.estilizarBotonPrimario(btnConfirmar);
        Tablas.estilizarBotonSecundario(btnQuitar);
        Tablas.estilizarBotonSuave(btnVaciar);

        JPanel filtros = new JPanel(new GridBagLayout());
        Tablas.estilizarTarjeta(filtros);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel titulo = Tablas.titulo("Nuevo préstamo");
        gbc.gridwidth = 6;
        filtros.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        filtros.add(Tablas.etiqueta("1. Laboratorio / sede:"), gbc);
        gbc.gridx = 1;
        cmbSede.setPrototypeDisplayValue(null);
        filtros.add(cmbSede, gbc);
        gbc.gridx = 2;
        filtros.add(Tablas.etiqueta("2. Categoría de equipo:"), gbc);
        gbc.gridx = 3;
        filtros.add(cmbCategoria, gbc);
        gbc.gridx = 4;
        filtros.add(Tablas.etiqueta("Días de préstamo (máx. " + PrestamoService.DIAS_MAXIMOS + "):"), gbc);
        gbc.gridx = 5;
        filtros.add(spnDias, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 6;
        lblAviso.setForeground(new Color(0xB00020));
        lblAviso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filtros.add(lblAviso, gbc);
        panel.add(filtros, BorderLayout.NORTH);

        JPanel arriba = new JPanel(new BorderLayout(4, 4));
        arriba.setBackground(Tablas.BLANCO_HIELO);
        arriba.add(Tablas.etiqueta("3. Equipos disponibles en la sede (una sola unidad por modelo)"), BorderLayout.NORTH);
        arriba.add(Tablas.conScroll(tblCatalogo), BorderLayout.CENTER);
        JPanel botonesArriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonesArriba.setBackground(Tablas.BLANCO_HIELO);
        botonesArriba.add(btnAgregar);
        arriba.add(botonesArriba, BorderLayout.SOUTH);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setBackground(Tablas.BLANCO_HIELO);
        abajo.add(Tablas.etiqueta("Mi carrito"), BorderLayout.NORTH);
        abajo.add(Tablas.conScroll(tblCarrito), BorderLayout.CENTER);
        JPanel botonesAbajo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonesAbajo.setOpaque(false);
        botonesAbajo.add(btnQuitar);
        botonesAbajo.add(btnVaciar);
        botonesAbajo.add(Box.createHorizontalStrut(20));
        lblResumen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblResumen.setForeground(Tablas.CELESTE_PRINCIPAL);
        botonesAbajo.add(lblResumen);
        JPanel pie = new JPanel(new BorderLayout());
        Tablas.estilizarTarjeta(pie);
        pie.add(botonesAbajo, BorderLayout.WEST);
        pie.add(btnConfirmar, BorderLayout.EAST);
        abajo.add(pie, BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        division.setResizeWeight(0.5);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setBackground(Tablas.BLANCO_HIELO);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Tablas.BLANCO_HIELO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel titulo = Tablas.titulo("Mi historial de préstamos (todas las sedes)");
        JButton btnActualizar = new JButton("Actualizar");
        Tablas.estilizarBotonPrimario(btnActualizar);
        btnActualizar.addActionListener(e -> cargarHistorial());
        JPanel norte = new JPanel(new BorderLayout());
        Tablas.estilizarTarjeta(norte);
        norte.add(titulo, BorderLayout.WEST);
        norte.add(btnActualizar, BorderLayout.EAST);
        panel.add(norte, BorderLayout.NORTH);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.setBackground(Tablas.BLANCO_HIELO);
        abajo.add(Tablas.etiqueta("Equipos del préstamo seleccionado"), BorderLayout.NORTH);
        abajo.add(Tablas.conScroll(tblLineas), BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tablas.conScroll(tblHistorial), abajo);
        division.setResizeWeight(0.55);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setBackground(Tablas.BLANCO_HIELO);
        panel.add(division, BorderLayout.CENTER);

        tblHistorial.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarLineas();
            }
        });
        return panel;
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------

    private void conectarEventos() {
        cmbSede.addActionListener(e -> {
            if (!cargando) {
                alCambiarSede();
            }
        });
        cmbCategoria.addActionListener(e -> {
            if (!cargando) {
                cargarCatalogo();
            }
        });
        btnAgregar.addActionListener(e -> agregarAlCarrito());
        btnQuitar.addActionListener(e -> quitarDelCarrito());
        btnVaciar.addActionListener(e -> {
            carrito.clear();
            pintarCarrito();
        });
        btnConfirmar.addActionListener(e -> confirmar());
    }

    private void mostrarNuevo() {
        tarjetas.show(contenido, TARJETA_NUEVO);
        cargarSedes();
    }

    private void mostrarHistorial() {
        tarjetas.show(contenido, TARJETA_HISTORIAL);
        cargarHistorial();
    }

    // ------------------------------------------------------------------
    // Nuevo préstamo
    // ------------------------------------------------------------------

    private void cargarSedes() {
        try {
            String bloqueo = controller.motivoBloqueo(usuario);
            bloqueado = bloqueo != null;
            lblAviso.setText(bloqueo == null ? " " : "<html>" + bloqueo + "</html>");

            List<Laboratorio> sedes = controller.laboratorios();
            cargando = true;
            try {
                cmbSede.removeAllItems();
                for (Laboratorio sede : sedes) {
                    cmbSede.addItem(sede);
                }
                cmbSede.setSelectedIndex(sedes.isEmpty() ? -1 : 0);
            } finally {
                cargando = false;
            }
            sedeActual = (Laboratorio) cmbSede.getSelectedItem();
            carrito.clear();
            pintarCarrito();
            cargarCategorias();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Nuevo préstamo", ex);
        }
        actualizarControles();
    }

    private void alCambiarSede() {
        Laboratorio nueva = (Laboratorio) cmbSede.getSelectedItem();
        if (!carrito.isEmpty() && sedeActual != null && nueva != null
                && nueva.getIdLaboratorio() != sedeActual.getIdLaboratorio()) {
            boolean vaciar = Mensajes.confirmar(panelPrincipal, "Cambiar de sede",
                    "Un préstamo sale de una sola sede. Si cambia de sede se vaciará el carrito.\n¿Desea continuar?");
            if (!vaciar) {
                cargando = true;
                try {
                    cmbSede.setSelectedItem(sedeActual);
                } finally {
                    cargando = false;
                }
                return;
            }
            carrito.clear();
            pintarCarrito();
        }
        sedeActual = nueva;
        cargarCategorias();
    }

    private void cargarCategorias() {
        try {
            cargando = true;
            try {
                cmbCategoria.removeAllItems();
                if (sedeActual != null) {
                    for (CategoriaStock categoria : controller.categorias(sedeActual.getIdLaboratorio())) {
                        cmbCategoria.addItem(categoria);
                    }
                }
                cmbCategoria.setSelectedIndex(cmbCategoria.getItemCount() == 0 ? -1 : 0);
            } finally {
                cargando = false;
            }
            cargarCatalogo();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Nuevo préstamo", ex);
        }
    }

    private void cargarCatalogo() {
        DefaultTableModel modelo = Tablas.modelo(tblCatalogo);
        modelo.setRowCount(0);
        catalogo = new ArrayList<>();
        CategoriaStock categoria = (CategoriaStock) cmbCategoria.getSelectedItem();
        if (sedeActual == null || categoria == null) {
            return;
        }
        try {
            catalogo = controller.modelos(sedeActual.getIdLaboratorio(), categoria.idTipoEquipo());
            for (ModeloDisponible m : catalogo) {
                modelo.addRow(new Object[]{m.nombre(), Tablas.texto(m.marca()), Tablas.texto(m.modelo()),
                        m.disponibles()});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Nuevo préstamo", ex);
        }
    }

    private void agregarAlCarrito() {
        if (bloqueado) {
            Mensajes.advertir(panelPrincipal, "Nuevo préstamo", "No puede realizar otro préstamo por ahora.");
            return;
        }
        int fila = Tablas.filaModelo(tblCatalogo);
        if (fila < 0) {
            Mensajes.advertir(panelPrincipal, "Nuevo préstamo", "Seleccione un equipo de la lista.");
            return;
        }
        ModeloDisponible elegido = catalogo.get(fila);
        boolean repetido = carrito.stream().anyMatch(m -> m.idProducto() == elegido.idProducto());
        if (repetido) {
            Mensajes.advertir(panelPrincipal, "Nuevo préstamo",
                    "Ya agregó este modelo. Solo se puede prestar una unidad de cada modelo.");
            return;
        }
        carrito.add(elegido);
        pintarCarrito();
    }

    private void quitarDelCarrito() {
        int fila = Tablas.filaModelo(tblCarrito);
        if (fila < 0) {
            Mensajes.advertir(panelPrincipal, "Carrito", "Seleccione el equipo que desea quitar.");
            return;
        }
        carrito.remove(fila);
        pintarCarrito();
    }

    private void pintarCarrito() {
        DefaultTableModel modelo = Tablas.modelo(tblCarrito);
        modelo.setRowCount(0);
        for (ModeloDisponible m : carrito) {
            modelo.addRow(new Object[]{m.categoria(), m.nombre(), Tablas.texto(m.marca()), Tablas.texto(m.modelo())});
        }
        lblResumen.setText(carrito.isEmpty() ? "Carrito vacío" : carrito.size() + " equipo(s) en el carrito");
        actualizarControles();
    }

    private void actualizarControles() {
        boolean puede = !bloqueado;
        btnAgregar.setEnabled(puede);
        btnConfirmar.setEnabled(puede && !carrito.isEmpty());
        btnQuitar.setEnabled(!carrito.isEmpty());
        btnVaciar.setEnabled(!carrito.isEmpty());
        spnDias.setEnabled(puede);
    }

    private void confirmar() {
        if (carrito.isEmpty() || sedeActual == null) {
            Mensajes.advertir(panelPrincipal, "Confirmar préstamo", "Agregue al menos un equipo al carrito.");
            return;
        }
        int dias = (Integer) spnDias.getValue();
        boolean acepta = Mensajes.confirmar(panelPrincipal, "Confirmar préstamo",
                "¿Confirmar la solicitud de " + carrito.size() + " equipo(s) en " + sedeActual.getNombre()
                        + " por " + dias + " día(s)?");
        if (!acepta) {
            return;
        }
        try {
            List<Integer> productos = new ArrayList<>();
            for (ModeloDisponible m : carrito) {
                productos.add(m.idProducto());
            }
            int idPrestamo = controller.solicitar(usuario, sedeActual.getIdLaboratorio(), dias, productos);
            JOptionPane.showMessageDialog(panelPrincipal,
                    "Comunicarse con el encargado de laboratorio para confirmar la salida de producto.\n\n"
                            + "Su solicitud #" + idPrestamo + " quedó pendiente de validación del encargado.",
                    "Solicitud registrada", JOptionPane.INFORMATION_MESSAGE);
            carrito.clear();
            pintarCarrito();
            cargarCatalogo();
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Confirmar préstamo", ex);
            cargarCatalogo();
        }
    }

    // ------------------------------------------------------------------
    // Historial
    // ------------------------------------------------------------------

    private void cargarHistorial() {
        DefaultTableModel modelo = Tablas.modelo(tblHistorial);
        modelo.setRowCount(0);
        Tablas.modelo(tblLineas).setRowCount(0);
        try {
            historial = controller.historial(usuario);
            for (PrestamoResumen p : historial) {
                modelo.addRow(new Object[]{p.idPrestamo(), p.laboratorio(), Tablas.fechaHora(p.fechaPrestamo()),
                        p.estadoVisible(), Tablas.texto(p.diasSolicitados()), Tablas.texto(p.diasPrestamo()),
                        Tablas.fecha(p.fechaDespacho()), Tablas.fecha(p.fechaEsperada()), p.total(),
                        p.estado() == umg.sigelab.enums.EstadoPrestamo.DESPACHADO ? p.pendientes() : ""});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Historial", ex);
        }
    }

    private void cargarLineas() {
        DefaultTableModel modelo = Tablas.modelo(tblLineas);
        modelo.setRowCount(0);
        int fila = Tablas.filaModelo(tblHistorial);
        if (fila < 0 || fila >= historial.size()) {
            return;
        }
        try {
            for (LineaPrestamo l : controller.lineas(usuario, historial.get(fila).idPrestamo())) {
                modelo.addRow(new Object[]{l.codigoInventario(), l.producto(), Tablas.texto(l.marca()),
                        Tablas.texto(l.modelo()), l.categoria(),
                        l.devuelto() ? l.condicion().getEtiqueta() : "Pendiente",
                        Tablas.fechaHora(l.fechaDevolucion())});
            }
        } catch (RuntimeException ex) {
            Mensajes.mostrarExcepcion(panelPrincipal, "Historial", ex);
        }
    }
}
