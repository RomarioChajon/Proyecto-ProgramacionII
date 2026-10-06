package SIGELAB.controller;

import SIGELAB.model.Usuario;
import SIGELAB.service.UsuarioService;

import java.util.List;
import java.util.Optional;

public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController() {
        this.usuarioService = new UsuarioService();
    }

    public boolean guardar(String nombre,
                           String apellido,
                           String nombreUsuario,
                           String password,
                           String rol,
                           boolean activo) {

        Usuario usuario = new Usuario();

        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setUsuario(nombreUsuario);
        usuario.setRol(rol);
        usuario.setActivo(activo);

        return usuarioService.guardar(usuario, password);
    }

    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {
        return usuarioService.buscarPorId(idUsuario);
    }

    public boolean actualizar(int idUsuario,
                              String nombre,
                              String apellido,
                              String nombreUsuario,
                              String nuevaPassword,
                              String rol,
                              boolean activo) {

        Usuario usuario = new Usuario();

        usuario.setIdUsuario(idUsuario);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setUsuario(nombreUsuario);
        usuario.setRol(rol);
        usuario.setActivo(activo);

        return usuarioService.actualizar(usuario, nuevaPassword);
    }

    public boolean eliminar(int idUsuario) {
        return usuarioService.eliminar(idUsuario);
    }
}
