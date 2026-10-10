package umg.sigelab.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Manejo de fotografías (por ejemplo, la foto de perfil de un usuario).
 *
 * <p>Estrategia: la foto se guarda en la base de datos como JPEG ya reducido
 * (máximo {@value #LADO_MAXIMO} px por lado). Así una foto de 8 MB tomada con
 * el celular termina ocupando ~30-80 KB y la tabla no crece sin control.</p>
 */
public final class ImagenUtils {

    /** Tamaño máximo del archivo original que aceptamos leer (protege la memoria). */
    private static final long BYTES_MAXIMOS_ORIGINAL = 10L * 1024 * 1024;
    /** Lado máximo (ancho o alto) de la foto que se guarda. */
    private static final int LADO_MAXIMO = 400;
    /** Lado mínimo aceptado: una imagen más pequeña se vería pixelada. */
    private static final int LADO_MINIMO = 64;
    /** Calidad JPEG (0.0 - 1.0). */
    private static final float CALIDAD_JPEG = 0.85f;

    private ImagenUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Lee un archivo de imagen (JPG, PNG, GIF, BMP), lo reduce si es necesario y
     * devuelve los bytes de un JPEG listo para guardar en la base de datos.
     *
     * @throws BusinessException si el archivo no existe, es muy grande o no es una imagen válida
     */
    public static byte[] prepararFoto(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            throw new BusinessException("Seleccione un archivo de imagen válido.");
        }
        if (archivo.length() > BYTES_MAXIMOS_ORIGINAL) {
            throw new BusinessException("La imagen pesa más de 10 MB. Elija una imagen más pequeña.");
        }

        BufferedImage original;
        try {
            original = ImageIO.read(archivo);
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer la imagen seleccionada.", e);
        }
        if (original == null) { // ImageIO devuelve null cuando el formato no se reconoce
            throw new BusinessException("El archivo no es una imagen válida. Use JPG, PNG, GIF o BMP.");
        }
        if (original.getWidth() < LADO_MINIMO || original.getHeight() < LADO_MINIMO) {
            throw new BusinessException("La imagen es demasiado pequeña (mínimo " + LADO_MINIMO + "x" + LADO_MINIMO + " px).");
        }

        BufferedImage reducida = reducir(original, LADO_MAXIMO);
        return aJpeg(reducida);
    }

    /**
     * Convierte los bytes guardados en un icono que cabe dentro de ancho x alto
     * manteniendo la proporción. Devuelve null si no hay foto o está dañada.
     */
    public static ImageIcon aIcono(byte[] foto, int ancho, int alto) {
        if (foto == null || foto.length == 0) {
            return null;
        }
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(foto));
            if (imagen == null) {
                return null;
            }
            double escala = Math.min((double) ancho / imagen.getWidth(), (double) alto / imagen.getHeight());
            int nuevoAncho = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
            int nuevoAlto = Math.max(1, (int) Math.round(imagen.getHeight() * escala));
            return new ImageIcon(escalar(imagen, nuevoAncho, nuevoAlto));
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Métodos privados
    // ------------------------------------------------------------------

    /** Reduce la imagen para que ninguno de sus lados supere {@code lado}. Nunca la agranda. */
    private static BufferedImage reducir(BufferedImage imagen, int lado) {
        int mayor = Math.max(imagen.getWidth(), imagen.getHeight());
        if (mayor <= lado) {
            return escalar(imagen, imagen.getWidth(), imagen.getHeight()); // también aplana transparencias
        }
        double escala = (double) lado / mayor;
        return escalar(imagen,
                Math.max(1, (int) Math.round(imagen.getWidth() * escala)),
                Math.max(1, (int) Math.round(imagen.getHeight() * escala)));
    }

    /**
     * Dibuja la imagen en un lienzo RGB de ancho x alto con fondo blanco.
     * El fondo blanco es necesario porque JPEG no soporta transparencia
     * (un PNG transparente saldría con fondo negro).
     */
    private static BufferedImage escalar(BufferedImage origen, int ancho, int alto) {
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.drawImage(origen, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    /** Codifica la imagen como JPEG con la calidad configurada. */
    private static byte[] aJpeg(BufferedImage imagen) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream flujo = new MemoryCacheImageOutputStream(salida)) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
            flujo.flush();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo procesar la imagen seleccionada.", e);
        } finally {
            escritor.dispose();
        }
    }
}
