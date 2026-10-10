package umg.sigelab.repository;

import umg.sigelab.config.DatabaseConnection;
import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.util.JdbcSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la tabla Usuario.
 *
 * <p>Regla de la capa Repository: aquí solo hay SQL. Las reglas de negocio
 * (¿el correo ya existe?, ¿es el último administrador?) las decide el Service;
 * este repositorio solo ofrece las consultas necesarias para que el Service decida.</p>
 *
 * <p>Seguridad: TODAS las consultas usan {@link PreparedStatement} con parámetros (?),
 * nunca se concatena texto del usuario dentro del SQL (evita inyección SQL).</p>
 *
 * <p>Las actualizaciones son ESTRECHAS a propósito: una sentencia para los datos,
 * otra para el estado. Así un formulario nunca pisa por accidente la contraseña ni la foto.</p>
 */
public class UsuarioRepository {

    /** Columnas comunes. NO incluye password_hash ni foto (se piden aparte cuando hacen falta). */
    private static final String COLUMNAS = """
            u.id_usuario, u.nombre, u.apellido, u.nombre_usuario, u.correo, u.id_rol, u.estado,
            u.fecha_creacion, l.id_laboratorio, l.codigo AS lab_codigo, l.nombre AS lab_nombre
            """;

    private static final String DESDE = """
            FROM dbo.Usuario u
            LEFT JOIN dbo.Laboratorio l ON l.id_laboratorio = u.id_laboratorio
            """;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Busca un usuario por id e INCLUYE su foto (es un solo registro, no pesa).
     */
    public Optional<Usuario> buscarPorId(int idUsuario) {
        String sql = "SELECT " + COLUMNAS + ", u.foto " + DESDE + " WHERE u.id_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            try (ResultSet rs = sentencia.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, true)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar el usuario", e);
        }
    }

    /**
     * Lista usuarios SIN foto, con filtros opcionales.
     *
     * @param texto  si no es null/vacío, filtra por nombre completo, usuario o correo (contiene)
     * @param estado si no es null, filtra por ese estado
     */
    public List<Usuario> listar(String texto, EstadoRegistro estado) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNAS).append(DESDE).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (CONCAT(u.nombre, N' ', u.apellido) LIKE ? OR u.nombre_usuario LIKE ? OR u.correo LIKE ?)");
            String patron = "%" + JdbcSupport.escaparLike(texto.trim()) + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }
        if (estado != null) {
            sql.append(" AND u.estado = ?");
            parametros.add(estado.name());
        }
        sql.append(" ORDER BY u.apellido, u.nombre");

        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                sentencia.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = sentencia.executeQuery()) {
                List<Usuario> usuarios = new ArrayList<>();
                while (rs.next()) {
                    usuarios.add(mapear(rs, false));
                }
                return usuarios;
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los usuarios", e);
        }
    }

    // ------------------------------------------------------------------
    // Consultas de apoyo para las reglas del Service
    // ------------------------------------------------------------------

    /** ¿Existe otro usuario con ese nombre de usuario? (idExcluido = el propio usuario al editar; 0 al crear). */
    public boolean existeNombreUsuario(String nombreUsuario, int idExcluido) {
        return existe("SELECT 1 FROM dbo.Usuario WHERE nombre_usuario = ? AND id_usuario <> ?",
                nombreUsuario, idExcluido);
    }

    /** ¿Existe otro usuario con ese correo? */
    public boolean existeCorreo(String correo, int idExcluido) {
        return existe("SELECT 1 FROM dbo.Usuario WHERE correo = ? AND id_usuario <> ?", correo, idExcluido);
    }

    /** ¿Ya hay OTRO encargado ACTIVO en ese laboratorio? (un laboratorio = un encargado activo) */
    public boolean existeEncargadoActivo(int idLaboratorio, int idExcluido) {
        return existe("SELECT 1 FROM dbo.Usuario WHERE id_rol = ? AND id_laboratorio = ? AND estado = ? "
                        + "AND id_usuario <> ?",
                RolUsuario.ENCARGADO.getId(), idLaboratorio, EstadoRegistro.ACTIVO.name(), idExcluido);
    }

    /**
     * Busca por nombre de usuario (sin distinguir mayúsculas, según la collation de la BD) e INCLUYE
     * {@code password_hash}. Es el ÚNICO método que lee el hash: se usa solo para iniciar sesión.
     * El hash queda en {@code usuario.getPasswordHash()}; no se imprime en toString().
     */
    public Optional<Usuario> buscarParaLogin(String nombreUsuario) {
        String sql = "SELECT " + COLUMNAS + ", u.password_hash " + DESDE + " WHERE u.nombre_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet rs = sentencia.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Usuario usuario = mapear(rs, false);
                usuario.setPasswordHash(rs.getString("password_hash"));
                return Optional.of(usuario);
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar el usuario", e);
        }
    }

    /** Busca por nombre de usuario SIN hash ni foto (para el flujo facial y el reinicio de contraseña). */
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String sql = "SELECT " + COLUMNAS + " " + DESDE + " WHERE u.nombre_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet rs = sentencia.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, false)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar el usuario", e);
        }
    }

    /** Cambia solo la contraseña (hash BCrypt ya calculado por el Service). */
    public boolean actualizarPassword(int idUsuario, String nuevoHash) {
        String sql = "UPDATE dbo.Usuario SET password_hash = ? WHERE id_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nuevoHash);
            sentencia.setInt(2, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("cambiar la contraseña", e);
        }
    }

    /**
     * ÚNICA eliminación física del sistema. Solo se usa para deshacer un auto-registro cuyo
     * rostro no se pudo capturar (la fila acaba de crearse y nadie la referencia todavía).
     * Si la fila ya tiene datos relacionados, SQL Server lo impide (error 547) y el usuario se conserva.
     */
    public boolean eliminarRegistroRecienCreado(int idUsuario) {
        String sql = "DELETE FROM dbo.Usuario WHERE id_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("deshacer el registro", e);
        }
    }

    /** Cantidad de administradores ACTIVO (para no dejar el sistema sin administrador). */
    public int contarAdministradoresActivos() {
        String sql = "SELECT COUNT(*) FROM dbo.Usuario WHERE id_rol = ? AND estado = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, RolUsuario.ADMIN.getId());
            sentencia.setString(2, EstadoRegistro.ACTIVO.name());
            try (ResultSet rs = sentencia.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("contar los administradores", e);
        }
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    /**
     * Inserta un usuario nuevo y devuelve el id generado por SQL Server (IDENTITY).
     * Espera que {@code passwordHash} ya venga cifrado con BCrypt (lo hace el Service).
     */
    public int guardar(Usuario usuario) {
        String sql = """
                INSERT INTO dbo.Usuario
                    (nombre, apellido, nombre_usuario, password_hash, correo, id_rol, id_laboratorio, estado, foto)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, usuario.getNombre());
            sentencia.setString(2, usuario.getApellido());
            sentencia.setString(3, usuario.getNombreUsuario());
            sentencia.setString(4, usuario.getPasswordHash());
            sentencia.setString(5, usuario.getCorreo());
            sentencia.setInt(6, usuario.getRol().getId());
            JdbcSupport.setIntegerNullable(sentencia, 7,
                    usuario.getLaboratorio() == null ? null : usuario.getLaboratorio().getIdLaboratorio());
            sentencia.setString(8, usuario.getEstado().name());
            JdbcSupport.setBytesNullable(sentencia, 9, usuario.getFoto());

            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("SQL Server no devolvió el id del usuario insertado.");
                }
                int id = claves.getInt(1);
                usuario.setIdUsuario(id);
                return id;
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("guardar el usuario", e);
        }
    }

    /**
     * Actualiza los datos del usuario (NO su estado).
     *
     * @param nuevoHash    hash BCrypt de la nueva contraseña, o null para conservar la actual
     * @param actualizarFoto si es true se escribe {@code usuario.getFoto()} (null = quitar la foto);
     *                       si es false la foto guardada no se toca
     * @return true si se actualizó una fila
     */
    public boolean actualizar(Usuario usuario, String nuevoHash, boolean actualizarFoto) {
        // El SQL se arma solo con fragmentos fijos de este método (nunca con texto del usuario).
        StringBuilder sql = new StringBuilder(
                "UPDATE dbo.Usuario SET nombre = ?, apellido = ?, nombre_usuario = ?, correo = ?, id_rol = ?, id_laboratorio = ?");
        if (nuevoHash != null) {
            sql.append(", password_hash = ?");
        }
        if (actualizarFoto) {
            sql.append(", foto = ?");
        }
        sql.append(" WHERE id_usuario = ?");

        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
            int i = 1;
            sentencia.setString(i++, usuario.getNombre());
            sentencia.setString(i++, usuario.getApellido());
            sentencia.setString(i++, usuario.getNombreUsuario());
            sentencia.setString(i++, usuario.getCorreo());
            sentencia.setInt(i++, usuario.getRol().getId());
            JdbcSupport.setIntegerNullable(sentencia, i++,
                    usuario.getLaboratorio() == null ? null : usuario.getLaboratorio().getIdLaboratorio());
            if (nuevoHash != null) {
                sentencia.setString(i++, nuevoHash);
            }
            if (actualizarFoto) {
                JdbcSupport.setBytesNullable(sentencia, i++, usuario.getFoto());
            }
            sentencia.setInt(i, usuario.getIdUsuario());
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("actualizar el usuario", e);
        }
    }

    /** Cambia solo el estado (baja/alta lógica). Los usuarios NUNCA se eliminan físicamente. */
    public boolean cambiarEstado(int idUsuario, EstadoRegistro nuevoEstado) {
        String sql = "UPDATE dbo.Usuario SET estado = ? WHERE id_usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nuevoEstado.name());
            sentencia.setInt(2, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw JdbcSupport.error("cambiar el estado del usuario", e);
        }
    }

    // ------------------------------------------------------------------
    // Métodos privados
    // ------------------------------------------------------------------

    /** Ejecuta un "SELECT 1 ..." y responde si devolvió alguna fila. Los parámetros van en orden. */
    private boolean existe(String sql, Object... parametros) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = sentencia.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("verificar los datos del usuario", e);
        }
    }

    /** Convierte la fila actual en un Usuario. {@code conFoto} indica si la consulta trae la columna foto. */
    private Usuario mapear(ResultSet rs, boolean conFoto) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellido(rs.getString("apellido"));
        usuario.setNombreUsuario(rs.getString("nombre_usuario"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setRol(RolUsuario.deId(rs.getInt("id_rol")));
        usuario.setEstado(EstadoRegistro.valueOf(rs.getString("estado")));
        usuario.setFechaCreacion(JdbcSupport.localDateTime(rs, "fecha_creacion"));

        Integer idLaboratorio = JdbcSupport.integerNullable(rs, "id_laboratorio");
        if (idLaboratorio != null) { // solo el ENCARGADO tiene laboratorio
            Laboratorio laboratorio = new Laboratorio();
            laboratorio.setIdLaboratorio(idLaboratorio);
            laboratorio.setCodigo(rs.getString("lab_codigo"));
            laboratorio.setNombre(rs.getString("lab_nombre"));
            usuario.setLaboratorio(laboratorio);
        }
        if (conFoto) {
            usuario.setFoto(rs.getBytes("foto"));
        }
        return usuario;
    }
}
