package edo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Herramienta de desarrollo: abre la aplicación, recorre sus pestañas y
 * guarda capturas PNG en la carpeta indicada.
 *
 * No forma parte de la aplicación que se entrega al usuario; sirve para
 * generar las evidencias del reporte de forma reproducible:
 *
 *   javac --release 21 -cp build/classes -d build/tools tools/Capturas.java
 *   java -cp build/classes:build/tools edo.Capturas capturas_java
 */
public final class Capturas {

    private Capturas() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "capturas_java");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear " + dir.getAbsolutePath());
        }
        System.out.println("Carpeta de salida: " + dir.getAbsolutePath());

        VentanaPrincipal v = enEdt(() -> {
            VentanaPrincipal w = new VentanaPrincipal();
            w.setSize(1200, 760);
            w.setLocation(40, 40);
            w.setVisible(true);
            return w;
        });
        esperar(1600);

        // 1. Ventana completa con el ejemplo del cuaderno
        capturar(v, new File(dir, "01_ventana_grafica.png"));

        // 2. Tabla de iteraciones
        pestana(v, 1);
        capturar(v, new File(dir, "02_tabla_iteraciones.png"));

        // 3. Paso a paso en i = 0 (el del cuaderno)
        pestana(v, 2);
        paso(v, 0);
        capturar(v, new File(dir, "03_paso_a_paso.png"));

        // 4. Convergencia
        pestana(v, 3);
        botonConvergencia(v);
        esperar(900);
        capturar(v, new File(dir, "04_convergencia.png"));

        // 5. Teoría y ayuda
        pestana(v, 4);
        capturar(v, new File(dir, "05_teoria_ayuda.png"));

        // 6. Gráfica con el cuadro de valores del ratón
        pestana(v, 0);
        hover(v);
        capturar(v, new File(dir, "06_grafica_hover.png"));

        // 7. Mensaje de error de validación
        campoTexto(v, "txtEcuacion").setText("x +");
        resolverSinDialogo(v);
        esperar(400);
        capturar(v, new File(dir, "07_error_validacion.png"));

        // 8-10. Otros modelos del catálogo
        ejemplo(v, 5);   // oscilante
        capturar(v, new File(dir, "08_ejemplo_oscilante.png"));
        ejemplo(v, 3);   // logístico
        capturar(v, new File(dir, "09_ejemplo_logistico.png"));
        ejemplo(v, 1);   // enfriamiento de Newton
        capturar(v, new File(dir, "10_ejemplo_enfriamiento.png"));

        // 11. Efecto de un h grande (Euler se separa)
        ejemplo(v, 0);
        campoTexto(v, "txtH").setText("0.5");
        resolverSinDialogo(v);
        esperar(400);
        capturar(v, new File(dir, "11_h_grande.png"));

        enEdt(() -> {
            v.dispose();
            return null;
        });
        System.out.println("Listo.");
    }

    // ------------------------------------------------------------------

    private static void capturar(JFrame v, File out) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComponent c = (JComponent) v.getContentPane();
            int w = Math.max(1, c.getWidth());
            int h = Math.max(1, c.getHeight());
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                c.printAll(g);
            } finally {
                g.dispose();
            }
            try {
                ImageIO.write(img, "png", out);
                System.out.println("  " + out.getName());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    private static void pestana(VentanaPrincipal v, int i) throws Exception {
        JTabbedPane tabs = (JTabbedPane) campo(v, "tabs");
        SwingUtilities.invokeAndWait(() -> {
            tabs.setSelectedIndex(i);
            tabs.revalidate();
            tabs.repaint();
        });
        esperar(250);
    }

    private static void paso(VentanaPrincipal v, int i) throws Exception {
        javax.swing.JSpinner sp = (javax.swing.JSpinner) campo(v, "spnPaso");
        SwingUtilities.invokeAndWait(() -> sp.setValue(i));
        esperar(150);
    }

    @SuppressWarnings("unchecked")
    private static void ejemplo(VentanaPrincipal v, int i) throws Exception {
        JComboBox<Ejemplo> cb = (JComboBox<Ejemplo>) campo(v, "cmbEjemplos");
        SwingUtilities.invokeAndWait(() -> cb.setSelectedIndex(i));
        esperar(700);
    }

    private static JTextField campoTexto(VentanaPrincipal v, String nombre) throws Exception {
        return (JTextField) campo(v, nombre);
    }

    private static void resolverSinDialogo(VentanaPrincipal v) throws Exception {
        Method m = VentanaPrincipal.class.getDeclaredMethod("resolver", boolean.class);
        m.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                m.invoke(v, false);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    private static void botonConvergencia(VentanaPrincipal v) throws Exception {
        javax.swing.JButton b = (javax.swing.JButton) campo(v, "btnConvergencia");
        SwingUtilities.invokeAndWait(b::doClick);
    }

    private static void hover(VentanaPrincipal v) throws Exception {
        JPanel g = (JPanel) campo(v, "grafica");
        SwingUtilities.invokeAndWait(() -> {
            int x = g.getWidth() / 2;
            int y = g.getHeight() / 2;
            g.dispatchEvent(new MouseEvent(g, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, x, y, 0, false));
        });
        esperar(200);
    }

    private static Object campo(Object obj, String nombre) throws Exception {
        Field f = obj.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static <T> T enEdt(java.util.concurrent.Callable<T> c) throws Exception {
        final Object[] res = new Object[1];
        final Exception[] err = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                res[0] = c.call();
            } catch (Exception ex) {
                err[0] = ex;
            }
        });
        if (err[0] != null) {
            throw err[0];
        }
        @SuppressWarnings("unchecked")
        T t = (T) res[0];
        return t;
    }

    private static void esperar(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
