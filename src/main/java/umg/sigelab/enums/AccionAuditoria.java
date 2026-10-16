package umg.sigelab.enums;

/**
 * Acciones que se registran en la tabla Auditoria (columna accion).
 * El valor guardado es {@link #name()}; así la base de datos nunca recibe textos escritos de formas distintas.
 */
public enum AccionAuditoria {
    LOGIN_EXITOSO,
    LOGIN_FALLIDO,
    PRESTAMO_CREADO,
    PRESTAMO_DESPACHADO,
    PRESTAMO_RECHAZADO,
    PRESTAMO_FINALIZADO,
    EQUIPO_PRESTADO,
    EQUIPO_DEVUELTO,
    EQUIPO_CAMBIO_ESTADO,
    EQUIPO_ENVIADO_MANTENIMIENTO,
    MANTENIMIENTO_FINALIZADO,
    BLOQUEO_CREADO,
    BLOQUEO_LIBERADO,
    USUARIO_DESACTIVADO
}
