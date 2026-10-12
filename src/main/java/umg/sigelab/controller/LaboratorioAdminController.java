
package umg.sigelab.controller;

import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.service.LaboratorioAdminService;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LaboratorioAdminController {

    private final LaboratorioAdminService service;

    public LaboratorioAdminController() {
        this.service = new LaboratorioAdminService();
    }

    public List<Laboratorio> listar() throws SQLException {
        return service.listar();
    }

    public List<Laboratorio> buscar(String texto)
            throws SQLException {

        return service.buscar(texto);
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio)
            throws SQLException {

        return service.buscarPorId(idLaboratorio);
    }

    public Laboratorio guardar(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            String estado
    ) throws SQLException {

        EstadoRegistro estadoLaboratorio =
                convertirEstado(estado);

        // El código se genera en el repositorio.
        // Conservamos este parámetro para que el
        // formulario actual siga siendo compatible.
        return service.guardar(
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estadoLaboratorio
        );
    }

    public boolean actualizar(
            int idLaboratorio,
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            String estado
    ) throws SQLException {

        EstadoRegistro estadoLaboratorio =
                convertirEstado(estado);

        // El servicio conserva el código original.
        return service.actualizar(
                idLaboratorio,
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estadoLaboratorio
        );
    }

    public boolean desactivar(int idLaboratorio)
            throws SQLException {

        return service.desactivar(idLaboratorio);
    }

    public boolean activar(int idLaboratorio)
            throws SQLException {

        return service.activar(idLaboratorio);
    }

    private EstadoRegistro convertirEstado(String estado) {

        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un estado."
            );
        }

        try {
            return EstadoRegistro.valueOf(
                    estado.trim().toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido.",
                    e
            );
        }
    }
}
