package umg.sigelab.model;

import java.time.LocalDateTime;

/** Un registro de la tabla Auditoria para mostrar en pantalla. */
public record FilaAuditoria(long idAuditoria, LocalDateTime fechaHora, String usuario, String laboratorio,
                            String accion, String entidad, Integer idRegistro, String descripcion) {
}
