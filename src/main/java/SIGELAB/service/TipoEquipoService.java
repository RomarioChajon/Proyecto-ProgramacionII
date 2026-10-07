package SIGELAB.service;

import SIGELAB.enums.EstadoTipoEquipo;
import SIGELAB.model.TipoEquipo;
import SIGELAB.repository.TipoEquipoRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TipoEquipoService {

    private final TipoEquipoRepository repository;

    public TipoEquipoService() {
        this.repository = new TipoEquipoRepository();
    }

    public List<TipoEquipo> listar() throws SQLException {
        return repository.listar();
    }

    public List<TipoEquipo> buscar(String texto) throws SQLException {

        if (texto == null || texto.trim().isEmpty()) {
            return repository.listar();
        }

        return repository.buscar(texto.trim());
    }

    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo)
            throws SQLException {

        validarId(idTipoEquipo);

        return repository.buscarPorId(idTipoEquipo);
    }

    public TipoEquipo guardar(
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado)
            throws SQLException {

        validarDatos(nombre, descripcion, estado);

        String nombreLimpio = nombre.trim();

        if (repository.buscarPorNombre(nombreLimpio).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un tipo de equipo con ese nombre."
            );
        }

        TipoEquipo tipoEquipo = new TipoEquipo();

        tipoEquipo.setNombre(nombreLimpio);

        tipoEquipo.setDescripcion(
                descripcion == null
                        ? null
                        : descripcion.trim()
        );

        tipoEquipo.setEstado(estado);

        return repository.guardar(tipoEquipo);
    }

    public boolean actualizar(
            int idTipoEquipo,
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado)
            throws SQLException {

        validarId(idTipoEquipo);
        validarDatos(nombre, descripcion, estado);

        Optional<TipoEquipo> existente =
                repository.buscarPorId(idTipoEquipo);

        if (existente.isEmpty()) {
            throw new IllegalArgumentException(
                    "El tipo de equipo no existe."
            );
        }

        String nombreLimpio = nombre.trim();

        Optional<TipoEquipo> mismoNombre =
                repository.buscarPorNombre(nombreLimpio);

        if (mismoNombre.isPresent()
                && mismoNombre.get().getIdTipoEquipo()
                != idTipoEquipo) {

            throw new IllegalArgumentException(
                    "Ya existe otro tipo de equipo con ese nombre."
            );
        }

        TipoEquipo tipoEquipo = existente.get();

        tipoEquipo.setNombre(nombreLimpio);

        tipoEquipo.setDescripcion(
                descripcion == null
                        ? null
                        : descripcion.trim()
        );

        tipoEquipo.setEstado(estado);

        return repository.actualizar(tipoEquipo);
    }

    public boolean desactivar(int idTipoEquipo)
            throws SQLException {

        validarId(idTipoEquipo);

        if (repository.buscarPorId(idTipoEquipo).isEmpty()) {
            throw new IllegalArgumentException(
                    "El tipo de equipo no existe."
            );
        }

        return repository.desactivar(idTipoEquipo);
    }

    public boolean activar(int idTipoEquipo)
            throws SQLException {

        validarId(idTipoEquipo);

        if (repository.buscarPorId(idTipoEquipo).isEmpty()) {
            throw new IllegalArgumentException(
                    "El tipo de equipo no existe."
            );
        }

        return repository.activar(idTipoEquipo);
    }

    private void validarId(int idTipoEquipo) {

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "El ID del tipo de equipo no es válido."
            );
        }
    }

    private void validarDatos(
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado) {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (nombre.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 50 caracteres."
            );
        }

        if (descripcion != null
                && descripcion.trim().length() > 200) {

            throw new IllegalArgumentException(
                    "La descripción no puede superar los 200 caracteres."
            );
        }

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio."
            );
        }
    }
}