package umg.sigelab.repository;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.model.FilaAuditoria;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla Auditoria: quién hizo qué, cuándo y sobre qué registro (RN-18). */
public class AuditoriaRepository {

    /**
     * Registra una acción usando la MISMA conexión/transacción de la operación principal:
     * si la operación se revierte, su auditoría también.
     */
    public void registrar(Connection cn, Integer idUsuario, Integer idLaboratorio, AccionAuditoria accion,
                          String entidad, Integer idRegistro, String descripcion) throws SQLException {
        String sql = "INSERT INTO dbo.Auditoria (id_usuario, id_laboratorio, accion, entidad, id_registro, descripcion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcSupport.setIntegerNullable(ps, 1, idUsuario);
            JdbcSupport.setIntegerNullable(ps, 2, idLaboratorio);
            ps.setString(3, accion.name());
            ps.setString(4, entidad);
            JdbcSupport.setIntegerNullable(ps, 5, idRegistro);
            ps.setString(6, descripcion.length() > 500 ? descripcion.substring(0, 497) + "..." : descripcion);
            ps.executeUpdate();
        }
    }

    /**
     * Registra un intento de acceso (LOGIN_EXITOSO / LOGIN_FALLIDO). Si el usuario no existe, idUsuario es null y
     * se guarda el texto digitado en usuario_intento.
     */
    public void registrarAcceso(Connection cn, Integer idUsuario, String usuarioIntento, AccionAuditoria accion,
                                String descripcion) throws SQLException {
        String sql = "INSERT INTO dbo.Auditoria (id_usuario, usuario_intento, accion, entidad, id_registro, descripcion) "
                + "VALUES (?, ?, ?, 'Usuario', ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcSupport.setIntegerNullable(ps, 1, idUsuario);
            ps.setString(2, usuarioIntento == null ? null
                    : (usuarioIntento.length() > 60 ? usuarioIntento.substring(0, 60) : usuarioIntento));
            ps.setString(3, accion.name());
            JdbcSupport.setIntegerNullable(ps, 4, idUsuario);
            ps.setString(5, descripcion.length() > 500 ? descripcion.substring(0, 497) + "..." : descripcion);
            ps.executeUpdate();
        }
    }

    /** Últimos registros de auditoría (más recientes primero). idLaboratorio null = todas las sedes. */
    public List<FilaAuditoria> listar(Connection cn, Integer idLaboratorio, int limite) throws SQLException {
        String sql = "SELECT TOP (" + limite + ") a.id_auditoria, a.fecha_hora, "
                + "ISNULL(u.nombre_usuario, a.usuario_intento) AS usuario, l.codigo AS laboratorio, "
                + "a.accion, a.entidad, a.id_registro, a.descripcion "
                + "FROM dbo.Auditoria a "
                + "LEFT JOIN dbo.Usuario u ON u.id_usuario = a.id_usuario "
                + "LEFT JOIN dbo.Laboratorio l ON l.id_laboratorio = a.id_laboratorio "
                + (idLaboratorio == null ? "" : "WHERE a.id_laboratorio = ? ")
                + "ORDER BY a.fecha_hora DESC, a.id_auditoria DESC";
        List<FilaAuditoria> filas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (idLaboratorio != null) {
                ps.setInt(1, idLaboratorio);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new FilaAuditoria(rs.getLong("id_auditoria"),
                            JdbcSupport.localDateTime(rs, "fecha_hora"), rs.getString("usuario"),
                            rs.getString("laboratorio"), rs.getString("accion"), rs.getString("entidad"),
                            JdbcSupport.integerNullable(rs, "id_registro"), rs.getString("descripcion")));
                }
            }
        }
        return filas;
    }
}
