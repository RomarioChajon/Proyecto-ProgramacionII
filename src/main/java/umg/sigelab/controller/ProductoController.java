package umg.sigelab.controller;

import umg.sigelab.enums.EstadoProducto;
import umg.sigelab.model.Producto;
import umg.sigelab.service.ProductoService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductoController {

    private final ProductoService service;

    public ProductoController() {
        this.service = new ProductoService();
    }

    public List<Producto> listar() throws SQLException {
        return service.listar();
    }

    public List<Producto> buscar(String texto) throws SQLException {
        return service.buscar(texto);
    }

    public Optional<Producto> buscarPorId(int idProducto)
            throws SQLException {

        return service.buscarPorId(idProducto);
    }

    public Producto guardar(String codigoProducto,
                            String nombre,
                            int idTipoEquipo,
                            String marca,
                            String modelo,
                            String descripcion,
                            String estado)
            throws SQLException {

        EstadoProducto estadoProducto =
                convertirEstado(estado);

        return service.guardar(
                codigoProducto,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                descripcion,
                estadoProducto
        );
    }

    public boolean actualizar(int idProducto,
                              String codigoProducto,
                              String nombre,
                              int idTipoEquipo,
                              String marca,
                              String modelo,
                              String descripcion,
                              String estado)
            throws SQLException {

        EstadoProducto estadoProducto =
                convertirEstado(estado);

        return service.actualizar(
                idProducto,
                codigoProducto,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                descripcion,
                estadoProducto
        );
    }

    public boolean desactivar(int idProducto)
            throws SQLException {

        return service.desactivar(idProducto);
    }

    public boolean activar(int idProducto)
            throws SQLException {

        return service.activar(idProducto);
    }

    private EstadoProducto convertirEstado(String estado) {

        if (estado == null || estado.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un estado."
            );
        }

        try {

            return EstadoProducto.valueOf(
                    estado.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "El estado seleccionado no es válido."
            );
        }
    }
}