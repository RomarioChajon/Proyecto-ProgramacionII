package umg.sigelab.service;

import org.mindrot.jbcrypt.BCrypt;
import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.LaboratorioRepository;
import umg.sigelab.repository.UsuarioRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;
import umg.sigelab.util.Validaciones;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de negocio de los usuarios.
 *
 * <p>Regla de la capa Service: aquí viven las DECISIONES (validar, comparar, rechazar).
 * No hay SQL (eso es del Repository) ni componentes Swing (eso es de la View).
 * La base de datos queda como segunda línea de defensa con sus restricciones UNIQUE/CHECK.</p>
 */
public class UsuarioService {

    // Longitudes de las columnas de la tabla Usuario.
    private static final int MAX_NOMBRE = 60;
    private static final int MAX_USUARIO = 30;
    private static final int MIN_USUARIO = 3;
    private static final int MAX_CORREO = 120;
    private static final int MAX_TELEFONO = 16;

    private static final Pattern PATRON_TELEFONO =
            Pattern.compile("^\\+[1-9]\\d{7,14}$");

    // Contraseña: BCrypt solo usa los primeros 72 bytes, por eso ese es el máximo.
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD_BYTES = 72;

    /** Letras, números, punto, guion y guion bajo. Sin espacios. */
    private static final Pattern PATRON_USUARIO = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final UsuarioRepository repository;
    private final LaboratorioRepository laboratorioRepository;

    public UsuarioService() {
        this(new UsuarioRepository(), new LaboratorioRepository());
    }

    /** Constructor que recibe sus dependencias: permite sustituirlas en pruebas. */
    public UsuarioService(UsuarioRepository repository, LaboratorioRepository laboratorioRepository) {
        this.repository = repository;
        this.laboratorioRepository = laboratorioRepository;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Lista usuarios (sin foto). Ambos filtros son opcionales (null = sin filtro). */
    public List<Usuario> listar(String texto, EstadoRegistro estado) {
        return repository.listar(texto, estado);
    }

    /** Busca un usuario con su foto. */
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return repository.buscarPorId(idUsuario);
    }

    // ------------------------------------------------------------------
    // Crear
    // ------------------------------------------------------------------

    /**
     * Crea un usuario ACTIVO.
     *
     * @param datos         nombre, apellido, usuario, correo, rol, laboratorio (si es ENCARGADO) y foto opcional
     * @param passwordPlano contraseña escrita por el administrador; se guarda cifrada con BCrypt
     * @return el usuario guardado, tal como quedó en la base de datos
     * @throws BusinessException si algún dato incumple una regla
     */
    public Usuario crear(Usuario datos, String passwordPlano) {
        validarDatos(datos);
        validarPassword(passwordPlano);
        datos.setEstado(EstadoRegistro.ACTIVO); // todo usuario nuevo nace ACTIVO

        validarUnicidad(datos, 0); // 0 = ningún usuario existente se excluye
        validarEncargadoUnico(datos, 0);

        datos.setPasswordHash(BCrypt.hashpw(passwordPlano, BCrypt.gensalt(10)));
        int id = repository.guardar(datos);
        return repository.buscarPorId(id)
                .orElseThrow(() -> new BusinessException("El usuario se guardó pero no se pudo volver a consultar."));
    }

    // ------------------------------------------------------------------
    // Actualizar
    // ------------------------------------------------------------------

    /**
     * Actualiza los datos de un usuario existente. NO cambia su estado (para eso está
     * {@link #cambiarEstado}).
     *
     * @param datos         datos nuevos; {@code idUsuario} debe ser el del usuario a editar
     * @param passwordNueva contraseña nueva, o null/vacía para conservar la actual
     * @param cambiarFoto   true si {@code datos.getFoto()} debe guardarse (null = quitar la foto);
     *                      false si la foto actual no debe tocarse
     */
    public Usuario actualizar(Usuario datos, String passwordNueva, boolean cambiarFoto) {
        if (datos == null || datos.getIdUsuario() <= 0) {
            throw new BusinessException("Seleccione el usuario que desea actualizar.");
        }
        Usuario actual = repository.buscarPorId(datos.getIdUsuario())
                .orElseThrow(() -> new BusinessException("El usuario ya no existe. Actualice la lista."));

        validarDatos(datos);
        datos.setEstado(actual.getEstado()); // actualizar no cambia el estado

        // Si es un ADMIN activo y se le quita el rol, debe quedar otro administrador activo.
        if (actual.getRol() == RolUsuario.ADMIN && actual.isActivo() && datos.getRol() != RolUsuario.ADMIN
                && repository.contarAdministradoresActivos() <= 1) {
            throw new BusinessException("No puede cambiar el rol del único administrador activo. "
                    + "Cree o active otro administrador primero.");
        }

        validarUnicidad(datos, datos.getIdUsuario());
        if (datos.isActivo()) {
            validarEncargadoUnico(datos, datos.getIdUsuario());
        }

        String nuevoHash = null;
        if (passwordNueva != null && !passwordNueva.isEmpty()) {
            validarPassword(passwordNueva);
            nuevoHash = BCrypt.hashpw(passwordNueva, BCrypt.gensalt(10));
        }

        if (!repository.actualizar(datos, nuevoHash, cambiarFoto)) {
            throw new BusinessException("No se pudo actualizar: el usuario ya no existe. Actualice la lista.");
        }
        return repository.buscarPorId(datos.getIdUsuario()).orElse(datos);
    }

