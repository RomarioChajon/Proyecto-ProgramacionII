package umg.sigelab.model;

import umg.sigelab.enums.EstadoRegistro;
import umg.sigelab.enums.RolUsuario;

import java.time.LocalDateTime;

/**
 * Usuario del sistema (tabla Usuario).
 *
 * <p>Una sola clase para los cuatro roles: el rol es un atributo ({@link RolUsuario}),
 * no una subclase. Ver la explicación en {@link RolUsuario}.</p>
 *
 * <p>Notas de diseño:</p>
 * <ul>
 *   <li>{@code laboratorio} solo se llena cuando el rol es ENCARGADO.</li>
 *   <li>{@code foto} solo se carga cuando se consulta UN usuario por id; en los
 *       listados viene en null para no traer imágenes de todos los registros.</li>
 *   <li>{@code passwordHash} solo lo usa el Service al crear o cambiar la contraseña.
 *       Las consultas normales no lo leen de la base de datos.</li>
 * </ul>
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String passwordHash;
    private String correo;
    private RolUsuario rol;
    private Laboratorio laboratorio;
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
    private LocalDateTime fechaCreacion;
    private byte[] foto;

    public Usuario() {
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }

    public Laboratorio getLaboratorio() { return laboratorio; }
    public void setLaboratorio(Laboratorio laboratorio) { this.laboratorio = laboratorio; }

    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }

    public boolean isActivo() {
        return estado == EstadoRegistro.ACTIVO;
    }

    /** "Nombre Apellido", para mostrar en tablas y mensajes. */
    public String getNombreCompleto() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }

    @Override
    public String toString() {
        // Nunca incluye passwordHash ni foto.
        return "Usuario{id=" + idUsuario + ", usuario='" + nombreUsuario + "', rol=" + rol + ", estado=" + estado + "}";
    }
}
