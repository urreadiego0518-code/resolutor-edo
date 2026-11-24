package edo;

import java.util.Locale;

// Aquí están los métodos numéricos (Euler y RK4) y los formatos de números
public final class Metodos {

    public static final int MAX_PASOS = 50_000;

    private Metodos() {
    }

    // Método de Euler: avanza usando la pendiente al inicio de cada paso
    public static double[] euler(Expresion f, double x0, double y0, double h, int n) {
        validar(f, x0, y0, h, n);
        double[] y = new double[n + 1];
        y[0] = y0;
        for (int i = 0; i < n; i++) {
            double x = x0 + i * h;
            y[i + 1] = y[i] + h * f.evaluar(x, y[i]);
        }
        return y;
    }

    // Método de Runge-Kutta de 4to orden: calcula 4 pendientes y las promedia
    public static double[] rk4(Expresion f, double x0, double y0, double h, int n) {
        validar(f, x0, y0, h, n);
        double[] y = new double[n + 1];
        y[0] = y0;
        for (int i = 0; i < n; i++) {
            double x = x0 + i * h;
            double yi = y[i];
            double k1 = f.evaluar(x, yi);
            double k2 = f.evaluar(x + h / 2, yi + k1 * h / 2);
            double k3 = f.evaluar(x + h / 2, yi + k2 * h / 2);
            double k4 = f.evaluar(x + h, yi + k3 * h);
            y[i + 1] = yi + (h / 6) * (k1 + 2 * k2 + 2 * k3 + k4);
        }
        return y;
    }

    // Revisa que los parámetros del método sean válidos antes de reservar memoria
    public static void validar(Expresion f, double x0, double y0, double h, int n) {
        if (f == null) {
            throw new IllegalArgumentException("La función f(x, y) es obligatoria.");
        }
        if (!finito(x0)) {
            throw new IllegalArgumentException("x0 debe ser un número finito.");
        }
        if (!finito(y0)) {
            throw new IllegalArgumentException("y0 debe ser un número finito.");
        }
        if (!finito(h) || h <= 0) {
            throw new IllegalArgumentException(
                    "El tamaño de paso h debe ser un número finito mayor que 0.");
        }
        if (n < 0) {
            throw new IllegalArgumentException("El número de pasos no puede ser negativo.");
        }
        if (n > MAX_PASOS) {
            throw new IllegalArgumentException("Demasiados pasos (" + n + "). El máximo es "
                    + MAX_PASOS + ".");
        }
    }

    // Calcula cuántos pasos n caben entre x0 y xf con paso h
    public static int pasosPara(double x0, double xf, double h) {
        if (!finito(x0) || !finito(xf)) {
            throw new IllegalArgumentException("x0 y x final deben ser números finitos.");
        }
        if (!finito(h) || h <= 0) {
            throw new IllegalArgumentException("El tamaño de paso h debe ser mayor que 0.");
        }
        if (!(xf > x0)) {
            throw new IllegalArgumentException("El x final debe ser mayor que x0.");
        }
        double bruto = (xf - x0) / h;
        if (!finito(bruto)) {
            throw new IllegalArgumentException("El intervalo es demasiado grande para ese h.");
        }
        if (bruto < 1 - 1e-9) {
            throw new IllegalArgumentException(
                    "h es mayor que el intervalo [x0, x final]. Usa un h más pequeño.");
        }
        long n = (long) Math.ceil(bruto - 1e-9);
        if (n > MAX_PASOS) {
            throw new IllegalArgumentException("Demasiados pasos (" + n + "). El máximo es "
                    + MAX_PASOS + ". Aumenta h o reduce el intervalo.");
        }
        return (int) n;
    }

