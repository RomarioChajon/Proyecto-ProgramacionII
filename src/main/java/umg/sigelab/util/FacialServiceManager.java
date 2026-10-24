
package umg.sigelab.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Administra el servicio facial de SIGELAB.
 *
 * Windows/local: inicia Python automáticamente si hace falta.
 * Docker/remoto: utiliza el servicio configurado sin iniciar Python.
 */
public final class FacialServiceManager {

    private static final String URL_BASE = obtenerUrlBase();
    private static final String HEALTH_URL = URL_BASE + "/health";

    private static final Object LOCK = new Object();
    private static Process procesoFacial;

    private FacialServiceManager() {
    }

    public static boolean iniciarSiEsNecesario() {
        synchronized (LOCK) {
            if (estaActivo()) {
                return true;
            }

            if (servicioExterno()) {
                System.err.println(
                        "El servicio facial configurado no está disponible: " + HEALTH_URL
                );
                return false;
            }

            Path carpeta = localizarServicio();

            if (carpeta == null) {
                System.err.println(
                        "No se encontró la carpeta facial-service en SIGELAB."
                );
                return false;
            }

            boolean windows = System.getProperty("os.name", "")
                    .toLowerCase(Locale.ROOT)
                    .contains("win");

            Path python = carpeta.resolve(
                    windows ? ".venv/Scripts/python.exe" : ".venv/bin/python"
            );

            if (!Files.isRegularFile(python)) {
                System.err.println(
                        "No se encontró el entorno Python: " + python
                );
                return false;
            }

            try {
                if (procesoFacial == null || !procesoFacial.isAlive()) {

                    ProcessBuilder builder = new ProcessBuilder(
                            python.toString(),
                            "-m",
                            "uvicorn",
                            "app.main:app",
                            "--host",
                            "127.0.0.1",
                            "--port",
                            "8765",
                            "--app-dir",
                            carpeta.toString()
                    );

                    builder.directory(carpeta.toFile());
                    builder.redirectErrorStream(true);

                    builder.redirectOutput(
                            ProcessBuilder.Redirect.appendTo(
                                    carpeta.resolve("facial-service.log").toFile()
                            )
                    );

                    procesoFacial = builder.start();
                }

                long limite = System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(90);

                while (System.nanoTime() < limite) {

                    if (estaActivo()) {
                        return true;
                    }

                    if (!procesoFacial.isAlive()) {
                        System.err.println(
                                "FastAPI terminó antes de estar disponible. Revise facial-service.log"
                        );
                        return false;
                    }

                    Thread.sleep(1000);
                }

                System.err.println(
                        "FastAPI tardó demasiado en iniciar. Revise facial-service.log"
                );

            } catch (IOException e) {
                System.err.println(
                        "No se pudo iniciar FastAPI: " + e.getMessage()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return false;
        }
    }

    private static String obtenerUrlBase() {
        String url = System.getProperty("sigelab.facial.url");

        if (url == null || url.isBlank()) {
            url = System.getenv("FACIAL_API_URL");
        }

        if (url == null || url.isBlank()) {
            url = "http://127.0.0.1:8765";
        }

        return url.trim().replaceAll("/+$", "");
    }

    private static boolean servicioExterno() {
        String modo = System.getenv("SIGELAB_FACIAL_EXTERNAL");

        if ("true".equalsIgnoreCase(modo)) {
            return true;
        }

        String url = URL_BASE.toLowerCase(Locale.ROOT);

        return !url.equals("http://127.0.0.1:8765")
                && !url.equals("http://localhost:8765");
    }

    private static Path localizarServicio() {
        Path actual = Path.of(
                System.getProperty("user.dir", ".")
        ).toAbsolutePath().normalize();

        for (int i = 0; i < 5 && actual != null; i++, actual = actual.getParent()) {

            Path candidato = actual.resolve("facial-service");

            if (Files.isDirectory(candidato)
                    && Files.isRegularFile(candidato.resolve("app/main.py"))) {
                return candidato;
            }
        }

        return null;
    }

    private static boolean estaActivo() {
        try {
            HttpClient cliente = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest peticion = HttpRequest.newBuilder(
                            URI.create(HEALTH_URL)
                    ).timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            return cliente.send(
                    peticion,
                    HttpResponse.BodyHandlers.discarding()
            ).statusCode() == 200;

        } catch (IOException | IllegalArgumentException e) {
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
