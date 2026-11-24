package edo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

// Aquí dibujamos la gráfica con Euler, RK4 y la solución de referencia
public class PanelGrafica extends JPanel {

    private static final Color C_EULER = new Color(214, 84, 40);
    private static final Color C_RK4 = new Color(30, 100, 200);
    private static final Color C_REF = new Color(120, 200, 140);
    private static final Color C_TEXTO = new Color(40, 40, 40);
    private static final Color C_GRID = new Color(225, 225, 225);

    private static final Stroke S_EULER = new BasicStroke(2f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10f, new float[]{10f, 4f, 2f, 4f}, 0f);
    private static final Stroke S_RK4 = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke S_REF = new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private double[] xs, yEuler, yRK4, yRef;
    private String nombreRef = "Exacta";
    private boolean verEuler = true, verRK4 = true, verRef = true;

    private final Rectangle area = new Rectangle();
    private double xmin, xmax, ymin, ymax;
    private int hoverIdx = -1;

    public PanelGrafica() {
        setBackground(Color.WHITE);
        // Detecta cuándo movemos el mouse encima de la gráfica
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                actualizarHover(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIdx = -1;
                repaint();
            }
        };
        addMouseMotionListener(ma);
        addMouseListener(ma);
    }

    // Recibe los arreglos calculados para dibujarlos
    public void setDatos(double[] xs, double[] yEuler, double[] yRK4, double[] yRef, String nombreRef) {
        this.xs = xs;
        this.yEuler = yEuler;
        this.yRK4 = yRK4;
        this.yRef = yRef;
        this.nombreRef = nombreRef;
        this.hoverIdx = -1;
        repaint();
    }

    // Activa o desactiva qué curvas se muestran
    public void setVisibles(boolean euler, boolean rk4, boolean ref) {
        this.verEuler = euler;
        this.verRK4 = rk4;
        this.verRef = ref;
        repaint();
    }

    public boolean hayDatos() {
        return xs != null && xs.length >= 2;
    }

    // Guarda la gráfica actual como imagen PNG
    public void guardarPng(java.io.File archivo) throws java.io.IOException {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            printAll(g);
        } finally {
            g.dispose();
        }
        if (!javax.imageio.ImageIO.write(img, "png", archivo)) {
            throw new java.io.IOException("No hay un codificador PNG disponible.");
        }
    }

    // Calcula qué punto de la gráfica está bajo el cursor
    private void actualizarHover(int px, int py) {
        if (xs == null || xs.length < 2 || !area.contains(px, py)) {
            if (hoverIdx != -1) {
                hoverIdx = -1;
                repaint();
            }
            return;
        }
        double dx = xs[1] - xs[0];
        if (!(dx > 0)) {
            if (hoverIdx != -1) {
                hoverIdx = -1;
                repaint();
            }
            return;
        }
        double x = xmin + (px - area.x) / (double) area.width * (xmax - xmin);
        int idx = (int) Math.round((x - xs[0]) / dx);
        idx = Math.max(0, Math.min(xs.length - 1, idx));
        if (idx != hoverIdx) {
            hoverIdx = idx;
            repaint();
        }
    }

    // Convierte coordenada matemática x a píxel en pantalla
    private double pantallaX(double x) {
        return area.x + (x - xmin) / (xmax - xmin) * area.width;
    }

    // Convierte coordenada matemática y a píxel en pantalla
    private double pantallaY(double y) {
        return area.y + area.height - (y - ymin) / (ymax - ymin) * area.height;
    }

    private static boolean finito(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    // Calcula los límites de los ejes evitando que valores que divergen aplasten la gráfica
    private void calcularLimites() {
        xmin = xs[0];
        xmax = xs[xs.length - 1];

        double rmin = Double.POSITIVE_INFINITY, rmax = Double.NEGATIVE_INFINITY;
        if (yRef != null) {
            for (double v : yRef) {
                if (finito(v)) {
                    rmin = Math.min(rmin, v);
                    rmax = Math.max(rmax, v);
                }
            }
        }

        double permLo = Double.NEGATIVE_INFINITY, permHi = Double.POSITIVE_INFINITY;
        if (rmin <= rmax) {
            double span = rmax - rmin;
            if (span <= 0) {
                span = Math.max(1, Math.abs(rmax));
            }
            permLo = rmin - 3 * span;
            permHi = rmax + 3 * span;
        }

        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (double[] serie : new double[][]{
            verEuler ? yEuler : null,
            verRK4 ? yRK4 : null,
            verRef ? yRef : null}) {
            if (serie == null) {
                continue;
            }
            for (double v : serie) {
                if (finito(v) && v >= permLo && v <= permHi) {
                    lo = Math.min(lo, v);
                    hi = Math.max(hi, v);
                }
            }
        }

        // Si nada entró en el filtro (por ejemplo todo divergió), usamos los valores finitos visibles
        if (lo > hi) {
            for (double[] serie : new double[][]{
                verEuler ? yEuler : null,
                verRK4 ? yRK4 : null,
                verRef ? yRef : null}) {
                if (serie == null) {
                    continue;
                }
                for (double v : serie) {
                    if (finito(v)) {
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
                }
            }
        }

        if (lo > hi) {
            lo = -1;
            hi = 1;
        }
        if (hi - lo < 1e-12) {
            lo -= 1;
            hi += 1;
        }
        double pad = (hi - lo) * 0.08;
        ymin = lo - pad;
        ymax = hi + pad;
    }

    private static double pasoNice(double rango, int maxTicks) {
        double raw = rango / maxTicks;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double r = raw / mag;
        double nice = r <= 1 ? 1 : r <= 2 ? 2 : r <= 5 ? 5 : 10;
        return nice * mag;
    }

    private static String etiqueta(double v, double paso) {
        if (Math.abs(v) < paso * 1e-9) {
            return "0";
        }
        if (Math.abs(v) >= 1e6 || paso < 1e-4) {
            return String.format(Locale.US, "%.2e", v);
        }
        int dec = paso >= 1 ? 0 : (int) Math.ceil(-Math.log10(paso) - 1e-9);
        dec = Math.max(0, Math.min(8, dec));
        return String.format(Locale.US, "%." + dec + "f", v);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        FontMetrics fm = g.getFontMetrics();

        if (xs == null || xs.length < 2) {
            g.setColor(Color.GRAY);
            String m = "Presiona «Resolver» para ver la gráfica";
            g.drawString(m, (w - fm.stringWidth(m)) / 2, h / 2);
            g.dispose();
            return;
        }

        area.setBounds(72, 24, Math.max(50, w - 72 - 24), Math.max(50, h - 24 - 46));
        calcularLimites();

        // Dibujamos la cuadrícula y etiquetas de los ejes
        double pasoX = pasoNice(xmax - xmin, 10);
        double pasoY = pasoNice(ymax - ymin, 8);

        long k0 = (long) Math.ceil(xmin / pasoX - 1e-9);
        for (long k = k0; k * pasoX <= xmax + pasoX * 1e-9; k++) {
            double v = k * pasoX;
            int px = (int) Math.round(pantallaX(v));
            g.setColor(C_GRID);
            g.drawLine(px, area.y, px, area.y + area.height);
            g.setColor(C_TEXTO);
            String s = etiqueta(v, pasoX);
            g.drawString(s, px - fm.stringWidth(s) / 2, area.y + area.height + 16);
        }
        k0 = (long) Math.ceil(ymin / pasoY - 1e-9);
        for (long k = k0; k * pasoY <= ymax + pasoY * 1e-9; k++) {
            double v = k * pasoY;
            int py = (int) Math.round(pantallaY(v));
            g.setColor(C_GRID);
            g.drawLine(area.x, py, area.x + area.width, py);
            g.setColor(C_TEXTO);
            String s = etiqueta(v, pasoY);
            g.drawString(s, area.x - 8 - fm.stringWidth(s), py + 4);
        }

        // Líneas de eje x = 0 y y = 0 si caen dentro del rango
        g.setColor(new Color(120, 120, 120));
        g.setStroke(new BasicStroke(1.2f));
        if (ymin < 0 && ymax > 0) {
            int py = (int) Math.round(pantallaY(0));
            g.drawLine(area.x, py, area.x + area.width, py);
        }
        if (xmin < 0 && xmax > 0) {
            int px = (int) Math.round(pantallaX(0));
            g.drawLine(px, area.y, px, area.y + area.height);
        }
        g.setColor(C_TEXTO);
        g.drawRect(area.x, area.y, area.width, area.height);
        g.drawString("x", area.x + area.width - 8, area.y + area.height + 34);
        g.drawString("y", 12, area.y + 12);

        // Dibujamos las curvas recortadas al recuadro de la gráfica
        java.awt.Shape clipAnterior = g.getClip();
        g.setClip(area);
        boolean marcadores = xs.length <= 61;
        if (verRef) {
            dibujarSerie(g, yRef, C_REF, S_REF, false);
        }
        if (verEuler) {
            dibujarSerie(g, yEuler, C_EULER, S_EULER, marcadores);
        }
        if (verRK4) {
            dibujarSerie(g, yRK4, C_RK4, S_RK4, marcadores);
        }
        g.setClip(clipAnterior);

        dibujarLeyenda(g, fm);
        if (hoverIdx >= 0) {
            dibujarHover(g, fm);
        }
        g.dispose();
    }

    private void dibujarSerie(Graphics2D g, double[] ys, Color c, Stroke st, boolean marcadores) {
        if (ys == null) {
            return;
        }
        Path2D.Double p = new Path2D.Double();
        boolean abierto = false;
        int salto = Math.max(1, ys.length / Math.max(200, area.width * 2));
        for (int i = 0; i < ys.length; i++) {
            boolean ultimo = i == ys.length - 1;
            if (i % salto != 0 && !ultimo) {
                continue;
            }
            double v = ys[i];
            if (!finito(v)) {
                abierto = false;
                continue;
            }
            double px = pantallaX(xs[i]);
            double py = Math.max(-1e5, Math.min(1e5, pantallaY(v)));
            if (!abierto) {
                p.moveTo(px, py);
                abierto = true;
            } else {
                p.lineTo(px, py);
            }
        }
        g.setColor(c);
        g.setStroke(st);
        g.draw(p);
        if (marcadores) {
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < ys.length; i++) {
                if (finito(ys[i])) {
                    g.fill(new Ellipse2D.Double(pantallaX(xs[i]) - 2.5, pantallaY(ys[i]) - 2.5, 5, 5));
                }
            }
        }
    }

    private void dibujarLeyenda(Graphics2D g, FontMetrics fm) {
        List<String> nombres = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        List<Stroke> trazos = new ArrayList<>();
        if (verEuler) {
            nombres.add("Euler");
            colores.add(C_EULER);
            trazos.add(S_EULER);
        }
        if (verRK4) {
            nombres.add("Runge-Kutta 4");
            colores.add(C_RK4);
            trazos.add(S_RK4);
        }
        if (verRef) {
            nombres.add(nombreRef);
            colores.add(C_REF);
            trazos.add(S_REF);
        }
        if (nombres.isEmpty()) {
            return;
        }
        int ancho = 0;
        for (String n : nombres) {
            ancho = Math.max(ancho, fm.stringWidth(n));
        }
        int bw = ancho + 56, bh = nombres.size() * 20 + 10;
        int bx = area.x + area.width - bw - 10, by = area.y + 10;
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(new Color(190, 190, 190));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, bw, bh, 8, 8);
        for (int i = 0; i < nombres.size(); i++) {
            int yy = by + 17 + i * 20;
            g.setColor(colores.get(i));
            g.setStroke(trazos.get(i));
            g.drawLine(bx + 8, yy - 4, bx + 38, yy - 4);
            g.setColor(C_TEXTO);
            g.drawString(nombres.get(i), bx + 46, yy);
        }
    }

    private void dibujarHover(Graphics2D g, FontMetrics fm) {
        int i = hoverIdx;
        int px = (int) Math.round(pantallaX(xs[i]));
        g.setColor(new Color(120, 120, 120, 160));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine(px, area.y, px, area.y + area.height);

        List<String> lineas = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        lineas.add("x = " + Metodos.num(xs[i]) + "   (i = " + i + ")");
        colores.add(C_TEXTO);
        java.awt.Shape clip = g.getClip();
        g.setClip(area);
        if (verEuler && yEuler != null) {
            lineas.add("Euler: " + Metodos.fmt(yEuler[i]));
            colores.add(C_EULER);
            marcarPunto(g, px, yEuler[i], C_EULER);
        }
        if (verRK4 && yRK4 != null) {
            lineas.add("RK4:   " + Metodos.fmt(yRK4[i]));
            colores.add(C_RK4);
            marcarPunto(g, px, yRK4[i], C_RK4);
        }
        if (verRef && yRef != null) {
            lineas.add(nombreRef + ": " + Metodos.fmt(yRef[i]));
            colores.add(C_REF);
            marcarPunto(g, px, yRef[i], C_REF);
        }
        g.setClip(clip);

        int ancho = 0;
        for (String s : lineas) {
            ancho = Math.max(ancho, fm.stringWidth(s));
        }
        int bw = ancho + 16, bh = lineas.size() * 17 + 8;
        int bx = px + 12;
        if (bx + bw > area.x + area.width) {
            bx = px - 12 - bw;
        }
        int by = area.y + area.height - bh - 10;
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(new Color(150, 150, 150));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, bw, bh, 8, 8);
        for (int k = 0; k < lineas.size(); k++) {
            g.setColor(colores.get(k));
            g.drawString(lineas.get(k), bx + 8, by + 18 + k * 17 - 4);
        }
    }

    private void marcarPunto(Graphics2D g, int px, double y, Color c) {
        if (!finito(y)) {
            return;
        }
        double py = Math.max(-1e5, Math.min(1e5, pantallaY(y)));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
        g.setColor(c);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
    }
}