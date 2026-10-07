package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoTipoEquipo;
import SIGELAB.model.TipoEquipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TipoEquipoRepository {

    public List<TipoEquipo> listar() throws SQLException {

        String sql =
                "SELECT id_tipo_equipo, nombre, descripcion, estado " +
                        "FROM dbo.TipoEquipo " +
                        "ORDER BY id_tipo_equipo";

        List<TipoEquipo> tipos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tipos.add(mapear(resultSet));
            }
        }

        return tipos;
    }

    public Optional<TipoEquipo> buscarPorId(int idTipoEquipo)
            throws SQLException {

        String sql =
                "SELECT id_tipo_equipo, nombre, descripcion, estado " +
                        "FROM dbo.TipoEquipo " +
                        "WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idTipoEquipo);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<TipoEquipo> buscarPorNombre(String nombre)
            throws SQLException {

        String sql =
                "SELECT id_tipo_equipo, nombre, descripcion, estado " +
                        "FROM dbo.TipoEquipo " +
                        "WHERE nombre = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nombre);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<TipoEquipo> buscar(String texto)
            throws SQLException {

        String sql =
                "SELECT id_tipo_equipo, nombre, descripcion, estado " +
                        "FROM dbo.TipoEquipo " +
                        "WHERE nombre LIKE ? " +
                        "OR descripcion LIKE ? " +
                        "ORDER BY nombre";

        List<TipoEquipo> tipos = new ArrayList<>();

        String criterio = "%" + texto + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, criterio);
            statement.setString(2, criterio);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    tipos.add(mapear(resultSet));
                }
            }
        }

        return tipos;
    }

    public TipoEquipo guardar(TipoEquipo tipoEquipo)
            throws SQLException {

        String sql =
                "INSERT INTO dbo.TipoEquipo " +
                        "(nombre, descripcion, estado) " +
                        "VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(
                    1,
                    tipoEquipo.getNombre()
            );

            statement.setString(
                    2,
                    tipoEquipo.getDescripcion()
            );

            statement.setString(
                    3,
                    tipoEquipo.getEstado().name()
            );

            int filas = statement.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se pudo guardar el tipo de equipo."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    tipoEquipo.setIdTipoEquipo(
                            generatedKeys.getInt(1)
                    );
                }
            }
        }

        return tipoEquipo;
    }

    public boolean actualizar(TipoEquipo tipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET nombre = ?, " +
                        "descripcion = ?, " +
                        "estado = ? " +
                        "WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    tipoEquipo.getNombre()
            );

            statement.setString(
                    2,
                    tipoEquipo.getDescripcion()
            );

            statement.setString(
                    3,
                    tipoEquipo.getEstado().name()
            );

            statement.setInt(
                    4,
                    tipoEquipo.getIdTipoEquipo()
            );

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idTipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET estado = ? " +
                        "WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoTipoEquipo.INACTIVO.name()
            );

            statement.setInt(
                    2,
                    idTipoEquipo
            );

            return statement.executeUpdate() > 0;
        }
    }

    public boolean activar(int idTipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET estado = ? " +
                        "WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoTipoEquipo.ACTIVO.name()
            );

            statement.setInt(
                    2,
                    idTipoEquipo
            );

            return statement.executeUpdate() > 0;
        }
    }

    private TipoEquipo mapear(ResultSet resultSet)
            throws SQLException {

        TipoEquipo tipoEquipo = new TipoEquipo();

        tipoEquipo.setIdTipoEquipo(
                resultSet.getInt("id_tipo_equipo")
        );

        tipoEquipo.setNombre(
                resultSet.getString("nombre")
        );

        tipoEquipo.setDescripcion(
                resultSet.getString("descripcion")
        );

        tipoEquipo.setEstado(
                EstadoTipoEquipo.valueOf(
                        resultSet.getString("estado")
                )
        );

        return tipoEquipo;
    }
}