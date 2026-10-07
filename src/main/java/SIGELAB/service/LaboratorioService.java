package SIGELAB.service;

import SIGELAB.enums.EstadoLaboratorio;
import SIGELAB.model.Laboratorio;
import SIGELAB.repository.LaboratorioRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LaboratorioService {

    private final LaboratorioRepository repository;

    public LaboratorioService() {
        this.repository = new LaboratorioRepository();
    }

    public List<Laboratorio> listar() throws SQLException {
        return repository.listar();
    }

    public List<Laboratorio> buscar(String texto) throws SQLException {

        if (texto == null || texto.trim().isEmpty()) {
            return repository.listar();
        }

        return repository.buscar(texto.trim());
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio)
            throws SQLException {

        if (idLaboratorio <= 0) {
            throw new IllegalArgumentException(
                    "El ID del laboratorio no es válido."
            );
        }

        return repository.buscarPorId(idLaboratorio);
    }

    public Laboratorio guardar(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) throws SQLException {

        validarDatos(
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estado
        );

        String codigoLimpio =
                codigo.trim().toUpperCase();

        if (repository.buscarPorCodigo(codigoLimpio).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un laboratorio con ese código."
            );
        }

        Laboratorio laboratorio = new Laboratorio();

        laboratorio.setCodigo(codigoLimpio);
        laboratorio.setNombre(nombre.trim());
        laboratorio.setUbicacion(ubicacion.trim());
        laboratorio.setCapacidad(capacidad);
        laboratorio.setEstado(estado);

        return repository.guardar(laboratorio);
    }

    public boolean actualizar(
            int idLaboratorio,
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) throws SQLException {

        if (idLaboratorio <= 0) {
            throw new IllegalArgumentException(
                    "El ID del laboratorio no es válido."
            );
        }

        validarDatos(
                codigo,
                nombre,
                ubicacion,
                capacidad,
                estado
        );

        Optional<Laboratorio> existente =
                repository.buscarPorId(idLaboratorio);

        if (existente.isEmpty()) {
            throw new IllegalArgumentException(
                    "El laboratorio no existe."
            );
        }

        String codigoLimpio =
                codigo.trim().toUpperCase();

        Optional<Laboratorio> mismoCodigo =
                repository.buscarPorCodigo(codigoLimpio);

        if (mismoCodigo.isPresent()
                && mismoCodigo.get().getIdLaboratorio()
                != idLaboratorio) {

            throw new IllegalArgumentException(
                    "Ya existe otro laboratorio con ese código."
            );
        }

        Laboratorio laboratorio = existente.get();

        laboratorio.setCodigo(codigoLimpio);
        laboratorio.setNombre(nombre.trim());
        laboratorio.setUbicacion(ubicacion.trim());
        laboratorio.setCapacidad(capacidad);
        laboratorio.setEstado(estado);

        return repository.actualizar(laboratorio);
    }

    public boolean desactivar(int idLaboratorio)
            throws SQLException {

        validarId(idLaboratorio);

        if (repository.buscarPorId(idLaboratorio).isEmpty()) {
            throw new IllegalArgumentException(
                    "El laboratorio no existe."
            );
        }

        return repository.desactivar(idLaboratorio);
    }

    public boolean activar(int idLaboratorio)
            throws SQLException {

        validarId(idLaboratorio);

        if (repository.buscarPorId(idLaboratorio).isEmpty()) {
            throw new IllegalArgumentException(
                    "El laboratorio no existe."
            );
        }

        return repository.activar(idLaboratorio);
    }

    private void validarId(int idLaboratorio) {

        if (idLaboratorio <= 0) {
            throw new IllegalArgumentException(
                    "El ID del laboratorio no es válido."
            );
        }
    }

    private void validarDatos(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {

        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El código es obligatorio."
            );
        }

        if (codigo.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "El código no puede superar los 20 caracteres."
            );
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio."
            );
        }

        if (nombre.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 100 caracteres."
            );
        }

        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La ubicación es obligatoria."
            );
        }

        if (ubicacion.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "La ubicación no puede superar los 200 caracteres."
            );
        }

        if (capacidad <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero."
            );
        }

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio."
            );
        }
    }
}