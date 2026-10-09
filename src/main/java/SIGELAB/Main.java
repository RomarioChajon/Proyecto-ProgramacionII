package SIGELAB;

import SIGELAB.view.LaboratorioForm;
import SIGELAB.view.TipoEquipoForm;
import SIGELAB.view.ProductoForm;
import SIGELAB.view.EquipoForm;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame("SIGELAB - Menú Principal");

            JPanel panel = new JPanel(
                    new GridLayout(4, 1, 10, 10)
            );

            panel.setBorder(
                    BorderFactory.createEmptyBorder(30, 50, 30, 50)
            );

            JButton btnLaboratorios = new JButton("Laboratorios");
            JButton btnTiposEquipo = new JButton("Tipos de Equipo");
            JButton btnProductos = new JButton("Productos");
            JButton btnEquipos = new JButton("Equipos");

            panel.add(btnLaboratorios);
            panel.add(btnTiposEquipo);
            panel.add(btnProductos);
            panel.add(btnEquipos);

            btnLaboratorios.addActionListener(e -> {
                try {
                    LaboratorioForm formulario = new LaboratorioForm();

                    abrirVentana(
                            "SIGELAB - Laboratorios",
                            formulario.getPanelPrincipal()
                    );

                } catch (Exception | LinkageError ex) {
                    mostrarError(ventana, "Laboratorios", ex);
                }
            });

            btnTiposEquipo.addActionListener(e -> {
                try {
                    System.out.println("Abriendo Tipos de Equipo...");

                    TipoEquipoForm formulario = new TipoEquipoForm();

                    JPanel panelFormulario = formulario.getPanelPrincipal();

                    if (panelFormulario == null) {
                        throw new IllegalStateException(
                                "El panel principal de TipoEquipoForm es null."
                        );
                    }

                    abrirVentana(
                            "SIGELAB - Tipos de Equipo",
                            panelFormulario
                    );

                    System.out.println(
                            "Ventana Tipos de Equipo abierta correctamente."
                    );

                } catch (Exception | LinkageError ex) {
                    mostrarError(ventana, "Tipos de Equipo", ex);
                }
            });

            btnProductos.addActionListener(e -> {
                try {
                    ProductoForm formulario = new ProductoForm();

                    abrirVentana(
                            "SIGELAB - Productos",
                            formulario.getPanelPrincipal()
                    );

                } catch (Exception | LinkageError ex) {
                    mostrarError(ventana, "Productos", ex);
                }
            });

            btnEquipos.addActionListener(e -> {
                try {
                    EquipoForm formulario = new EquipoForm();

                    abrirVentana(
                            "SIGELAB - Equipos",
                            formulario.getPanelPrincipal()
                    );

                } catch (Exception | LinkageError ex) {
                    mostrarError(ventana, "Equipos", ex);
                }
            });

            ventana.setContentPane(panel);

            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    private static void abrirVentana(
            String titulo,
            JPanel panelFormulario) {

        if (panelFormulario == null) {
            throw new IllegalStateException(
                    "No se puede abrir " + titulo +
                            " porque el panel principal es null."
            );
        }

        JFrame ventana = new JFrame(titulo);

        ventana.setContentPane(panelFormulario);

        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ventana.pack();

        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }

    private static void mostrarError(
            JFrame ventanaPrincipal,
            String modulo,
            Throwable error) {

        error.printStackTrace();

        String detalle = error.getMessage();

        if (detalle == null || detalle.isBlank()) {
            detalle = "No se proporcionaron detalles adicionales.";
        }

        JOptionPane.showMessageDialog(
                ventanaPrincipal,
                "No se pudo abrir el módulo: " + modulo +
                        "\n\nTipo de error: " +
                        error.getClass().getSimpleName() +
                        "\n\nDetalle: " + detalle,
                "Error - SIGELAB",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

