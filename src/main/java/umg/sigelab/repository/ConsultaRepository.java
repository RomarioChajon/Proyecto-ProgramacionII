package umg.sigelab.repository;

import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas del dashboard (conteos), bloqueos y reportes. Solo lectura, salvo liberar bloqueos. */
public class ConsultaRepository {

    /** Conteo de equipos por estado. idLaboratorio null = todas las sedes. */
    public Map<String, Integer> equiposPorEstado(Connection cn, Integer idLaboratorio) throws SQLException {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (String estado : new String[]{"DISPONIBLE", "PRESTADO", "DANADO", "MANTENIMIENTO", "BAJA"}) {
            mapa.put(estado, 0);
        }
        String sql = "SELECT estado, COUNT(*) FROM dbo.Equipo "
                + (idLaboratorio == null ? "" : "WHERE id_laboratorio = ? ") + "GROUP BY estado";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (idLaboratorio != null) {
                ps.setInt(1, idLaboratorio);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return mapa;
    }

    /** Conteo de préstamos por estado. */
    public Map<String, Integer> prestamosPorEstado(Connection cn, Integer idLaboratorio) throws SQLException {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT estado, COUNT(*) FROM dbo.Prestamo "
                + (idLaboratorio == null ? "" : "WHERE id_laboratorio = ? ") + "GROUP BY estado ORDER BY estado";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (idLaboratorio != null) {
                ps.setInt(1, idLaboratorio);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return mapa;
    }

    /** Cantidad de préstamos vencidos (fecha esperada pasada y equipos pendientes). */
    public int contarVencidos(Connection cn, Integer idLaboratorio) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.Prestamo pr WHERE pr.estado = 'DESPACHADO' "
                + "AND pr.fecha_esperada_devolucion < SYSDATETIME() AND EXISTS (SELECT 1 FROM dbo.DetallePrestamo d "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle WHERE d.id_prestamo = pr.id_prestamo "
                + "AND dv.id_devolucion IS NULL)" + (idLaboratorio == null ? "" : " AND pr.id_laboratorio = ?");
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (idLaboratorio != null) {
                ps.setInt(1, idLaboratorio);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<FilaBloqueo> bloqueos(Connection cn, boolean soloActivos) throws SQLException {
        String sql = "SELECT b.id_bloqueo, b.id_usuario, u.nombre_usuario, b.id_prestamo, b.motivo, b.estado, "
                + "b.fecha_bloqueo, b.fecha_liberacion FROM dbo.BloqueoUsuario b "
                + "JOIN dbo.Usuario u ON u.id_usuario = b.id_usuario "
                + (soloActivos ? "WHERE b.estado = 'ACTIVO' " : "") + "ORDER BY b.fecha_bloqueo DESC";
        List<FilaBloqueo> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new FilaBloqueo(rs.getInt("id_bloqueo"), rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"), rs.getInt("id_prestamo"), rs.getString("motivo"),
                        rs.getString("estado"), JdbcSupport.localDateTime(rs, "fecha_bloqueo"),
                        JdbcSupport.localDateTime(rs, "fecha_liberacion")));
            }
        }
        return lista;
    }

    /** Libera manualmente un bloqueo (solo ADMIN). */
    public boolean liberarManual(Connection cn, int idBloqueo, int idAdmin) throws SQLException {
        String sql = "UPDATE dbo.BloqueoUsuario SET estado = 'LIBERADO', fecha_liberacion = SYSDATETIME(), "
                + "tipo_liberacion = 'MANUAL', id_usuario_libera = ?, "
                + "observacion_liberacion = 'Liberado manualmente por el administrador' "
                + "WHERE id_bloqueo = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            ps.setInt(2, idBloqueo);
            return ps.executeUpdate() == 1;
        }
    }
}
