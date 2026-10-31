// ============================================================================
// ARCHIVO: src/main/java/umg/sigelab/view/TipoEquipoForm.java
// INSTRUCCIONES: Copia todo desde "package umg.sigelab.view;" hasta la última
// llave "}" y pégalo reemplazando todo el contenido de TipoEquipoForm.java.
// (Nota: Requiere tener TemaCeleste.java en la misma carpeta umg/sigelab/view)
// ============================================================================

package umg.sigelab.view;

import umg.sigelab.controller.TipoEquipoController;
import umg.sigelab.model.TipoEquipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ventana de Categorías / Tipos de Equipo con diseño Celeste y Blanco
 * ("Laboratorios de Computación").
 */
public class TipoEquipoForm {

    private static final int COL_ID = 0;
    private static final int COL_CODIGO = 1;
    private static final int COL_ESTADO = 4;

    private JPanel panelPrincipal;

    private JTextField txtId;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JTextField textField1;
    private JTextField txtCotegoria;
    private JTextField txtBuscar;

    private JComboBox<String> cmbEstado;

    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnSalir;
    private JButton btnBuscar;
    private JButton btnVerTodos;

    private JTable tblTipoEquipo;
    private JScrollPane scrollTabla;

    private final TipoEquipoController controller;
    private boolean cargandoTabla = false;

    public TipoEquipoForm() {
        controller = new TipoEquipoController();

        crearInterfaz();
        configurarFormulario();
        configurarTabla();
        configurarEventos();
        cargarTiposEquipo();
        nuevo();
    }

