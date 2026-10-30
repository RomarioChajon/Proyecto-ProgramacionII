package umg.sigelab.repository;

import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.model.TablaReporte;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
                + "AND pr.fecha_esperada_devolucion < " + JdbcSupport.AHORA_SQL + " AND EXISTS (SELECT 1 FROM dbo.DetallePrestamo d "
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
        String sql = "UPDATE dbo.BloqueoUsuario SET estado = 'LIBERADO', fecha_liberacion = " + JdbcSupport.AHORA_SQL + ", "
                + "tipo_liberacion = 'MANUAL', id_usuario_libera = ?, "
                + "observacion_liberacion = 'Liberado manualmente por el administrador' "
                + "WHERE id_bloqueo = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            ps.setInt(2, idBloqueo);
            return ps.executeUpdate() == 1;
        }
    }

    // =====================================================================
    // Reportes y consultas del PDF (6.11). Usan SELECT, JOIN, WHERE, ORDER BY, BETWEEN, COUNT y GROUP BY.
    // Todas devuelven una TablaReporte lista para mostrar; los filtros se agregan solo si llegan con valor.
    // =====================================================================

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Equipos con su estado: JOIN de Equipo, Producto, TipoEquipo y Laboratorio.
     * Filtros opcionales: sede, estado (DISPONIBLE, PRESTADO...) y categoría.
     */
    public TablaReporte equiposDetallePorEstado(Connection cn, Integer idLaboratorio, String estado,
                                                Integer idTipoEquipo) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT e.codigo_inventario, p.nombre, t.nombre, l.nombre, "
                + "e.numero_serie, e.estado FROM dbo.Equipo e "
                + "JOIN dbo.Producto p ON p.id_producto = e.id_producto "
                + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo "
                + "JOIN dbo.Laboratorio l ON l.id_laboratorio = e.id_laboratorio WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();
        if (idLaboratorio != null) {
            sql.append("AND e.id_laboratorio = ? ");
            parametros.add(idLaboratorio);
        }
        if (estado != null) {
            sql.append("AND e.estado = ? ");
            parametros.add(estado);
        }
        if (idTipoEquipo != null) {
            sql.append("AND p.id_tipo_equipo = ? ");
            parametros.add(idTipoEquipo);
        }
        sql.append("ORDER BY e.estado, l.nombre, e.codigo_inventario");
        return tabla(cn, sql.toString(), parametros,
                "Código", "Equipo", "Categoría", "Sede", "Serie", "Estado");
    }

    /**
     * Préstamos entre dos fechas (BETWEEN sobre la fecha de solicitud), leídos de la vista vw_prestamos_resumen,
     * que ya calcula los equipos devueltos/pendientes y el estado VENCIDO.
     *
     * @param grupo TODOS, PENDIENTES, ACTIVOS, VENCIDOS, FINALIZADOS o NO_ATENDIDOS (rechazados y cancelados)
     */
    public TablaReporte prestamosEntreFechas(Connection cn, Integer idLaboratorio, String grupo,
                                             LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id_prestamo, laboratorio, solicitante, rol_solicitante, "
                + "estado_calculado, fecha_prestamo, fecha_despacho, fecha_esperada_devolucion, fecha_cierre, "
                + "total_equipos, devueltos, pendientes FROM dbo.vw_prestamos_resumen "
                + "WHERE CAST(fecha_prestamo AS DATE) BETWEEN ? AND ? ");
        List<Object> parametros = new ArrayList<>();
        parametros.add(java.sql.Date.valueOf(desde));
        parametros.add(java.sql.Date.valueOf(hasta));
        if (idLaboratorio != null) {
            sql.append("AND id_laboratorio = ? ");
            parametros.add(idLaboratorio);
        }
        // El grupo viene de una lista fija (nunca del usuario): por eso es seguro concatenarlo.
        sql.append(switch (grupo == null ? "TODOS" : grupo) {
            case "PENDIENTES" -> "AND estado IN ('PENDIENTE','EN_TRAMITE') ";
            case "ACTIVOS" -> "AND estado = 'DESPACHADO' ";
            case "VENCIDOS" -> "AND estado_calculado = 'VENCIDO' ";
            case "FINALIZADOS" -> "AND estado = 'FINALIZADO' ";
            case "NO_ATENDIDOS" -> "AND estado IN ('RECHAZADO','CANCELADO') ";
            default -> "";
        });
        sql.append("ORDER BY fecha_prestamo DESC, id_prestamo DESC");
        return tabla(cn, sql.toString(), parametros, "N.º", "Sede", "Solicitante", "Rol", "Estado", "Solicitado",
                "Entregado", "Devolución esperada", "Cerrado", "Equipos", "Devueltos", "Pendientes");
    }

    /** Historial de préstamos de UN equipo (vista vw_historial_equipo), del más reciente al más antiguo. */
    public TablaReporte historialDeEquipo(Connection cn, String codigoInventario) throws SQLException {
        String sql = "SELECT id_prestamo, solicitante, estado_prestamo, fecha_despacho, fecha_esperada_devolucion, "
                + "fecha_devolucion, condicion, observacion FROM dbo.vw_historial_equipo "
                + "WHERE codigo_inventario = ? ORDER BY id_prestamo DESC";
        return tabla(cn, sql, List.of(codigoInventario), "Préstamo", "Solicitante", "Estado del préstamo", "Entregado",
                "Devolución esperada", "Devuelto", "Condición", "Observación");
    }

    /** Historial de mantenimientos (vista vw_historial_mantenimiento), de una sede y/o de un equipo. */
    public TablaReporte historialMantenimiento(Connection cn, Integer idLaboratorio, String codigoInventario)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT codigo_inventario, equipo, tipo, descripcion, responsable, "
                + "fecha_ingreso, fecha_salida, resultado, estado FROM dbo.vw_historial_mantenimiento WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();
        if (idLaboratorio != null) {
            sql.append("AND id_laboratorio = ? ");
            parametros.add(idLaboratorio);
        }
        if (codigoInventario != null) {
            sql.append("AND codigo_inventario = ? ");
            parametros.add(codigoInventario);
        }
        sql.append("ORDER BY fecha_ingreso DESC");
        return tabla(cn, sql.toString(), parametros, "Código", "Equipo", "Tipo", "Descripción", "Responsable",
                "Ingreso", "Salida", "Resultado", "Estado");
    }

    /** Ejecuta la consulta y convierte cada fila en texto listo para la tabla. */
    private TablaReporte tabla(Connection cn, String sql, List<Object> parametros, String... columnas)
            throws SQLException {
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[columnas.length];
                    for (int c = 0; c < columnas.length; c++) {
                        fila[c] = valor(rs.getObject(c + 1));
                    }
                    filas.add(fila);
                }
            }
        }
        return new TablaReporte(columnas, filas);
    }

    private static Object valor(Object v) {
        if (v == null) {
            return "-";
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().format(FECHA_HORA);
        }
        // Solo los valores que parecen códigos de enum (DISPONIBLE, EN_TRAMITE...) se presentan "bonitos";
        // el texto libre (observaciones, descripciones) se deja tal cual.
        if (v instanceof String texto && texto.matches("[A-Z_]+")) {
            return texto.equals("DANADO") ? "DAÑADO" : texto.replace('_', ' ');
        }
        return v;
    }
}
