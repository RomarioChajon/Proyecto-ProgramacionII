package umg.sigelab.repository;

import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.ResultadoMantenimiento;
import umg.sigelab.enums.TipoMantenimiento;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL del inventario por sede, del cambio de estado de equipos y del mantenimiento. */
public class InventarioRepository {

    private static final String SELECT_EQUIPO = "SELECT e.id_equipo, p.id_producto, e.codigo_inventario, p.nombre AS producto, "
            + "p.marca, p.modelo, t.id_tipo_equipo, t.nombre AS categoria, e.numero_serie, e.id_laboratorio, "
            + "l.codigo + ' - ' + l.nombre AS laboratorio, e.estado, e.observaciones "
            + "FROM dbo.Equipo e JOIN dbo.Producto p ON p.id_producto = e.id_producto "
            + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo "
            + "JOIN dbo.Laboratorio l ON l.id_laboratorio = e.id_laboratorio ";

    /** Categorías que tienen al menos un equipo en la sede (para el filtro del inventario). */
    public List<CategoriaStock> categoriasDeSede(Connection cn, int idLaboratorio) throws SQLException {
        String sql = "SELECT t.id_tipo_equipo, t.nombre, COUNT(e.id_equipo) AS total FROM dbo.Equipo e "
                + "JOIN dbo.Producto p ON p.id_producto = e.id_producto "
                + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo "
                + "WHERE e.id_laboratorio = ? GROUP BY t.id_tipo_equipo, t.nombre ORDER BY t.nombre";
        List<CategoriaStock> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idLaboratorio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CategoriaStock(rs.getInt(1), rs.getString(2), rs.getInt(3)));
                }
            }
        }
        return lista;
    }

    /** Equipos de una sede, filtrados opcionalmente por categoría y por estado. */
    public List<FilaInventario> listar(Connection cn, int idLaboratorio, Integer idTipoEquipo, EstadoEquipo estado)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_EQUIPO).append("WHERE e.id_laboratorio = ? ");
        if (idTipoEquipo != null) {
            sql.append("AND t.id_tipo_equipo = ? ");
        }
        if (estado != null) {
            sql.append("AND e.estado = ? ");
        }
        sql.append("ORDER BY t.nombre, p.nombre, e.codigo_inventario");
        List<FilaInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setInt(i++, idLaboratorio);
            if (idTipoEquipo != null) {
                ps.setInt(i++, idTipoEquipo);
            }
            if (estado != null) {
                ps.setString(i, estado.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Busca un equipo por el código que trae su QR (código de inventario). */
    public Optional<FilaInventario> buscarPorCodigo(Connection cn, String codigo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SELECT_EQUIPO + "WHERE e.codigo_inventario = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Optional<FilaInventario> buscarPorId(Connection cn, int idEquipo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(SELECT_EQUIPO + "WHERE e.id_equipo = ? ")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Equipos de la sede en un estado concreto (p. ej. MANTENIMIENTO), con filtro opcional de categoría. */
    public List<FilaInventario> listarPorEstado(Connection cn, int idLaboratorio, EstadoEquipo estado,
                                                Integer idTipoEquipo) throws SQLException {
        return listar(cn, idLaboratorio, idTipoEquipo, estado);
    }

    /** Número de la solicitud PENDIENTE que reserva el equipo, o null si no está reservado. */
    public Integer solicitudQueReserva(Connection cn, int idEquipo) throws SQLException {
        String sql = "SELECT TOP 1 pr.id_prestamo FROM dbo.DetallePrestamo d "
                + "JOIN dbo.Prestamo pr ON pr.id_prestamo = d.id_prestamo "
                + "WHERE d.id_equipo = ? AND pr.estado IN ('PENDIENTE','EN_TRAMITE') ORDER BY pr.id_prestamo";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /**
     * Préstamo activo donde el equipo está PRESTADO: {idPrestamo, solicitante, fecha esperada de devolución}.
     * Se usa en la ficha del equipo (lectura de QR).
     */
    public Optional<String[]> prestamoActivoDelEquipo(Connection cn, int idEquipo) throws SQLException {
        String sql = "SELECT TOP 1 pr.id_prestamo, u.nombre + ' ' + u.apellido AS solicitante, "
                + "CONVERT(VARCHAR(16), pr.fecha_esperada_devolucion, 120) AS esperada "
                + "FROM dbo.DetallePrestamo d JOIN dbo.Prestamo pr ON pr.id_prestamo = d.id_prestamo "
                + "JOIN dbo.Usuario u ON u.id_usuario = pr.id_solicitante "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_equipo = ? AND pr.estado = 'DESPACHADO' AND dv.id_devolucion IS NULL "
                + "ORDER BY d.id_detalle DESC";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3)})
                        : Optional.empty();
            }
        }
    }

    /** Mantenimiento EN_PROCESO del equipo: {id, tipo, responsable, ingreso (texto), descripcion}. */
    public Optional<String[]> mantenimientoEnProceso(Connection cn, int idEquipo) throws SQLException {
        String sql = "SELECT TOP 1 id_mantenimiento, tipo, responsable, "
                + "CONVERT(VARCHAR(16), fecha_ingreso, 120) AS ingreso, descripcion "
                + "FROM dbo.Mantenimiento WITH (UPDLOCK, ROWLOCK) WHERE id_equipo = ? AND estado = 'EN_PROCESO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)})
                        : Optional.empty();
            }
        }
    }

    public void abrirMantenimiento(Connection cn, int idEquipo, TipoMantenimiento tipo, String descripcion,
                                   String responsable, int idUsuarioRegistra) throws SQLException {
        String sql = "INSERT INTO dbo.Mantenimiento (id_equipo, tipo, descripcion, responsable, estado, "
                + "id_usuario_registra) VALUES (?, ?, ?, ?, 'EN_PROCESO', ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            ps.setString(2, tipo.name());
            ps.setString(3, descripcion);
            ps.setString(4, responsable);
            ps.setInt(5, idUsuarioRegistra);
            ps.executeUpdate();
        }
    }

    public boolean cerrarMantenimiento(Connection cn, int idMantenimiento, ResultadoMantenimiento resultado,
                                       String observaciones, int idUsuarioCierra) throws SQLException {
        String sql = "UPDATE dbo.Mantenimiento SET estado = 'FINALIZADO', fecha_salida = "
                + "CASE WHEN " + JdbcSupport.AHORA_SQL + " < fecha_ingreso THEN fecha_ingreso ELSE " + JdbcSupport.AHORA_SQL + " END, "
                + "resultado = ?, observaciones = ?, id_usuario_cierra = ? "
                + "WHERE id_mantenimiento = ? AND estado = 'EN_PROCESO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, resultado.name());
            ps.setString(2, observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
            ps.setInt(3, idUsuarioCierra);
            ps.setInt(4, idMantenimiento);
            return ps.executeUpdate() == 1;
        }
    }

    private FilaInventario mapear(ResultSet rs) throws SQLException {
        return new FilaInventario(rs.getInt("id_equipo"), rs.getInt("id_producto"), rs.getString("codigo_inventario"),
                rs.getString("producto"), rs.getString("marca"), rs.getString("modelo"),
                rs.getInt("id_tipo_equipo"), rs.getString("categoria"), rs.getString("numero_serie"),
                rs.getInt("id_laboratorio"), rs.getString("laboratorio"),
                EstadoEquipo.valueOf(rs.getString("estado")), rs.getString("observaciones"));
    }
}
