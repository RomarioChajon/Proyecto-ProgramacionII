package umg.sigelab.view;

import umg.sigelab.controller.ProductoController;
import umg.sigelab.controller.TipoEquipoController;
import umg.sigelab.model.Producto;
import umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductoForm {

    private static final Color CELESTE_PRINCIPAL = new Color(0x2B8FD6);
    private static final Color CELESTE_MEDIO = new Color(0x63B3ED);
    private static final Color CELESTE_SUAVE = new Color(0xBEE3F8);
    private static final Color BLANCO_HIELO = new Color(0xF0F8FF);
    private static final Color BLANCO_PURO = new Color(0xFFFFFF);
    private static final Color TEXTO_OSCURO = new Color(0x1A202C);

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtDescripcion;

    private JComboBox<TipoEquipo> cmbTipoEquipo;
    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JButton buscarButton;
    private JButton btnBuscar;

    private JTable tblProducto;

    private final ProductoController productoController;
    private final TipoEquipoController tipoEquipoController;

    private final Map<Integer, String> nombresTipos = new HashMap<>();
    private final Map<Integer, Integer> tiposPorProducto = new HashMap<>();

    private DefaultTableModel modeloTabla;
    private boolean cargandoTabla = false;

    public ProductoForm() {
        productoController = new ProductoController();
        tipoEquipoController = new TipoEquipoController();

        if (!componentesDisponibles()) {
            crearInterfaz();
        }

        configurarFormulario();
        configurarTabla();
        aplicarTemaCeleste();
        configurarEventos();

        cargarTiposEquipo();
        cargarProductos();
        nuevo();
    }

    private boolean componentesDisponibles() {
        return panelPrincipal != null
                && txtId != null
                && txtCodigo != null
                && txtNombre != null
                && txtMarca != null
                && txtModelo != null
                && txtDescripcion != null
                && cmbTipoEquipo != null
                && cmbEstado != null
                && btnNuevo != null
                && btnGuardar != null
                && btnActualizar != null
                && btnDesactivar != null
                && btnSalir != null
                && (buscarButton != null || btnBuscar != null)
                && tblProducto != null;
    }

    private void crearInterfaz() {
        panelPrincipal = new JPanel(new BorderLayout(12, 12));
        panelPrincipal.setBackground(BLANCO_HIELO);
        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );

        JLabel titulo = new JLabel(
                "Catálogo de Productos",
                SwingConstants.CENTER
        );
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(CELESTE_PRINCIPAL);

        txtId = new JTextField();
        txtCodigo = new JTextField();
        txtNombre = new JTextField();
        txtMarca = new JTextField();
        txtModelo = new JTextField();
        txtDescripcion = new JTextField();

        cmbTipoEquipo = new JComboBox<>();
        cmbEstado = new JComboBox<>();

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setBackground(BLANCO_PURO);
        campos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_SUAVE, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(campos, gbc, 0, "ID", txtId);
        agregarCampo(campos, gbc, 1, "Código", txtCodigo);
        agregarCampo(campos, gbc, 2, "Nombre", txtNombre);
        agregarCampo(campos, gbc, 3, "Tipo Equipo", cmbTipoEquipo);
        agregarCampo(campos, gbc, 4, "Marca", txtMarca);
        agregarCampo(campos, gbc, 5, "Modelo", txtModelo);
        agregarCampo(campos, gbc, 6, "Descripción", txtDescripcion);
        agregarCampo(campos, gbc, 7, "Estado", cmbEstado);

        btnNuevo = new JButton("Nuevo");
        btnGuardar = new JButton("Guardar");
        btnActualizar = new JButton("Actualizar");
        btnDesactivar = new JButton("Desactivar");
        buscarButton = new JButton("Buscar");
        btnBuscar = buscarButton;
        btnSalir = new JButton("Salir");

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 8)
        );
        botones.setBackground(BLANCO_HIELO);

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnDesactivar);
        botones.add(buscarButton);
        botones.add(btnSalir);

        JPanel superior = new JPanel(new BorderLayout(8, 10));
        superior.setBackground(BLANCO_HIELO);
        superior.add(titulo, BorderLayout.NORTH);
        superior.add(campos, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.SOUTH);

        tblProducto = new JTable();
        JScrollPane scroll = new JScrollPane(tblProducto);
        scroll.getViewport().setBackground(BLANCO_PURO);
        scroll.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));

        panelPrincipal.add(superior, BorderLayout.NORTH);
        panelPrincipal.add(scroll, BorderLayout.CENTER);

        panelPrincipal.setPreferredSize(
                new Dimension(1050, 680)
        );
    }

    private void agregarCampo(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            String etiqueta,
            JComponent componente
    ) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXTO_OSCURO);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(componente, gbc);
    }

    private void aplicarTemaCeleste() {
        panelPrincipal.setBackground(BLANCO_HIELO);
        panelPrincipal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, CELESTE_PRINCIPAL),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        estilizarContenedor(panelPrincipal);

        estilizarCampoTexto(txtId, false);
        estilizarCampoTexto(txtCodigo, true);
        estilizarCampoTexto(txtNombre, true);
        estilizarCampoTexto(txtMarca, true);
        estilizarCampoTexto(txtModelo, true);
        estilizarCampoTexto(txtDescripcion, true);

        estilizarCombo(cmbTipoEquipo);
        estilizarCombo(cmbEstado);

        estilizarBoton(btnGuardar, CELESTE_PRINCIPAL, BLANCO_PURO);
        estilizarBoton(btnActualizar, CELESTE_PRINCIPAL, BLANCO_PURO);
        if (buscarButton != null) {
            estilizarBoton(buscarButton, CELESTE_PRINCIPAL, BLANCO_PURO);
        }
        if (btnBuscar != null) {
            estilizarBoton(btnBuscar, CELESTE_PRINCIPAL, BLANCO_PURO);
        }
        estilizarBoton(btnNuevo, CELESTE_MEDIO, TEXTO_OSCURO);
        estilizarBoton(btnDesactivar, CELESTE_SUAVE, TEXTO_OSCURO);
        estilizarBoton(btnSalir, BLANCO_PURO, TEXTO_OSCURO);

        estilizarTabla(tblProducto);
    }

    private void estilizarContenedor(Component comp) {
        if (comp instanceof JLabel label) {
            String texto = label.getText() != null ? label.getText().trim() : "";
            if (texto.toLowerCase(Locale.ROOT).contains("gesti") || texto.toLowerCase(Locale.ROOT).contains("catálogo")) {
                label.setFont(new Font("Segoe UI", Font.BOLD, 20));
                label.setForeground(CELESTE_PRINCIPAL);
            } else {
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setForeground(TEXTO_OSCURO);
            }
        } else if (comp instanceof JScrollPane scroll) {
            scroll.getViewport().setBackground(BLANCO_PURO);
            scroll.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
        }
        if (comp instanceof Container container) {
            for (Component hijo : container.getComponents()) {
                estilizarContenedor(hijo);
            }
        }
    }

    private void estilizarCampoTexto(JTextField campo, boolean editable) {
        if (campo == null) {
            return;
        }
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setForeground(TEXTO_OSCURO);
        campo.setBackground(editable ? BLANCO_PURO : BLANCO_HIELO);
        campo.setCaretColor(CELESTE_PRINCIPAL);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_MEDIO, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void estilizarCombo(JComboBox<?> combo) {
        if (combo == null) {
            return;
        }
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXTO_OSCURO);
        combo.setBackground(BLANCO_PURO);
        combo.setBorder(BorderFactory.createLineBorder(CELESTE_MEDIO, 1));
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto) {
        if (boton == null) {
            return;
        }
        boton.setUI(new BasicButtonUI());
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELESTE_PRINCIPAL, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        Color hoverFondo = fondo.equals(CELESTE_PRINCIPAL) ? CELESTE_MEDIO : CELESTE_SUAVE;
        Color hoverTexto = fondo.equals(CELESTE_PRINCIPAL) ? BLANCO_PURO : TEXTO_OSCURO;
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(hoverFondo);
                    boton.setForeground(hoverTexto);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
                boton.setForeground(texto);
            }
        });
    }

    private void estilizarTabla(JTable tabla) {
        if (tabla == null) {
            return;
        }
        tabla.setBackground(BLANCO_PURO);
        tabla.setForeground(TEXTO_OSCURO);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setGridColor(CELESTE_SUAVE);
        tabla.setSelectionBackground(CELESTE_SUAVE);
        tabla.setSelectionForeground(TEXTO_OSCURO);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(CELESTE_PRINCIPAL);
                lbl.setForeground(BLANCO_PURO);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, CELESTE_MEDIO),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        });
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        // Código editable: vacío al crear = se genera automáticamente.
        txtCodigo.setEditable(true);

        txtId.setFocusable(false);
        txtCodigo.setFocusable(true);

        txtCodigo.setToolTipText(
                "Puede escribirlo o dejarlo vacío para que se genere automáticamente."
        );

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");
        cmbEstado.setSelectedItem("ACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {
        modeloTabla = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código",
                        "Nombre",
                        "Tipo Equipo",
                        "Marca",
                        "Modelo",
                        "Descripción",
                        "Estado"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tblProducto.setModel(modeloTabla);
        tblProducto.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tblProducto.setAutoCreateRowSorter(true);
        tblProducto.setFillsViewportHeight(true);
        tblProducto.setRowHeight(26);
        tblProducto.getTableHeader().setReorderingAllowed(false);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnSalir.addActionListener(e -> salir());

        if (buscarButton != null) {
            buscarButton.addActionListener(e -> buscar());
        }

        if (btnBuscar != null && btnBuscar != buscarButton) {
            btnBuscar.addActionListener(e -> buscar());
        }

        tblProducto.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoTabla) {
                cargarProductoSeleccionado();
            }
        });
    }

    private void cargarTiposEquipo() {
        try {
            List<TipoEquipo> tipos = tipoEquipoController.listar();

            nombresTipos.clear();
            cmbTipoEquipo.removeAllItems();

            for (TipoEquipo tipo : tipos) {
                nombresTipos.put(
                        tipo.getIdTipoEquipo(),
                        tipo.getNombre()
                );

                if (tipo.getEstado() != null
                        && "ACTIVO".equals(tipo.getEstado().name())) {
                    cmbTipoEquipo.addItem(tipo);
                }
            }

            cmbTipoEquipo.setSelectedIndex(-1);

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarProductos() {
        try {
            cargarTabla(productoController.listar());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los productos.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<Producto> productos) {
        cargandoTabla = true;

        try {
            tblProducto.clearSelection();
            modeloTabla.setRowCount(0);
            tiposPorProducto.clear();

            for (Producto producto : productos) {
                tiposPorProducto.put(
                        producto.getIdProducto(),
                        producto.getIdTipoEquipo()
                );

                modeloTabla.addRow(new Object[]{
                        producto.getIdProducto(),
                        producto.getCodigoProducto(),
                        producto.getNombre(),
                        nombresTipos.getOrDefault(
                                producto.getIdTipoEquipo(),
                                String.valueOf(producto.getIdTipoEquipo())
                        ),
                        producto.getMarca(),
                        producto.getModelo(),
                        producto.getDescripcion(),
                        producto.getEstado()
                });
            }

        } finally {
            cargandoTabla = false;
        }
    }

    private void nuevo() {
        limpiarCampos();
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        if (!txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                "Presione Nuevo antes de registrar otro producto."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        TipoEquipo tipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        try {
            Producto producto = productoController.guardar(
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            cargarProductos();
            seleccionarFilaPorId(producto.getIdProducto());

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Producto guardado correctamente.\n\n"
                            + "ID: " + producto.getIdProducto()
                            + "\nCódigo: " + producto.getCodigoProducto()
                            + "\nNombre: " + producto.getNombre(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo guardar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione un producto de la tabla."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        TipoEquipo tipo =
                (TipoEquipo) cmbTipoEquipo.getSelectedItem();

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean actualizado = productoController.actualizar(
                    id,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    tipo.getIdTipoEquipo(),
                    txtMarca.getText().trim(),
                    txtModelo.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            if (actualizado) {
                cargarProductos();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                mostrarAdvertencia(
                        "No se pudo actualizar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo actualizar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void desactivar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione un producto de la tabla."
            );
            return;
        }

        if ("INACTIVO".equalsIgnoreCase(
                String.valueOf(cmbEstado.getSelectedItem())
        )) {
            mostrarAdvertencia(
                    "Este producto ya está inactivo."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar este producto?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean desactivado = productoController.desactivar(id);

            if (desactivado) {
                cargarProductos();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Producto desactivado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {
                mostrarAdvertencia(
                        "No se pudo desactivar el producto."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo desactivar el producto.\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {
        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Buscar por ID, código, nombre, tipo de equipo,\n"
                        + "marca, modelo, descripción o estado.\n\n"
                        + "Deje vacío para mostrar todos:",
                "Buscar producto",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        String criterio = entrada.trim();

        if (criterio.isEmpty()) {
            cargarProductos();
            nuevo();
            return;
        }

        try {
            List<Producto> exactos = new ArrayList<>();
            List<Producto> parciales = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (Producto producto : productoController.listar()) {
                String id = String.valueOf(producto.getIdProducto());
                String codigo = textoSeguro(producto.getCodigoProducto());
                String nombre = textoSeguro(producto.getNombre());
                String tipo = textoSeguro(
                        nombresTipos.get(producto.getIdTipoEquipo())
                );
                String marca = textoSeguro(producto.getMarca());
                String modelo = textoSeguro(producto.getModelo());
                String descripcion = textoSeguro(producto.getDescripcion());
                String estado = producto.getEstado() == null
                        ? ""
                        : producto.getEstado().toString();

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {
                    exactos.add(producto);

                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || tipo.toLowerCase(Locale.ROOT).contains(filtro)
                                || marca.toLowerCase(Locale.ROOT).contains(filtro)
                                || modelo.toLowerCase(Locale.ROOT).contains(filtro)
                                || descripcion.toLowerCase(Locale.ROOT).contains(filtro)
                                || estado.toLowerCase(Locale.ROOT).contains(filtro)
                ) {
                    parciales.add(producto);
                }
            }

            List<Producto> resultados =
                    exactos.isEmpty() ? parciales : exactos;

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {
                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "No se encontraron productos con el criterio:\n"
                                + criterio,
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else if (resultados.size() == 1) {
                seleccionarFilaPorId(
                        resultados.get(0).getIdProducto()
                );
            }

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    private void seleccionarFilaPorId(int idProducto) {
        for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {
            Object valor = modeloTabla.getValueAt(fila, 0);

            if (valor != null
                    && String.valueOf(valor).equals(
                    String.valueOf(idProducto)
            )) {

                int filaVista =
                        tblProducto.convertRowIndexToView(fila);

                if (filaVista >= 0) {
                    tblProducto.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblProducto.scrollRectToVisible(
                            tblProducto.getCellRect(filaVista, 0, true)
                    );

                    cargarProductoSeleccionado();
                }
                return;
            }
        }
    }

    private void cargarProductoSeleccionado() {
        if (cargandoTabla) {
            return;
        }

        int filaVista = tblProducto.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int fila = tblProducto.convertRowIndexToModel(filaVista);

        int idProducto = Integer.parseInt(
                valorTabla(fila, 0)
        );

        Integer idTipoEquipo = tiposPorProducto.get(idProducto);

        txtId.setText(String.valueOf(idProducto));
        txtCodigo.setText(valorTabla(fila, 1));
        txtNombre.setText(valorTabla(fila, 2));
        txtMarca.setText(valorTabla(fila, 4));
        txtModelo.setText(valorTabla(fila, 5));
        txtDescripcion.setText(valorTabla(fila, 6));

        seleccionarTipoEquipo(idTipoEquipo);

        String estado = valorTabla(fila, 7);
        cmbEstado.setSelectedItem(estado);

        cmbTipoEquipo.setEnabled(false);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(
                cmbTipoEquipo.getSelectedItem() != null
        );
        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private void seleccionarTipoEquipo(Integer idTipoEquipo) {
        cmbTipoEquipo.setSelectedIndex(-1);

        if (idTipoEquipo == null) {
            return;
        }

        for (int i = 0; i < cmbTipoEquipo.getItemCount(); i++) {
            TipoEquipo tipo = cmbTipoEquipo.getItemAt(i);

            if (tipo != null
                    && tipo.getIdTipoEquipo() == idTipoEquipo) {
                cmbTipoEquipo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String valorTabla(int fila, int columna) {
        Object valor = modeloTabla.getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean validarCampos() {
        String nombre = txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAdvertencia(
                    "Ingrese el nombre del producto."
            );
            txtNombre.requestFocusInWindow();
            return false;
        }

        if (nombre.length() > 100) {
            mostrarAdvertencia(
                    "El nombre no puede superar 100 caracteres."
            );
            return false;
        }

        if (cmbTipoEquipo.getSelectedItem() == null) {
            mostrarAdvertencia(
                    "Seleccione un tipo de equipo activo."
            );
            return false;
        }

        if (cmbEstado.getSelectedItem() == null) {
            mostrarAdvertencia(
                    "Seleccione el estado del producto."
            );
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtDescripcion.setText("");

        cmbTipoEquipo.setEnabled(true);

        if (cmbTipoEquipo.getItemCount() > 0) {
            cmbTipoEquipo.setSelectedIndex(0);
        } else {
            cmbTipoEquipo.setSelectedIndex(-1);
        }

        cmbEstado.setSelectedItem("ACTIVO");

        cargandoTabla = true;

        try {
            tblProducto.clearSelection();
        } finally {
            cargandoTabla = false;
        }

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "SIGELAB",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                panelPrincipal,
                mensaje,
                "Error - SIGELAB",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void salir() {
        Window ventana =
                SwingUtilities.getWindowAncestor(panelPrincipal);

        if (ventana != null) {
            ventana.dispose();
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}