    // ------------------------------------------------------------------
    // Activar / desactivar (baja lógica)
    // ------------------------------------------------------------------

    /**
     * Cambia el estado de un usuario. Los usuarios no se eliminan: se desactivan para conservar el historial.
     *
     * @param idUsuarioActual id del usuario que tiene la sesión abierta (0 si no se conoce); no puede desactivarse a sí mismo
     */
    public void cambiarEstado(int idUsuario, EstadoRegistro nuevoEstado, int idUsuarioActual) {
        Validaciones.seleccion(nuevoEstado, "Seleccione el nuevo estado.");
        Usuario usuario = repository.buscarPorId(idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario ya no existe. Actualice la lista."));

        if (usuario.getEstado() == nuevoEstado) {
            throw new BusinessException("El usuario ya está " + nuevoEstado.getEtiqueta().toLowerCase() + ".");
        }

        if (nuevoEstado == EstadoRegistro.INACTIVO) {
            if (idUsuario == idUsuarioActual) {
                throw new BusinessException("No puede desactivar el usuario con el que inició sesión.");
            }
            if (usuario.getRol() == RolUsuario.ADMIN && repository.contarAdministradoresActivos() <= 1) {
                throw new BusinessException("No puede desactivar al único administrador activo.");
            }
        } else if (usuario.getRol() == RolUsuario.ENCARGADO) {
            // Al reactivar un encargado, su laboratorio no debe tener ya otro encargado activo.
            validarEncargadoUnico(usuario, idUsuario);
        }

        repository.cambiarEstado(idUsuario, nuevoEstado);
        if (nuevoEstado == EstadoRegistro.INACTIVO) {
            // RN-18: desactivar un usuario deja rastro en Auditoría (quién lo hizo y a quién).
            Transaccion.ejecutar("registrar la auditoría", cn -> {
                new AuditoriaRepository().registrar(cn, idUsuarioActual > 0 ? idUsuarioActual : null, null,
                        AccionAuditoria.USUARIO_DESACTIVADO, "Usuario", idUsuario,
                        "Usuario " + usuario.getNombreUsuario() + " desactivado.");
                return null;
            });
        }
    }

    // ------------------------------------------------------------------
    // Cambiar contraseña (reinicio con verificación facial)
    // ------------------------------------------------------------------

    /**
     * Guarda una contraseña nueva. La verificación de identidad (usuario + correo + rostro)
     * la hace quien llama ANTES de invocar este método.
     */
    public void cambiarPassword(int idUsuario, String passwordNueva) {
        validarPassword(passwordNueva);
        Usuario usuario = repository.buscarPorId(idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario ya no existe."));
        if (!usuario.isActivo()) {
            throw new BusinessException("El usuario está inactivo. Contacte al administrador.");
        }
        repository.actualizarPassword(idUsuario, BCrypt.hashpw(passwordNueva, BCrypt.gensalt(10)));
    }

    // ------------------------------------------------------------------
    // Validaciones privadas
    // ------------------------------------------------------------------

    /** Valida y normaliza (recorta espacios) los datos que comparten el alta y la edición. */
    private void validarDatos(Usuario u) {
        if (u == null) {
            throw new BusinessException("Los datos del usuario son obligatorios.");
        }
        u.setNombre(Validaciones.requerido(u.getNombre(), "El nombre es obligatorio."));
        Validaciones.longitudMaxima(u.getNombre(), MAX_NOMBRE, "El nombre no puede superar " + MAX_NOMBRE + " caracteres.");

        u.setApellido(Validaciones.requerido(u.getApellido(), "El apellido es obligatorio."));
        Validaciones.longitudMaxima(u.getApellido(), MAX_NOMBRE, "El apellido no puede superar " + MAX_NOMBRE + " caracteres.");

        u.setNombreUsuario(Validaciones.requerido(u.getNombreUsuario(), "El nombre de usuario es obligatorio."));
        if (u.getNombreUsuario().length() < MIN_USUARIO || u.getNombreUsuario().length() > MAX_USUARIO) {
            throw new BusinessException("El nombre de usuario debe tener entre " + MIN_USUARIO + " y " + MAX_USUARIO + " caracteres.");
        }
        Validaciones.patron(u.getNombreUsuario(), PATRON_USUARIO,
                "El nombre de usuario solo puede tener letras, números, punto, guion y guion bajo (sin espacios).");

        u.setCorreo(Validaciones.requerido(u.getCorreo(), "El correo es obligatorio."));
        Validaciones.longitudMaxima(u.getCorreo(), MAX_CORREO, "El correo no puede superar " + MAX_CORREO + " caracteres.");
        Validaciones.correo(u.getCorreo(), "Ingrese un correo válido, por ejemplo nombre@dominio.com.");

        String telefono = Validaciones.requerido(
                u.getTelefono(),
                "El teléfono es obligatorio para enviar notificaciones SMS."
        );

        telefono = telefono.trim();

        if (telefono.length() > MAX_TELEFONO) {
            throw new BusinessException(
                    "El teléfono no puede superar " + MAX_TELEFONO + " caracteres."
            );
        }

        if (!PATRON_TELEFONO.matcher(telefono).matches()) {
            throw new BusinessException(
                    "Ingrese el teléfono en formato internacional, por ejemplo +50212345678."
            );
        }

        u.setTelefono(telefono);

        Validaciones.seleccion(u.getRol(), "Seleccione el rol del usuario.");
        validarLaboratorioSegunRol(u);
    }

    /**
     * ENCARGADO debe tener un laboratorio ACTIVO existente; los demás roles NO deben tener laboratorio
     * (la base de datos también lo exige con el CHECK CK_Usuario_LabEncargado).
     */
    private void validarLaboratorioSegunRol(Usuario u) {
        if (u.getRol().requiereLaboratorio()) {
            Laboratorio seleccionado = Validaciones.seleccion(u.getLaboratorio(),
                    "Seleccione el laboratorio del encargado.");
            Laboratorio real = laboratorioRepository.buscarPorId(seleccionado.getIdLaboratorio())
                    .orElseThrow(() -> new BusinessException("El laboratorio seleccionado ya no existe."));
            if (real.getEstado() != EstadoRegistro.ACTIVO) {
                throw new BusinessException("El laboratorio " + real.getCodigo() + " está inactivo. Elija otro laboratorio.");
            }
            u.setLaboratorio(real); // usamos los datos reales de la base de datos
        } else {
            u.setLaboratorio(null);
        }
    }

    /** Nombre de usuario y correo no pueden repetirse (RN: datos únicos). */
    private void validarUnicidad(Usuario u, int idExcluido) {
        if (repository.existeNombreUsuario(u.getNombreUsuario(), idExcluido)) {
            throw new BusinessException("Ya existe un usuario con el nombre de usuario \"" + u.getNombreUsuario() + "\".");
        }
        if (repository.existeCorreo(u.getCorreo(), idExcluido)) {
            throw new BusinessException("Ya existe un usuario con el correo \"" + u.getCorreo() + "\".");
        }
    }

    /** Un laboratorio solo puede tener UN encargado activo. */
    private void validarEncargadoUnico(Usuario u, int idExcluido) {
        if (u.getRol() == RolUsuario.ENCARGADO && u.getLaboratorio() != null
                && repository.existeEncargadoActivo(u.getLaboratorio().getIdLaboratorio(), idExcluido)) {
            throw new BusinessException("El laboratorio " + u.getLaboratorio().getCodigo()
                    + " ya tiene un encargado activo. Desactívelo o elija otro laboratorio.");
        }
    }

    private void validarPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("La contraseña es obligatoria.");
        }
        if (password.length() < MIN_PASSWORD) {
            throw new BusinessException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException("La contraseña es demasiado larga (máximo " + MAX_PASSWORD_BYTES + " bytes).");
        }
    }
}
