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

/** Inicia el servicio facial local únicamente cuando hace falta. */
public final class FacialServiceManager {

    private static final String HEALTH_URL = "http://127.0.0.1:8765/health";
    private static final Object LOCK = new Object();
    private static Process procesoFacial;

    private FacialServiceManager() { }

    public static boolean iniciarSiEsNecesario() {
        synchronized (LOCK) {
            if (estaActivo()) {
                return true;
            }

            Path carpeta = localizarServicio();
            if (carpeta == null) {
                System.err.println("No se encontró la carpeta facial-service en el proyecto SIGELAB.");
                return false;
            }

            boolean windows = System.getProperty("os.name", "")
                    .toLowerCase(Locale.ROOT).contains("win");
            Path python = carpeta.resolve(windows
                    ? ".venv/Scripts/python.exe"
                    : ".venv/bin/python");

            if (!Files.isRegularFile(python)) {
                System.err.println("No se encontró el entorno Python: " + python);
                return false;
            }

            try {
                if (procesoFacial == null || !procesoFacial.isAlive()) {
                    ProcessBuilder builder = new ProcessBuilder(
                            python.toString(), "-m", "uvicorn", "app.main:app",
                            "--host", "127.0.0.1", "--port", "8765",
                            "--app-dir", carpeta.toString()
                    );
                    builder.directory(carpeta.toFile());
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(ProcessBuilder.Redirect.appendTo(
                            carpeta.resolve("facial-service.log").toFile()));
                    procesoFacial = builder.start();
                }

                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
                while (System.nanoTime() < limite) {
                    if (estaActivo()) {
                        return true;
                    }
                    if (!procesoFacial.isAlive()) {
                        System.err.println("FastAPI terminó antes de estar disponible. Revise facial-service.log");
                        return false;
                    }
                    Thread.sleep(1000);
                }
                System.err.println("FastAPI tardó demasiado en iniciar. Revise facial-service.log");
            } catch (IOException e) {
                System.err.println("No se pudo iniciar FastAPI: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static Path localizarServicio() {
        Path actual = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        for (int i = 0; i < 5 && actual != null; i++, actual = actual.getParent()) {
            Path candidato = actual.resolve("facial-service");
            if (Files.isDirectory(candidato) &&
                    Files.isRegularFile(candidato.resolve("app/main.py"))) {
                return candidato;
            }
        }
        return null;
    }

    private static boolean estaActivo() {
        try {
            HttpClient cliente = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(HEALTH_URL))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            return cliente.send(peticion, HttpResponse.BodyHandlers.discarding())
                    .statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
