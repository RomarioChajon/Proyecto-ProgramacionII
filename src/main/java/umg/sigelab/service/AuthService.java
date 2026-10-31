package umg.sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.UsuarioRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;
import umg.sigelab.util.Validaciones;

import java.util.Optional;

/**
 * Reglas de acceso al sistema: iniciar sesión, auto-registro y reinicio de contraseña.
 *
 * <p>La verificación FACIAL no está aquí: la cámara la maneja el servicio Python y la pantalla
 * la consulta con {@code FacialAuthClient}. Este servicio solo decide quién PUEDE intentar
 * ingresar (existe, está activo, coincide el correo, etc.).</p>
 */
public class AuthService {
// complemento de auditoria Registra los logins exitosos y fallidos
    /** Mensaje único para usuario o contraseña incorrectos: no revela cuál de los dos falló. */
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";

    /** Hash de relleno: se compara aunque el usuario no exista, para que el tiempo de respuesta no lo delate. */
    private static final String HASH_RELLENO = BCrypt.hashpw("relleno-no-valido", BCrypt.gensalt(10));

    private final UsuarioRepository repository;
    private final UsuarioService usuarioService;
    private final AuditoriaRepository auditoria = new AuditoriaRepository();

    public AuthService() {
        this(new UsuarioRepository(), new UsuarioService());
    }

    /** Recibe sus dependencias para poder sustituirlas en pruebas. */
    public AuthService(UsuarioRepository repository, UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
    }

    // ------------------------------------------------------------------
    // Ingreso: primer paso (usuario y contraseña); el segundo paso es el rostro
    // ------------------------------------------------------------------

    /**
     * Valida usuario y contraseña.
     *
     * @return el usuario autenticado (sin hash de contraseña)
     * @throws BusinessException si las credenciales son incorrectas o el usuario está inactivo
     */
    public Usuario autenticar(String nombreUsuario, String password) {
        String usuario = Validaciones.requerido(nombreUsuario, "Ingrese su usuario.");
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Ingrese su contraseña.");
        }

        Optional<Usuario> encontrado = repository.buscarParaLogin(usuario);
        // Siempre se ejecuta BCrypt, exista o no el usuario.
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(HASH_RELLENO);
        boolean coincide = BCrypt.checkpw(password, hash);

        if (encontrado.isEmpty() || !coincide) {
            auditarAcceso(encontrado.map(Usuario::getIdUsuario).orElse(null), usuario, AccionAuditoria.LOGIN_FALLIDO,
                    "Usuario o contraseña incorrectos.");
            throw new BusinessException(CREDENCIALES_INVALIDAS);
        }
        Usuario autenticado = encontrado.get();
        if (!autenticado.isActivo()) {
            // RN-01: un usuario INACTIVO no inicia sesión. Aquí ya demostró conocer la contraseña,
            // por eso sí se le dice el motivo.
            auditarAcceso(autenticado.getIdUsuario(), usuario, AccionAuditoria.LOGIN_FALLIDO,
                    "Intento de ingreso con un usuario INACTIVO.");
            throw new BusinessException("Su usuario está inactivo. Contacte al administrador.");
        }
        autenticado.setPasswordHash(null); // el hash no debe salir de esta capa
        return autenticado;
    }

    /** Segundo paso terminado: el rostro coincidió, el ingreso se completó (RN-02 y RN-18). */
    public void registrarIngresoExitoso(Usuario usuario) {
        auditarAcceso(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_EXITOSO,
                "Ingreso con contraseña y reconocimiento facial.");
    }

    /** La contraseña era correcta pero el rostro no coincidió (o no pudo verificarse). */
    public void registrarIngresoFallidoFacial(Usuario usuario, String motivo) {
        auditarAcceso(usuario.getIdUsuario(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_FALLIDO,
                "Verificación facial no aprobada: " + motivo);
    }

    /**
     * Guarda el rastro en Auditoría. Si la propia auditoría fallara, el ingreso no se bloquea:
     * el usuario no debe quedarse fuera por un problema de registro.
     */
    private void auditarAcceso(Integer idUsuario, String usuarioIntento, AccionAuditoria accion, String descripcion) {
        try {
            Transaccion.ejecutar("registrar la auditoría", cn -> {
                auditoria.registrarAcceso(cn, idUsuario, usuarioIntento, accion, descripcion);
                return null;
            });
        } catch (RuntimeException ignorado) {
            // Sin auditoría no se detiene el flujo de acceso.
        }
    }

    // ------------------------------------------------------------------
    // Auto-registro (siempre como ESTUDIANTE)
    // ------------------------------------------------------------------

    /**
     * Crea una cuenta nueva con rol ESTUDIANTE. Un docente o encargado debe pedir al
     * administrador que le cambie el rol. Reutiliza todas las validaciones de {@link UsuarioService#crear}.
     */
    public Usuario registrarEstudiante(Usuario datos, String passwordPlano) {
        if (datos == null) {
            throw new BusinessException("Complete los datos del registro.");
        }
        datos.setRol(RolUsuario.ESTUDIANTE); // el rol NO lo elige quien se registra
        datos.setLaboratorio(null);
        datos.setFoto(null);
        return usuarioService.crear(datos, passwordPlano);
    }

    /**
     * Deshace un registro cuyo rostro no se pudo capturar, para que la persona pueda volver
     * a intentarlo con el mismo usuario. Es la única eliminación física del sistema.
     */
    public void deshacerRegistro(int idUsuario) {
        repository.eliminarRegistroRecienCreado(idUsuario);
    }

    // ------------------------------------------------------------------
    // Reinicio de contraseña (usuario + correo + rostro)
    // ------------------------------------------------------------------

    /**
     * Paso 1: comprueba que el usuario y el correo pertenecen a la misma cuenta ACTIVA.
     * El rostro se verifica después, fuera de este servicio.
     */
    public Usuario verificarDatosParaReinicio(String nombreUsuario, String correo) {
        String usuario = Validaciones.requerido(nombreUsuario, "Ingrese su usuario.");
        String mail = Validaciones.requerido(correo, "Ingrese su correo.");
        return repository.buscarPorNombreUsuario(usuario)
                .filter(u -> u.isActivo() && u.getCorreo().equalsIgnoreCase(mail))
                .orElseThrow(() -> new BusinessException(
                        "El usuario y el correo no coinciden con una cuenta activa."));
    }

    /** Paso 2: guarda la contraseña nueva. Solo debe llamarse DESPUÉS de una verificación facial positiva. */
    public void reiniciarPassword(int idUsuario, String passwordNueva) {
        usuarioService.cambiarPassword(idUsuario, passwordNueva);
    }
}
