package umg.sigelab.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class QrGenerator {

    // CONFIGURACION GENERAL

    private static final int TAMANO_QR = 600;
    private static final int MARGEN_QR = 2;

    private static final String FORMATO_IMAGEN = "PNG";
    private static final String CODIFICACION = "UTF-8";

    // CONSTRUCTOR PRIVADO

    private QrGenerator() {
        // Evitar instancias de esta clase.
    }

    // CONFIGURAR OPCIONES DEL QR

    private static Map<EncodeHintType, Object> configurarOpciones() {

        Map<EncodeHintType, Object> opciones =
                new EnumMap<>(EncodeHintType.class);

        opciones.put(
                EncodeHintType.CHARACTER_SET,
                CODIFICACION
        );

        opciones.put(
                EncodeHintType.ERROR_CORRECTION,
                ErrorCorrectionLevel.H
        );

        opciones.put(
                EncodeHintType.MARGIN,
                MARGEN_QR
        );

        return opciones;
    }

    // VALIDAR EL CONTENIDO

    private static String validarContenido(String contenido) {

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del codigo QR no puede estar vacio."
            );
        }

        return contenido.trim();
    }

    // GENERAR QR CON DIMENSIONES PERSONALIZADAS

    public static BufferedImage generarQR(
            String contenido,
            int ancho,
            int alto
    ) throws WriterException {

        String texto = validarContenido(contenido);

        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException(
                    "Las dimensiones del QR deben ser mayores que cero."
            );
        }

        QRCodeWriter escritor = new QRCodeWriter();

        BitMatrix matriz = escritor.encode(
                texto,
                BarcodeFormat.QR_CODE,
                ancho,
                alto,
                configurarOpciones()
        );

        return MatrixToImageWriter.toBufferedImage(matriz);
    }

    // GENERAR QR CON TAMANO PREDETERMINADO

    public static BufferedImage generarQR(
            String contenido
    ) throws WriterException {

        return generarQR(
                contenido,
                TAMANO_QR,
                TAMANO_QR
        );
    }

    // GUARDAR QR COMO ARCHIVO PNG

    public static void guardarQR(
            String contenido,
            Path destino
    ) throws WriterException, IOException {

        String texto = validarContenido(contenido);

        if (destino == null) {
            throw new IllegalArgumentException(
                    "Debe indicar la ruta donde guardar el QR."
            );
        }

        Path rutaAbsoluta = destino.toAbsolutePath();
        Path carpeta = rutaAbsoluta.getParent();

        // CREAR CARPETA SI NO EXISTE

        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        // GENERAR MATRIZ QR

        QRCodeWriter escritor = new QRCodeWriter();

        BitMatrix matriz = escritor.encode(
                texto,
                BarcodeFormat.QR_CODE,
                TAMANO_QR,
                TAMANO_QR,
                configurarOpciones()
        );

        // GUARDAR IMAGEN

        MatrixToImageWriter.writeToPath(
                matriz,
                FORMATO_IMAGEN,
                rutaAbsoluta
        );
    }
}
