
package umg.sigelab.integration.sms;

public class TextBeeSmsPrueba {

    public static void main(String[] args) {
        TextBeeSmsService servicio = new TextBeeSmsService();

        String telefono = "+50255638520";
        String mensaje = "Prueba de TextBee desde SIGELAB.";

        servicio.enviarSms(telefono, mensaje);
    }
}
