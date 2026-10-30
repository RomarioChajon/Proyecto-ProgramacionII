package umg.sigelab.view;

import com.google.zxing.NotFoundException;
import umg.sigelab.integration.QrCamaraClient;
import umg.sigelab.qr.QrLector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Campo para identificar un equipo por su código QR: botón "Leer QR (cámara)", botón "Imagen QR..."
 * (cargar el PNG del QR) y caja de texto (un lector de códigos USB escribe aquí y envía Enter).
 *
 * <p>Cualquiera de las tres formas llama a {@code alLeer} con el CÓDIGO DE INVENTARIO ya extraído; la pantalla
 * que lo usa llena sus campos. El QR solo identifica al equipo: no cambia estados (RN-19).</p>
 */
class QrCampo extends JPanel {

    private final JTextField txtCodigo = new JTextField(14);
    private final JButton btnCamara = new JButton("Leer QR (cámara)");
    private final JButton btnImagen = new JButton("Imagen QR...");
    private final JButton btnBuscar = new JButton("Buscar");
    private final Consumer<String> alLeer;

    QrCampo(String etiqueta, Consumer<String> alLeer) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));
        this.alLeer = alLeer;
        setBackground(Tablas.BLANCO_PURO);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(Tablas.etiqueta(etiqueta));
        Tablas.estilizarCampo(txtCodigo);
        Tablas.estilizarBotonPrimario(btnBuscar);
        Tablas.estilizarBotonPrimario(btnCamara);
        Tablas.estilizarBotonSuave(btnImagen);
        add(txtCodigo);
        add(btnBuscar);
        add(btnCamara);
        add(btnImagen);

        btnBuscar.addActionListener(e -> enviar(txtCodigo.getText()));
        txtCodigo.addActionListener(e -> enviar(txtCodigo.getText()));
        btnCamara.addActionListener(e -> leerConCamara());
        btnImagen.addActionListener(e -> leerDeImagen());
        btnCamara.setToolTipText("Abre la cámara y lee el QR del equipo (requiere el servicio facial/QR iniciado).");
    }

    String getCodigo() {
        return txtCodigo.getText().trim();
    }

    void limpiar() {
        txtCodigo.setText("");
        txtCodigo.requestFocusInWindow();
    }

    private void enviar(String texto) {
        String codigo = QrLector.extraerCodigo(texto);
        if (codigo.isBlank()) {
            Mensajes.advertir(this, "Código QR", "Escriba o lea el código del equipo.");
            return;
        }
        txtCodigo.setText(codigo);
        alLeer.accept(codigo);
    }

    private void leerConCamara() {
        btnCamara.setEnabled(false);
        btnCamara.setText("Leyendo...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new QrCamaraClient().leer(30);
            }

            @Override
            protected void done() {
                btnCamara.setEnabled(true);
                btnCamara.setText("Leer QR (cámara)");
                try {
                    enviar(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    Mensajes.advertir(QrCampo.this, "Lector de QR",
                            causa instanceof IOException ? causa.getMessage() : "No se pudo leer el código QR.");
                }
            }
        }.execute();
    }

    private void leerDeImagen() {
        JFileChooser selector = new JFileChooser(new File("qr-equipos").isDirectory() ? new File("qr-equipos") : null);
        selector.setDialogTitle("Seleccione la imagen del código QR");
        selector.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            enviar(QrLector.leerImagen(selector.getSelectedFile()));
        } catch (NotFoundException e) {
            Mensajes.advertir(this, "Lector de QR", "No se encontró un código QR en la imagen.");
        } catch (IOException e) {
            Mensajes.advertir(this, "Lector de QR", "No se pudo leer la imagen: " + e.getMessage());
        }
    }
}
