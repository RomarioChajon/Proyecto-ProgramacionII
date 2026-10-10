package umg.sigelab.repository;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la tabla Laboratorio.
 *
 * <p>En este paso solo tiene CONSULTAS (las necesita la pantalla de usuarios para
 * elegir la sede del encargado). El CRUD completo de laboratorios se agrega en el siguiente paso.</p>
 *
 * <p>Regla de la capa Repository: aquí solo hay SQL. Ninguna regla de negocio.</p>
 */
public class LaboratorioRepository {

    private static final String SELECT = """
            SELECT id_laboratorio, codigo, nombre, ubicacion, capacidad, estado, fecha_creacion
            FROM dbo.Laboratorio
            """;

    /** Todos los laboratorios, ordenados por código. */
    public List<Laboratorio> listar() {
        return consultar(SELECT + " ORDER BY codigo", null);
    }

    /** Solo los laboratorios ACTIVO (los que se pueden elegir en un ComboBox). */
    public List<Laboratorio> listarActivos() {
        return consultar(SELECT + " WHERE estado = ? ORDER BY codigo", EstadoRegistro.ACTIVO.name());
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio) {
        List<Laboratorio> resultado = consultar(SELECT + " WHERE id_laboratorio = ?", idLaboratorio);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    // ------------------------------------------------------------------

    /** Ejecuta un SELECT con 0 o 1 parámetro y convierte cada fila en un Laboratorio. */
    private List<Laboratorio> consultar(String sql, Object parametro) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (parametro != null) {
                sentencia.setObject(1, parametro);
            }
            try (ResultSet rs = sentencia.executeQuery()) {
                List<Laboratorio> laboratorios = new ArrayList<>();
                while (rs.next()) {
                    laboratorios.add(mapear(rs));
                }
                return laboratorios;
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los laboratorios", e);
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Laboratorio. */
    private Laboratorio mapear(ResultSet rs) throws SQLException {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setIdLaboratorio(rs.getInt("id_laboratorio"));
        laboratorio.setCodigo(rs.getString("codigo"));
        laboratorio.setNombre(rs.getString("nombre"));
        laboratorio.setUbicacion(rs.getString("ubicacion"));
        laboratorio.setCapacidad(rs.getInt("capacidad"));
        laboratorio.setEstado(EstadoRegistro.valueOf(rs.getString("estado")));
        laboratorio.setFechaCreacion(JdbcSupport.localDateTime(rs, "fecha_creacion"));
        return laboratorio;
    }
}
