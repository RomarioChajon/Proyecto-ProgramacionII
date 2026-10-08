
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

            // Ahora tenemos 4 módulos
            JPanel panel = new JPanel(
                    new GridLayout(4, 1, 10, 10)
            );

            panel.setBorder(
                    BorderFactory.createEmptyBorder(
                            30, 50, 30, 50
                    )
            );

            // BOTONES DEL MENÚ
            JButton btnLaboratorios =
                    new JButton("Laboratorios");

            JButton btnTiposEquipo =
                    new JButton("Tipos de Equipo");

            JButton btnProductos =
                    new JButton("Productos");

            JButton btnEquipos =
                    new JButton("Equipos");

            // AGREGAR BOTONES AL PANEL
            panel.add(btnLaboratorios);
            panel.add(btnTiposEquipo);
            panel.add(btnProductos);
            panel.add(btnEquipos);

            // ABRIR LABORATORIOS
            btnLaboratorios.addActionListener(e -> {

                LaboratorioForm formulario =
                        new LaboratorioForm();

                abrirVentana(
                        "SIGELAB - Laboratorios",
                        formulario.getPanelPrincipal()
                );
            });

            // ABRIR TIPOS DE EQUIPO
            btnTiposEquipo.addActionListener(e -> {

                TipoEquipoForm formulario =
                        new TipoEquipoForm();

                abrirVentana(
                        "SIGELAB - Tipos de Equipo",
                        formulario.getPanelPrincipal()
                );
            });

            // ABRIR PRODUCTOS
            btnProductos.addActionListener(e -> {

                ProductoForm formulario =
                        new ProductoForm();

                abrirVentana(
                        "SIGELAB - Productos",
                        formulario.getPanelPrincipal()
                );
            });

            // ABRIR EQUIPOS
            btnEquipos.addActionListener(e -> {

                EquipoForm formulario =
                        new EquipoForm();

                abrirVentana(
                        "SIGELAB - Equipos",
                        formulario.getPanelPrincipal()
                );
            });

            // CONFIGURAR VENTANA PRINCIPAL
            ventana.setContentPane(panel);

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    // MÉTODO PARA ABRIR FORMULARIOS
    private static void abrirVentana(
            String titulo,
            JPanel panelFormulario) {

        JFrame ventana = new JFrame(titulo);

        ventana.setContentPane(panelFormulario);

        ventana.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        ventana.pack();
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
}
