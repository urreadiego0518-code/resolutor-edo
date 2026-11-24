package edo;

// Aquí calculamos qué tanto error tienen los métodos comparados con la referencia
public final class Analisis {

    private Analisis() {
    }

    // Busca el error más grande en todo el intervalo
    public static double errorMaximo(double[] ref, double[] aprox) {
        validarSeries(ref, aprox);
        double max = 0;
        for (int i = 0; i < ref.length; i++) {
            double e = Math.abs(ref[i] - aprox[i]);
            // Si hay NaN o infinito, el método se salió de control
            if (!Metodos.finito(e)) {
                return Double.NaN;
            }
            if (e > max) {
                max = e;
            }
        }
        return max;
    }

    // Calcula el error solo en el último punto
    public static double errorFinal(double[] ref, double[] aprox) {
        validarSeries(ref, aprox);
        int n = ref.length - 1;
        double e = Math.abs(ref[n] - aprox[n]);
        return Metodos.finito(e) ? e : Double.NaN;
    }

    // Revisa cuántas veces se redujo el error al pasar de un paso grueso a uno fino
    public static double factorMejora(double errorGrueso, double errorFino) {
        if (!Metodos.finito(errorGrueso) || !Metodos.finito(errorFino)
                || errorGrueso <= 0 || errorFino < 0) {
            return Double.NaN;
        }
        if (errorFino == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return errorGrueso / errorFino;
    }

    // Estima el orden del método usando logaritmos
    public static double ordenEmpirico(double errorGrueso, double errorFino, double factorH) {
        if (!Metodos.finito(errorGrueso) || !Metodos.finito(errorFino)
                || errorGrueso <= 0 || errorFino <= 0 || !(factorH > 1)) {
            return Double.NaN;
        }
        return Math.log(errorGrueso / errorFino) / Math.log(factorH);
    }

    // Revisa si algún arreglo tiene valores inválidos (NaN o infinito)
    public static boolean hayNoFinitos(double[]... series) {
        if (series == null) {
            return false;
        }
        for (double[] s : series) {
            if (s == null) {
                continue;
            }
            for (double v : s) {
                if (!Metodos.finito(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Revisa que los dos arreglos existan, tengan el mismo tamaño y no estén vacíos
    private static void validarSeries(double[] ref, double[] aprox) {
        if (ref == null || aprox == null) {
            throw new IllegalArgumentException("Faltan las series para comparar.");
        }
        if (ref.length != aprox.length) {
            throw new IllegalArgumentException("Las series tienen distinto tamaño ("
                    + ref.length + " y " + aprox.length + ").");
        }
        if (ref.length == 0) {
            throw new IllegalArgumentException("Las series están vacías.");
        }
    }
}