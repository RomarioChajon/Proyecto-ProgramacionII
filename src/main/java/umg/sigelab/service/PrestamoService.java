
package umg.sigelab.service;

import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;

import java.util.List;

public class PrestamoService {

    private final PrestamoRepository prestamoRepository;

    public PrestamoService() {
        this.prestamoRepository = new PrestamoRepository();
    }

    public PrestamoService(PrestamoRepository prestamoRepository) {
        if (prestamoRepository == null) {
            throw new IllegalArgumentException(
                    "El repositorio de préstamos es obligatorio.");
        }

        this.prestamoRepository = prestamoRepository;
    }

    /**
     * Consulta equipos disponibles de una sede.
     *
     * @param idLaboratorio sede seleccionada.
     * @param idTipoEquipo categoría; null para todas.
     */
    public List<PrestamoRepository.EquipoDisponible>
    listarEquiposDisponibles(
            int idLaboratorio,
            Integer idTipoEquipo) {

        if (idLaboratorio <= 0) {
            throw new BusinessException(
                    "Debe seleccionar una sede válida.");
        }

        if (idTipoEquipo != null && idTipoEquipo <= 0) {
            throw new BusinessException(
                    "La categoría seleccionada no es válida.");
        }

        return prestamoRepository.listarEquiposDisponibles(
                idLaboratorio,
                idTipoEquipo
        );
    }

    /**
     * Registra una solicitud pendiente de revisión.
     *
     * Los días del préstamo serán establecidos
     * posteriormente por el encargado.
     *
     * @return ID del préstamo registrado.
     */
    public int registrarSolicitud(
            Usuario solicitante,
            int idLaboratorio,
            List<Integer> idsEquipos,
            String observaciones) {

        validarSolicitante(solicitante);

        if (idLaboratorio <= 0) {
            throw new BusinessException(
                    "Debe seleccionar una sede válida.");
        }

        if (idsEquipos == null || idsEquipos.isEmpty()) {
            throw new BusinessException(
                    "Debe seleccionar al menos un equipo.");
        }

        if (idsEquipos.stream().anyMatch(
                id -> id == null || id <= 0)) {
            throw new BusinessException(
                    "La selección contiene equipos inválidos.");
        }

        if (idsEquipos.stream().distinct().count()
                != idsEquipos.size()) {
            throw new BusinessException(
                    "No puede seleccionar el mismo equipo dos veces.");
        }

        String textoObservaciones =
                observaciones == null
                        ? null
                        : observaciones.trim();

        if (textoObservaciones != null
                && textoObservaciones.length() > 600) {
            throw new BusinessException(
                    "Las observaciones no pueden superar 600 caracteres.");
        }

        return prestamoRepository.registrarSolicitud(
                solicitante.getIdUsuario(),
                idLaboratorio,
                idsEquipos,
                textoObservaciones
        );
    }

    /**
     * Solo ESTUDIANTE y DOCENTE pueden realizar
     * solicitudes mediante este servicio.
     */
    private void validarSolicitante(Usuario solicitante) {

        if (solicitante == null) {
            throw new BusinessException(
                    "Debe iniciar sesión para solicitar un préstamo.");
        }

        if (solicitante.getIdUsuario() <= 0) {
            throw new BusinessException(
                    "El usuario solicitante no es válido.");
        }

        if (!solicitante.isActivo()) {
            throw new BusinessException(
                    "El usuario solicitante está inactivo.");
        }

        RolUsuario rol = solicitante.getRol();

        if (rol != RolUsuario.ESTUDIANTE
                && rol != RolUsuario.DOCENTE) {
            throw new BusinessException(
                    "Solo estudiantes y docentes pueden "
                            + "solicitar préstamos.");
        }
    }
}
