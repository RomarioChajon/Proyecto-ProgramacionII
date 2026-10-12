
package umg.sigelab.repository;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.enums.EstadoProducto;
import umg.sigelab.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductoRepository {

    private static final String COLUMNAS =
            "id_producto, codigo_producto, nombre, " +
                    "id_tipo_equipo, marca, modelo, descripcion, estado";

    private static final String RECURSO_BLOQUEO =
            "SIGELAB_Producto_Codigo";

    public List<Producto> listar() throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Producto ORDER BY id_producto";

        List<Producto> productos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                productos.add(mapear(rs));
            }
        }

        return productos;
    }

    public Optional<Producto> buscarPorId(int idProducto)
            throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Producto WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProducto);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Producto> buscarPorCodigo(String codigoProducto)
            throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Producto WHERE codigo_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, codigoProducto);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Producto> buscar(String texto) throws SQLException {

        String sql =
                "SELECT " + COLUMNAS + " FROM dbo.Producto " +
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

            for (int i = 1; i <= 5; i++) {
                statement.setString(i, criterio);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapear(rs));
                }
            }
        }

        return productos;
    }

    public Producto guardar(Producto producto) throws SQLException {

        String sql =
                "INSERT INTO dbo.Producto " +
                        "(codigo_producto, nombre, id_tipo_equipo, " +
                        "marca, modelo, descripcion, estado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {

            boolean bloqueoAdquirido = false;
            boolean transaccionIniciada = false;

            try {

                bloquearGeneracion(connection);
                bloqueoAdquirido = true;

                connection.setAutoCommit(false);
                transaccionIniciada = true;

                String prefijo = obtenerPrefijo(
                        connection,
                        producto.getIdTipoEquipo()
                );

                int siguiente = obtenerSiguienteNumero(
                        connection,
                        prefijo
                );

                String codigo = String.format(
                        Locale.ROOT,
                        "%s-%04d",
                        prefijo,
                        siguiente
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setString(1, codigo);
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

                    try (ResultSet keys = statement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "No se obtuvo el ID del producto."
                            );
                        }

                        producto.setIdProducto(keys.getInt(1));
                    }
                }

                connection.commit();
                transaccionIniciada = false;

                producto.setCodigoProducto(codigo);

                return producto;

            } catch (SQLException | RuntimeException ex) {

                if (transaccionIniciada) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        ex.addSuppressed(rollbackError);
                    }
                }

                throw ex;

            } finally {

                if (bloqueoAdquirido) {
                    try {
                        liberarGeneracion(connection);
                    } catch (SQLException ex) {
                        throw ex;
                    }
                }
            }
        }
    }

    private void bloquearGeneracion(Connection connection)
            throws SQLException {

        String sql =
                "DECLARE @resultado INT; " +
                        "EXEC @resultado = sys.sp_getapplock " +
                        "@Resource = ?, " +
                        "@LockMode = 'Exclusive', " +
                        "@LockOwner = 'Session', " +
                        "@LockTimeout = 10000; " +
                        "SELECT @resultado AS resultado";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, RECURSO_BLOQUEO);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el resultado del bloqueo."
                    );
                }

                int resultado = rs.getInt("resultado");

                if (resultado < 0) {
                    throw new SQLException(
                            "No se pudo bloquear la generación de códigos. " +
                                    "Resultado SQL Server: " + resultado
                    );
                }
            }
        }
    }

    private void liberarGeneracion(Connection connection)
            throws SQLException {

        String sql =
                "DECLARE @resultado INT; " +
                        "EXEC @resultado = sys.sp_releaseapplock " +
                        "@Resource = ?, " +
                        "@LockOwner = 'Session'; " +
                        "SELECT @resultado AS resultado";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, RECURSO_BLOQUEO);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el resultado " +
                                    "de la liberación del bloqueo."
                    );
                }

                int resultado = rs.getInt("resultado");

                if (resultado < 0) {
                    throw new SQLException(
                            "No se pudo liberar el bloqueo de códigos. " +
                                    "Resultado SQL Server: " + resultado
                    );
                }
            }
        }
    }

    private String obtenerPrefijo(
            Connection connection,
            int idTipoEquipo
    ) throws SQLException {

        String sql =
                "SELECT codigo FROM dbo.TipoEquipo " +
                        "WHERE id_tipo_equipo = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idTipoEquipo);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "El tipo de equipo seleccionado no existe."
                    );
                }

                String codigoCategoria = rs.getString("codigo");

                if (codigoCategoria == null) {
                    throw new SQLException(
                            "La categoría seleccionada no tiene código."
                    );
                }

                codigoCategoria = codigoCategoria
                        .trim()
                        .toUpperCase(Locale.ROOT);

                if (!codigoCategoria.matches(
                        "CAT-[A-Z]{4}-[0-9]{3,4}"
                )) {

                    throw new SQLException(
                            "La categoría tiene un código inválido: " +
                                    codigoCategoria +
                                    ". Debe tener el formato CAT-MONI-0001."
                    );
                }

                return codigoCategoria.substring(4, 8);
            }
        }
    }

    private int obtenerSiguienteNumero(
            Connection connection,
            String prefijo
    ) throws SQLException {

        String sql =
                "SELECT codigo_producto FROM dbo.Producto " +
                        "WHERE codigo_producto LIKE ?";

        Pattern patron = Pattern.compile(
                "^" + Pattern.quote(prefijo) + "-([0-9]{3,4})$"
        );

        int maximo = 0;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, prefijo + "-%");

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    String codigo = rs.getString("codigo_producto");

                    if (codigo == null) {
                        continue;
                    }

                    Matcher matcher = patron.matcher(
                            codigo.trim().toUpperCase(Locale.ROOT)
                    );

                    if (matcher.matches()) {

                        int numero = Integer.parseInt(
                                matcher.group(1)
                        );

                        maximo = Math.max(maximo, numero);
                    }
                }
            }
        }

        if (maximo >= 9999) {
            throw new SQLException(
                    "La categoría " + prefijo +
                            " alcanzó el límite de 9999 productos."
            );
        }

        return maximo + 1;
    }

    public boolean actualizar(Producto producto)
            throws SQLException {

        String sql =
                "UPDATE dbo.Producto SET " +
                        "nombre = ?, " +
                        "id_tipo_equipo = ?, " +
                        "marca = ?, " +
                        "modelo = ?, " +
                        "descripcion = ?, " +
                        "estado = ? " +
                        "WHERE id_producto = ? " +
                        "AND id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, producto.getNombre());
            statement.setInt(2, producto.getIdTipoEquipo());
            statement.setString(3, producto.getMarca());
            statement.setString(4, producto.getModelo());
            statement.setString(5, producto.getDescripcion());
            statement.setString(6, producto.getEstado().name());
            statement.setInt(7, producto.getIdProducto());
            statement.setInt(8, producto.getIdTipoEquipo());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idProducto)
            throws SQLException {

        return cambiarEstado(
                idProducto,
                EstadoProducto.INACTIVO
        );
    }

    public boolean activar(int idProducto)
            throws SQLException {

        return cambiarEstado(
                idProducto,
                EstadoProducto.ACTIVO
        );
    }

    private boolean cambiarEstado(
            int idProducto,
            EstadoProducto estado
    ) throws SQLException {

        String sql =
                "UPDATE dbo.Producto " +
                        "SET estado = ? " +
                        "WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, estado.name());
            statement.setInt(2, idProducto);

            return statement.executeUpdate() > 0;
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {

        Producto producto = new Producto();

        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigoProducto(rs.getString("codigo_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setIdTipoEquipo(rs.getInt("id_tipo_equipo"));
        producto.setMarca(rs.getString("marca"));
        producto.setModelo(rs.getString("modelo"));
        producto.setDescripcion(rs.getString("descripcion"));

        producto.setEstado(
                EstadoProducto.valueOf(
                        rs.getString("estado")
                )
        );

        return producto;
    }
}