    private void crearInterfaz() {
        // 1. Encabezado superior azul (#2B8FD6)
        JPanel barraSuperior = TemaCeleste.crearEncabezadoAzul(new BorderLayout(16, 0));
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 24));

        JPanel izqHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izqHeader.setOpaque(false);
        JLabel iconoHeader = new JLabel(TemaCeleste.crearIconoDashboard(
                TemaCeleste.TipoIcono.CATEGORIAS, 36, new Color(255, 255, 255, 38)));
        JLabel titulo = new JLabel("SIGELAB - Categorías / Tipos de Equipo");
        titulo.setFont(TemaCeleste.fuente(Font.BOLD, 20f));
        titulo.setForeground(Color.WHITE);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        izqHeader.add(iconoHeader);
        izqHeader.add(titulo);

        btnSalir = new JButton("Cerrar ventana");
        TemaCeleste.estilizarBotonEncabezado(btnSalir);

        barraSuperior.add(izqHeader, BorderLayout.WEST);
        barraSuperior.add(btnSalir, BorderLayout.EAST);

        // 2. Tarjeta superior: Formulario de Categoría / Tipo de Equipo
        JPanel tarjetaFormulario = TemaCeleste.crearTarjetaBlanca(
                new BorderLayout(0, 14), 18, new Insets(16, 22, 16, 22));

        txtId = TemaCeleste.crearCampoTextoRedondeado();
        txtCodigo = TemaCeleste.crearCampoTextoRedondeado();
        txtNombre = TemaCeleste.crearCampoTextoRedondeado();
        txtDescripcion = TemaCeleste.crearCampoTextoRedondeado();

        cmbEstado = new JComboBox<>();
        TemaCeleste.estilizarCombo(cmbEstado);

        // Fila 1: ID, Código, Nombre de la categoría
        JPanel fila1 = new JPanel(new GridLayout(1, 3, 16, 0));
        fila1.setOpaque(false);
        fila1.add(campoConEtiqueta("ID Categoría", txtId));
        fila1.add(campoConEtiqueta("Código (Ejemplo = CAT-XXXX-NNNN)", txtCodigo));
        fila1.add(campoConEtiqueta("Nombre de la categoría(Puede ir marca)", txtNombre));

        // Fila 2: Descripción amplia (2/3) + Estado (1/3)
        JPanel fila2 = new JPanel(new BorderLayout(16, 0));
        fila2.setOpaque(false);
        fila2.add(campoConEtiqueta("Descripción detallada", txtDescripcion), BorderLayout.CENTER);

        JPanel panelEstadoDerecha = new JPanel(new GridLayout(1, 2, 16, 0));
        panelEstadoDerecha.setOpaque(false);
        panelEstadoDerecha.setPreferredSize(new Dimension(380, 60));
        panelEstadoDerecha.add(campoConEtiqueta("Estado del registro", cmbEstado));

        JPanel notaCodigo = TemaCeleste.crearSubTarjetaCeleste(new BorderLayout(), 16, new Insets(6, 10, 6, 10));
        JLabel lblNota = new JLabel(
                "<html><div style='text-align:center;'>Formato <b>CAT-XXXX-NNNN</b><br><span style='color:#4A5568;font-size:10px;'>automático si queda vacío</span></div></html>",
                SwingConstants.CENTER);
        lblNota.setFont(TemaCeleste.fuente(Font.PLAIN, 12f));
        lblNota.setForeground(TemaCeleste.AZUL_PRINCIPAL);
        notaCodigo.add(lblNota, BorderLayout.CENTER);
        panelEstadoDerecha.add(campoConEtiqueta("Formato de código", notaCodigo));

        fila2.add(panelEstadoDerecha, BorderLayout.EAST);

        JPanel contenedorCampos = new JPanel(new GridLayout(2, 1, 0, 12));
        contenedorCampos.setOpaque(false);
        contenedorCampos.add(fila1);
        contenedorCampos.add(fila2);

        // Barra de botones de acción CRUD
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botones.setOpaque(false);

        btnNuevo = new JButton("Nuevo / Limpiar");
        TemaCeleste.estilizarBotonClaro(btnNuevo, 28, 13.5f);
        btnNuevo.setPreferredSize(new Dimension(150, 36));

        btnGuardar = new JButton("Guardar categoría");
        TemaCeleste.estilizarBotonPrimario(btnGuardar, 28, 14f);
        btnGuardar.setPreferredSize(new Dimension(175, 36));

        btnActualizar = new JButton("Actualizar datos");
        TemaCeleste.estilizarBotonPrimario(btnActualizar, 28, 14f);
        btnActualizar.setPreferredSize(new Dimension(160, 36));

        btnDesactivar = new JButton("Desactivar");
        TemaCeleste.estilizarBotonPeligro(btnDesactivar, 28, 13.5f);
        btnDesactivar.setPreferredSize(new Dimension(135, 36));

        botones.add(btnNuevo);
        botones.add(btnGuardar);
        botones.add(btnActualizar);
        botones.add(btnDesactivar);

        tarjetaFormulario.add(contenedorCampos, BorderLayout.CENTER);
        tarjetaFormulario.add(botones, BorderLayout.SOUTH);

        // 3. Tarjeta inferior: Buscador + Tabla de categorías
        JPanel tarjetaTabla = TemaCeleste.crearTarjetaBlanca(
                new BorderLayout(0, 12), 18, new Insets(16, 22, 18, 22));

        JPanel barraBusqueda = new JPanel(new BorderLayout(12, 0));
        barraBusqueda.setOpaque(false);

        JLabel lblListado = new JLabel("Categorías de Equipo Registradas");
        lblListado.setFont(TemaCeleste.fuente(Font.BOLD, 18f));
        lblListado.setForeground(TemaCeleste.TEXTO_PRINCIPAL);

        JPanel controlesFiltro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlesFiltro.setOpaque(false);

        JLabel lblBuscarTxt = new JLabel("Buscar:");
        lblBuscarTxt.setFont(TemaCeleste.fuente(Font.PLAIN, 13.5f));
        lblBuscarTxt.setForeground(TemaCeleste.TEXTO_SECUNDARIO);

        txtBuscar = TemaCeleste.crearCampoTextoRedondeado();
        txtBuscar.setPreferredSize(new Dimension(240, 36));
        txtBuscar.setToolTipText("Buscar por ID, código, categoría, descripción o estado");

        btnBuscar = new JButton("Buscar");
        TemaCeleste.estilizarBotonPrimario(btnBuscar, 24, 13.5f);
        btnBuscar.setPreferredSize(new Dimension(105, 36));

        btnVerTodos = new JButton("Ver todos");
        TemaCeleste.estilizarBotonCelesteSuave(btnVerTodos, 24, 13f);
        btnVerTodos.setPreferredSize(new Dimension(105, 36));

        controlesFiltro.add(lblBuscarTxt);
        controlesFiltro.add(txtBuscar);
        controlesFiltro.add(btnBuscar);
        controlesFiltro.add(btnVerTodos);

        barraBusqueda.add(lblListado, BorderLayout.WEST);
        barraBusqueda.add(controlesFiltro, BorderLayout.EAST);

        tblTipoEquipo = new JTable();
        scrollTabla = new JScrollPane(tblTipoEquipo);
        scrollTabla.setPreferredSize(new Dimension(950, 270));

        tarjetaTabla.add(barraBusqueda, BorderLayout.NORTH);
        tarjetaTabla.add(scrollTabla, BorderLayout.CENTER);

        // Cuerpo principal
        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        cuerpo.add(tarjetaFormulario, BorderLayout.NORTH);
        cuerpo.add(tarjetaTabla, BorderLayout.CENTER);

        panelPrincipal = TemaCeleste.crearPanelFondo(new BorderLayout());
        panelPrincipal.setPreferredSize(new Dimension(1060, 660));
        panelPrincipal.add(barraSuperior, BorderLayout.NORTH);
        panelPrincipal.add(cuerpo, BorderLayout.CENTER);
    }

    private JPanel campoConEtiqueta(String etiqueta, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(etiqueta);
        l.setFont(TemaCeleste.fuente(Font.PLAIN, 12.5f));
        l.setForeground(TemaCeleste.TEXTO_PRINCIPAL);
        componente.setPreferredSize(new Dimension(160, 38));
        p.add(l, BorderLayout.NORTH);
        p.add(componente, BorderLayout.CENTER);
        return p;
    }

    private void configurarFormulario() {
        txtId.setEditable(false);
        txtId.setFocusable(false);
        // Código editable: vacío al crear = se genera automáticamente.
        txtCodigo.setEditable(true);
        txtCodigo.setToolTipText("Formato CAT-XXXX-NNNN. Déjelo vacío para generarlo automáticamente.");

        cmbEstado.removeAllItems();
        cmbEstado.addItem("ACTIVO");
        cmbEstado.addItem("INACTIVO");

        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
        btnDesactivar.setEnabled(false);
    }

    private void configurarTabla() {
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Código",
                        "Categoría",
                        "Descripción",
                        "Estado"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                return columna == COL_ID ? Integer.class : Object.class;
            }
        };

        tblTipoEquipo.setModel(modelo);
        tblTipoEquipo.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tblTipoEquipo.setAutoCreateRowSorter(true);
        tblTipoEquipo.getColumnModel().getColumn(COL_ID).setMaxWidth(65);
        tblTipoEquipo.getColumnModel().getColumn(COL_CODIGO).setPreferredWidth(145);
        tblTipoEquipo.getColumnModel().getColumn(COL_ESTADO).setPreferredWidth(115);

        TemaCeleste.estilizarTabla(tblTipoEquipo, scrollTabla, COL_ESTADO);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnBuscar.addActionListener(e -> buscar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnSalir.addActionListener(e -> salir());

        if (txtBuscar != null) {
            txtBuscar.addActionListener(e -> ejecutarBusqueda(txtBuscar.getText().trim()));
        }

        if (btnVerTodos != null) {
            btnVerTodos.addActionListener(e -> {
                if (txtBuscar != null) {
                    txtBuscar.setText("");
                }
                cargarTiposEquipo();
                nuevo();
            });
        }

        tblTipoEquipo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargandoTabla) {
                cargarTipoSeleccionado();
            }
        });
    }

    private void cargarTiposEquipo() {
        try {
            cargarTabla(controller.listar());
        } catch (SQLException ex) {
            mostrarError(
                    "No se pudieron cargar los tipos de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void cargarTabla(List<TipoEquipo> tipos) {
        cargandoTabla = true;

        try {
            tblTipoEquipo.clearSelection();

            DefaultTableModel modelo =
                    (DefaultTableModel) tblTipoEquipo.getModel();

            modelo.setRowCount(0);

            for (TipoEquipo tipo : tipos) {
                modelo.addRow(new Object[]{
                        tipo.getIdTipoEquipo(),
                        tipo.getCodigo(),
                        tipo.getNombre(),
                        tipo.getDescripcion(),
                        tipo.getEstado()
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
                    "Presione Nuevo antes de registrar otra categoría."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {
            TipoEquipo tipo = controller.guardar(
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            cargarTiposEquipo();
            seleccionarFilaPorId(tipo.getIdTipoEquipo());

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Tipo de equipo guardado correctamente.\n"
                            + "ID: " + tipo.getIdTipoEquipo()
                            + "\nCódigo: " + tipo.getCodigo()
                            + "\nCategoría: " + tipo.getNombre(),
                    "SIGELAB",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo guardar el tipo de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione una categoría de la tabla."
            );
            return;
        }

        if (!validarCampos()) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean actualizado = controller.actualizar(
                    id,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    String.valueOf(cmbEstado.getSelectedItem())
            );

            if (actualizado) {
                cargarTiposEquipo();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Tipo de equipo actualizado correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                mostrarAdvertencia(
                        "No se pudo actualizar el tipo de equipo."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo actualizar el tipo de equipo.\n"
                            + ex.getMessage()
            );
        }
    }

    private void desactivar() {
        if (txtId.getText().trim().isEmpty()) {
            mostrarAdvertencia(
                    "Seleccione una categoría de la tabla."
            );
            return;
        }

        if ("INACTIVO".equals(
                String.valueOf(cmbEstado.getSelectedItem())
        )) {
            mostrarAdvertencia(
                    "Esta categoría ya está inactiva."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                panelPrincipal,
                "¿Desea desactivar esta categoría?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText().trim());

            boolean desactivado = controller.desactivar(id);

            if (desactivado) {
                cargarTiposEquipo();
                seleccionarFilaPorId(id);

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Categoría desactivada correctamente.",
                        "SIGELAB",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                mostrarAdvertencia(
                        "No se pudo desactivar la categoría."
                );
            }

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo desactivar la categoría.\n"
                            + ex.getMessage()
            );
        }
    }

    private void buscar() {
        String textoBarra = txtBuscar == null ? "" : txtBuscar.getText().trim();
        if (!textoBarra.isEmpty()) {
            ejecutarBusqueda(textoBarra);
            return;
        }

        String entrada = JOptionPane.showInputDialog(
                panelPrincipal,
                "Buscar por ID, código, categoría, descripción o estado.\n"
                        + "Deje vacío para mostrar todos:",
                "Buscar tipo de equipo",
                JOptionPane.QUESTION_MESSAGE
        );

        if (entrada == null) {
            return;
        }

        ejecutarBusqueda(entrada.trim());
    }

    private void ejecutarBusqueda(String criterio) {
        if (criterio.isEmpty()) {
            cargarTiposEquipo();
            nuevo();
            return;
        }

        try {
            List<TipoEquipo> exactos = new ArrayList<>();
            List<TipoEquipo> parciales = new ArrayList<>();

            String filtro = criterio.toLowerCase(Locale.ROOT);

            for (TipoEquipo tipo : controller.listar()) {
                String id = String.valueOf(tipo.getIdTipoEquipo());
                String codigo = textoSeguro(tipo.getCodigo());
                String nombre = textoSeguro(tipo.getNombre());
                String descripcion = textoSeguro(tipo.getDescripcion());
                String estado = tipo.getEstado() == null
                        ? ""
                        : tipo.getEstado().toString();

                if (id.equals(criterio)
                        || codigo.equalsIgnoreCase(criterio)
                        || nombre.equalsIgnoreCase(criterio)) {
                    exactos.add(tipo);
                } else if (
                        codigo.toLowerCase(Locale.ROOT).contains(filtro)
                                || nombre.toLowerCase(Locale.ROOT).contains(filtro)
                                || descripcion.toLowerCase(Locale.ROOT).contains(filtro)
                                || estado.toLowerCase(Locale.ROOT).contains(filtro)
                ) {
                    parciales.add(tipo);
                }
            }

            List<TipoEquipo> resultados =
                    exactos.isEmpty() ? parciales : exactos;

            cargarTabla(resultados);
            limpiarCampos();

            if (resultados.isEmpty()) {
                mostrarAdvertencia(
                        "No se encontraron categorías con ese criterio."
                );
            } else if (resultados.size() == 1) {
                seleccionarFilaPorId(
                        resultados.get(0).getIdTipoEquipo()
                );
            }

        } catch (SQLException ex) {
            mostrarError(
                    "No se pudo realizar la búsqueda.\n"
                            + ex.getMessage()
            );
        }
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private void seleccionarFilaPorId(int id) {
        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            Object valor = modelo.getValueAt(fila, 0);

            if (valor != null
                    && String.valueOf(valor).equals(String.valueOf(id))) {

                int filaVista = tblTipoEquipo.convertRowIndexToView(fila);

                if (filaVista >= 0) {
                    tblTipoEquipo.setRowSelectionInterval(
                            filaVista,
                            filaVista
                    );

                    tblTipoEquipo.scrollRectToVisible(
                            tblTipoEquipo.getCellRect(filaVista, 0, true)
                    );

                    cargarTipoSeleccionado();
                }
                return;
            }
        }
    }

    private void cargarTipoSeleccionado() {
        if (cargandoTabla) {
            return;
        }

        int filaVista = tblTipoEquipo.getSelectedRow();

        if (filaVista < 0) {
            return;
        }

        int filaModelo =
                tblTipoEquipo.convertRowIndexToModel(filaVista);

        DefaultTableModel modelo =
                (DefaultTableModel) tblTipoEquipo.getModel();

        txtId.setText(valorTexto(modelo.getValueAt(filaModelo, 0)));
        txtCodigo.setText(valorTexto(modelo.getValueAt(filaModelo, 1)));
        txtNombre.setText(valorTexto(modelo.getValueAt(filaModelo, 2)));
        txtDescripcion.setText(valorTexto(modelo.getValueAt(filaModelo, 3)));

        String estado = valorTexto(
                modelo.getValueAt(filaModelo, 4)
        );

        cmbEstado.setSelectedItem(estado);

        btnGuardar.setEnabled(false);
        btnActualizar.setEnabled(true);
        btnDesactivar.setEnabled(
                "ACTIVO".equalsIgnoreCase(estado)
        );
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private boolean validarCampos() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAdvertencia(
                    "Ingrese el nombre de la categoría."
            );
            txtNombre.requestFocusInWindow();
            return false;
        }

        if (nombre.length() > 50) {
            mostrarAdvertencia(
                    "La categoría no puede superar 50 caracteres."
            );
            return false;
        }

        if (descripcion.length() > 200) {
            mostrarAdvertencia(
                    "La descripción no puede superar 200 caracteres."
            );
            return false;
        }

        if (cmbEstado.getSelectedItem() == null) {
            mostrarAdvertencia("Seleccione un estado.");
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtCodigo.setText("");
        txtNombre.setText("");
        txtDescripcion.setText("");

        cmbEstado.setSelectedItem("ACTIVO");

        cargandoTabla = true;
        try {
            tblTipoEquipo.clearSelection();
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
