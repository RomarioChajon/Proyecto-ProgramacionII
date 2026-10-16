package umg.sigelab.controller;

import umg.sigelab.enums.EstadoTipoEquipo;
import umg.sigelab.model.TipoEquipo;
import umg.sigelab.service.TipoEquipoService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TipoEquipoController {

    private final TipoEquipoService service;

    public TipoEquipoController() {
        this.service = new TipoEquipoService();
    }

    public List<TipoEquipo> listar() throws SQLException {
        return service.listar();
    }

    public List<TipoEquipo> buscar(String texto) throws SQLException {
        return service.buscar(texto);
    }

    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo)
            throws SQLException {

        return service.buscarPorId(idTipoEquipo);
    }

    public TipoEquipo guardar(
            String codigo,
            String nombre,
            String descripcion,
            String estado)
            throws SQLException {

        EstadoTipoEquipo estadoTipoEquipo =
                convertirEstado(estado);

        return service.guardar(
                codigo,
                nombre,
                descripcion,
                estadoTipoEquipo
        );
    }

    public boolean actualizar(
            int idTipoEquipo,
            String codigo,
            String nombre,
            String descripcion,
            String estado)
            throws SQLException {

        EstadoTipoEquipo estadoTipoEquipo =
                convertirEstado(estado);

        return service.actualizar(
                idTipoEquipo,
                codigo,
                nombre,
                descripcion,
                estadoTipoEquipo
        );
    }

    public boolean desactivar(int idTipoEquipo)
            throws SQLException {

        return service.desactivar(idTipoEquipo);
    }

    public boolean activar(int idTipoEquipo)
            throws SQLException {

        return service.activar(idTipoEquipo);
    }

    private EstadoTipoEquipo convertirEstado(String estado) {

        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un estado."
            );
        }

        try {

            return EstadoTipoEquipo.valueOf(
                    estado.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido."
            );
        }
    }
}
