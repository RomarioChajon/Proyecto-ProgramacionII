package SIGELAB.controller;

import SIGELAB.model.Equipo;
import SIGELAB.service.EquipoService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EquipoController {

    private final EquipoService service;

    public EquipoController() {
        this.service = new EquipoService();
    }

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    // LISTAR TODOS LOS EQUIPOS
    public List<Equipo> listar() throws SQLException {
        return service.listar();
    }

    // BUSCAR EQUIPO POR ID
    public Optional<Equipo> buscarPorId(int idEquipo)
            throws SQLException {

        return service.buscarPorId(idEquipo);
    }

    // BUSCAR POR CÓDIGO DE INVENTARIO
    public Optional<Equipo> buscarPorCodigo(String codigo)
            throws SQLException {

        return service.buscarPorCodigo(codigo);
    }

    // BUSCAR EQUIPOS POR TEXTO
    public List<Equipo> buscar(String texto)
            throws SQLException {

        return service.buscar(texto);
    }

    // REGISTRAR NUEVO EQUIPO
    public Equipo guardar(Equipo equipo)
            throws SQLException {

        return service.guardar(equipo);
    }

    // ACTUALIZAR DATOS GENERALES
    public boolean actualizar(Equipo equipo)
            throws SQLException {

        return service.actualizar(equipo);
    }

    // LISTAR EQUIPOS POR LABORATORIO
    public List<Equipo> listarPorLaboratorio(int idLaboratorio)
            throws SQLException {

        return service.listarPorLaboratorio(idLaboratorio);
    }
}
