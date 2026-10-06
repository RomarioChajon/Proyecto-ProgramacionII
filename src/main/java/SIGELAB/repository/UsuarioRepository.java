package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

    public boolean guardar(Usuario usuario) {

        String sql = """
                INSERT INTO Usuario
                (nombre, apellido, usuario, password_hash, rol, activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.getNombre());
            statement.setString(2, usuario.getApellido());
            statement.setString(3, usuario.getUsuario());
            statement.setString(4, usuario.getPasswordHash());
            statement.setString(5, usuario.getRol());
            statement.setBoolean(6, usuario.isActivo());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al guardar el usuario.",
                    e
            );
        }
    }

    public List<Usuario> listar() {

        List<Usuario> usuarios = new ArrayList<>();

        String sql = """
                SELECT id_usuario,
                       nombre,
                       apellido,
                       usuario,
                       password_hash,
                       rol,
                       activo,
                       fecha_creacion
                FROM Usuario
                ORDER BY id_usuario
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                usuarios.add(mapearUsuario(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al listar los usuarios.",
                    e
            );
        }

        return usuarios;
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {

        String sql = """
                SELECT id_usuario,
                       nombre,
                       apellido,
                       usuario,
                       password_hash,
                       rol,
                       activo,
                       fecha_creacion
                FROM Usuario
                WHERE id_usuario = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapearUsuario(resultSet)
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al buscar el usuario.",
                    e
            );
        }

        return Optional.empty();
    }

    public boolean actualizar(Usuario usuario) {

        String sql = """
                UPDATE Usuario
                SET nombre = ?,
                    apellido = ?,
                    usuario = ?,
                    password_hash = ?,
                    rol = ?,
                    activo = ?
                WHERE id_usuario = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.getNombre());
            statement.setString(2, usuario.getApellido());
            statement.setString(3, usuario.getUsuario());
            statement.setString(4, usuario.getPasswordHash());
            statement.setString(5, usuario.getRol());
            statement.setBoolean(6, usuario.isActivo());
            statement.setInt(7, usuario.getIdUsuario());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al actualizar el usuario.",
                    e
            );
        }
    }

    public boolean eliminar(int idUsuario) {

        String sql = """
                DELETE FROM Usuario
                WHERE id_usuario = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al eliminar el usuario.",
                    e
            );
        }
    }

    private Usuario mapearUsuario(ResultSet resultSet)
            throws SQLException {

        Usuario usuario = new Usuario();

        usuario.setIdUsuario(
                resultSet.getInt("id_usuario")
        );

        usuario.setNombre(
                resultSet.getString("nombre")
        );

        usuario.setApellido(
                resultSet.getString("apellido")
        );

        usuario.setUsuario(
                resultSet.getString("usuario")
        );

        usuario.setPasswordHash(
                resultSet.getString("password_hash")
        );

        usuario.setRol(
                resultSet.getString("rol")
        );

        usuario.setActivo(
                resultSet.getBoolean("activo")
        );

        Timestamp fecha =
                resultSet.getTimestamp("fecha_creacion");

        if (fecha != null) {
            usuario.setFechaCreacion(
                    fecha.toLocalDateTime()
            );
        }

        return usuario;
    }
}