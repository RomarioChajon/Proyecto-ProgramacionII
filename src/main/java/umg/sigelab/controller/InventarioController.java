package umg.sigelab.controller;

import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.ResultadoMantenimiento;
import umg.sigelab.enums.TipoMantenimiento;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.InventarioService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Coordina las pantallas de inventario por sede y mantenimiento con {@link InventarioService}. */
public class InventarioController {

    private final InventarioService service = new InventarioService();

    public List<Laboratorio> laboratorios() {
        return service.laboratorios();
    }

    public List<CategoriaStock> categorias(Usuario operador, int idLaboratorio) {
        return service.categorias(operador, idLaboratorio);
    }

    public List<FilaInventario> listar(Usuario operador, int idLaboratorio, Integer idTipoEquipo, EstadoEquipo estado) {
        return service.listar(operador, idLaboratorio, idTipoEquipo, estado);
    }

    public Set<EstadoEquipo> transicionesDesde(EstadoEquipo estado) {
        return InventarioService.transicionesDesde(estado);
    }

    public String ficha(Usuario operador, int idLaboratorio, String codigo) {
        return service.ficha(operador, idLaboratorio, codigo);
    }

    public Optional<FilaInventario> buscarPorCodigo(Usuario operador, String codigo) {
        return service.buscarPorCodigo(operador, codigo);
    }

    public void cambiarEstado(Usuario operador, int idEquipo, EstadoEquipo nuevo, TipoMantenimiento tipo,
                              String descripcion, String responsable) {
        service.cambiarEstado(operador, idEquipo, nuevo, tipo, descripcion, responsable);
    }

    public void finalizarMantenimiento(Usuario operador, int idEquipo, ResultadoMantenimiento resultado,
                                       String observaciones) {
        service.finalizarMantenimiento(operador, idEquipo, resultado, observaciones);
    }
}
