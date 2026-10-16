package umg.sigelab.service;

import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.BusinessException;

/**
 * Validación de permisos en la LÓGICA de negocio (RN-20): ocultar un botón no es suficiente,
 * cada operación vuelve a comprobar quién la pide.
 */
final class Permisos {

    private Permisos() {
        // Clase de utilidades: no se instancia.
    }

    /** Solo DOCENTE o ESTUDIANTE pueden solicitar préstamos. */
    static void exigirSolicitante(Usuario usuario) {
        if (usuario == null || (usuario.getRol() != RolUsuario.DOCENTE && usuario.getRol() != RolUsuario.ESTUDIANTE)) {
            throw new BusinessException("Solo los docentes y estudiantes pueden solicitar préstamos.");
        }
    }

    /**
     * Opera sobre una sede: el ADMIN en cualquiera y el ENCARGADO solo en la suya.
     */
    static void exigirOperador(Usuario usuario, int idLaboratorio) {
        if (usuario == null) {
            throw new BusinessException("Debe iniciar sesión.");
        }
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (usuario.getRol() == RolUsuario.ENCARGADO && usuario.getLaboratorio() != null
                && usuario.getLaboratorio().getIdLaboratorio() == idLaboratorio) {
            return;
        }
        throw new BusinessException("No tiene permiso para operar en este laboratorio.");
    }

    /** Solo el administrador. */
    static void exigirAdmin(Usuario usuario) {
        if (usuario == null || usuario.getRol() != RolUsuario.ADMIN) {
            throw new BusinessException("Esta acción es solo para el administrador.");
        }
    }
}
