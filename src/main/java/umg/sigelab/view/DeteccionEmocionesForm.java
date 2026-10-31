
package umg.sigelab.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class DeteccionEmocionesForm {

    private final JPanel panelPrincipal;
    private final JLabel lblEstado;
    private final JLabel lblCantidad;
    private final JTextArea txtResultados;
    private final JButton btnDetectar;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String ENDPOINT =
            "http://127.0.0.1:8765/face/detect-emotions/camera";

    public DeteccionEmocionesForm() {

        panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBackground(new Color(241, 248, 252));
        panelPrincipal.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel titulo = new JLabel("Detección de expresiones faciales");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(new Color(35, 78, 112));

        JLabel descripcion = new JLabel(
                "Captura un fotograma y consulta las expresiones visibles."
        );
        descripcion.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel encabezado = new JPanel(new GridLayout(2, 1, 5, 5));
        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(descripcion);

        lblEstado = new JLabel("Servicio facial: pendiente de consulta");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblCantidad = new JLabel("Rostros detectados: 0");
        lblCantidad.setFont(new Font("Segoe UI", Font.BOLD, 16));

        txtResultados = new JTextArea();
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtResultados.setLineWrap(true);
        txtResultados.setWrapStyleWord(true);
        txtResultados.setText(
                "Pulsa el botón para realizar una captura con la cámara."
        );

        JScrollPane scroll = new JScrollPane(txtResultados);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Resultados de detección"
        ));

        btnDetectar = new JButton("Detectar expresiones");
        btnDetectar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDetectar.setBackground(new Color(43, 143, 214));
        btnDetectar.setForeground(Color.WHITE);
        btnDetectar.setFocusPainted(false);

        btnDetectar.addActionListener(e -> detectar());

        JPanel inferior = new JPanel(new GridLayout(3, 1, 8, 8));
        inferior.setOpaque(false);
        inferior.add(lblEstado);
        inferior.add(lblCantidad);
        inferior.add(btnDetectar);

        panelPrincipal.add(encabezado, BorderLayout.NORTH);
        panelPrincipal.add(scroll, BorderLayout.CENTER);
        panelPrincipal.add(inferior, BorderLayout.SOUTH);
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    private void detectar() {

        btnDetectar.setEnabled(false);
        lblEstado.setText("Consultando cámara...");
        txtResultados.setText("Esperando respuesta del servicio facial...");

        new SwingWorker<JsonNode, Void>() {

            @Override
            protected JsonNode doInBackground() throws Exception {

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                JsonNode json = mapper.readTree(response.body());

                if (response.statusCode() < 200 ||
                        response.statusCode() >= 300) {

                    String detail = json.has("detail")
                            ? json.get("detail").asText()
                            : "Error al consultar el servicio facial.";

                    throw new IllegalStateException(detail);
                }

                return json;
            }

            @Override
            protected void done() {

                btnDetectar.setEnabled(true);

                try {
                    mostrarResultados(get());

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mostrarError("La operación fue interrumpida.");

                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    mostrarError(causa != null
                            ? causa.getMessage()
                            : ex.getMessage());

                } catch (Exception ex) {
                    mostrarError(ex.getMessage());
                }
            }

        }.execute();
    }

    private void mostrarResultados(JsonNode json) {

        int cantidad = json.path("count").asInt(0);
        lblCantidad.setText("Rostros detectados: " + cantidad);
        lblEstado.setText("Servicio facial: respuesta recibida");

        StringBuilder resultado = new StringBuilder();

        resultado.append(json.path("message").asText())
                .append("\n\n");

        JsonNode faces = json.path("faces");

        for (JsonNode face : faces) {

            resultado.append("Rostro ")
                    .append(face.path("faceNumber").asInt())
                    .append("\n");

            resultado.append("Expresión estimada: ")
                    .append(face.path("expression").asText("No disponible"))
                    .append("\n");

            resultado.append("Confianza del modelo: ")
                    .append(face.path("confidence").asText("No disponible"))
                    .append("\n\n");
        }

        resultado.append(json.path("note").asText(""));

        txtResultados.setText(resultado.toString());
        txtResultados.setCaretPosition(0);
    }

    private void mostrarError(String mensaje) {

        lblEstado.setText("Servicio facial: error");
        txtResultados.setText(
                mensaje != null
                        ? mensaje
                        : "No se pudo consultar el servicio facial."
        );
    }

    public static void abrir(Component padre) {

        DeteccionEmocionesForm formulario = new DeteccionEmocionesForm();

        Window propietario = padre == null
                ? null
                : SwingUtilities.getWindowAncestor(padre);

        JDialog dialogo = new JDialog(
                propietario,
                "SIGELAB - Detección de expresiones faciales"
        );

        dialogo.setContentPane(formulario.getPanelPrincipal());
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialogo.setMinimumSize(new Dimension(650, 450));
        dialogo.pack();
        dialogo.setLocationRelativeTo(padre);
        dialogo.setVisible(true);
    }

}

