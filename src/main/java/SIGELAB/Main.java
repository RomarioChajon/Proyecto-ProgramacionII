package SIGELAB;

import SIGELAB.view.LaboratorioForm;
import SIGELAB.view.TipoEquipoForm;
import SIGELAB.view.ProductoForm;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame("SIGELAB - Menú Principal");

            JPanel panel = new JPanel(
                    new GridLayout(3, 1, 10, 10)
            );

            panel.setBorder(
                    BorderFactory.createEmptyBorder(
                            30, 50, 30, 50
                    )
            );

            JButton btnLaboratorios =
                    new JButton("Laboratorios");

            JButton btnTiposEquipo =
                    new JButton("Tipos de Equipo");

            JButton btnProductos =
                    new JButton("Productos");

            panel.add(btnLaboratorios);
            panel.add(btnTiposEquipo);
            panel.add(btnProductos);

            btnLaboratorios.addActionListener(e -> {

                LaboratorioForm formulario =
                        new LaboratorioForm();

                abrirVentana(
                        "SIGELAB - Laboratorios",
                        formulario.getPanelPrincipal()
                );
            });

            btnTiposEquipo.addActionListener(e -> {

                TipoEquipoForm formulario =
                        new TipoEquipoForm();

                abrirVentana(
                        "SIGELAB - Tipos de Equipo",
                        formulario.getPanelPrincipal()
                );
            });

            btnProductos.addActionListener(e -> {

                ProductoForm formulario =
                        new ProductoForm();

                abrirVentana(
                        "SIGELAB - Productos",
                        formulario.getPanelPrincipal()
                );
            });

            ventana.setContentPane(panel);

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    private static void abrirVentana(
            String titulo,
            JPanel panelFormulario) {

        JFrame ventana =
                new JFrame(titulo);

        ventana.setContentPane(
                panelFormulario
        );

        ventana.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        ventana.pack();
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
}