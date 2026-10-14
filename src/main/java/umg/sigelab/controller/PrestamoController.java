
package umg.sigelab.controller;

import umg.sigelab.model.Usuario;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.service.PrestamoService;

import java.util.List;

public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController() {
        this.prestamoService = new PrestamoService();
    }

    public PrestamoController(PrestamoService prestamoService) {
        if (prestamoService == null) {
            throw new IllegalArgumentException(
                    "El servicio de préstamos es obligatorio.");
        }

        this.prestamoService = prestamoService;
    }

    /**
     * Consulta los equipos disponibles de una sede.
     *
     * @param idLaboratorio ID de la sede.
     * @param idTipoEquipo ID de la categoría.
     *                     Puede ser null para todas.
     */
    public List<PrestamoRepository.EquipoDisponible>
    listarEquiposDisponibles(
            int idLaboratorio,
            Integer idTipoEquipo) {

        return prestamoService.listarEquiposDisponibles(
                idLaboratorio,
                idTipoEquipo
        );
    }

    /**
     * Registra una solicitud de préstamo.
     *
     * La solicitud se guarda inicialmente como PENDIENTE.
     * Los días serán asignados posteriormente por el encargado.
     *
     * @return ID del préstamo generado en SQL Server.
     */
    public int registrarSolicitud(
            Usuario solicitante,
            int idLaboratorio,
            List<Integer> idsEquipos,
            String observaciones) {

        return prestamoService.registrarSolicitud(
                solicitante,
                idLaboratorio,
                idsEquipos,
                observaciones
        );
    }
}
