
package umg.sigelab.service;

import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.repository.LaboratorioAdminRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LaboratorioAdminService {

    private final LaboratorioAdminRepository repository;

    public LaboratorioAdminService() {
        this.repository = new LaboratorioAdminRepository();
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

        validarId(idLaboratorio);

        return repository.buscarPorId(idLaboratorio);
    }

    public Laboratorio guardar(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoRegistro estado
    ) throws SQLException {

        validarDatos(nombre, ubicacion, capacidad, estado);

        Laboratorio laboratorio = new Laboratorio();

        // Código vacío = se genera automáticamente en el repositorio.
        // Código escrito = se usa el del usuario (si es válido y no existe).
        String codigoLimpio = validarCodigoOpcional(codigo, null);
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
            EstadoRegistro estado
    ) throws SQLException {

        validarId(idLaboratorio);
        validarDatos(nombre, ubicacion, capacidad, estado);

        Optional<Laboratorio> existente =
                repository.buscarPorId(idLaboratorio);

        if (existente.isEmpty()) {
            throw new IllegalArgumentException(
                    "El laboratorio no existe."
            );
        }

        Laboratorio laboratorio = existente.get();

        // El código ahora es editable: se valida formato y que no lo use otro laboratorio.
        String codigoNuevo = validarCodigoOpcional(codigo, idLaboratorio);
        if (codigoNuevo != null) {
            laboratorio.setCodigo(codigoNuevo);
        }
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

    /**
     * Valida el código escrito por el usuario. Devuelve null si está vacío (se generará solo).
     *
     * @param idActual id del laboratorio que se edita (null al crear), para permitir conservar su propio código
     */
    private String validarCodigoOpcional(String codigo, Integer idActual) throws SQLException {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        String limpio = codigo.trim().toUpperCase(java.util.Locale.ROOT);

        if (limpio.length() > 20) {
            throw new IllegalArgumentException(
                    "El código no puede superar los 20 caracteres.");
        }

        if (!limpio.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El código solo puede tener letras, números, guion y guion bajo.");
        }

        Optional<Laboratorio> otro = repository.buscarPorCodigo(limpio);
        if (otro.isPresent() && (idActual == null || otro.get().getIdLaboratorio() != idActual)) {
            throw new IllegalArgumentException(
                    "Ya existe un laboratorio con el código " + limpio + ".");
        }

        return limpio;
    }

    private void validarId(int idLaboratorio) {

        if (idLaboratorio <= 0) {
            throw new IllegalArgumentException(
                    "El ID del laboratorio no es válido."
            );
        }
    }

    private void validarDatos(
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoRegistro estado
    ) {

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
