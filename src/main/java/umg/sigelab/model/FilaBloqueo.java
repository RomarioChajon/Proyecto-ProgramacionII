package umg.sigelab.model;

import java.time.LocalDateTime;

/** Un bloqueo de usuario para mostrar en la tabla de bloqueos. */
public record FilaBloqueo(int idBloqueo, int idUsuario, String usuario, int idPrestamo, String motivo,
                          String estado, LocalDateTime fechaBloqueo, LocalDateTime fechaLiberacion) {
}
