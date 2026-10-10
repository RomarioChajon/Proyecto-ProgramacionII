package umg.sigelab.integration.facial;

/** Error controlado al comunicarse con el servicio facial local. */
public class FacialClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public FacialClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public FacialClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** true si ni siquiera se pudo hablar con el servicio (apagado, puerto equivocado, etc.). */
    public boolean esSinConexion() {
        return statusCode == 0;
    }
}