    // Revisa que el número no sea NaN ni infinito
    public static boolean finito(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    // Arma el texto explicando paso a paso cómo se hace Euler
    public static String detalleEuler(Expresion f, double x, double y, double h, int i) {
        double fv = f.evaluar(x, y);
        double y1 = y + h * fv;
        StringBuilder sb = new StringBuilder();
        sb.append("MÉTODO DE EULER   (paso i = ").append(i).append(")\n");
        sb.append("  y(i+1) = y(i) + h·f(x(i), y(i))\n\n");
        sb.append("  x(i) = ").append(num(x)).append("      y(i) = ").append(num(y))
                .append("      h = ").append(num(h)).append("\n");
        sb.append("  f(").append(num(x)).append(", ").append(num(y)).append(") = ")
                .append(num(fv)).append("\n\n");
        sb.append("  y(i+1) = ").append(num(y)).append(" + ").append(num(h))
                .append("·(").append(num(fv)).append(")\n");
        sb.append("  y(i+1) = ").append(num(y1)).append("\n");
        return sb.toString();
    }

    // Arma el texto explicando paso a paso cómo se hace RK4
    public static String detalleRK4(Expresion f, double x, double y, double h, int i) {
        double k1 = f.evaluar(x, y);
        double y2 = y + k1 * h / 2;
        double k2 = f.evaluar(x + h / 2, y2);
        double y3 = y + k2 * h / 2;
        double k3 = f.evaluar(x + h / 2, y3);
        double y4 = y + k3 * h;
        double k4 = f.evaluar(x + h, y4);
        double y1 = y + (h / 6) * (k1 + 2 * k2 + 2 * k3 + k4);

        StringBuilder sb = new StringBuilder();
        sb.append("MÉTODO DE RUNGE-KUTTA 4   (paso i = ").append(i).append(")\n\n");
        sb.append("  x(i) = ").append(num(x)).append("      y(i) = ").append(num(y))
                .append("      h = ").append(num(h)).append("\n\n");
        sb.append("  k1 = f(x, y)\n");
        sb.append("     = f(").append(num(x)).append(", ").append(num(y)).append(") = ")
                .append(num(k1)).append("\n");
        sb.append("  k2 = f(x + h/2, y + k1·h/2)\n");
        sb.append("     = f(").append(num(x + h / 2)).append(", ").append(num(y2)).append(") = ")
                .append(num(k2)).append("\n");
        sb.append("  k3 = f(x + h/2, y + k2·h/2)\n");
        sb.append("     = f(").append(num(x + h / 2)).append(", ").append(num(y3)).append(") = ")
                .append(num(k3)).append("\n");
        sb.append("  k4 = f(x + h, y + k3·h)\n");
        sb.append("     = f(").append(num(x + h)).append(", ").append(num(y4)).append(") = ")
                .append(num(k4)).append("\n\n");
        sb.append("  y(i+1) = y(i) + (h/6)·(k1 + 2·k2 + 2·k3 + k4)\n");
        sb.append("  y(i+1) = ").append(num(y)).append(" + (").append(num(h)).append("/6)·(")
                .append(num(k1)).append(" + 2·(").append(num(k2)).append(") + 2·(")
                .append(num(k3)).append(") + ").append(num(k4)).append(")\n");
        sb.append("  y(i+1) = ").append(num(y1)).append("\n");
        return sb.toString();
    }

    // Formato de 6 decimales para la tabla
    public static String fmt(double v) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "Infinito" : "-Infinito";
        }
        double a = Math.abs(v);
        if (a != 0 && (a >= 1e9 || a < 1e-6)) {
            return String.format(Locale.US, "%.6e", v);
        }
        return String.format(Locale.US, "%.6f", v);
    }

    // Formato limpio sin ceros de más para mostrar números en textos
    public static String num(double v) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "Infinito" : "-Infinito";
        }
        double a = Math.abs(v);
        if (a != 0 && (a >= 1e9 || a < 1e-6)) {
            return String.format(Locale.US, "%.4e", v);
        }
        String s = String.format(Locale.US, "%.6f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        if (s.equals("-0") || s.isEmpty()) {
            s = "0";
        }
        return s;
    }

    // Formato científico corto para errores
    public static String fmtError(double v) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (Double.isInfinite(v)) {
            return "Infinito";
        }
        return String.format(Locale.US, "%.3e", v);
    }
}