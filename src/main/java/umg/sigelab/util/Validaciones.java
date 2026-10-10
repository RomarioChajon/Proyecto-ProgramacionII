package umg.sigelab.util;

import java.util.regex.Pattern;

/**
 * Validaciones de texto reutilizables por los Service.
 * Todas lanzan {@link BusinessException} con un mensaje listo para el usuario.
 */
public final class Validaciones {

    /** Forma básica de correo: algo@dominio.ext (coincide con el CHECK de la tabla Usuario). */
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validaciones() {
        // Clase de utilidades: no se instancia.
    }

    /** Exige texto no vacío. Devuelve el valor SIN espacios en los extremos. */
    public static String requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(mensaje);
        }
        return valor.trim();
    }

    /** Exige que el texto (ya recortado) no supere la longitud de la columna. */
    public static void longitudMaxima(String valor, int maximo, String mensaje) {
        if (valor != null && valor.length() > maximo) {
            throw new BusinessException(mensaje);
        }
    }

    /** Exige un correo con formato válido. */
    public static void correo(String valor, String mensaje) {
        if (valor == null || !CORREO.matcher(valor).matches()) {
            throw new BusinessException(mensaje);
        }
    }

    /** Exige que el texto cumpla una expresión regular. */
    public static void patron(String valor, Pattern patron, String mensaje) {
        if (valor == null || !patron.matcher(valor).matches()) {
            throw new BusinessException(mensaje);
        }
    }

    /** Exige un objeto no nulo (por ejemplo, una selección de ComboBox). */
    public static <T> T seleccion(T valor, String mensaje) {
        if (valor == null) {
            throw new BusinessException(mensaje);
        }
        return valor;
    }
}
