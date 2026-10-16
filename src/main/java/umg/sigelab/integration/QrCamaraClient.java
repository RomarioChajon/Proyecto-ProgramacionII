package umg.sigelab.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente del lector de QR por cámara. La cámara la abre el servicio local de Python/OpenCV
 * (el mismo del reconocimiento facial, puerto 8765): Java solo pide "lee un QR" y recibe el texto.
 */
public class QrCamaraClient {

    private static final Pattern TEXTO = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern DETALLE = Pattern.compile("\"detail\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(4)).build();
    private final String baseUrl = System.getProperty("sigelab.facial.url",
            System.getenv().getOrDefault("FACIAL_API_URL", "http://127.0.0.1:8765")).replaceAll("/+$", "");

    /**
     * Abre la cámara y espera un QR.
     *
     * @return el texto del QR leído
     * @throws IOException con un mensaje entendible si el servicio no responde o no se leyó nada
     */
    public String leer(int segundos) throws IOException {
        try {
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(baseUrl + "/qr/scan?timeout=" + segundos))
                    .timeout(Duration.ofSeconds(segundos + 40))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            String cuerpo = respuesta.body() == null ? "" : respuesta.body();
            if (respuesta.statusCode() / 100 == 2) {
                Matcher m = TEXTO.matcher(cuerpo);
                if (m.find()) {
                    return desescapar(m.group(1));
                }
                throw new IOException("El lector no devolvió ningún texto.");
            }
            Matcher d = DETALLE.matcher(cuerpo);
            throw new IOException(d.find() ? desescapar(d.group(1)) : "El lector de QR respondió con un error.");
        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            throw new IOException("No se pudo conectar con el servicio de la cámara. Inicie el servicio facial "
                    + "(start-facial) o use \"Cargar imagen QR\" / escriba el código.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Lectura de QR interrumpida.");
        }
    }

    /** Convierte las secuencias de escape de JSON (\n, \", \\, \\uXXXX) en texto normal. */
    static String desescapar(String json) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c != '\\' || i + 1 >= json.length()) {
                sb.append(c);
                continue;
            }
            char n = json.charAt(++i);
            switch (n) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> sb.append(n);
            }
        }
        return sb.toString();
    }
}
