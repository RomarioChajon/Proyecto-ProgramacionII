

package SIGELAB.service;

import SIGELAB.enums.EstadoEquipo;
import SIGELAB.model.Equipo;
import SIGELAB.repository.EquipoRepository;

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

    // REGISTRAR EQUIPO
    public Equipo guardar(Equipo equipo) throws SQLException {

        validarEquipo(equipo);

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

        // No permitir cambiar el laboratorio de un equipo
        // que no esté disponible.
        if (actual.getIdLaboratorio() != equipo.getIdLaboratorio()
                && actual.getEstado() != EstadoEquipo.DISPONIBLE) {

            throw new IllegalArgumentException(
                    "No puede cambiar de laboratorio un equipo "
                            + "que no está DISPONIBLE."
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

    // VALIDACIONES GENERALES
    private void validarEquipo(Equipo equipo) {

        if (equipo == null) {
            throw new IllegalArgumentException(
                    "Los datos del equipo son obligatorios."
            );
        }

        validarTexto(
                equipo.getCodigoInventario(),
                "Código de inventario",
                30
        );

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

        equipo.setCodigoInventario(
                equipo.getCodigoInventario().trim()
        );

        equipo.setNumeroSerie(
                equipo.getNumeroSerie().trim()
        );
    }

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

