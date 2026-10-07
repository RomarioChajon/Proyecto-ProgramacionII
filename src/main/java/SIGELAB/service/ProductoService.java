package SIGELAB.service;

import SIGELAB.enums.EstadoProducto;
import SIGELAB.model.Producto;
import SIGELAB.repository.ProductoRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService() {
        this.repository = new ProductoRepository();
    }

    public List<Producto> listar() throws SQLException {
        return repository.listar();
    }

    public List<Producto> buscar(String texto) throws SQLException {

        if (texto == null || texto.trim().isEmpty()) {
            return repository.listar();
        }

        return repository.buscar(texto.trim());
    }

    public Optional<Producto> buscarPorId(int idProducto)
            throws SQLException {

        validarId(idProducto);

        return repository.buscarPorId(idProducto);
    }

    public Producto guardar(String codigoProducto,
                            String nombre,
                            int idTipoEquipo,
                            String marca,
                            String modelo,
                            String descripcion,
                            EstadoProducto estado)
            throws SQLException {

        validarDatos(
                codigoProducto,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                descripcion,
                estado
        );

        String codigoLimpio = codigoProducto.trim();

        if (repository.buscarPorCodigo(codigoLimpio).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un producto con ese código."
            );
        }

        Producto producto = new Producto();

        producto.setCodigoProducto(codigoLimpio);
        producto.setNombre(nombre.trim());
        producto.setIdTipoEquipo(idTipoEquipo);

        producto.setMarca(
                marca == null ? null : marca.trim()
        );

        producto.setModelo(
                modelo == null ? null : modelo.trim()
        );

        producto.setDescripcion(
                descripcion == null ? null : descripcion.trim()
        );

        producto.setEstado(estado);

        return repository.guardar(producto);
    }

    public boolean actualizar(int idProducto,
                              String codigoProducto,
                              String nombre,
                              int idTipoEquipo,
                              String marca,
                              String modelo,
                              String descripcion,
                              EstadoProducto estado)
            throws SQLException {

        validarId(idProducto);

        validarDatos(
                codigoProducto,
                nombre,
                idTipoEquipo,
                marca,
                modelo,
                descripcion,
                estado
        );

        Optional<Producto> existente =
                repository.buscarPorId(idProducto);

        if (existente.isEmpty()) {
            throw new IllegalArgumentException(
                    "El producto no existe."
            );
        }

        String codigoLimpio = codigoProducto.trim();

        Optional<Producto> mismoCodigo =
                repository.buscarPorCodigo(codigoLimpio);

        if (mismoCodigo.isPresent()
                && mismoCodigo.get().getIdProducto() != idProducto) {

            throw new IllegalArgumentException(
                    "Ya existe otro producto con ese código."
            );
        }

        Producto producto = existente.get();

        producto.setCodigoProducto(codigoLimpio);
        producto.setNombre(nombre.trim());
        producto.setIdTipoEquipo(idTipoEquipo);

        producto.setMarca(
                marca == null ? null : marca.trim()
        );

        producto.setModelo(
                modelo == null ? null : modelo.trim()
        );

        producto.setDescripcion(
                descripcion == null ? null : descripcion.trim()
        );

        producto.setEstado(estado);

        return repository.actualizar(producto);
    }

    public boolean desactivar(int idProducto)
            throws SQLException {

        validarId(idProducto);

        if (repository.buscarPorId(idProducto).isEmpty()) {
            throw new IllegalArgumentException(
                    "El producto no existe."
            );
        }

        return repository.desactivar(idProducto);
    }

    public boolean activar(int idProducto)
            throws SQLException {

        validarId(idProducto);

        if (repository.buscarPorId(idProducto).isEmpty()) {
            throw new IllegalArgumentException(
                    "El producto no existe."
            );
        }

        return repository.activar(idProducto);
    }

    private void validarId(int idProducto) {

        if (idProducto <= 0) {
            throw new IllegalArgumentException(
                    "El ID del producto no es válido."
            );
        }
    }

    private void validarDatos(String codigoProducto,
                              String nombre,
                              int idTipoEquipo,
                              String marca,
                              String modelo,
                              String descripcion,
                              EstadoProducto estado) {

        if (codigoProducto == null
                || codigoProducto.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "El código del producto es obligatorio."
            );
        }

        if (nombre == null
                || nombre.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio."
            );
        }

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un tipo de equipo."
            );
        }

        if (marca != null && marca.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La marca no puede superar los 100 caracteres."
            );
        }

        if (modelo != null && modelo.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El modelo no puede superar los 100 caracteres."
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
