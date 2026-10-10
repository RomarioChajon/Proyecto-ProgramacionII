package umg.sigelab.util;

import umg.sigelab.model.Usuario;

/**
 * Guarda quién tiene la sesión abierta mientras la aplicación está en ejecución.
 *
 * <p>Es una clase estática porque la aplicación de escritorio solo tiene UN usuario conectado a la vez.
 * Las pantallas consultan {@link #getUsuario()} para saber quién es y qué rol tiene.
 * Nunca se guarda la contraseña: el {@link Usuario} que llega aquí ya viene sin hash.</p>
 */
public final class Sesion {

    private static Usuario usuario;

    private Sesion() {
        // Clase de utilidades: no se instancia.
    }

    /** Registra al usuario que acaba de ingresar. */
    public static synchronized void iniciar(Usuario autenticado) {
        usuario = autenticado;
    }

    /** Cierra la sesión actual. */
    public static synchronized void cerrar() {
        usuario = null;
    }

    /** @return el usuario conectado, o null si nadie ha ingresado */
    public static synchronized Usuario getUsuario() {
        return usuario;
    }

    public static synchronized boolean haySesion() {
        return usuario != null;
    }
}
