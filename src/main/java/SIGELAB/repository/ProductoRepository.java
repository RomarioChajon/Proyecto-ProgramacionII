package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoProducto;
import SIGELAB.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoRepository {

    public List<Producto> listar() throws SQLException {

        String sql =
                "SELECT id_producto, codigo_producto, nombre, id_tipo_equipo, " +
                        "marca, modelo, descripcion, estado " +
                        "FROM dbo.Producto " +
                        "ORDER BY id_producto";

        List<Producto> productos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                productos.add(mapear(resultSet));
            }
        }

        return productos;
    }

    public Optional<Producto> buscarPorId(int idProducto)
            throws SQLException {

        String sql =
                "SELECT id_producto, codigo_producto, nombre, id_tipo_equipo, " +
                        "marca, modelo, descripcion, estado " +
                        "FROM dbo.Producto " +
                        "WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProducto);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Producto> buscarPorCodigo(String codigoProducto)
            throws SQLException {

        String sql =
                "SELECT id_producto, codigo_producto, nombre, id_tipo_equipo, " +
                        "marca, modelo, descripcion, estado " +
                        "FROM dbo.Producto " +
                        "WHERE codigo_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, codigoProducto);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Producto> buscar(String texto)
            throws SQLException {

        String sql =
                "SELECT id_producto, codigo_producto, nombre, id_tipo_equipo, " +
                        "marca, modelo, descripcion, estado " +
                        "FROM dbo.Producto " +
                        "WHERE codigo_producto LIKE ? " +
                        "OR nombre LIKE ? " +
                        "OR marca LIKE ? " +
                        "OR modelo LIKE ? " +
                        "OR descripcion LIKE ? " +
                        "ORDER BY nombre";

        List<Producto> productos = new ArrayList<>();

        String criterio = "%" + texto + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, criterio);
            statement.setString(2, criterio);
            statement.setString(3, criterio);
            statement.setString(4, criterio);
            statement.setString(5, criterio);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    productos.add(mapear(resultSet));
                }
            }
        }

        return productos;
    }

    public Producto guardar(Producto producto)
            throws SQLException {

        String sql =
                "INSERT INTO dbo.Producto " +
                        "(codigo_producto, nombre, id_tipo_equipo, marca, modelo, descripcion, estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(1, producto.getCodigoProducto());
            statement.setString(2, producto.getNombre());
            statement.setInt(3, producto.getIdTipoEquipo());
            statement.setString(4, producto.getMarca());
            statement.setString(5, producto.getModelo());
            statement.setString(6, producto.getDescripcion());
            statement.setString(7, producto.getEstado().name());

            int filas = statement.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se pudo guardar el producto."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {

                    producto.setIdProducto(
                            generatedKeys.getInt(1)
                    );
                }
            }
        }

        return producto;
    }

    public boolean actualizar(Producto producto)
            throws SQLException {

        String sql =
                "UPDATE dbo.Producto " +
                        "SET codigo_producto = ?, " +
                        "nombre = ?, " +
                        "id_tipo_equipo = ?, " +
                        "marca = ?, " +
                        "modelo = ?, " +
                        "descripcion = ?, " +
                        "estado = ? " +
                        "WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, producto.getCodigoProducto());
            statement.setString(2, producto.getNombre());
            statement.setInt(3, producto.getIdTipoEquipo());
            statement.setString(4, producto.getMarca());
            statement.setString(5, producto.getModelo());
            statement.setString(6, producto.getDescripcion());
            statement.setString(7, producto.getEstado().name());
            statement.setInt(8, producto.getIdProducto());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idProducto)
            throws SQLException {

        String sql =
                "UPDATE dbo.Producto " +
                        "SET estado = ? " +
                        "WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoProducto.INACTIVO.name()
            );

            statement.setInt(
                    2,
                    idProducto
            );

            return statement.executeUpdate() > 0;
        }
    }

    public boolean activar(int idProducto)
            throws SQLException {

        String sql =
                "UPDATE dbo.Producto " +
                        "SET estado = ? " +
                        "WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoProducto.ACTIVO.name()
            );

            statement.setInt(
                    2,
                    idProducto
            );

            return statement.executeUpdate() > 0;
        }
    }

    private Producto mapear(ResultSet resultSet)
            throws SQLException {

        Producto producto = new Producto();

        producto.setIdProducto(
                resultSet.getInt("id_producto")
        );

        producto.setCodigoProducto(
                resultSet.getString("codigo_producto")
        );

        producto.setNombre(
                resultSet.getString("nombre")
        );

        producto.setIdTipoEquipo(
                resultSet.getInt("id_tipo_equipo")
        );

        producto.setMarca(
                resultSet.getString("marca")
        );

        producto.setModelo(
                resultSet.getString("modelo")
        );

        producto.setDescripcion(
                resultSet.getString("descripcion")
        );

        producto.setEstado(
                EstadoProducto.valueOf(
                        resultSet.getString("estado")
                )
        );

        return producto;
    }
}
