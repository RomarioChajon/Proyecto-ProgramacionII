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

    public Producto guardar(
            String codigoProducto,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String descripcion,
            EstadoProducto estado
    ) throws SQLException {

        validarDatos(nombre, idTipoEquipo, marca,
                modelo, descripcion, estado);

        Producto producto = new Producto();

        producto.setNombre(nombre.trim());
        producto.setIdTipoEquipo(idTipoEquipo);
        producto.setMarca(limpiar(marca));
        producto.setModelo(limpiar(modelo));
        producto.setDescripcion(limpiar(descripcion));
        producto.setEstado(estado);

        // El repositorio genera el codigo automaticamente.
        return repository.guardar(producto);
    }

    public boolean actualizar(
            int idProducto,
            String codigoProducto,
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String descripcion,
            EstadoProducto estado
    ) throws SQLException {

        validarId(idProducto);

        validarDatos(nombre, idTipoEquipo, marca,
                modelo, descripcion, estado);

        Producto producto = repository.buscarPorId(idProducto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe."
                ));

        if (producto.getIdTipoEquipo() != idTipoEquipo) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el tipo de equipo " +
                            "porque el codigo del producto es permanente."
            );
        }

        producto.setNombre(nombre.trim());
        producto.setMarca(limpiar(marca));
        producto.setModelo(limpiar(modelo));
        producto.setDescripcion(limpiar(descripcion));
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
                    "El ID del producto no es valido."
            );
        }
    }

    private void validarDatos(
            String nombre,
            int idTipoEquipo,
            String marca,
            String modelo,
            String descripcion,
            EstadoProducto estado
    ) {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio."
            );
        }

        if (idTipoEquipo <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un tipo de equipo."
            );
        }

        if (nombre.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar 150 caracteres."
            );
        }

        if (marca != null && marca.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La marca no puede superar 100 caracteres."
            );
        }

        if (modelo != null && modelo.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El modelo no puede superar 100 caracteres."
            );
        }

        if (descripcion != null &&
                descripcion.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "La descripcion no puede superar 200 caracteres."
            );
        }

        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio."
            );
        }
    }

    private String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
