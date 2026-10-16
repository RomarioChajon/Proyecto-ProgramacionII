
package umg.sigelab.service;

import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.model.Equipo;
import umg.sigelab.repository.EquipoRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EquipoService {

    private final EquipoRepository repository;

    public EquipoService() {
        this.repository = new EquipoRepository();
    }

    public EquipoService(EquipoRepository repository) {
        this.repository = repository;
    }

    // LISTAR TODOS LOS EQUIPOS
    public List<Equipo> listar() throws SQLException {
        return repository.listar();
    }

    // BUSCAR EQUIPO POR ID
    public Optional<Equipo> buscarPorId(int idEquipo)
            throws SQLException {

        if (idEquipo <= 0) {
            throw new IllegalArgumentException(
                    "El ID del equipo debe ser mayor que cero."
            );
        }

        return repository.buscarPorId(idEquipo);
    }

    // BUSCAR POR CÓDIGO DE INVENTARIO
    public Optional<Equipo> buscarPorCodigo(String codigo)
            throws SQLException {

        validarTexto(codigo, "Código de inventario", 30);

        return repository.buscarPorCodigo(codigo.trim());
    }

    // BUSCAR EQUIPOS POR TEXTO
    public List<Equipo> buscar(String texto)
            throws SQLException {

        if (texto == null || texto.isBlank()) {
            return repository.listar();
        }

        return repository.buscar(texto.trim());
    }

    // REGISTRAR NUEVO EQUIPO
    public Equipo guardar(Equipo equipo)
            throws SQLException {

        validarEquipo(equipo);

        // Código vacío = lo genera el repositorio; escrito = se usa el del usuario.
        equipo.setCodigoInventario(validarCodigoOpcional(equipo.getCodigoInventario(), null));

        // Todo equipo nuevo comienza DISPONIBLE.
        equipo.setEstado(EstadoEquipo.DISPONIBLE);

        return repository.guardar(equipo);
    }

    // ACTUALIZAR DATOS GENERALES
    public boolean actualizar(Equipo equipo)
            throws SQLException {

        validarEquipo(equipo);

        if (equipo.getIdEquipo() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un equipo válido."
            );
        }

        Optional<Equipo> existente =
                repository.buscarPorId(equipo.getIdEquipo());

        if (existente.isEmpty()) {
            throw new IllegalArgumentException(
                    "El equipo no existe."
            );
        }

        Equipo actual = existente.get();

        // El código de inventario ahora es editable (si queda vacío se conserva el actual).
        String codigoNuevo = validarCodigoOpcional(
                equipo.getCodigoInventario(), equipo.getIdEquipo());
        equipo.setCodigoInventario(
                codigoNuevo != null ? codigoNuevo : actual.getCodigoInventario()
        );

        // Conservar el estado real del equipo.
        equipo.setEstado(
                actual.getEstado()
        );

        // Solo se pueden actualizar equipos disponibles.
        if (actual.getEstado() != EstadoEquipo.DISPONIBLE) {

            throw new IllegalArgumentException(
                    "Solo se pueden actualizar equipos DISPONIBLES."
            );
        }

        // Conservar el laboratorio original.
        if (actual.getIdLaboratorio()
                != equipo.getIdLaboratorio()) {

            throw new IllegalArgumentException(
                    "No se puede cambiar el laboratorio "
                            + "asignado al equipo."
            );
        }

        return repository.actualizar(equipo);
    }

    // LISTAR EQUIPOS DE UN LABORATORIO
    public List<Equipo> listarPorLaboratorio(int idLaboratorio)
            throws SQLException {

        if (idLaboratorio <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un laboratorio válido."
            );
        }

        return repository.listarPorLaboratorio(idLaboratorio);
    }

    // VALIDAR CÓDIGO ESCRITO POR EL USUARIO (null si viene vacío)
    private String validarCodigoOpcional(String codigo, Integer idActual)
            throws SQLException {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        String limpio = codigo.trim().toUpperCase(java.util.Locale.ROOT);

        if (limpio.length() > 30) {
            throw new IllegalArgumentException(
                    "El código de inventario no puede superar los 30 caracteres.");
        }

        if (!limpio.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El código solo puede tener letras, números, guion y guion bajo.");
        }

        Optional<Equipo> otro = repository.buscarPorCodigo(limpio);
        if (otro.isPresent() && (idActual == null || otro.get().getIdEquipo() != idActual)) {
            throw new IllegalArgumentException(
                    "Ya existe un equipo con el código " + limpio + ".");
        }

        return limpio;
    }

    // VALIDACIONES GENERALES
    private void validarEquipo(Equipo equipo) {

        if (equipo == null) {

            throw new IllegalArgumentException(
                    "Los datos del equipo son obligatorios."
            );
        }

        // El código de inventario NO se valida aquí
        // porque se genera automáticamente al guardar.

        validarTexto(
                equipo.getNumeroSerie(),
                "Número de serie",
                60
        );

        if (equipo.getIdProducto() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un producto válido."
            );
        }

        if (equipo.getIdLaboratorio() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un laboratorio válido."
            );
        }

        if (equipo.getObservaciones() != null
                && equipo.getObservaciones().length() > 300) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar "
                            + "300 caracteres."
            );
        }

        equipo.setNumeroSerie(
                equipo.getNumeroSerie().trim()
        );
    }

    // VALIDAR TEXTO OBLIGATORIO
    private void validarTexto(
            String valor,
            String campo,
            int maximo) {

        if (valor == null || valor.isBlank()) {

            throw new IllegalArgumentException(
                    campo + " es obligatorio."
            );
        }

        if (valor.length() > maximo) {

            throw new IllegalArgumentException(
                    campo + " no puede superar "
                            + maximo + " caracteres."
            );
        }
    }
}


