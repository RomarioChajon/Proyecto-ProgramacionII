package umg.sigelab.integration.facial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Instala, inicia y detiene el servicio facial (Python) desde Java, para no tener que usar
 * la línea de comandos a mano.
 *
 * <p>Reutiliza los scripts que ya trae la carpeta {@code facial-service}
 * ({@code setup-facial-*.sh/.ps1} y {@code start-facial.sh/.bat}); esta clase solo los ejecuta
 * y muestra su salida. Es un singleton porque el proceso del servicio debe sobrevivir aunque
 * se cierre y se vuelva a abrir la ventana de administración.</p>
 *
 * <p><b>Modo Terminal (macOS):</b> los instaladores de Homebrew pueden pedir la contraseña del
 * equipo y macOS asocia el permiso de la CÁMARA a la aplicación que abre el proceso. Por eso, en macOS
 * se abren en la app Terminal (la misma con la que ya se probó el servicio). En ese modo Java no ve
 * la salida; solo consulta si el servicio ya responde y si existe el marcador {@code .setup-ok}.</p>
 */
public final class FacialServiceLauncher {

    /** Archivo que se crea cuando la instalación termina bien. */
    private static final String MARCADOR_INSTALACION = ".setup-ok";
    private static final int PUERTO_POR_DEFECTO = 8765;
    private static final int MAX_LINEAS_LOG = 2000;

    private static final FacialServiceLauncher INSTANCIA = new FacialServiceLauncher();

    public static FacialServiceLauncher getInstance() {
        return INSTANCIA;
    }

    private final Preferences preferencias = Preferences.userNodeForPackage(FacialServiceLauncher.class);
    private final FacialAuthClient cliente = new FacialAuthClient();
    private final Deque<String> log = new ArrayDeque<>();
    private final List<Consumer<String>> oyentes = new CopyOnWriteArrayList<>();

    private volatile Process procesoServicio;   // solo si lo iniciamos directamente desde Java
    private volatile boolean instalando;

