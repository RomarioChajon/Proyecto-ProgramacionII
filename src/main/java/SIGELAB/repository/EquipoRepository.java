
package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.model.Equipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipoRepository {

    // LISTAR EQUIPOS
    public List<Equipo> listar() throws SQLException {
        String sql = """
                SELECT id_equipo, codigo_inventario, id_producto,
                       numero_serie, id_laboratorio, estado,
                       observaciones, fecha_registro
                FROM dbo.Equipo
                ORDER BY id_equipo
                """;

        List<Equipo> equipos = new ArrayList<>();

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                equipos.add(mapear(rs));
            }
        }

        return equipos;
    }

    // BUSCAR POR ID
    public Optional<Equipo> buscarPorId(int idEquipo)
            throws SQLException {

        String sql = """
                SELECT id_equipo, codigo_inventario, id_producto,
                       numero_serie, id_laboratorio, estado,
                       observaciones, fecha_registro
                FROM dbo.Equipo
                WHERE id_equipo = ?
                """;

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idEquipo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    // BUSCAR POR CÓDIGO DE INVENTARIO
    public Optional<Equipo> buscarPorCodigo(String codigo)
            throws SQLException {

        String sql = """
                SELECT id_equipo, codigo_inventario, id_producto,
                       numero_serie, id_laboratorio, estado,
                       observaciones, fecha_registro
                FROM dbo.Equipo
                WHERE codigo_inventario = ?
                """;

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    // BUSCAR POR TEXTO
    public List<Equipo> buscar(String texto) throws SQLException {

        String sql = """
                SELECT id_equipo, codigo_inventario, id_producto,
                       numero_serie, id_laboratorio, estado,
                       observaciones, fecha_registro
                FROM dbo.Equipo
                WHERE codigo_inventario LIKE ?
                   OR numero_serie LIKE ?
                   OR observaciones LIKE ?
                ORDER BY id_equipo
                """;

        List<Equipo> equipos = new ArrayList<>();
        String filtro = "%" + texto + "%";

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, filtro);
            ps.setString(2, filtro);
            ps.setString(3, filtro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapear(rs));
                }
            }
        }

        return equipos;
    }

    // GUARDAR EQUIPO
    public Equipo guardar(Equipo equipo) throws SQLException {

        String sql = """
                INSERT INTO dbo.Equipo
                (codigo_inventario, id_producto, numero_serie,
                 id_laboratorio, estado, observaciones)
                OUTPUT INSERTED.id_equipo,
                       INSERTED.fecha_registro
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, equipo.getCodigoInventario());
            ps.setInt(2, equipo.getIdProducto());
            ps.setString(3, equipo.getNumeroSerie());
            ps.setInt(4, equipo.getIdLaboratorio());
            ps.setString(5, equipo.getEstado().name());
            ps.setString(6, equipo.getObservaciones());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    equipo.setIdEquipo(rs.getInt("id_equipo"));

                    Timestamp fecha =
                            rs.getTimestamp("fecha_registro");

                    if (fecha != null) {
                        equipo.setFechaRegistro(
                                fecha.toLocalDateTime()
                        );
                    }
                } else {
                    throw new SQLException(
                            "No se pudo obtener el equipo registrado."
                    );
                }
            }
        }

        return equipo;
    }

    // ACTUALIZAR EQUIPO
    public boolean actualizar(Equipo equipo) throws SQLException {

        String sql = """
                UPDATE dbo.Equipo
                SET codigo_inventario = ?,
                    id_producto = ?,
                    numero_serie = ?,
                    id_laboratorio = ?,
                    observaciones = ?
                WHERE id_equipo = ?
                """;

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, equipo.getCodigoInventario());
            ps.setInt(2, equipo.getIdProducto());
            ps.setString(3, equipo.getNumeroSerie());
            ps.setInt(4, equipo.getIdLaboratorio());
            ps.setString(5, equipo.getObservaciones());
            ps.setInt(6, equipo.getIdEquipo());

            return ps.executeUpdate() > 0;
        }
    }

    // LISTAR POR LABORATORIO
    public List<Equipo> listarPorLaboratorio(int idLaboratorio)
            throws SQLException {

        String sql = """
                SELECT id_equipo, codigo_inventario, id_producto,
                       numero_serie, id_laboratorio, estado,
                       observaciones, fecha_registro
                FROM dbo.Equipo
                WHERE id_laboratorio = ?
                ORDER BY id_equipo
                """;

        List<Equipo> equipos = new ArrayList<>();

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idLaboratorio);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipos.add(mapear(rs));
                }
            }
        }

        return equipos;
    }

    // CONVERTIR RESULTSET A EQUIPO
    private Equipo mapear(ResultSet rs) throws SQLException {

        Equipo equipo = new Equipo();

        equipo.setIdEquipo(rs.getInt("id_equipo"));
        equipo.setCodigoInventario(
                rs.getString("codigo_inventario")
        );
        equipo.setIdProducto(rs.getInt("id_producto"));
        equipo.setNumeroSerie(rs.getString("numero_serie"));
        equipo.setIdLaboratorio(rs.getInt("id_laboratorio"));

        equipo.setEstado(
                EstadoEquipo.valueOf(rs.getString("estado"))
        );

        equipo.setObservaciones(
                rs.getString("observaciones")
        );

        Timestamp fecha = rs.getTimestamp("fecha_registro");

        if (fecha != null) {
            equipo.setFechaRegistro(fecha.toLocalDateTime());
        }

        return equipo;
    }
}
