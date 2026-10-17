package umg.sigelab.controller;

import umg.sigelab.model.Usuario;
import umg.sigelab.service.AuthService;

/** Controlador delgado: solo delega en {@link AuthService}; las reglas viven en el Service. */
public class AuthController {

    private final AuthService service;

    public AuthController() {
        this(new AuthService());
    }

    public AuthController(AuthService service) {
        this.service = service;
    }

    public Usuario autenticar(String usuario, String password) {
        return service.autenticar(usuario, password);
    }

    public void registrarIngresoExitoso(Usuario usuario) {
        service.registrarIngresoExitoso(usuario);
    }

    public void registrarIngresoFallidoFacial(Usuario usuario, String motivo) {
        service.registrarIngresoFallidoFacial(usuario, motivo);
    }

    public Usuario registrarEstudiante(Usuario datos, String password) {
        return service.registrarEstudiante(datos, password);
    }

    public void deshacerRegistro(int idUsuario) {
        service.deshacerRegistro(idUsuario);
    }

    public Usuario verificarDatosParaReinicio(String usuario, String correo) {
        return service.verificarDatosParaReinicio(usuario, correo);
    }

    public void reiniciarPassword(int idUsuario, String passwordNueva) {
        service.reiniciarPassword(idUsuario, passwordNueva);
    }
}
