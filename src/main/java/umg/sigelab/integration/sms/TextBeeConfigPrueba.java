
package umg.sigelab.integration.sms;

public class TextBeeConfigPrueba {

    public static void main(String[] args) {
        String apiKey = System.getenv("TEXTBEE_API_KEY");
        String deviceId = System.getenv("TEXTBEE_DEVICE_ID");

        System.out.println("TEXTBEE_API_KEY configurada: "
                + (apiKey != null && !apiKey.isBlank()));

        System.out.println("TEXTBEE_DEVICE_ID configurado: "
                + (deviceId != null && !deviceId.isBlank()));
    }
}
