package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoEquipo;
import SIGELAB.model.Equipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EquipoRepository {

    private static final String COLUMNAS = """
            id_equipo, codigo_inventario, id_producto,
            numero_serie, id_laboratorio, estado,
            observaciones, fecha_registro
            """;

    public List<Equipo> listar() throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Equipo ORDER BY id_equipo";

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

    public Optional<Equipo> buscarPorId(int idEquipo)
            throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Equipo WHERE id_equipo = ?";

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

    public Optional<Equipo> buscarPorCodigo(String codigo)
            throws SQLException {

        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.Equipo WHERE codigo_inventario = ?";

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

    public List<Equipo> buscar(String texto)
            throws SQLException {

        String sql = "SELECT " + COLUMNAS + """
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

    private String generarCodigoInventario(Connection cn)
            throws SQLException {

        String sql = """
                SELECT codigo_inventario
                FROM dbo.Equipo WITH (TABLOCKX, HOLDLOCK)
                """;

        long maximo = 0;

        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String codigo =
                        rs.getString("codigo_inventario");

                if (codigo == null ||
                        !codigo.matches("INV-[0-9]{4,}")) {
                    continue;
                }

                try {

                    long numero = Long.parseLong(
                            codigo.substring(4)
                    );

                    if (numero > maximo) {
                        maximo = numero;
                    }

                } catch (NumberFormatException ex) {

                    throw new SQLException(
                            "Código de inventario fuera de rango: "
                                    + codigo,
                            ex
                    );
                }
            }
        }

        if (maximo == Long.MAX_VALUE) {

            throw new SQLException(
                    "Se agotó el correlativo de inventario."
            );
        }

        String codigo = String.format(
                Locale.ROOT,
                "INV-%04d",
                maximo + 1
        );

        if (codigo.length() > 30) {

            throw new SQLException(
                    "El código generado supera "
                            + "la longitud permitida."
            );
        }

        return codigo;
    }

    public Equipo guardar(Equipo equipo)
            throws SQLException {

        String sql = """
                INSERT INTO dbo.Equipo
                (codigo_inventario,
                 id_producto,
                 numero_serie,
                 id_laboratorio,
                 estado,
                 observaciones)
                OUTPUT INSERTED.id_equipo,
                       INSERTED.fecha_registro
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = DatabaseConnection.getConnection()) {

            cn.setAutoCommit(false);

            try {

                String codigo =
                        generarCodigoInventario(cn);

                try (PreparedStatement ps =
                             cn.prepareStatement(sql)) {

                    ps.setString(1, codigo);
                    ps.setInt(2, equipo.getIdProducto());
                    ps.setString(3, equipo.getNumeroSerie());
                    ps.setInt(4, equipo.getIdLaboratorio());
                    ps.setString(
                            5,
                            EstadoEquipo.DISPONIBLE.name()
                    );
                    ps.setString(
                            6,
                            equipo.getObservaciones()
                    );

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "No se pudo obtener "
                                            + "el equipo registrado."
                            );
                        }

                        equipo.setIdEquipo(
                                rs.getInt("id_equipo")
                        );

                        Timestamp fecha =
                                rs.getTimestamp(
                                        "fecha_registro"
                                );

                        if (fecha != null) {

                            equipo.setFechaRegistro(
                                    fecha.toLocalDateTime()
                            );
                        }
                    }
                }

                cn.commit();

                equipo.setCodigoInventario(codigo);
                equipo.setEstado(EstadoEquipo.DISPONIBLE);

                return equipo;

            } catch (SQLException | RuntimeException ex) {

                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }

                throw ex;
            }
        }
    }

    public boolean actualizar(Equipo equipo)
            throws SQLException {

        String sql = """
                UPDATE dbo.Equipo
                SET id_producto = ?,
                    numero_serie = ?,
                    id_laboratorio = ?,
                    observaciones = ?
                WHERE id_equipo = ?
                """;

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, equipo.getIdProducto());
            ps.setString(2, equipo.getNumeroSerie());
            ps.setInt(3, equipo.getIdLaboratorio());
            ps.setString(4, equipo.getObservaciones());
            ps.setInt(5, equipo.getIdEquipo());

            return ps.executeUpdate() > 0;
        }
    }

    public List<Equipo> listarPorLaboratorio(
            int idLaboratorio
    ) throws SQLException {

        String sql = "SELECT " + COLUMNAS + """
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

    private Equipo mapear(ResultSet rs)
            throws SQLException {

        Equipo equipo = new Equipo();

        equipo.setIdEquipo(
                rs.getInt("id_equipo")
        );

        equipo.setCodigoInventario(
                rs.getString("codigo_inventario")
        );

        equipo.setIdProducto(
                rs.getInt("id_producto")
        );

        equipo.setNumeroSerie(
                rs.getString("numero_serie")
        );

        equipo.setIdLaboratorio(
                rs.getInt("id_laboratorio")
        );

        equipo.setEstado(
                EstadoEquipo.valueOf(
                        rs.getString("estado")
                )
        );

        equipo.setObservaciones(
                rs.getString("observaciones")
        );

        Timestamp fecha =
                rs.getTimestamp("fecha_registro");

        if (fecha != null) {

            equipo.setFechaRegistro(
                    fecha.toLocalDateTime()
            );
        }

        return equipo;
    }
}


