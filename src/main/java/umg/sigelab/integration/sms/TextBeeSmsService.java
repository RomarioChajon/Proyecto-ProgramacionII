
package umg.sigelab.integration.sms;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TextBeeSmsService {

    private static final String API_URL =
            "https://api.textbee.dev/api/v1/gateway/send-sms";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void enviarSms(String telefono, String mensaje) {

        String apiKey = System.getenv("TEXTBEE_API_KEY");
        String deviceId = System.getenv("TEXTBEE_DEVICE_ID");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar TEXTBEE_API_KEY."
            );
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar TEXTBEE_DEVICE_ID."
            );
        }

        if (telefono == null
                || !telefono.matches("\\+[1-9]\\d{7,14}")) {
            throw new IllegalArgumentException(
                    "Usá el código internacional, por ejemplo +502XXXXXXXX."
            );
        }

        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacío."
            );
        }

        String body = "{"
                + "\"recipients\":[" + json(telefono) + "],"
                + "\"message\":" + json(mensaje) + ","
                + "\"deviceId\":" + json(deviceId)
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "TextBee respondió HTTP "
                                + response.statusCode()
                                + ". Detalle: " + response.body()
                );
            }

            System.out.println("Respuesta de TextBee: "
                    + response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "El envío fue interrumpido.", e
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo conectar con TextBee.", e
            );
        }
    }

    private static String json(String value) {
        StringBuilder result = new StringBuilder("\"");

        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        result.append(String.format(
                                "\\u%04x", (int) c
                        ));
                    } else {
                        result.append(c);
                    }
            }
        }

        return result.append('"').toString();
    }
}
