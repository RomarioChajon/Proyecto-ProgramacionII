package umg.sigelab.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lectura de códigos QR de equipos. El QR solo IDENTIFICA al equipo (RN-19): del texto leído se obtiene
 * el código de inventario y con él se consulta la base de datos; el QR nunca cambia estados por sí solo.
 */
public final class QrLector {

    private static final Pattern CODIGO_EN_TEXTO =
            Pattern.compile("C[oó]digo de inventario:\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    private QrLector() {
        // Clase de utilidades: no se instancia.
    }

    /** Lee el QR de una imagen (PNG/JPG). Devuelve el texto completo del QR. */
    public static String leerImagen(File archivo) throws IOException, NotFoundException {
        BufferedImage imagen = ImageIO.read(archivo);
        if (imagen == null) {
            throw new IOException("El archivo no es una imagen válida.");
        }
        LuminanceSource fuente = new BufferedImageLuminanceSource(imagen);
        BinaryBitmap mapa = new BinaryBitmap(new HybridBinarizer(fuente));
        Map<DecodeHintType, Object> pistas = new EnumMap<>(DecodeHintType.class);
        pistas.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        pistas.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result resultado = new MultiFormatReader().decode(mapa, pistas);
        return resultado.getText();
    }

    /**
     * Obtiene el código de inventario a partir de lo que trae el QR: si es el texto informativo de SIGELAB
     * toma la línea "Código de inventario: X"; si es solo el código, lo devuelve tal cual.
     */
    public static String extraerCodigo(String textoQr) {
        if (textoQr == null) {
            return "";
        }
        Matcher m = CODIGO_EN_TEXTO.matcher(textoQr);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }
        return textoQr.trim().toUpperCase();
    }
}
