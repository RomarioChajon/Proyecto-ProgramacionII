package umg.sigelab.view;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Paleta de colores, componentes redondeados e íconos vectoriales del tema
 * Celeste y Blanco ("Laboratorios de Computación") de SIGELAB.
 */
public final class TemaCeleste {

    // --- Paleta oficial del diseño celeste y blanco ---
    public static final Color AZUL_PRINCIPAL = new Color(0x2B8FD6);
    public static final Color AZUL_BRILLANTE = new Color(0x0095E8);
    public static final Color AZUL_HOVER = new Color(0x1E78BD);
    public static final Color AZUL_ACENTO = new Color(0x63B3ED);
    public static final Color CELESTE_SUAVE = new Color(0xBEE3F8);
    public static final Color CELESTE_CLARO = new Color(0xE1F2FE);
    public static final Color CELESTE_PESTAÑA = new Color(0xE8F5FE);
    public static final Color FONDO_VENTANA = new Color(0xF0F8FF);
    public static final Color FONDO_GRADIENTE_TOP = new Color(0xD8EEFE);
    public static final Color FONDO_PIE = new Color(0xD2EBFF);
    public static final Color BLANCO_TARJETA = Color.WHITE;
    public static final Color TEXTO_PRINCIPAL = new Color(0x1A202C);
    public static final Color TEXTO_SECUNDARIO = new Color(0x4A5568);
    public static final Color BORDE_CAMPO = new Color(0xB8D8F0);

    // Colores semánticos (estado y tarjetas del dashboard)
    public static final Color VERDE_EXITO = new Color(0x2EAD4B);
    public static final Color VERDE_PASTILLA = new Color(0x8CE09A);
    public static final Color NARANJA_ALERTA = new Color(0xED801E);
    public static final Color AMARILLO_PASTILLA = new Color(0xE5C158);
    public static final Color ROJO_PELIGRO = new Color(0xDC2626);

    public static final String FUENTE_BASE = "Segoe UI";