    private FacialServiceLauncher() {
        // Si cerramos SIGELAB, no dejamos el servicio Python huérfano.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Process p = procesoServicio;
            if (p != null && p.isAlive()) {
                terminarConDescendientes(p);
            }
        }, "sigelab-facial-shutdown"));
    }

    // ------------------------------------------------------------------
    // Configuración (la carpeta se recuerda entre ejecuciones)
    // ------------------------------------------------------------------

    /** Carpeta {@code facial-service} configurada o, si no hay, la que se encuentre junto al proyecto. */
    public Path getCarpeta() {
        String guardada = preferencias.get("carpeta", "");
        if (!guardada.isBlank() && validarCarpeta(Paths.get(guardada)) == null) {
            return Paths.get(guardada);
        }
        return detectarCarpeta();
    }

    public void setCarpeta(Path carpeta) {
        preferencias.put("carpeta", carpeta.toAbsolutePath().normalize().toString());
    }

    /** Busca {@code facial-service} en la carpeta de trabajo (IntelliJ usa la raíz del proyecto) y alrededor. */
    public static Path detectarCarpeta() {
        Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        Path[] candidatas = {
                base.resolve("facial-service"),
                base.getParent() == null ? base : base.getParent().resolve("facial-service"),
                base.resolve("Proyecto-ProgramacionII").resolve("facial-service")
        };
        for (Path c : candidatas) {
            if (validarCarpeta(c) == null) {
                return c;
            }
        }
        return candidatas[0];
    }

    /** @return null si la carpeta sirve; si no, el motivo en español */
    public static String validarCarpeta(Path carpeta) {
        if (carpeta == null || !Files.isDirectory(carpeta)) {
            return "La carpeta no existe.";
        }
        if (!Files.isRegularFile(carpeta.resolve("requirements.txt")) || !Files.isRegularFile(carpeta.resolve("app").resolve("main.py"))) {
            return "Esa carpeta no es facial-service (no se encontró requirements.txt ni app/main.py).";
        }
        return null;
    }

    /** Si es true, en macOS se usa la app Terminal. Por defecto true en macOS. */
    public boolean isUsarTerminal() {
        return esMac() && preferencias.getBoolean("usarTerminal", true);
    }

    public void setUsarTerminal(boolean usar) {
        preferencias.putBoolean("usarTerminal", usar);
    }

    public static boolean esMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    /** ¿Responde el servicio? (hace una llamada HTTP corta: no llamar desde el hilo de Swing). */
    public boolean servicioActivo() {
        return cliente.disponible();
    }

    /**
     * ¿Está instalado? Sí si existe el marcador del instalador, o si ya hay un .venv y un .env
     * (instalación hecha antes a mano desde la Terminal).
     */
    public boolean instalado() {
        Path carpeta = getCarpeta();
        return Files.isRegularFile(carpeta.resolve(MARCADOR_INSTALACION))
                || (entornoPresente(carpeta) && Files.isRegularFile(carpeta.resolve(".env")));
    }

    public boolean isInstalando() {
        return instalando;
    }

    // ------------------------------------------------------------------
    // Registro de mensajes (la ventana se suscribe para mostrarlos)
    // ------------------------------------------------------------------

    public void agregarOyente(Consumer<String> oyente) {
        oyentes.add(oyente);
    }

    public void quitarOyente(Consumer<String> oyente) {
        oyentes.remove(oyente);
    }

    /** Todo lo registrado hasta ahora (para mostrarlo al abrir la ventana). */
    public synchronized List<String> historial() {
        return new ArrayList<>(log);
    }

    private synchronized void registrar(String linea) {
        log.addLast(linea);
        while (log.size() > MAX_LINEAS_LOG) {
            log.removeFirst();
        }
        for (Consumer<String> oyente : oyentes) {
            oyente.accept(linea);
        }
    }

    // ------------------------------------------------------------------
    // Acciones (todas BLOQUEAN: llamarlas desde un SwingWorker o un hilo)
    // ------------------------------------------------------------------

    /**
     * Instala el entorno: crea el .venv, instala las dependencias, genera el .env desde
     * db.properties y descarga el modelo. Puede tardar varios minutos.
     */
    public void instalar() {
        Path carpeta = carpetaValida();
        if (instalando) {
            registrar("Ya hay una instalación en curso.");
            return;
        }
        borrarMarcador(carpeta);
        if (isUsarTerminal()) {
            String script = "setup-facial-macos.sh";
            abrirEnTerminal(carpeta, "bash " + script + " && touch " + MARCADOR_INSTALACION);
            registrar("Se abrió la Terminal con la instalación. Si pide una contraseña, es la de su Mac.");
            registrar("Este panel detecta solo cuando termina (aparecerá \"Instalación: completa\").");
            return;
        }
        instalando = true;
        try {
            List<String> comando;
            if (esWindows()) {
                comando = List.of("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "setup-facial.ps1");
            } else if (esMac()) {
                comando = List.of("bash", "setup-facial-macos.sh");
            } else {
                comando = List.of("bash", "setup-facial-linux.sh");
            }
            registrar("=== Instalación del servicio facial (puede tardar varios minutos) ===");
            int salida = ejecutarYEsperar(comando, carpeta);
            if (salida == 0) {
                crearMarcador(carpeta);
                registrar("=== Instalación completada. Ya puede iniciar el servicio. ===");
            } else {
                registrar("=== La instalación terminó con errores (código " + salida + "). Revise el registro. ===");
            }
        } finally {
            instalando = false;
        }
    }

    /**
     * Inicia el servicio y espera hasta que responda.
     *
     * @return true si el servicio quedó respondiendo
     */
    public boolean iniciar() {
        Path carpeta = carpetaValida();
        if (servicioActivo()) {
            registrar("El servicio facial ya está activo.");
            return true;
        }
        if (!instalado() && !entornoPresente(carpeta)) {
            registrar("Primero debe instalar el servicio (botón Instalar).");
            return false;
        }

        long limite = System.currentTimeMillis() + 180_000L; // el primer arranque carga el modelo: puede tardar
        if (isUsarTerminal()) {
            abrirEnTerminal(carpeta, "bash start-facial.sh");
            registrar("Se abrió la Terminal con el servicio. Déjela abierta mientras use SIGELAB.");
        } else {
            List<String> comando = esWindows() ? List.of("cmd", "/c", "start-facial.bat") : List.of("bash", "start-facial.sh");
            try {
                procesoServicio = lanzar(comando, carpeta, true);
            } catch (IOException e) {
                registrar("No se pudo iniciar el servicio: " + e.getMessage());
                return false;
            }
        }

        registrar("Esperando a que el servicio responda (la primera vez carga el modelo)...");
        while (System.currentTimeMillis() < limite) {
            if (servicioActivo()) {
                registrar("Servicio facial ACTIVO.");
                return true;
            }
            Process p = procesoServicio;
            if (p != null && !p.isAlive()) {
                registrar("El servicio se cerró inesperadamente (código " + p.exitValue() + "). Revise el registro.");
                return false;
            }
            dormir(1000);
        }
        registrar("El servicio no respondió a tiempo. Revise el registro o la Terminal.");
        return false;
    }

    /** Detiene el servicio, lo haya iniciado esta aplicación o la Terminal. */
    public void detener() {
        boolean hizoAlgo = false;
        Process p = procesoServicio;
        if (p != null && p.isAlive()) {
            terminarConDescendientes(p);
            procesoServicio = null;
            hizoAlgo = true;
        }
        if (servicioActivo()) { // lo inició otro proceso (Terminal, línea de comandos...)
            hizoAlgo |= detenerPorPuerto();
        }
        registrar(hizoAlgo ? "Servicio facial detenido." : "El servicio no estaba activo.");
    }

    // ------------------------------------------------------------------
    // Detalles internos
    // ------------------------------------------------------------------

    private Path carpetaValida() {
        Path carpeta = getCarpeta();
        String error = validarCarpeta(carpeta);
        if (error != null) {
            throw new IllegalStateException(error);
        }
        return carpeta;
    }

    private static boolean entornoPresente(Path carpeta) {
        return Files.isRegularFile(carpeta.resolve(".venv").resolve("bin").resolve("python"))
                || Files.isRegularFile(carpeta.resolve(".venv").resolve("Scripts").resolve("python.exe"));
    }

    private void crearMarcador(Path carpeta) {
        try {
            Files.writeString(carpeta.resolve(MARCADOR_INSTALACION), "ok", StandardCharsets.UTF_8);
        } catch (IOException e) {
            registrar("No se pudo crear el marcador de instalación: " + e.getMessage());
        }
    }

    private void borrarMarcador(Path carpeta) {
        try {
            Files.deleteIfExists(carpeta.resolve(MARCADOR_INSTALACION));
        } catch (IOException e) {
            // No es grave: el instalador lo volverá a crear.
        }
    }

    /** Ejecuta un comando mostrando su salida en el registro y espera a que termine. */
    private int ejecutarYEsperar(List<String> comando, Path carpeta) {
        try {
            Process p = lanzar(comando, carpeta, false);
            return p.waitFor();
        } catch (IOException e) {
            registrar("No se pudo ejecutar el comando: " + e.getMessage());
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Arranca un proceso en la carpeta indicada y manda su salida al registro.
     * El PATH se amplía con las rutas de Homebrew: una app abierta desde el Dock no las hereda
     * y los scripts creerían que Homebrew/Python no están instalados.
     */
    private Process lanzar(List<String> comando, Path carpeta, boolean demonio) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(comando)
                .directory(carpeta.toFile())
                .redirectErrorStream(true);
        Map<String, String> entorno = pb.environment();
        if (!esWindows()) {
            String actual = entorno.getOrDefault("PATH", "/usr/bin:/bin");
            entorno.put("PATH", "/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:" + actual);
        }
        entorno.put("PYTHONUNBUFFERED", "1");
        entorno.put("PYTHONIOENCODING", "utf-8");

        Process proceso = pb.start();
        proceso.getOutputStream().close(); // sin teclado: si un script pregunta algo, recibe fin de entrada

        Thread lector = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = r.readLine()) != null) {
                    // Las consultas periódicas a /health llenarían el registro de ruido: se omiten.
                    if (!linea.contains("/health")) {
                        registrar(linea);
                    }
                }
            } catch (IOException e) {
                // El proceso terminó o se cerró el flujo: no hay nada más que leer.
            }
        }, "sigelab-facial-log");
        lector.setDaemon(true);
        lector.start();
        return proceso;
    }

    /** Abre la app Terminal de macOS y ejecuta ahí un comando dentro de la carpeta del servicio. */
    private void abrirEnTerminal(Path carpeta, String comando) {
        try {
            new ProcessBuilder("osascript", "-e", scriptTerminal(carpeta, comando)).redirectErrorStream(true).start();
        } catch (IOException e) {
            registrar("No se pudo abrir la Terminal: " + e.getMessage());
        }
    }

    /** Arma el AppleScript que pide a Terminal ejecutar {@code cd <carpeta> && <comando>}. */
    static String scriptTerminal(Path carpeta, String comando) {
        String shell = "cd " + comillasShell(carpeta.toString()) + " && " + comando;
        return "tell application \"Terminal\"\n"
                + "activate\n"
                + "do script \"" + shell.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n"
                + "end tell";
    }

    /** Envuelve un texto en comillas simples para el shell (las comillas simples internas se escapan). */
    static String comillasShell(String texto) {
        return "'" + texto.replace("'", "'\\''") + "'";
    }

    /** Detiene lo que esté escuchando en el puerto del servicio (macOS/Linux con lsof; Windows con PowerShell). */
    private boolean detenerPorPuerto() {
        int puerto = puertoDelServicio();
        try {
            List<String> comando = esWindows()
                    ? List.of("powershell", "-NoProfile", "-Command",
                    "Get-NetTCPConnection -LocalPort " + puerto + " -State Listen | "
                            + "ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }")
                    : List.of("bash", "-c", "kill $(lsof -ti tcp:" + puerto + " -sTCP:LISTEN) 2>/dev/null");
            ProcessBuilder pb = new ProcessBuilder(comando).redirectErrorStream(true);
            if (!esWindows()) {
                pb.environment().merge("PATH", "/usr/sbin:/usr/bin", (a, b) -> b + ":" + a);
            }
            Process p = pb.start();
            p.getOutputStream().close();
            p.getInputStream().readAllBytes();
            p.waitFor();
            dormir(800);
            return !servicioActivo();
        } catch (IOException e) {
            registrar("No se pudo detener el servicio por el puerto: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Puerto de la URL configurada del servicio (por defecto 8765). */
    private static int puertoDelServicio() {
        String url = System.getProperty("sigelab.facial.url",
                System.getenv().getOrDefault("FACIAL_API_URL", "http://127.0.0.1:" + PUERTO_POR_DEFECTO));
        try {
            int puerto = java.net.URI.create(url).getPort();
            return puerto > 0 ? puerto : PUERTO_POR_DEFECTO;
        } catch (IllegalArgumentException e) {
            return PUERTO_POR_DEFECTO;
        }
    }

    /** Termina el proceso y los procesos que él creó (el .bat de Windows lanza a Python como hijo). */
    private static void terminarConDescendientes(Process proceso) {
        proceso.descendants().forEach(ProcessHandle::destroy);
        proceso.destroy();
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
