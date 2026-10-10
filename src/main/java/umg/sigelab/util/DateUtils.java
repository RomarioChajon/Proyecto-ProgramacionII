package umg.sigelab.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Conversiones y formatos de fecha.
 *
 * <ul>
 *   <li>El MODELO usa {@code java.time} (LocalDate / LocalDateTime).</li>
 *   <li>JCalendar (selector de fechas de Swing) usa {@code java.util.Date}.</li>
 *   <li>La VISTA muestra fechas con formato día/mes/año.</li>
 * </ul>
 */
public final class DateUtils {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {
        // Clase de utilidades: no se instancia.
    }

    /** Date (Swing/JCalendar) -> LocalDate (modelo). Acepta null. */
    public static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** LocalDate (modelo) -> Date (Swing/JCalendar). Acepta null. */
    public static Date toDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }

    /** Devuelve "dd/MM/yyyy", o cadena vacía si la fecha es null. */
    public static String formatear(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    /** Devuelve "dd/MM/yyyy HH:mm", o cadena vacía si la fecha es null. */
    public static String formatear(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(FECHA_HORA);
    }
}
