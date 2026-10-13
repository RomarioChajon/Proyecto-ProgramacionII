package umg.sigelab.repository;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrestamoRepository {

    /**
     * Consulta equipos disponibles de una sede.
     * idTipoEquipo puede ser null para mostrar todas las categorías.
     */
    public List<EquipoDisponible> listarEquiposDisponibles(
            int idLaboratorio,
            Integer idTipoEquipo) {

        String sql = """
                SELECT
                    e.id_equipo,
                    e.codigo_inventario,
                    p.nombre AS producto,
                    t.nombre AS categoria,
                    e.numero_serie
                FROM dbo.Equipo e
                INNER JOIN dbo.Producto p
                    ON e.id_producto = p.id_producto
                INNER JOIN dbo.TipoEquipo t
                    ON p.id_tipo_equipo = t.id_tipo_equipo
                INNER JOIN dbo.Laboratorio l
                    ON e.id_laboratorio = l.id_laboratorio
                WHERE e.id_laboratorio = ?
                  AND e.estado = 'DISPONIBLE'
                  AND l.estado = 'ACTIVO'
                  AND (? IS NULL OR p.id_tipo_equipo = ?)
                ORDER BY t.nombre, p.nombre, e.codigo_inventario
                """;

        List<EquipoDisponible> equipos = new ArrayList<>();

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idLaboratorio);
            JdbcSupport.setIntegerNullable(ps, 2, idTipoEquipo);
            JdbcSupport.setIntegerNullable(ps, 3, idTipoEquipo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipos.add(new EquipoDisponible(
                            rs.getInt("id_equipo"),
                            rs.getString("codigo_inventario"),
                            rs.getString("producto"),
                            rs.getString("categoria"),
                            rs.getString("numero_serie")
                    ));
                }
            }

            return equipos;

        } catch (SQLException e) {
            throw JdbcSupport.error(
                    "consultar los equipos disponibles", e);
        }
    }

    /**
     * Registra una solicitud de préstamo pendiente.
     *
     * El solicitante registra su propia solicitud.
     * Los días serán establecidos posteriormente por el encargado.
     *
     * @return ID del préstamo generado por SQL Server.
     */
    public int registrarSolicitud(
            int idSolicitante,
            int idLaboratorio,
            List<Integer> idsEquipos,
            String observaciones) {

        if (idSolicitante <= 0 || idLaboratorio <= 0) {
            throw new BusinessException(
                    "El solicitante y la sede deben ser válidos.");
        }

        if (idsEquipos == null || idsEquipos.isEmpty()) {
            throw new BusinessException(
                    "Debe seleccionar al menos un equipo.");
        }

        Set<Integer> equiposUnicos = new HashSet<>();

        for (Integer idEquipo : idsEquipos) {
            if (idEquipo == null || idEquipo <= 0
                    || !equiposUnicos.add(idEquipo)) {
                throw new BusinessException(
                        "La solicitud contiene equipos inválidos o repetidos.");
            }
        }

        if (observaciones != null && observaciones.length() > 600) {
            throw new BusinessException(
                    "Las observaciones no pueden superar 600 caracteres.");
        }

        try (Connection cn = DatabaseConnection.getConnection()) {

            cn.setAutoCommit(false);

            try {
                validarSolicitante(cn, idSolicitante);
                validarLaboratorio(cn, idLaboratorio);
                validarPrestamosPendientes(cn, idSolicitante);

                for (Integer idEquipo : idsEquipos) {
                    validarEquipoDisponible(
                            cn, idEquipo, idLaboratorio);
                }

                int idPrestamo = insertarPrestamo(
                        cn, idSolicitante, idLaboratorio, observaciones);

                for (Integer idEquipo : idsEquipos) {
                    insertarDetalle(
                            cn, idPrestamo, idEquipo, idLaboratorio);
                }

                cn.commit();
                return idPrestamo;

            } catch (SQLException | RuntimeException ex) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            }

        } catch (SQLException e) {
            throw JdbcSupport.error(
                    "registrar la solicitud de préstamo", e);
        }
    }

    private void validarSolicitante(
            Connection cn, int idSolicitante) throws SQLException {

        String sql = """
                SELECT u.estado, r.nombre AS rol
                FROM dbo.Usuario u WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN dbo.Rol r ON u.id_rol = r.id_rol
                WHERE u.id_usuario = ?
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitante);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException(
                            "El usuario solicitante no existe.");
                }

                String estado = rs.getString("estado");
                String rol = rs.getString("rol");

                if (!"ACTIVO".equalsIgnoreCase(estado)) {
                    throw new BusinessException(
                            "El usuario solicitante no está activo.");
                }

                if (!"ESTUDIANTE".equalsIgnoreCase(rol)
                        && !"DOCENTE".equalsIgnoreCase(rol)) {
                    throw new BusinessException(
                            "Solo estudiantes y docentes pueden solicitar préstamos.");
                }
            }
        }
    }

    private void validarLaboratorio(
            Connection cn, int idLaboratorio) throws SQLException {

        String sql = """
                SELECT estado
                FROM dbo.Laboratorio
                WHERE id_laboratorio = ?
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idLaboratorio);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !"ACTIVO".equalsIgnoreCase(
                        rs.getString("estado"))) {
                    throw new BusinessException(
                            "La sede seleccionada no está activa.");
                }
            }
        }
    }

    /**
     * Impide nuevas solicitudes mientras el usuario tenga
     * préstamos activos con equipos aún no devueltos.
     */
    private void validarPrestamosPendientes(
            Connection cn, int idSolicitante) throws SQLException {

        String sql = """
                SELECT TOP (1) p.id_prestamo
                FROM dbo.Prestamo p
                INNER JOIN dbo.DetallePrestamo d
                    ON d.id_prestamo = p.id_prestamo
                WHERE p.id_solicitante = ?
                  AND p.estado = 'DESPACHADO'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM dbo.Devolucion v
                      WHERE v.id_detalle = d.id_detalle
                  )
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitante);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new BusinessException(
                            "Tiene equipos pendientes de devolución. "
                                    + "No puede solicitar otro préstamo.");
                }
            }
        }
    }

    private void validarEquipoDisponible(
            Connection cn,
            int idEquipo,
            int idLaboratorio) throws SQLException {

        String sql = """
                SELECT estado, id_laboratorio
                FROM dbo.Equipo WITH (UPDLOCK, HOLDLOCK)
                WHERE id_equipo = ?
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException(
                            "Uno de los equipos seleccionados no existe.");
                }

                if (rs.getInt("id_laboratorio") != idLaboratorio) {
                    throw new BusinessException(
                            "Todos los equipos deben pertenecer "
                                    + "a la misma sede.");
                }

                if (!"DISPONIBLE".equalsIgnoreCase(
                        rs.getString("estado"))) {
                    throw new BusinessException(
                            "Uno de los equipos ya no está disponible.");
                }
            }
        }
    }

    private int insertarPrestamo(
            Connection cn,
            int idSolicitante,
            int idLaboratorio,
            String observaciones) throws SQLException {

        String sql = """
                INSERT INTO dbo.Prestamo (
                    id_solicitante,
                    id_laboratorio,
                    id_usuario_registra,
                    dias_prestamo,
                    estado,
                    observaciones
                )
                OUTPUT INSERTED.id_prestamo
                VALUES (?, ?, ?, NULL, 'PENDIENTE', ?)
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitante);
            ps.setInt(2, idLaboratorio);
            ps.setInt(3, idSolicitante);
            ps.setString(4, observaciones);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "SQL Server no devolvió el ID del préstamo.");
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertarDetalle(
            Connection cn,
            int idPrestamo,
            int idEquipo,
            int idLaboratorio) throws SQLException {

        String sql = """
                INSERT INTO dbo.DetallePrestamo (
                    id_prestamo,
                    id_equipo,
                    id_laboratorio
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.setInt(2, idEquipo);
            ps.setInt(3, idLaboratorio);
            ps.executeUpdate();
        }
    }

    public record EquipoDisponible(
            int idEquipo,
            String codigoInventario,
            String producto,
            String categoria,
            String numeroSerie) {
    }
}