    private TemaCeleste() {
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FUENTE_BASE, estilo, Math.round(tamano));
    }

    public static void activarAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // ------------------------------------------------------------------
    // Paneles y tarjetas
    // ------------------------------------------------------------------

    /** Panel de fondo principal con degradado suave celeste y blanco. */
    public static JPanel crearPanelFondo(LayoutManager layout) {
        return new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0xDCF0FF),
                        0, getHeight(), FONDO_VENTANA);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    /** Encabezado superior azul (#2B8FD6) al estilo del mockup del administrador. */
    public static JPanel crearEncabezadoAzul(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x2583C9),
                        getWidth(), 0, AZUL_PRINCIPAL);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0x1D6FA5));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    /** Tarjeta blanca con esquinas redondeadas, sombra sutil y borde #BEE3F8. */
    public static JPanel crearTarjetaBlanca(LayoutManager layout, int radio, Insets padding) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int w = getWidth();
                int h = getHeight();
                // Sombra suave inferior
                g2.setColor(new Color(30, 90, 150, 18));
                g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 4, radio, radio));
                g2.setColor(new Color(30, 90, 150, 10));
                g2.fill(new RoundRectangle2D.Float(1, 2, w - 2, h - 2, radio + 2, radio + 2));
                // Cuerpo blanco
                g2.setColor(BLANCO_TARJETA);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 4, h - 5, radio, radio));
                // Borde celeste suave
                g2.setColor(CELESTE_SUAVE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 4, h - 5, radio, radio));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        if (padding != null) {
            p.setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
        }
        return p;
    }

    /** Sub-panel celeste muy suave (#F5FAFF) con borde redondeado #BEE3F8. */
    public static JPanel crearSubTarjetaCeleste(LayoutManager layout, int radio, Insets padding) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0xF5FAFF));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, radio, radio));
                g2.setColor(CELESTE_SUAVE);
                g2.setStroke(new BasicStroke(1.1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, radio, radio));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        if (padding != null) {
            p.setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
        }
        return p;
    }

    // ------------------------------------------------------------------
    // Campos de texto, contraseña y combos
    // ------------------------------------------------------------------

    public static void estilizarCampoTexto(JTextField campo) {
        if (campo == null) {
            return;
        }
        campo.setOpaque(false);
        campo.setFont(fuente(Font.PLAIN, 14f));
        campo.setForeground(TEXTO_PRINCIPAL);
        campo.setCaretColor(AZUL_PRINCIPAL);
        campo.setBorder(new BordeCampoRedondeado(campo));
        campo.setPreferredSize(new Dimension(campo.getPreferredSize().width, 38));
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                campo.repaint();
            }
        });
    }

    public static void estilizarCombo(JComboBox<?> combo) {
        if (combo == null) {
            return;
        }
        combo.setFont(fuente(Font.PLAIN, 13.5f));
        combo.setForeground(TEXTO_PRINCIPAL);
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new BordeRedondeadoSimple(CELESTE_SUAVE, 12, 1.3f),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 36));
    }

    private static final class BordeCampoRedondeado extends AbstractBorder {
        private final JComponent componente;

        BordeCampoRedondeado(JComponent componente) {
            this.componente = componente;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            activarAntialiasing(g2);
            int r = 18;
            boolean foco = componente.hasFocus();
            boolean habilitado = componente.isEnabled() && (!(componente instanceof JTextField tf) || tf.isEditable());

            // Relleno blanco (o celeste tenue si es de solo lectura)
            g2.setColor(habilitado ? Color.WHITE : new Color(0xEDF5FC));
            g2.fill(new RoundRectangle2D.Float(x + 1, y + 1, width - 3, height - 3, r, r));

            // Re-pintamos el contenido del JTextField encima del fondo blanco
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(7, 14, 7, 14);
        }
    }

    /** Crea un JTextField pintado con fondo blanco redondeado y borde azul al enfocar. */
    public static JTextField crearCampoTextoRedondeado() {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int r = 20;
                boolean editable = isEnabled() && isEditable();
                g2.setColor(editable ? Color.WHITE : new Color(0xEBF4FA));
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, r, r));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int r = 20;
                if (hasFocus()) {
                    g2.setColor(new Color(0, 149, 232, 45));
                    g2.setStroke(new BasicStroke(3.2f));
                    g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 4, getHeight() - 4, r, r));
                    g2.setColor(AZUL_BRILLANTE);
                    g2.setStroke(new BasicStroke(1.8f));
                } else {
                    g2.setColor(BORDE_CAMPO);
                    g2.setStroke(new BasicStroke(1.3f));
                }
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 4, getHeight() - 4, r, r));
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setFont(fuente(Font.PLAIN, 14f));
        tf.setForeground(TEXTO_PRINCIPAL);
        tf.setCaretColor(AZUL_PRINCIPAL);
        tf.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        tf.setPreferredSize(new Dimension(180, 38));
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.repaint();
            }
        });
        return tf;
    }

    /** Crea un JPasswordField pintado con fondo blanco redondeado y borde azul al enfocar. */
    public static JPasswordField crearCampoPasswordRedondeado() {
        JPasswordField pf = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int r = 20;
                boolean editable = isEnabled() && isEditable();
                g2.setColor(editable ? Color.WHITE : new Color(0xEBF4FA));
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, r, r));
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int r = 20;
                if (hasFocus()) {
                    g2.setColor(new Color(0, 149, 232, 45));
                    g2.setStroke(new BasicStroke(3.2f));
                    g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 4, getHeight() - 4, r, r));
                    g2.setColor(AZUL_BRILLANTE);
                    g2.setStroke(new BasicStroke(1.8f));
                } else {
                    g2.setColor(BORDE_CAMPO);
                    g2.setStroke(new BasicStroke(1.3f));
                }
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 4, getHeight() - 4, r, r));
                g2.dispose();
            }
        };
        pf.setOpaque(false);
        pf.setFont(fuente(Font.PLAIN, 14f));
        pf.setForeground(TEXTO_PRINCIPAL);
        pf.setCaretColor(AZUL_PRINCIPAL);
        pf.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        pf.setPreferredSize(new Dimension(180, 38));
        pf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                pf.repaint();
            }
        });
        return pf;
    }

    public static final class BordeRedondeadoSimple extends AbstractBorder {
        private final Color color;
        private final int radio;
        private final float grosor;

        public BordeRedondeadoSimple(Color color, int radio, float grosor) {
            this.color = color;
            this.radio = radio;
            this.grosor = grosor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            activarAntialiasing(g2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(grosor));
            g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, width - 3, height - 3, radio, radio));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 8, 4, 8);
        }
    }

    // ------------------------------------------------------------------
    // Botones redondeados personalizados (funcionan igual en cualquier Look & Feel)
    // ------------------------------------------------------------------

    /** Botón principal azul celeste brillante (#0095E8 / #2B8FD6) con texto blanco en negrita. */
    public static void estilizarBotonPrimario(JButton boton, int radio, float tamanoFuente) {
        configurarBotonPintado(boton,
                AZUL_BRILLANTE, AZUL_PRINCIPAL,
                AZUL_HOVER, new Color(0x16649E),
                Color.WHITE, new Color(0x1778B8),
                radio, Font.BOLD, tamanoFuente);
    }

    /** Botón secundario celeste suave con degradado (#E6F4FE -> #C5E5FD) como "Servicio facial..." en el mockup. */
    public static void estilizarBotonCelesteSuave(JButton boton, int radio, float tamanoFuente) {
        configurarBotonPintado(boton,
                new Color(0xE8F5FE), new Color(0xC2E4FC),
                new Color(0xD4ECFE), new Color(0xAEDBFA),
                TEXTO_PRINCIPAL, new Color(0x8CC2EB),
                radio, Font.PLAIN, tamanoFuente);
    }

    /** Botón claro ("Salir" / "Cerrar") con fondo blanco-celeste (#F4FAFF -> #E3F1FD). */
    public static void estilizarBotonClaro(JButton boton, int radio, float tamanoFuente) {
        configurarBotonPintado(boton,
                new Color(0xF5FAFF), new Color(0xE3F2FD),
                new Color(0xE4F2FE), new Color(0xCFE8FC),
                TEXTO_PRINCIPAL, new Color(0xA8CFEA),
                radio, Font.PLAIN, tamanoFuente);
    }

    /** Botón de peligro / detener (rojo suave o intenso). */
    public static void estilizarBotonPeligro(JButton boton, int radio, float tamanoFuente) {
        configurarBotonPintado(boton,
                new Color(0xFEE2E2), new Color(0xFECACA),
                new Color(0xFCA5A5), new Color(0xF87171),
                new Color(0x991B1B), new Color(0xF87171),
                radio, Font.BOLD, tamanoFuente);
    }

    /** Botón con borde blanco sobre el encabezado azul ("Cerrar sesión" en el mockup del menú admin). */
    public static void estilizarBotonEncabezado(JButton boton) {
        if (boton == null) {
            return;
        }
        boton.setFont(fuente(Font.PLAIN, 13f));
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setOpaque(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        boton.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int w = c.getWidth();
                int h = c.getHeight();
                int r = 14;
                if (b.getModel().isPressed()) {
                    g2.setColor(new Color(255, 255, 255, 55));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, r, r));
                } else if (b.getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 35));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, r, r));
                } else {
                    g2.setColor(new Color(255, 255, 255, 18));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, r, r));
                }
                g2.setColor(new Color(255, 255, 255, 210));
                g2.setStroke(new BasicStroke(1.3f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, r, r));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private static void configurarBotonPintado(
            JButton boton,
            Color arribaNormal, Color abajoNormal,
            Color arribaHover, Color abajoHover,
            Color colorTexto, Color colorBorde,
            int radio, int estiloFuente, float tamanoFuente) {
        if (boton == null) {
            return;
        }
        boton.setFont(fuente(estiloFuente, tamanoFuente));
        boton.setForeground(colorTexto);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setOpaque(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        boton.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int w = c.getWidth();
                int h = c.getHeight();

                if (!b.isEnabled()) {
                    g2.setColor(new Color(0xE2E8F0));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, radio, radio));
                    g2.setColor(new Color(0xCBD5E1));
                    g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, radio, radio));
                    b.setForeground(new Color(0x94A3B8));
                } else {
                    b.setForeground(colorTexto);
                    boolean hover = b.getModel().isRollover() || b.getModel().isPressed();
                    Color top = hover ? arribaHover : arribaNormal;
                    Color bot = hover ? abajoHover : abajoNormal;
                    g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, radio, radio));
                    g2.setColor(colorBorde);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, radio, radio));
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    /**
     * Convierte un JButton de módulo del Panel Principal en una tarjeta blanca redondeada
     * con ícono celeste a la izquierda y texto a la derecha (idéntico al mockup).
     */
    public static void estilizarBotonModuloAdmin(JButton boton, String texto, TipoIcono icono) {
        if (boton == null) {
            return;
        }
        boton.setText(texto);
        boton.setIcon(crearIconoModulo(icono, 48));
        boton.setHorizontalAlignment(JButton.LEFT);
        boton.setIconTextGap(16);
        boton.setFont(fuente(Font.PLAIN, 17f));
        boton.setForeground(TEXTO_PRINCIPAL);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setOpaque(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        boton.setPreferredSize(new Dimension(225, 78));
        boton.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                int w = c.getWidth();
                int h = c.getHeight();
                int r = 20;
                boolean hover = b.getModel().isRollover();
                boolean pressed = b.getModel().isPressed();

                // Sombra
                g2.setColor(new Color(25, 85, 145, hover ? 28 : 16));
                g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 5, r, r));

                // Fondo de tarjeta
                g2.setColor(pressed ? new Color(0xE1F2FE) : (hover ? new Color(0xF5FAFF) : Color.WHITE));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 4, h - 6, r, r));

                // Borde
                g2.setColor(hover ? AZUL_ACENTO : CELESTE_SUAVE);
                g2.setStroke(new BasicStroke(hover ? 1.6f : 1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 4, h - 6, r, r));
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    // ------------------------------------------------------------------
    // Pestañas (JTabbedPane) estilo mockup Login
    // ------------------------------------------------------------------

    public static void estilizarPestanasLogin(JTabbedPane tabs) {
        if (tabs == null) {
            return;
        }
        tabs.setFont(fuente(Font.PLAIN, 14f));
        tabs.setOpaque(false);
        tabs.setBackground(Color.WHITE);
        tabs.setForeground(TEXTO_PRINCIPAL);
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabInsets = new Insets(11, 20, 11, 20);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                int totalWidth = tabPane.getWidth();
                int count = tabPane.getTabCount();
                if (totalWidth > 100 && count > 0) {
                    // Distribuir proporcionalmente para llenar toda la cabecera de la tarjeta
                    int base = super.calculateTabWidth(tabPlacement, tabIndex, metrics);
                    int sum = 0;
                    for (int i = 0; i < count; i++) {
                        sum += super.calculateTabWidth(tabPlacement, i, metrics);
                    }
                    if (sum > 0 && totalWidth > sum) {
                        return Math.round((float) base * totalWidth / sum) - 1;
                    }
                }
                return super.calculateTabWidth(tabPlacement, tabIndex, metrics);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                if (isSelected) {
                    g2.setPaint(new GradientPaint(x, y, AZUL_BRILLANTE, x, y + h, AZUL_PRINCIPAL));
                } else {
                    g2.setColor(CELESTE_PESTAÑA);
                }
                g2.fill(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h + 8, 14, 14));
                g2.fillRect(x + 1, y + h - 6, w - 2, 7);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                g2.setColor(isSelected ? AZUL_PRINCIPAL : CELESTE_SUAVE);
                g2.setStroke(new BasicStroke(1.1f));
                g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h + 6, 14, 14));
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, java.awt.Rectangle textRect, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                Font f = fuente(isSelected ? Font.BOLD : Font.PLAIN, 14f);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(isSelected ? Color.WHITE : TEXTO_PRINCIPAL);
                int tx = textRect.x + (textRect.width - fm.stringWidth(title)) / 2;
                int ty = textRect.y + fm.getAscent();
                g2.drawString(title, tx, ty);
                g2.dispose();
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, java.awt.Rectangle[] rects,
                                               int tabIndex, java.awt.Rectangle iconRect, java.awt.Rectangle textRect,
                                               boolean isSelected) {
                // Sin recuadro punteado de foco
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(CELESTE_SUAVE);
                int top = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g2.drawLine(0, top, tabPane.getWidth(), top);
                g2.dispose();
            }
        });
    }

    // ------------------------------------------------------------------
    // Tablas (JTable) estilo mockup Préstamo de Equipos / Usuarios
    // ------------------------------------------------------------------

    public static void estilizarTabla(JTable tabla, JScrollPane scroll, int columnaEstado) {
        if (tabla == null) {
            return;
        }
        tabla.setFont(fuente(Font.PLAIN, 13.5f));
        tabla.setForeground(TEXTO_PRINCIPAL);
        tabla.setRowHeight(34);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(0xDCEBFA));
        tabla.setSelectionBackground(new Color(0xB8E0FC));
        tabla.setSelectionForeground(TEXTO_PRINCIPAL);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setFillsViewportHeight(true);

        JTableHeader header = tabla.getTableHeader();
        if (header != null) {
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
            header.setReorderingAllowed(false);
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object val, boolean sel,
                                                               boolean foc, int row, int col) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    l.setFont(fuente(Font.BOLD, 13.5f));
                    l.setBackground(Color.WHITE);
                    l.setForeground(TEXTO_PRINCIPAL);
                    l.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 2, 1, CELESTE_SUAVE),
                            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                    return l;
                }
            });
        }

        DefaultTableCellRenderer celdaRenderer = new DefaultTableCellRenderer() {
            private String textoPastilla = null;
            private Color colorPastilla = null;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel,
                                                           boolean foc, int row, int col) {
                textoPastilla = null;
                colorPastilla = null;
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                l.setFont(fuente(Font.PLAIN, 13.5f));
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

                Color bg = sel ? new Color(0xBEE3F8) : (row % 2 == 0 ? new Color(0xDCF0FF) : Color.WHITE);
                l.setBackground(bg);

                int filaModelo = t.convertRowIndexToModel(row);
                boolean filaInactiva = false;
                if (columnaEstado >= 0 && columnaEstado < t.getModel().getColumnCount()) {
                    Object estObj = t.getModel().getValueAt(filaModelo, columnaEstado);
                    String estStr = estObj == null ? "" : estObj.toString();
                    filaInactiva = "Inactivo".equalsIgnoreCase(estStr);
                }

                l.setForeground(filaInactiva && !sel ? new Color(0x64748B) : TEXTO_PRINCIPAL);

                if (col == columnaEstado && val != null) {
                    textoPastilla = val.toString();
                    boolean activo = "Activo".equalsIgnoreCase(textoPastilla)
                            || "Disponible".equalsIgnoreCase(textoPastilla);
                    colorPastilla = activo ? VERDE_PASTILLA : new Color(0xFCD34D);
                    l.setText("");
                }
                return l;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (textoPastilla != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    activarAntialiasing(g2);
                    g2.setFont(fuente(Font.PLAIN, 12.5f));
                    FontMetrics fm = g2.getFontMetrics();
                    int pw = Math.max(78, fm.stringWidth(textoPastilla) + 24);
                    int ph = 24;
                    int px = 12;
                    int py = (getHeight() - ph) / 2;
                    g2.setColor(colorPastilla);
                    g2.fill(new RoundRectangle2D.Float(px, py, pw, ph, 22, 22));
                    g2.setColor(TEXTO_PRINCIPAL);
                    int tx = px + (pw - fm.stringWidth(textoPastilla)) / 2;
                    int ty = py + (ph - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(textoPastilla, tx, ty);
                    g2.dispose();
                }
            }
        };
        tabla.setDefaultRenderer(Object.class, celdaRenderer);
        tabla.setDefaultRenderer(Integer.class, celdaRenderer);

        if (scroll != null) {
            scroll.setBorder(new BordeRedondeadoSimple(CELESTE_SUAVE, 14, 1.2f));
            scroll.getViewport().setBackground(Color.WHITE);
        }
    }

    // ------------------------------------------------------------------
    // Íconos vectoriales personalizados (sin archivos externos)
    // ------------------------------------------------------------------

    public enum TipoIcono {
        MONITOR_FACIAL,
        USUARIOS,
        LABORATORIOS,
        CATEGORIAS,
        PRODUCTOS,
        EQUIPOS,
        PRESTAMOS,
        DEVOLUCIONES,
        MANTENIMIENTO,
        BLOQUEOS,
        REPORTES,
        AUDITORIA,
        SERVICIO_FACIAL,
        FLECHAS_PRESTAMO,
        ALERTA_CIRCULO,
        CRUZ_BAJA,
        BICHO_DANADO,
        AVATAR_BLANCO
    }

    /** Ícono grande del monitor con reconocimiento facial (encabezado de LoginForm). */
    public static Icon crearIconoMonitorFacial(int ancho, int alto) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                g2.translate(x, y);

                int mw = ancho - 8;
                int mh = alto - 18;

                // Base / soporte del monitor
                g2.setColor(new Color(0xCBD5E1));
                g2.fillRect(mw / 2 - 4, mh + 2, 16, 10);
                g2.setColor(new Color(0xE2E8F0));
                g2.fill(new RoundRectangle2D.Float(mw / 2 - 16, mh + 10, 40, 5, 4, 4));
                g2.setColor(new Color(0x94A3B8));
                g2.drawLine(mw / 2 - 14, mh + 15, mw / 2 + 22, mh + 15);

                // Carcasa exterior del monitor
                g2.setColor(new Color(0x234E70));
                g2.fill(new RoundRectangle2D.Float(4, 2, mw, mh, 10, 10));

                // Franja inferior plateada del monitor
                g2.setColor(new Color(0xDCE6F0));
                g2.fill(new RoundRectangle2D.Float(4, mh - 7, mw, 9, 6, 6));
                g2.setColor(new Color(0x334155));
                g2.fill(new Ellipse2D.Float(4 + mw / 2f - 2f, mh - 4.5f, 4, 4));

                // Pantalla azul celeste con degradado
                g2.setPaint(new GradientPaint(6, 5, new Color(0x2B8FD6), 6 + mw - 6, mh - 9, new Color(0x63B3ED)));
                g2.fillRect(7, 5, mw - 6, mh - 13);

                // Esquinas de escaneo [ ]
                g2.setColor(new Color(0xE0F2FE));
                g2.setStroke(new BasicStroke(1.8f));
                int bx = 18;
                int by = 10;
                int bw = mw - 28;
                int bh = mh - 23;
                int len = 7;
                // Sup-Izq
                g2.drawLine(bx, by, bx + len, by);
                g2.drawLine(bx, by, bx, by + len);
                // Sup-Der
                g2.drawLine(bx + bw - len, by, bx + bw, by);
                g2.drawLine(bx + bw, by, bx + bw, by + len);
                // Inf-Izq
                g2.drawLine(bx, by + bh - len, bx, by + bh);
                g2.drawLine(bx, by + bh, bx + len, by + bh);
                // Inf-Der
                g2.drawLine(bx + bw - len, by + bh, bx + bw, by + bh);
                g2.drawLine(bx + bw, by + bh - len, bx + bw, by + bh);

                // Silueta de rostro en el centro
                int cx = bx + bw / 2;
                int cy = by + bh / 2;
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new Ellipse2D.Float(cx - 8, cy - 11, 16, 20));
                // Hombros sutiles
                g2.draw(new Arc2D.Float(cx - 13, cy + 8, 26, 12, 15, 150, Arc2D.OPEN));

                // Línea horizontal de escaneo facial
                g2.setColor(new Color(0xBAE6FD));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(bx - 3, cy, bx + bw + 3, cy);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return ancho;
            }

            @Override
            public int getIconHeight() {
                return alto;
            }
        };
    }

    /** Ícono de módulo (recuadro redondeado celeste suave #DCF0FF con pictograma #2B8FD6). */
    public static Icon crearIconoModulo(TipoIcono tipo, int lado) {
        return crearIconoCuadrado(tipo, lado, new Color(0xD8EEFE), AZUL_PRINCIPAL);
    }

    /** Ícono de tarjeta del Dashboard (recuadro redondeado de color sólido con pictograma blanco). */
    public static Icon crearIconoDashboard(TipoIcono tipo, int lado, Color colorFondo) {
        return crearIconoCuadrado(tipo, lado, colorFondo, Color.WHITE);
    }

    public static Icon crearIconoCuadrado(TipoIcono tipo, int lado, Color fondo, Color trazo) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                g2.translate(x, y);

                if (fondo != null) {
                    g2.setColor(fondo);
                    g2.fill(new RoundRectangle2D.Float(0, 0, lado, lado, 14, 14));
                }

                g2.setColor(trazo);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int m = lado / 2;

                switch (tipo) {
                    case USUARIOS -> {
                        g2.fill(new Ellipse2D.Float(m - 6, m - 12, 12, 12));
                        g2.fill(new Arc2D.Float(m - 12, m + 2, 24, 18, 0, 180, Arc2D.PIE));
                    }
                    case LABORATORIOS -> {
                        GeneralPath matraz = new GeneralPath();
                        matraz.moveTo(m - 4, m - 12);
                        matraz.lineTo(m + 4, m - 12);
                        matraz.moveTo(m - 3, m - 12);
                        matraz.lineTo(m - 3, m - 4);
                        matraz.lineTo(m - 11, m + 11);
                        matraz.lineTo(m + 11, m + 11);
                        matraz.lineTo(m + 3, m - 4);
                        matraz.lineTo(m + 3, m - 12);
                        g2.draw(matraz);
                        GeneralPath liquido = new GeneralPath();
                        liquido.moveTo(m - 7, m + 4);
                        liquido.lineTo(m + 7, m + 4);
                        liquido.lineTo(m + 10, m + 10);
                        liquido.lineTo(m - 10, m + 10);
                        liquido.closePath();
                        g2.fill(liquido);
                    }
                    case CATEGORIAS -> {
                        GeneralPath carpeta = new GeneralPath();
                        carpeta.moveTo(m - 12, m - 8);
                        carpeta.lineTo(m - 3, m - 8);
                        carpeta.lineTo(m, m - 5);
                        carpeta.lineTo(m + 12, m - 5);
                        carpeta.lineTo(m + 12, m + 10);
                        carpeta.lineTo(m - 12, m + 10);
                        carpeta.closePath();
                        g2.fill(carpeta);
                    }
                    case PRODUCTOS -> {
                        // Cubo isométrico 3D
                        g2.draw(new RoundRectangle2D.Float(m - 10, m - 9, 20, 18, 4, 4));
                        g2.drawLine(m - 10, m - 3, m, m + 2);
                        g2.drawLine(m + 10, m - 3, m, m + 2);
                        g2.drawLine(m, m + 2, m, m + 9);
                    }
                    case EQUIPOS, MONITOR_FACIAL -> {
                        g2.draw(new RoundRectangle2D.Float(m - 12, m - 10, 24, 16, 4, 4));
                        g2.drawLine(m - 4, m + 6, m - 4, m + 11);
                        g2.drawLine(m + 4, m + 6, m + 4, m + 11);
                        g2.drawLine(m - 8, m + 11, m + 8, m + 11);
                    }
                    case PRESTAMOS -> {
                        g2.draw(new RoundRectangle2D.Float(m - 12, m - 6, 16, 13, 3, 3));
                        g2.drawLine(m - 12, m - 2, m + 4, m - 2);
                        // Flecha hacia la derecha
                        g2.drawLine(m, m - 10, m + 11, m - 10);
                        g2.drawLine(m + 7, m - 14, m + 11, m - 10);
                        g2.drawLine(m + 7, m - 6, m + 11, m - 10);
                    }
                    case DEVOLUCIONES -> {
                        // Flecha curva de retorno
                        g2.draw(new Arc2D.Float(m - 9, m - 8, 18, 16, -90, 220, Arc2D.OPEN));
                        g2.drawLine(m - 9, m - 4, m - 4, m - 9);
                        g2.drawLine(m - 9, m - 4, m - 4, m + 1);
                    }
                    case MANTENIMIENTO -> {
                        // Llave y destornillador cruzados
                        g2.draw(new Line2D.Float(m - 9, m - 9, m + 9, m + 9));
                        g2.draw(new Line2D.Float(m + 9, m - 9, m - 9, m + 9));
                        g2.draw(new Ellipse2D.Float(m - 12, m - 12, 7, 7));
                        g2.draw(new Ellipse2D.Float(m + 5, m - 12, 7, 7));
                    }
                    case BLOQUEOS -> {
                        g2.fill(new RoundRectangle2D.Float(m - 10, m - 2, 20, 14, 5, 5));
                        g2.draw(new Arc2D.Float(m - 6, m - 11, 12, 14, 0, 180, Arc2D.OPEN));
                        g2.setColor(fondo != null ? fondo : Color.WHITE);
                        g2.fill(new Ellipse2D.Float(m - 2, m + 3, 4, 4));
                    }
                    case REPORTES, AUDITORIA -> {
                        g2.draw(new RoundRectangle2D.Float(m - 9, m - 11, 18, 22, 4, 4));
                        g2.drawLine(m - 5, m - 4, m + 5, m - 4);
                        g2.drawLine(m - 5, m + 1, m + 5, m + 1);
                        g2.drawLine(m - 5, m + 6, m + 2, m + 6);
                    }
                    case SERVICIO_FACIAL -> {
                        g2.fill(new Ellipse2D.Float(m - 6, m - 11, 12, 12));
                        g2.fill(new Arc2D.Float(m - 11, m + 2, 22, 16, 0, 180, Arc2D.PIE));
                        g2.draw(new RoundRectangle2D.Float(m + 5, m - 3, 6, 6, 2, 2));
                    }
                    case FLECHAS_PRESTAMO -> {
                        // Dos flechas opuestas <->
                        g2.drawLine(m - 9, m - 4, m + 9, m - 4);
                        g2.drawLine(m + 4, m - 8, m + 9, m - 4);
                        g2.drawLine(m + 4, m, m + 9, m - 4);

                        g2.drawLine(m - 9, m + 5, m + 9, m + 5);
                        g2.drawLine(m - 4, m + 1, m - 9, m + 5);
                        g2.drawLine(m - 4, m + 9, m - 9, m + 5);
                    }
                    case ALERTA_CIRCULO -> {
                        g2.draw(new Ellipse2D.Float(m - 10, m - 10, 20, 20));
                        g2.drawLine(m, m - 5, m, m + 2);
                        g2.fill(new Ellipse2D.Float(m - 1.5f, m + 5, 3, 3));
                    }
                    case CRUZ_BAJA -> {
                        g2.fill(new Ellipse2D.Float(m - 10, m - 10, 20, 20));
                        g2.setColor(fondo != null ? fondo : AZUL_PRINCIPAL);
                        g2.drawLine(m - 4, m - 4, m + 4, m + 4);
                        g2.drawLine(m + 4, m - 4, m - 4, m + 4);
                    }
                    case BICHO_DANADO -> {
                        g2.fill(new Ellipse2D.Float(m - 6, m - 6, 12, 12));
                        g2.drawLine(m - 10, m - 8, m + 10, m + 8);
                        g2.drawLine(m - 10, m + 8, m + 10, m - 8);
                        g2.drawLine(m - 11, m, m + 11, m);
                    }
                    case AVATAR_BLANCO -> {
                        g2.setColor(Color.WHITE);
                        g2.fill(new Ellipse2D.Float(2, 2, lado - 4, lado - 4));
                        g2.setColor(AZUL_PRINCIPAL);
                        g2.fill(new Ellipse2D.Float(m - 4, m - 7, 8, 8));
                        g2.fill(new Arc2D.Float(m - 8, m + 2, 16, 12, 0, 180, Arc2D.PIE));
                    }
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return lado;
            }

            @Override
            public int getIconHeight() {
                return lado;
            }
        };
    }

    /** Punto luminoso de estado (verde cuando activo, ámbar en arranque, rojo si detenido). */
    public static Icon crearPuntoEstado(Color colorPrincipal) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                activarAntialiasing(g2);
                g2.translate(x, y);
                // Halo exterior
                g2.setColor(new Color(colorPrincipal.getRed(), colorPrincipal.getGreen(), colorPrincipal.getBlue(), 65));
                g2.fill(new Ellipse2D.Float(0, 0, 18, 18));
                // Círculo interior brillante
                g2.setColor(colorPrincipal);
                g2.fill(new Ellipse2D.Float(3, 3, 12, 12));
                g2.setColor(new Color(255, 255, 255, 130));
                g2.fill(new Ellipse2D.Float(5, 5, 4, 4));
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 18;
            }

            @Override
            public int getIconHeight() {
                return 18;
            }
        };
    }

    public static JLabel crearEtiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.PLAIN, 14f));
        l.setForeground(TEXTO_PRINCIPAL);
        return l;
    }

    public static Border bordeTarjetaRedondeada() {
        return new BordeRedondeadoSimple(CELESTE_SUAVE, 18, 1.2f);
    }
}
