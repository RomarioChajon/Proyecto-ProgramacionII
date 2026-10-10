package umg.sigelab.controller;

import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.UsuarioService;

import java.util.List;
import java.util.Optional;

/**
 * Controlador de usuarios: intermediario DELGADO entre la vista y el Service.
 * No tiene reglas de negocio ni SQL; solo delega. Las excepciones
 * ({@code BusinessException}, {@code DataAccessException}) suben hasta la vista,
 * que decide cómo mostrarlas.
 */
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController() {
        this(new UsuarioService());
    }

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    public List<Usuario> listar(String texto, EstadoRegistro estado) {
        return service.listar(texto, estado);
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {
        return service.buscarPorId(idUsuario);
    }

    public Usuario crear(Usuario datos, String passwordPlano) {
        return service.crear(datos, passwordPlano);
    }

    public Usuario actualizar(Usuario datos, String passwordNueva, boolean cambiarFoto) {
        return service.actualizar(datos, passwordNueva, cambiarFoto);
    }

    public void cambiarEstado(int idUsuario, EstadoRegistro nuevoEstado, int idUsuarioActual) {
        service.cambiarEstado(idUsuario, nuevoEstado, idUsuarioActual);
    }
}
