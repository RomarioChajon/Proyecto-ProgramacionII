package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoLaboratorio;
import SIGELAB.model.Laboratorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LaboratorioRepository {

    public List<Laboratorio> listar() throws SQLException {
        String sql =
                "SELECT id_laboratorio, codigo, nombre, ubicacion, " +
                        "capacidad, estado, fecha_creacion " +
                        "FROM dbo.Laboratorio " +
                        "ORDER BY id_laboratorio";

        List<Laboratorio> laboratorios = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                laboratorios.add(mapear(resultSet));
            }
        }

        return laboratorios;
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio)
            throws SQLException {

        String sql =
                "SELECT id_laboratorio, codigo, nombre, ubicacion, " +
                        "capacidad, estado, fecha_creacion " +
                        "FROM dbo.Laboratorio " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idLaboratorio);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Laboratorio> buscarPorCodigo(String codigo)
            throws SQLException {

        String sql =
                "SELECT id_laboratorio, codigo, nombre, ubicacion, " +
                        "capacidad, estado, fecha_creacion " +
                        "FROM dbo.Laboratorio " +
                        "WHERE codigo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, codigo);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Laboratorio> buscar(String texto)
            throws SQLException {

        String sql =
                "SELECT id_laboratorio, codigo, nombre, ubicacion, " +
                        "capacidad, estado, fecha_creacion " +
                        "FROM dbo.Laboratorio " +
                        "WHERE codigo LIKE ? " +
                        "OR nombre LIKE ? " +
                        "OR ubicacion LIKE ? " +
                        "ORDER BY nombre";

        List<Laboratorio> laboratorios = new ArrayList<>();

        String criterio = "%" + texto + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, criterio);
            statement.setString(2, criterio);
            statement.setString(3, criterio);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    laboratorios.add(mapear(resultSet));
                }
            }
        }

        return laboratorios;
    }

    public Laboratorio guardar(Laboratorio laboratorio)
            throws SQLException {

        String sql =
                "INSERT INTO dbo.Laboratorio " +
                        "(codigo, nombre, ubicacion, capacidad, estado) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(
                    1,
                    laboratorio.getCodigo()
            );

            statement.setString(
                    2,
                    laboratorio.getNombre()
            );

            statement.setString(
                    3,
                    laboratorio.getUbicacion()
            );

            statement.setInt(
                    4,
                    laboratorio.getCapacidad()
            );

            statement.setString(
                    5,
                    laboratorio.getEstado().name()
            );

            int filas = statement.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se pudo guardar el laboratorio."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    laboratorio.setIdLaboratorio(
                            generatedKeys.getInt(1)
                    );
                }
            }
        }

        if (laboratorio.getIdLaboratorio() > 0) {

            Optional<Laboratorio> guardado =
                    buscarPorId(
                            laboratorio.getIdLaboratorio()
                    );

            if (guardado.isPresent()) {
                return guardado.get();
            }
        }

        return laboratorio;
    }

    public boolean actualizar(Laboratorio laboratorio)
            throws SQLException {

        String sql =
                "UPDATE dbo.Laboratorio " +
                        "SET codigo = ?, " +
                        "nombre = ?, " +
                        "ubicacion = ?, " +
                        "capacidad = ?, " +
                        "estado = ? " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    laboratorio.getCodigo()
            );

            statement.setString(
                    2,
                    laboratorio.getNombre()
            );

            statement.setString(
                    3,
                    laboratorio.getUbicacion()
            );

            statement.setInt(
                    4,
                    laboratorio.getCapacidad()
            );

            statement.setString(
                    5,
                    laboratorio.getEstado().name()
            );

            statement.setInt(
                    6,
                    laboratorio.getIdLaboratorio()
            );

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idLaboratorio)
            throws SQLException {

        String sql =
                "UPDATE dbo.Laboratorio " +
                        "SET estado = ? " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoLaboratorio.INACTIVO.name()
            );

            statement.setInt(
                    2,
                    idLaboratorio
            );

            return statement.executeUpdate() > 0;
        }
    }

    public boolean activar(int idLaboratorio)
            throws SQLException {

        String sql =
                "UPDATE dbo.Laboratorio " +
                        "SET estado = ? " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoLaboratorio.ACTIVO.name()
            );

            statement.setInt(
                    2,
                    idLaboratorio
            );

            return statement.executeUpdate() > 0;
        }
    }

    private Laboratorio mapear(ResultSet resultSet)
            throws SQLException {

        Laboratorio laboratorio = new Laboratorio();

        laboratorio.setIdLaboratorio(
                resultSet.getInt("id_laboratorio")
        );

        laboratorio.setCodigo(
                resultSet.getString("codigo")
        );

        laboratorio.setNombre(
                resultSet.getString("nombre")
        );

        laboratorio.setUbicacion(
                resultSet.getString("ubicacion")
        );

        laboratorio.setCapacidad(
                resultSet.getInt("capacidad")
        );

        String estado =
                resultSet.getString("estado");

        laboratorio.setEstado(
                EstadoLaboratorio.valueOf(estado)
        );

        Timestamp fecha =
                resultSet.getTimestamp("fecha_creacion");

        if (fecha != null) {
            laboratorio.setFechaCreacion(
                    fecha.toLocalDateTime()
            );
        }

        return laboratorio;
    }
}