package umg.sigelab.repository;

import umg.sigelab.enums.CondicionDevolucion;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.EstadoPrestamo;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.ModeloDisponible;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Todo el SQL del flujo de préstamos y devoluciones. Cada método recibe la {@link Connection}
 * para que el Service pueda agrupar varias operaciones en una sola transacción.
 *
 * <p>Esta clase NO decide reglas de negocio: solo lee y escribe. Las reglas viven en
 * {@code PrestamoService} y {@code DevolucionService} (RN-20).</p>
 */
public class PrestamoRepository {

    /** Condición SQL: el equipo está en una solicitud que aún espera despacho (reservado). */
    private static final String RESERVADO = "EXISTS (SELECT 1 FROM dbo.DetallePrestamo dr "
            + "JOIN dbo.Prestamo pr ON pr.id_prestamo = dr.id_prestamo "
            + "WHERE dr.id_equipo = e.id_equipo AND pr.estado IN ('PENDIENTE','EN_TRAMITE'))";

    // ------------------------------------------------------------------
    // Catálogo para armar el carrito
    // ------------------------------------------------------------------

    public List<Laboratorio> laboratoriosActivos(Connection cn) throws SQLException {
        String sql = "SELECT id_laboratorio, codigo, nombre, ubicacion FROM dbo.Laboratorio "
                + "WHERE estado = 'ACTIVO' ORDER BY nombre";
        List<Laboratorio> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Laboratorio lab = new Laboratorio();
                lab.setIdLaboratorio(rs.getInt("id_laboratorio"));
                lab.setCodigo(rs.getString("codigo"));
                lab.setNombre(rs.getString("nombre"));
                lab.setUbicacion(rs.getString("ubicacion"));
                lista.add(lab);
            }
        }
        return lista;
    }

    /** Categorías activas con al menos una unidad DISPONIBLE (y no reservada) en la sede. */
    public List<CategoriaStock> categoriasConStock(Connection cn, int idLaboratorio) throws SQLException {
        String sql = "SELECT t.id_tipo_equipo, t.nombre, COUNT(e.id_equipo) AS disponibles "
                + "FROM dbo.Equipo e "
                + "JOIN dbo.Producto p ON p.id_producto = e.id_producto AND p.estado = 'ACTIVO' "
                + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo AND t.estado = 'ACTIVO' "
                + "WHERE e.id_laboratorio = ? AND e.estado = 'DISPONIBLE' AND NOT " + RESERVADO + " "
                + "GROUP BY t.id_tipo_equipo, t.nombre ORDER BY t.nombre";
        List<CategoriaStock> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idLaboratorio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CategoriaStock(rs.getInt("id_tipo_equipo"), rs.getString("nombre"),
                            rs.getInt("disponibles")));
                }
            }
        }
        return lista;
    }

    /** Modelos de una categoría con unidades disponibles en la sede. */
    public List<ModeloDisponible> modelosDisponibles(Connection cn, int idLaboratorio, int idTipoEquipo)
            throws SQLException {
        String sql = "SELECT p.id_producto, p.codigo_producto, p.nombre, p.marca, p.modelo, "
                + "t.id_tipo_equipo, t.nombre AS categoria, COUNT(e.id_equipo) AS disponibles "
                + "FROM dbo.Equipo e "
                + "JOIN dbo.Producto p ON p.id_producto = e.id_producto AND p.estado = 'ACTIVO' "
                + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo AND t.estado = 'ACTIVO' "
                + "WHERE e.id_laboratorio = ? AND p.id_tipo_equipo = ? AND e.estado = 'DISPONIBLE' AND NOT "
                + RESERVADO + " "
                + "GROUP BY p.id_producto, p.codigo_producto, p.nombre, p.marca, p.modelo, t.id_tipo_equipo, t.nombre "
                + "ORDER BY p.nombre";
        List<ModeloDisponible> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idLaboratorio);
            ps.setInt(2, idTipoEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ModeloDisponible(rs.getInt("id_producto"), rs.getString("codigo_producto"),
                            rs.getString("nombre"), rs.getString("marca"), rs.getString("modelo"),
                            rs.getInt("id_tipo_equipo"), rs.getString("categoria"), rs.getInt("disponibles")));
                }
            }
        }
        return lista;
    }

    /**
     * Elige UNA unidad libre del modelo en la sede y la bloquea hasta terminar la transacción
     * (UPDLOCK + READPAST: dos solicitudes simultáneas nunca toman la misma unidad).
     */
    public Optional<Integer> unidadLibre(Connection cn, int idLaboratorio, int idProducto) throws SQLException {
        String sql = "SELECT TOP 1 e.id_equipo FROM dbo.Equipo e WITH (UPDLOCK, ROWLOCK, READPAST) "
                + "WHERE e.id_laboratorio = ? AND e.id_producto = ? AND e.estado = 'DISPONIBLE' AND NOT "
                + RESERVADO + " ORDER BY e.id_equipo";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idLaboratorio);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    // ------------------------------------------------------------------
    // Reglas de bloqueo del solicitante
    // ------------------------------------------------------------------

    /** Id del primer préstamo DESPACHADO con equipos sin devolver, o null si no tiene pendientes. */
    public Integer prestamoConPendientes(Connection cn, int idUsuario) throws SQLException {
        String sql = "SELECT TOP 1 p.id_prestamo FROM dbo.Prestamo p "
                + "JOIN dbo.DetallePrestamo d ON d.id_prestamo = p.id_prestamo "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE p.id_solicitante = ? AND p.estado = 'DESPACHADO' AND dv.id_devolucion IS NULL "
                + "ORDER BY p.id_prestamo";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** Id de un bloqueo ACTIVO del usuario (tabla BloqueoUsuario), o null. */
    public Integer bloqueoActivo(Connection cn, int idUsuario) throws SQLException {
        String sql = "SELECT TOP 1 id_prestamo FROM dbo.BloqueoUsuario WHERE id_usuario = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** Rol y estado del usuario tal como están en la base (no se confía solo en la sesión). */
    public Optional<String[]> rolYEstado(Connection cn, int idUsuario) throws SQLException {
        String sql = "SELECT id_rol, estado, nombre_usuario FROM dbo.Usuario WHERE id_usuario = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{String.valueOf(rs.getInt("id_rol")), rs.getString("estado"),
                            rs.getString("nombre_usuario")});
                }
                return Optional.empty();
            }
        }
    }

    /** Busca un usuario por id numérico o por nombre de usuario. Devuelve {id, rol, estado, nombreCompleto}. */
    public Optional<String[]> buscarUsuario(Connection cn, String idONombre) throws SQLException {
        String sql = "SELECT id_usuario, id_rol, estado, nombre + ' ' + apellido AS completo FROM dbo.Usuario "
                + "WHERE nombre_usuario = ? OR CAST(id_usuario AS NVARCHAR(20)) = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, idONombre);
            ps.setString(2, idONombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{String.valueOf(rs.getInt("id_usuario")),
                            String.valueOf(rs.getInt("id_rol")), rs.getString("estado"), rs.getString("completo")});
                }
                return Optional.empty();
            }
        }
    }

    // ------------------------------------------------------------------
    // Crear préstamo
    // ------------------------------------------------------------------

    /** Inserta el encabezado de una SOLICITUD (estado PENDIENTE). Devuelve el id generado. */
    public int crearSolicitud(Connection cn, int idSolicitante, int idLaboratorio, int diasSolicitados)
            throws SQLException {
        String sql = "INSERT INTO dbo.Prestamo (id_solicitante, id_laboratorio, id_usuario_registra, "
                + "dias_solicitados, estado) OUTPUT INSERTED.id_prestamo VALUES (?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitante);
            ps.setInt(2, idLaboratorio);
            ps.setInt(3, idSolicitante);
            ps.setInt(4, diasSolicitados);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Inserta un préstamo ya DESPACHADO (lo crea el encargado entregando equipos físicos con QR).
     *
     * @param inicio fecha de entrega: si es hoy se usa la hora del servidor; si es futura, ese día a las 08:00
     */
    public int crearDespachado(Connection cn, int idSolicitante, int idLaboratorio, int idEncargado, int dias,
                               LocalDateTime inicio, LocalDateTime fin, boolean inicioHoy) throws SQLException {
        String despacho = inicioHoy ? JdbcSupport.AHORA_SQL : "?";
        String sql = "INSERT INTO dbo.Prestamo (id_solicitante, id_laboratorio, id_usuario_registra, "
                + "id_usuario_revisa, fecha_revision, dias_solicitados, dias_prestamo, fecha_despacho, "
                + "fecha_esperada_devolucion, id_usuario_despacha, estado) OUTPUT INSERTED.id_prestamo "
                + "VALUES (?, ?, ?, ?, " + JdbcSupport.AHORA_SQL + ", ?, ?, " + despacho + ", ?, ?, 'DESPACHADO')";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, idSolicitante);
            ps.setInt(i++, idLaboratorio);
            ps.setInt(i++, idEncargado);
            ps.setInt(i++, idEncargado);
            ps.setInt(i++, dias);
            ps.setInt(i++, dias);
            if (!inicioHoy) {
                ps.setObject(i++, inicio);
            }
            ps.setObject(i++, fin);
            ps.setInt(i, idEncargado);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void agregarDetalle(Connection cn, int idPrestamo, int idEquipo, int idLaboratorio, int idProducto)
            throws SQLException {
        String sql = "INSERT INTO dbo.DetallePrestamo (id_prestamo, id_equipo, id_laboratorio, id_producto) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.setInt(2, idEquipo);
            ps.setInt(3, idLaboratorio);
            ps.setInt(4, idProducto);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Despacho / rechazo
    // ------------------------------------------------------------------

    /** Lee estado, sede y solicitante del préstamo bloqueándolo. Devuelve {estado, idLab, idSolicitante}. */
    public Optional<String[]> encabezadoParaActualizar(Connection cn, int idPrestamo) throws SQLException {
        String sql = "SELECT estado, id_laboratorio, id_solicitante FROM dbo.Prestamo WITH (UPDLOCK, ROWLOCK) "
                + "WHERE id_prestamo = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{rs.getString("estado"),
                            String.valueOf(rs.getInt("id_laboratorio")), String.valueOf(rs.getInt("id_solicitante"))});
                }
                return Optional.empty();
            }
        }
    }

    public boolean despachar(Connection cn, int idPrestamo, int idEncargado, int dias, LocalDateTime inicio,
                             LocalDateTime fin, boolean inicioHoy) throws SQLException {
        String despacho = inicioHoy ? JdbcSupport.AHORA_SQL : "?";
        String sql = "UPDATE dbo.Prestamo SET estado = 'DESPACHADO', id_usuario_revisa = ?, "
                + "fecha_revision = " + JdbcSupport.AHORA_SQL + ", dias_prestamo = ?, fecha_despacho = " + despacho + ", "
                + "fecha_esperada_devolucion = ?, id_usuario_despacha = ? "
                + "WHERE id_prestamo = ? AND estado = 'PENDIENTE'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, idEncargado);
            ps.setInt(i++, dias);
            if (!inicioHoy) {
                ps.setObject(i++, inicio);
            }
            ps.setObject(i++, fin);
            ps.setInt(i++, idEncargado);
            ps.setInt(i, idPrestamo);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean rechazar(Connection cn, int idPrestamo, int idEncargado, String motivo) throws SQLException {
        String sql = "UPDATE dbo.Prestamo SET estado = 'RECHAZADO', id_usuario_revisa = ?, "
                + "fecha_revision = " + JdbcSupport.AHORA_SQL + ", motivo_rechazo = ? WHERE id_prestamo = ? AND estado = 'PENDIENTE'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEncargado);
            ps.setString(2, motivo);
            ps.setInt(3, idPrestamo);
            return ps.executeUpdate() == 1;
        }
    }

    /** Cambia el estado de un equipo solo si todavía está en el estado esperado (evita carreras). */
    public boolean cambiarEstadoEquipo(Connection cn, int idEquipo, EstadoEquipo desde, EstadoEquipo hasta)
            throws SQLException {
        String sql = "UPDATE dbo.Equipo SET estado = ? WHERE id_equipo = ? AND estado = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, hasta.name());
            ps.setInt(2, idEquipo);
            ps.setString(3, desde.name());
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------------
    // Devoluciones
    // ------------------------------------------------------------------

    /** Datos de una línea para validar su devolución: {idPrestamo, estadoPrestamo, idLab, idSolicitante, idEquipo, estadoEquipo, devuelta(0/1)}. */
    public Optional<String[]> lineaParaDevolver(Connection cn, int idDetalle) throws SQLException {
        String sql = "SELECT d.id_prestamo, p.estado AS estado_prestamo, p.id_laboratorio, p.id_solicitante, "
                + "d.id_equipo, e.estado AS estado_equipo, "
                + "(SELECT COUNT(*) FROM dbo.Devolucion dv WHERE dv.id_detalle = d.id_detalle) AS devuelta "
                + "FROM dbo.DetallePrestamo d WITH (UPDLOCK, ROWLOCK) "
                + "JOIN dbo.Prestamo p ON p.id_prestamo = d.id_prestamo "
                + "JOIN dbo.Equipo e ON e.id_equipo = d.id_equipo WHERE d.id_detalle = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{String.valueOf(rs.getInt("id_prestamo")),
                            rs.getString("estado_prestamo"), String.valueOf(rs.getInt("id_laboratorio")),
                            String.valueOf(rs.getInt("id_solicitante")), String.valueOf(rs.getInt("id_equipo")),
                            rs.getString("estado_equipo"), String.valueOf(rs.getInt("devuelta"))});
                }
                return Optional.empty();
            }
        }
    }

    public void registrarDevolucion(Connection cn, int idDetalle, CondicionDevolucion condicion, String observacion,
                                    int idRecibe) throws SQLException {
        String sql = "INSERT INTO dbo.Devolucion (id_detalle, condicion, observacion, id_usuario_recibe) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);
            ps.setString(2, condicion.name());
            ps.setString(3, observacion == null || observacion.isBlank() ? null : observacion.trim());
            ps.setInt(4, idRecibe);
            ps.executeUpdate();
        }
    }

    public int contarPendientes(Connection cn, int idPrestamo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.DetallePrestamo d "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_prestamo = ? AND dv.id_devolucion IS NULL";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Marca el préstamo FINALIZADO (RN-16: solo cuando ya no queda ningún equipo pendiente). */
    public boolean finalizar(Connection cn, int idPrestamo) throws SQLException {
        String sql = "UPDATE dbo.Prestamo SET estado = 'FINALIZADO', "
                + "fecha_cierre = CASE WHEN " + JdbcSupport.AHORA_SQL + " < fecha_despacho THEN fecha_despacho ELSE " + JdbcSupport.AHORA_SQL + " END "
                + "WHERE id_prestamo = ? AND estado = 'DESPACHADO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------------
    // Bloqueos por devolución parcial
    // ------------------------------------------------------------------

    /** Crea el bloqueo PARCIAL del préstamo si todavía no existe uno activo. Devuelve true si lo creó. */
    public boolean crearBloqueoParcial(Connection cn, int idUsuario, int idPrestamo) throws SQLException {
        String sql = "INSERT INTO dbo.BloqueoUsuario (id_usuario, id_prestamo, motivo) "
                + "SELECT ?, ?, 'PARCIAL' WHERE NOT EXISTS (SELECT 1 FROM dbo.BloqueoUsuario "
                + "WHERE id_prestamo = ? AND motivo = 'PARCIAL' AND estado = 'ACTIVO')";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idPrestamo);
            ps.setInt(3, idPrestamo);
            return ps.executeUpdate() == 1;
        }
    }

    /** Libera los bloqueos activos de un préstamo al completarse la devolución. */
    public int liberarBloqueosDePrestamo(Connection cn, int idPrestamo, int idUsuarioLibera) throws SQLException {
        String sql = "UPDATE dbo.BloqueoUsuario SET estado = 'LIBERADO', fecha_liberacion = " + JdbcSupport.AHORA_SQL + ", "
                + "tipo_liberacion = 'DEVOLUCION_COMPLETA', id_usuario_libera = ? "
                + "WHERE id_prestamo = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuarioLibera);
            ps.setInt(2, idPrestamo);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Consultas para tablas
    // ------------------------------------------------------------------

    private static final String SELECT_RESUMEN = "SELECT x.*, "
            + "CASE WHEN x.estado = 'DESPACHADO' AND x.fecha_esperada_devolucion < " + JdbcSupport.AHORA_SQL + " "
            + "AND x.pendientes > 0 THEN 1 ELSE 0 END AS vencido FROM ("
            + "SELECT pr.id_prestamo, pr.id_solicitante, u.nombre + ' ' + u.apellido AS solicitante, "
            + "u.nombre_usuario, pr.id_laboratorio, l.codigo + ' - ' + l.nombre AS laboratorio, "
            + "pr.fecha_prestamo, pr.estado, pr.dias_solicitados, pr.dias_prestamo, pr.fecha_despacho, "
            + "pr.fecha_esperada_devolucion, pr.fecha_cierre, pr.motivo_rechazo, "
            + "(SELECT COUNT(*) FROM dbo.DetallePrestamo d WHERE d.id_prestamo = pr.id_prestamo) AS total, "
            + "(SELECT COUNT(*) FROM dbo.DetallePrestamo d LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
            + "WHERE d.id_prestamo = pr.id_prestamo AND dv.id_devolucion IS NULL) AS pendientes "
            + "FROM dbo.Prestamo pr JOIN dbo.Usuario u ON u.id_usuario = pr.id_solicitante "
            + "JOIN dbo.Laboratorio l ON l.id_laboratorio = pr.id_laboratorio WHERE ";

    private static final String FIN_RESUMEN = ") x ORDER BY x.fecha_prestamo DESC, x.id_prestamo DESC";

    /** Historial de un usuario: TODOS sus préstamos, de cualquier sede. */
    public List<PrestamoResumen> historialDeUsuario(Connection cn, int idUsuario) throws SQLException {
        return resumen(cn, "pr.id_solicitante = ?", idUsuario);
    }

    /** Solicitudes esperando que el encargado las valide (PENDIENTE) en una sede. */
    public List<PrestamoResumen> pendientesDeDespacho(Connection cn, int idLaboratorio) throws SQLException {
        return resumen(cn, "pr.id_laboratorio = ? AND pr.estado = 'PENDIENTE'", idLaboratorio);
    }

    /** Préstamos entregados con al menos un equipo sin devolver, de una sede. */
    public List<PrestamoResumen> activosConPendientes(Connection cn, int idLaboratorio) throws SQLException {
        return resumen(cn, "pr.id_laboratorio = ? AND pr.estado = 'DESPACHADO'", idLaboratorio);
    }

    /** Todos los préstamos vencidos (RN-17). idLaboratorio null = todas las sedes. */
    public List<PrestamoResumen> vencidos(Connection cn, Integer idLaboratorio) throws SQLException {
        List<PrestamoResumen> todos = idLaboratorio == null
                ? resumen(cn, "pr.estado = 'DESPACHADO'")
                : resumen(cn, "pr.estado = 'DESPACHADO' AND pr.id_laboratorio = ?", idLaboratorio);
        List<PrestamoResumen> vencidos = new ArrayList<>();
        for (PrestamoResumen p : todos) {
            if (p.vencido()) {
                vencidos.add(p);
            }
        }
        return vencidos;
    }

    public Optional<PrestamoResumen> buscarResumen(Connection cn, int idPrestamo) throws SQLException {
        List<PrestamoResumen> lista = resumen(cn, "pr.id_prestamo = ?", idPrestamo);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private List<PrestamoResumen> resumen(Connection cn, String where, Object... parametros) throws SQLException {
        List<PrestamoResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(SELECT_RESUMEN + where + FIN_RESUMEN)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PrestamoResumen(rs.getInt("id_prestamo"), rs.getInt("id_solicitante"),
                            rs.getString("solicitante"), rs.getString("nombre_usuario"),
                            rs.getInt("id_laboratorio"), rs.getString("laboratorio"),
                            JdbcSupport.localDateTime(rs, "fecha_prestamo"),
                            EstadoPrestamo.valueOf(rs.getString("estado")),
                            JdbcSupport.integerNullable(rs, "dias_solicitados"),
                            JdbcSupport.integerNullable(rs, "dias_prestamo"),
                            JdbcSupport.localDateTime(rs, "fecha_despacho"),
                            JdbcSupport.localDateTime(rs, "fecha_esperada_devolucion"),
                            JdbcSupport.localDateTime(rs, "fecha_cierre"), rs.getString("motivo_rechazo"),
                            rs.getInt("total"), rs.getInt("pendientes"), rs.getInt("vencido") == 1));
                }
            }
        }
        return lista;
    }

    /** Equipos de un préstamo (con su devolución si ya ocurrió). */
    public List<LineaPrestamo> lineas(Connection cn, int idPrestamo) throws SQLException {
        String sql = "SELECT d.id_detalle, d.id_prestamo, e.id_equipo, e.codigo_inventario, p.nombre AS producto, "
                + "p.marca, p.modelo, t.nombre AS categoria, e.estado AS estado_equipo, dv.condicion, "
                + "dv.fecha_devolucion, dv.observacion "
                + "FROM dbo.DetallePrestamo d JOIN dbo.Equipo e ON e.id_equipo = d.id_equipo "
                + "JOIN dbo.Producto p ON p.id_producto = e.id_producto "
                + "JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = p.id_tipo_equipo "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_prestamo = ? ORDER BY d.id_detalle";
        List<LineaPrestamo> lista = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String condicion = rs.getString("condicion");
                    lista.add(new LineaPrestamo(rs.getInt("id_detalle"), rs.getInt("id_prestamo"),
                            rs.getInt("id_equipo"), rs.getString("codigo_inventario"), rs.getString("producto"),
                            rs.getString("marca"), rs.getString("modelo"), rs.getString("categoria"),
                            EstadoEquipo.valueOf(rs.getString("estado_equipo")),
                            condicion == null ? null : CondicionDevolucion.valueOf(condicion),
                            JdbcSupport.localDateTime(rs, "fecha_devolucion"), rs.getString("observacion")));
                }
            }
        }
        return lista;
    }

    /**
     * Línea pendiente de devolución de un equipo (para leer su QR en Devoluciones).
     * Devuelve {idDetalle, idPrestamo} o vacío si el equipo no está pendiente en un préstamo activo (RN-10).
     */
    public Optional<int[]> lineaPendienteDeEquipo(Connection cn, int idEquipo) throws SQLException {
        String sql = "SELECT TOP 1 d.id_detalle, d.id_prestamo FROM dbo.DetallePrestamo d "
                + "JOIN dbo.Prestamo p ON p.id_prestamo = d.id_prestamo AND p.estado = 'DESPACHADO' "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_equipo = ? AND dv.id_devolucion IS NULL ORDER BY d.id_detalle DESC";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new int[]{rs.getInt(1), rs.getInt(2)}) : Optional.empty();
            }
        }
    }
}
