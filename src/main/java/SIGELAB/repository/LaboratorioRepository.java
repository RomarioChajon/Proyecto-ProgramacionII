
package SIGELAB.repository;

import SIGELAB.config.DatabaseConnection;
import SIGELAB.enums.EstadoLaboratorio;
import SIGELAB.model.Laboratorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LaboratorioRepository {

    private static final String COLUMNAS =
            "id_laboratorio, codigo, nombre, ubicacion, " +
                    "capacidad, estado, fecha_creacion";

    public List<Laboratorio> listar() throws SQLException {

        String sql =
                "SELECT " + COLUMNAS +
                        " FROM dbo.Laboratorio ORDER BY id_laboratorio";

        List<Laboratorio> laboratorios = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                laboratorios.add(mapear(rs));
            }
        }

        return laboratorios;
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio)
            throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {
            return buscarPorId(connection, idLaboratorio);
        }
    }

    private Optional<Laboratorio> buscarPorId(
            Connection connection,
            int idLaboratorio
    ) throws SQLException {

        String sql =
                "SELECT " + COLUMNAS +
                        " FROM dbo.Laboratorio WHERE id_laboratorio = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idLaboratorio);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Laboratorio> buscarPorCodigo(String codigo)
            throws SQLException {

        String sql =
                "SELECT " + COLUMNAS +
                        " FROM dbo.Laboratorio WHERE codigo = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, codigo);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Laboratorio> buscar(String texto)
            throws SQLException {

        String sql =
                "SELECT " + COLUMNAS +
                        " FROM dbo.Laboratorio " +
                        "WHERE codigo LIKE ? " +
                        "OR nombre LIKE ? " +
                        "OR ubicacion LIKE ? " +
                        "ORDER BY nombre";

        List<Laboratorio> laboratorios = new ArrayList<>();

        String criterio =
                "%" + (texto == null ? "" : texto.trim()) + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, criterio);
            statement.setString(2, criterio);
            statement.setString(3, criterio);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    laboratorios.add(mapear(rs));
                }
            }
        }

        return laboratorios;
    }

    public Laboratorio guardar(Laboratorio laboratorio)
            throws SQLException {

        if (laboratorio == null || laboratorio.getEstado() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar un laboratorio y su estado."
            );
        }

        String prefijo = construirPrefijo(
                laboratorio.getNombre(),
                laboratorio.getUbicacion()
        );

        String sqlInsert =
                "INSERT INTO dbo.Laboratorio " +
                        "(codigo, nombre, ubicacion, capacidad, estado) " +
                        "OUTPUT INSERTED.id_laboratorio " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int siguiente = obtenerSiguienteCorrelativo(
                        connection, prefijo, true
                );

                String codigo = prefijo + String.format(
                        Locale.ROOT, "%04d", siguiente
                );

                if (codigo.length() > 20) {
                    throw new SQLException(
                            "El código supera los 20 caracteres permitidos: "
                                    + codigo
                    );
                }

                int id;

                try (PreparedStatement statement =
                             connection.prepareStatement(sqlInsert)) {

                    statement.setString(1, codigo);
                    statement.setString(
                            2, laboratorio.getNombre().trim()
                    );
                    statement.setString(
                            3, laboratorio.getUbicacion().trim()
                    );
                    statement.setInt(
                            4, laboratorio.getCapacidad()
                    );
                    statement.setString(
                            5, laboratorio.getEstado().name()
                    );

                    try (ResultSet rs = statement.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "No se pudo guardar el laboratorio."
                            );
                        }

                        id = rs.getInt(1);
                    }
                }

                Laboratorio guardado =
                        buscarPorId(connection, id)
                                .orElseThrow(() -> new SQLException(
                                        "No se encontró el laboratorio recién creado."
                                ));

                connection.commit();

                laboratorio.setIdLaboratorio(
                        guardado.getIdLaboratorio()
                );
                laboratorio.setCodigo(
                        guardado.getCodigo()
                );
                laboratorio.setFechaCreacion(
                        guardado.getFechaCreacion()
                );

                return guardado;

            } catch (SQLException | RuntimeException ex) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;
            }
        }
    }

    /*
     * Correlativo independiente por sede.
     *
     * LABINF-SEDESANJ-0001
     * LABRED-SEDESANJ-0002
     *
     * Otra sede inicia su propio correlativo:
     *
     * LABINF-SEDECENT-0001
     *
     * Se conservan los bloqueos para evitar duplicados
     * durante registros simultáneos.
     */
    private int obtenerSiguienteCorrelativo(
            Connection connection,
            String prefijo,
            boolean bloquearParaGuardar
    ) throws SQLException {

        int inicioSede = prefijo.indexOf("-SEDE");

        if (inicioSede < 0) {
            throw new SQLException(
                    "Prefijo de laboratorio inválido: " + prefijo
            );
        }

        String identificadorSede = prefijo.substring(inicioSede);

        String sql =
                "SELECT codigo FROM dbo.Laboratorio " +
                        (bloquearParaGuardar
                                ? "WITH (TABLOCKX, HOLDLOCK) "
                                : "");

        int maximo = 0;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                String codigo = rs.getString("codigo");

                if (codigo == null) {
                    continue;
                }

                codigo = codigo.trim().toUpperCase(Locale.ROOT);

                int inicio = codigo.indexOf("-SEDE");

                if (inicio < 0) {
                    continue;
                }

                String resto = codigo.substring(inicio);

                if (!resto.startsWith(identificadorSede)) {
                    continue;
                }

                String sufijo =
                        resto.substring(identificadorSede.length());

                if (!sufijo.matches("[0-9]{3,4}")) {
                    continue;
                }

                int numero = Integer.parseInt(sufijo);
                maximo = Math.max(maximo, numero);
            }
        }

        if (maximo >= 9999) {
            throw new SQLException(
                    "Se alcanzó el límite de 9999 laboratorios para esta sede."
            );
        }

        return maximo + 1;
    }

    public String previsualizarCodigo(
            String nombre,
            String ubicacion
    ) throws SQLException {

        String prefijo = construirPrefijo(nombre, ubicacion);

        try (Connection connection = DatabaseConnection.getConnection()) {

            int siguiente = obtenerSiguienteCorrelativo(
                    connection, prefijo, false
            );

            String codigo = prefijo + String.format(
                    Locale.ROOT, "%04d", siguiente
            );

            if (codigo.length() > 20) {
                throw new SQLException(
                        "El código supera los 20 caracteres permitidos: "
                                + codigo
                );
            }

            return codigo;
        }
    }

    /*
     * Genera el prefijo según el nombre del laboratorio
     * y la sede indicada en el campo ubicación.
     */
    private String construirPrefijo(
            String nombre,
            String ubicacion
    ) {

        String tipo = normalizar(nombre)
                .replaceFirst("^LABORATORIO( DE(L)? )?", "")
                .replaceFirst("^DE ", "")
                .trim();

        String letrasTipo = tipo.replaceAll("[^A-Z]", "");

        if (letrasTipo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Indique el nombre del laboratorio."
            );
        }

        String abreviaturaTipo =
                (letrasTipo + "XXX").substring(0, 3);

        String sede = normalizar(ubicacion)
                .replaceAll("\\s+", " ")
                .trim();

        if (sede.isEmpty()) {
            throw new IllegalArgumentException(
                    "Indique la sede en Ubicación."
            );
        }

        String abreviaturaSede;

        if (sede.contains("SAN JUAN SACATEPEQUEZ")) {

            abreviaturaSede = "SANJ";

        } else if (sede.contains("CAMPUS CENTRAL")
                || sede.contains("SEDE CENTRAL")) {

            abreviaturaSede = "CENT";

        } else if (sede.contains("ANTIGUA GUATEMALA")) {

            abreviaturaSede = "ANTI";

        } else if (sede.contains("CHIMALTENANGO")) {

            abreviaturaSede = "CHIM";

        } else if (sede.startsWith("SAN ")) {

            String resto = sede.substring(4)
                    .replaceAll("[^A-Z]", "");

            abreviaturaSede = "SAN" +
                    (resto.isEmpty()
                            ? "X"
                            : resto.substring(0, 1));

        } else if (sede.startsWith("SANTA ")) {

            String resto = sede.substring(6)
                    .replaceAll("[^A-Z]", "");

            abreviaturaSede = "STA" +
                    (resto.isEmpty()
                            ? "X"
                            : resto.substring(0, 1));

        } else {

            String letras = sede.replaceAll("[^A-Z]", "");

            if (letras.isEmpty()) {
                throw new IllegalArgumentException(
                        "La ubicación debe contener letras."
                );
            }

            abreviaturaSede =
                    (letras + "XXXX").substring(0, 4);
        }

        return "LAB" + abreviaturaTipo +
                "-SEDE" + abreviaturaSede + "-";
    }

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                        texto.trim(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    public boolean actualizar(Laboratorio laboratorio)
            throws SQLException {

        String sql =
                "UPDATE dbo.Laboratorio " +
                        "SET nombre = ?, " +
                        "ubicacion = ?, " +
                        "capacidad = ?, " +
                        "estado = ? " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, laboratorio.getNombre());
            statement.setString(2, laboratorio.getUbicacion());
            statement.setInt(3, laboratorio.getCapacidad());
            statement.setString(4, laboratorio.getEstado().name());
            statement.setInt(5, laboratorio.getIdLaboratorio());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean desactivar(int idLaboratorio)
            throws SQLException {

        return cambiarEstado(
                idLaboratorio,
                EstadoLaboratorio.INACTIVO
        );
    }

    public boolean activar(int idLaboratorio)
            throws SQLException {

        return cambiarEstado(
                idLaboratorio,
                EstadoLaboratorio.ACTIVO
        );
    }

    private boolean cambiarEstado(
            int idLaboratorio,
            EstadoLaboratorio estado
    ) throws SQLException {

        String sql =
                "UPDATE dbo.Laboratorio " +
                        "SET estado = ? " +
                        "WHERE id_laboratorio = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, estado.name());
            statement.setInt(2, idLaboratorio);

            return statement.executeUpdate() > 0;
        }
    }

    private Laboratorio mapear(ResultSet rs)
            throws SQLException {

        Laboratorio laboratorio = new Laboratorio();

        laboratorio.setIdLaboratorio(
                rs.getInt("id_laboratorio")
        );

        laboratorio.setCodigo(
                rs.getString("codigo")
        );

        laboratorio.setNombre(
                rs.getString("nombre")
        );

        laboratorio.setUbicacion(
                rs.getString("ubicacion")
        );

        laboratorio.setCapacidad(
                rs.getInt("capacidad")
        );

        laboratorio.setEstado(
                EstadoLaboratorio.valueOf(
                        rs.getString("estado")
                )
        );

        Timestamp fecha = rs.getTimestamp("fecha_creacion");

        if (fecha != null) {
            laboratorio.setFechaCreacion(
                    fecha.toLocalDateTime()
            );
        }

        return laboratorio;
    }
}


