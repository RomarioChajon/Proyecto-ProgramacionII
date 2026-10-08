
package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoTipoEquipo;
import SIGELAB.model.TipoEquipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TipoEquipoRepository {

    private static final String COLUMNAS =
            "id_tipo_equipo, codigo, nombre, descripcion, estado";

    private static final String RECURSO_BLOQUEO =
            "SIGELAB_TipoEquipo_Codigo";

    public List<TipoEquipo> listar() throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.TipoEquipo ORDER BY id_tipo_equipo";

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

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.TipoEquipo WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.TipoEquipo WHERE nombre = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.TipoEquipo " +
                "WHERE codigo LIKE ? " +
                "OR nombre LIKE ? " +
                "OR descripcion LIKE ? " +
                "ORDER BY nombre";

        List<TipoEquipo> tipos = new ArrayList<>();
        String criterio = "%" + texto + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, criterio);
            statement.setString(2, criterio);
            statement.setString(3, criterio);

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

        if (tipoEquipo == null ||
                tipoEquipo.getNombre() == null ||
                tipoEquipo.getNombre().trim().isEmpty() ||
                tipoEquipo.getEstado() == null) {

            throw new IllegalArgumentException(
                    "Ingrese una categoría y un estado válidos."
            );
        }

        String nombre = tipoEquipo.getNombre().trim();
        String prefijo = generarPrefijo(nombre);

        String sqlInsertar =
                "INSERT INTO dbo.TipoEquipo " +
                        "(codigo, nombre, descripcion, estado) " +
                        "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {

            boolean bloqueoAdquirido = false;
            boolean transaccionIniciada = false;
            Throwable errorPrincipal = null;

            try {

                bloquearGeneracion(connection);
                bloqueoAdquirido = true;

                connection.setAutoCommit(false);
                transaccionIniciada = true;

                String sqlPrefijos =
                        "SELECT nombre FROM dbo.TipoEquipo " +
                                "WHERE SUBSTRING(codigo, 5, 4) = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(sqlPrefijos)) {

                    statement.setString(1, prefijo);

                    try (ResultSet resultSet = statement.executeQuery()) {

                        if (resultSet.next()) {
                            throw new IllegalArgumentException(
                                    "El prefijo " + prefijo +
                                            " ya pertenece a la categoría \"" +
                                            resultSet.getString("nombre") + "\"."
                            );
                        }
                    }
                }

                int ultimoNumero = 0;

                String sqlNumeros =
                        "SELECT codigo FROM dbo.TipoEquipo " +
                                "WHERE codigo IS NOT NULL";

                try (PreparedStatement statement =
                             connection.prepareStatement(sqlNumeros);
                     ResultSet resultSet = statement.executeQuery()) {

                    while (resultSet.next()) {

                        String codigoExistente =
                                resultSet.getString("codigo");

                        if (codigoExistente != null) {

                            String codigoNormalizado =
                                    codigoExistente.toUpperCase(Locale.ROOT);

                            if (codigoNormalizado.matches(
                                    "CAT-[A-Z]{4}-[0-9]{3}"
                            )) {

                                int numero = Integer.parseInt(
                                        codigoNormalizado.substring(9)
                                );

                                ultimoNumero = Math.max(
                                        ultimoNumero,
                                        numero
                                );
                            }
                        }
                    }
                }

                int siguienteNumero = ultimoNumero + 1;

                if (siguienteNumero > 999) {
                    throw new SQLException(
                            "Se alcanzó el límite de 999 categorías."
                    );
                }

                String codigo = String.format(
                        Locale.ROOT,
                        "CAT-%s-%03d",
                        prefijo,
                        siguienteNumero
                );

                int idGenerado;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sqlInsertar,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setString(1, codigo);
                    statement.setString(2, nombre);
                    statement.setString(
                            3,
                            tipoEquipo.getDescripcion()
                    );
                    statement.setString(
                            4,
                            tipoEquipo.getEstado().name()
                    );

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo guardar el tipo de equipo."
                        );
                    }

                    try (ResultSet generatedKeys =
                                 statement.getGeneratedKeys()) {

                        if (!generatedKeys.next()) {
                            throw new SQLException(
                                    "No se obtuvo el ID generado."
                            );
                        }

                        idGenerado = generatedKeys.getInt(1);
                    }
                }

                connection.commit();
                transaccionIniciada = false;

                tipoEquipo.setIdTipoEquipo(idGenerado);
                tipoEquipo.setCodigo(codigo);
                tipoEquipo.setNombre(nombre);

                return tipoEquipo;

            } catch (SQLException | RuntimeException ex) {

                errorPrincipal = ex;

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

                        if (errorPrincipal != null) {
                            errorPrincipal.addSuppressed(ex);
                        } else {
                            throw ex;
                        }
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

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el resultado del bloqueo."
                    );
                }

                int resultado = resultSet.getInt("resultado");

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

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el resultado " +
                                    "de la liberación del bloqueo."
                    );
                }

                int resultado = resultSet.getInt("resultado");

                if (resultado < 0) {
                    throw new SQLException(
                            "No se pudo liberar el bloqueo de códigos. " +
                                    "Resultado SQL Server: " + resultado
                    );
                }
            }
        }
    }

    private String generarPrefijo(String nombre) {

        String normalizado = Normalizer.normalize(
                nombre.toUpperCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        normalizado = normalizado
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Z]", "");

        if (normalizado.length() < 4) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría debe tener " +
                            "al menos cuatro letras."
            );
        }

        return normalizado.substring(0, 4);
    }

    public boolean actualizar(TipoEquipo tipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET nombre = ?, descripcion = ?, estado = ? " +
                        "WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tipoEquipo.getNombre());
            statement.setString(2, tipoEquipo.getDescripcion());
            statement.setString(3, tipoEquipo.getEstado().name());
            statement.setInt(4, tipoEquipo.getIdTipoEquipo());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idTipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET estado = ? WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoTipoEquipo.INACTIVO.name()
            );

            statement.setInt(2, idTipoEquipo);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean activar(int idTipoEquipo)
            throws SQLException {

        String sql =
                "UPDATE dbo.TipoEquipo " +
                        "SET estado = ? WHERE id_tipo_equipo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    EstadoTipoEquipo.ACTIVO.name()
            );

            statement.setInt(2, idTipoEquipo);

            return statement.executeUpdate() > 0;
        }
    }

    private TipoEquipo mapear(ResultSet resultSet)
            throws SQLException {

        TipoEquipo tipoEquipo = new TipoEquipo();

        tipoEquipo.setIdTipoEquipo(
                resultSet.getInt("id_tipo_equipo")
        );

        tipoEquipo.setCodigo(
                resultSet.getString("codigo")
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



