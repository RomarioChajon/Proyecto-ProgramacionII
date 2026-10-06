package SIGELAB.service;

import SIGELAB.model.Usuario;
import SIGELAB.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public boolean guardar(Usuario usuario, String password) {
        validarUsuario(usuario);
        validarPassword(password);

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        usuario.setPasswordHash(passwordHash);

        return usuarioRepository.guardar(usuario);
    }

    public List<Usuario> listar() {
        return usuarioRepository.listar();
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El ID del usuario no es válido."
            );
        }

        return usuarioRepository.buscarPorId(idUsuario);
    }

    public boolean actualizar(Usuario usuario, String nuevaPassword) {
        validarUsuario(usuario);

        Optional<Usuario> usuarioActual =
                usuarioRepository.buscarPorId(usuario.getIdUsuario());

        if (usuarioActual.isEmpty()) {
            throw new IllegalArgumentException(
                    "El usuario no existe."
            );
        }

        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            validarPassword(nuevaPassword);

            String passwordHash =
                    BCrypt.hashpw(
                            nuevaPassword,
                            BCrypt.gensalt()
                    );

            usuario.setPasswordHash(passwordHash);

        } else {
            usuario.setPasswordHash(
                    usuarioActual.get().getPasswordHash()
            );
        }

        return usuarioRepository.actualizar(usuario);
    }

    public boolean eliminar(int idUsuario) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El ID del usuario no es válido."
            );
        }

        return usuarioRepository.eliminar(idUsuario);
    }

    public boolean verificarPassword(
            String password,
            String passwordHash
    ) {

        if (password == null || passwordHash == null) {
            return false;
        }

        return BCrypt.checkpw(password, passwordHash);
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario es obligatorio."
            );
        }

        if (usuario.getNombre() == null ||
                usuario.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (usuario.getApellido() == null ||
                usuario.getApellido().isBlank()) {

            throw new IllegalArgumentException(
                    "El apellido es obligatorio."
            );
        }

        if (usuario.getUsuario() == null ||
                usuario.getUsuario().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio."
            );
        }

        if (usuario.getRol() == null ||
                usuario.getRol().isBlank()) {

            throw new IllegalArgumentException(
                    "El rol es obligatorio."
            );
        }

        switch (usuario.getRol()) {
            case "ADMIN",
                 "ENCARGADO",
                 "DOCENTE",
                 "ESTUDIANTE" -> {
            }

            default -> throw new IllegalArgumentException(
                    "El rol seleccionado no es válido."
            );
        }
    }

    private void validarPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres."
            );
        }
    }
}