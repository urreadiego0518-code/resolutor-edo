package edo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del núcleo numérico.
 *
 * Casos cubiertos: fórmula de Euler y RK4 contra el ejemplo del cuaderno,
 * orden de convergencia, parámetros inválidos, pasos límite y divergencia.
 */
class MetodosTest {

    private static final double EPS = 1e-12;

    private static Expresion f(String s) {
        return new Expresion(s);
    }

    // ------------------------------------------------------------------
    //  Valores contra el ejemplo del cuaderno: y' = x - y, y(0) = 1
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Euler reproduce el cuaderno: y(0.1)=0.9 y y(0.2)=0.82")
    void eulerEjemploCuaderno() {
        double[] y = Metodos.euler(f("x - y"), 0, 1, 0.1, 20);
        assertEquals(1.0, y[0], EPS);
        assertEquals(0.9, y[1], EPS);
        assertEquals(0.82, y[2], EPS);
    }

    @Test
    @DisplayName("RK4 da y(0.1)=0.909675 y y(0.2)=0.837462")
    void rk4EjemploCuaderno() {
        double[] y = Metodos.rk4(f("x - y"), 0, 1, 0.1, 20);
        // los valores del cuaderno están redondeados a 6 decimales
        assertEquals(0.909675, y[1], 1e-6);
        assertEquals(0.837462, y[2], 1e-6);
    }

    @Test
    @DisplayName("Ambos métodos parten exactamente de y0")
    void primerValorEsCondicionInicial() {
        double[] e = Metodos.euler(f("x - y"), 0, 7.5, 0.1, 3);
        double[] r = Metodos.rk4(f("x - y"), 0, 7.5, 0.1, 3);
        assertEquals(7.5, e[0], 0.0);
        assertEquals(7.5, r[0], 0.0);
    }

    @Test
    @DisplayName("Con n = 0 se devuelve solo la condición inicial")
    void ceroPasos() {
        assertArrayEquals(new double[]{2.0}, Metodos.euler(f("x"), 0, 2, 0.1, 0), 0.0);
        assertArrayEquals(new double[]{2.0}, Metodos.rk4(f("x"), 0, 2, 0.1, 0), 0.0);
    }

    // ------------------------------------------------------------------
    //  Precisión y orden de convergencia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RK4 es más preciso que Euler en el ejemplo del cuaderno")
    void rk4MasPrecisoQueEuler() {
        Expresion f = f("x - y");
        Expresion exacta = f("x - 1 + 2*exp(-x)");
        int n = 20;
        double[] x = new double[n + 1];
        double[] ref = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            x[i] = i * 0.1;
            ref[i] = exacta.evaluar(x[i], Double.NaN);
        }
        double errE = Analisis.errorMaximo(ref, Metodos.euler(f, 0, 1, 0.1, n));
        double errR = Analisis.errorMaximo(ref, Metodos.rk4(f, 0, 1, 0.1, n));
        assertTrue(errR < errE, "RK4 debería tener menos error");
        assertTrue(errR < 1e-6, "RK4 debería ser casi exacto con h=0.1");
    }

    @Test
    @DisplayName("Al dividir h entre 2, Euler mejora ~2 veces y RK4 ~16")
    void ordenDeConvergencia() {
        Expresion f = f("x - y");
        Expresion exacta = f("x - 1 + 2*exp(-x)");

        double[] ref1 = referencia(exacta, 0, 2, 0.2);
        double[] ref2 = referencia(exacta, 0, 2, 0.1);
        double errE1 = Analisis.errorFinal(ref1, Metodos.euler(f, 0, 1, 0.2, 10));
        double errE2 = Analisis.errorFinal(ref2, Metodos.euler(f, 0, 1, 0.1, 20));
        double errR1 = Analisis.errorFinal(ref1, Metodos.rk4(f, 0, 1, 0.2, 10));
        double errR2 = Analisis.errorFinal(ref2, Metodos.rk4(f, 0, 1, 0.1, 20));

        double pE = Analisis.ordenEmpirico(errE1, errE2, 2);
        double pR = Analisis.ordenEmpirico(errR1, errR2, 2);
        assertEquals(1.0, pE, 0.15, "Euler es de orden 1");
        assertEquals(4.0, pR, 0.35, "RK4 es de orden 4");
    }

    @Test
    @DisplayName("Euler es exacto para y' = constante")
    void eulerExactoConPendienteConstante() {
        double[] y = Metodos.euler(f("2"), 0, 1, 0.5, 4);
        assertArrayEquals(new double[]{1, 2, 3, 4, 5}, y, 1e-12);
    }

    @Test
    @DisplayName("Una EDO con solución constante se queda constante")
    void solucionConstante() {
        double[] e = Metodos.euler(f("0"), 0, 3.25, 0.1, 50);
        double[] r = Metodos.rk4(f("0"), 0, 3.25, 0.1, 50);
        for (int i = 0; i <= 50; i++) {
            assertEquals(3.25, e[i], 0.0);
            assertEquals(3.25, r[i], 0.0);
        }
    }

    // ------------------------------------------------------------------
    //  Validación de argumentos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("h <= 0 se rechaza")
    void hInvalida() {
        assertThrows(IllegalArgumentException.class, () -> Metodos.euler(f("x"), 0, 1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> Metodos.rk4(f("x"), 0, 1, -0.1, 10));
    }

    @Test
    @DisplayName("h = NaN o infinito se rechaza")
    void hNoFinita() {
        assertThrows(IllegalArgumentException.class,
                () -> Metodos.euler(f("x"), 0, 1, Double.NaN, 10));
        assertThrows(IllegalArgumentException.class,
                () -> Metodos.rk4(f("x"), 0, 1, Double.POSITIVE_INFINITY, 10));
    }

    @Test
    @DisplayName("n negativo se rechaza en vez de lanzar NegativeArraySizeException")
    void nNegativo() {
        assertThrows(IllegalArgumentException.class, () -> Metodos.euler(f("x"), 0, 1, 0.1, -1));
    }

    @Test
    @DisplayName("n mayor que el máximo se rechaza")
    void nDemasiadoGrande() {
        assertThrows(IllegalArgumentException.class,
                () -> Metodos.rk4(f("x"), 0, 1, 0.1, Metodos.MAX_PASOS + 1));
    }

    @Test
    @DisplayName("f nula se rechaza")
    void funcionNula() {
        assertThrows(IllegalArgumentException.class, () -> Metodos.euler(null, 0, 1, 0.1, 10));
    }

    @Test
    @DisplayName("x0 o y0 no finitos se rechazan")
    void condicionesNoFinitas() {
        assertThrows(IllegalArgumentException.class,
                () -> Metodos.euler(f("x"), Double.NaN, 1, 0.1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> Metodos.rk4(f("x"), 0, Double.POSITIVE_INFINITY, 0.1, 10));
    }

    // ------------------------------------------------------------------
    //  pasosPara: redondeo y tolerancia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("2.0/0.1 da 20 pasos y no 21 por error de coma flotante")
    void pasosConTolerancia() {
        assertEquals(20, Metodos.pasosPara(0, 2, 0.1));
        assertEquals(10, Metodos.pasosPara(0, 1, 0.1));
        assertEquals(1, Metodos.pasosPara(0, 0.1, 0.1));
    }

    @Test
    @DisplayName("Si xf no es múltiplo de h se usa techo, no redondeo")
    void pasosUsaTecho() {
        assertEquals(21, Metodos.pasosPara(0, 2.04, 0.1));
        assertEquals(21, Metodos.pasosPara(0, 2.0000001, 0.1));
    }

    @Test
    @DisplayName("Intervalos y pasos inválidos se rechazan")
    void pasosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Metodos.pasosPara(0, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> Metodos.pasosPara(2, 2, 0.1));
        assertThrows(IllegalArgumentException.class, () -> Metodos.pasosPara(2, 0, 0.1));
        assertThrows(IllegalArgumentException.class, () -> Metodos.pasosPara(0, 0.1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> Metodos.pasosPara(0, 2, 1e-9));
    }

    // ------------------------------------------------------------------
    //  Divergencia y singularidades
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Una singularidad produce infinito, no una excepción")
    void singularidadNoRompe() {
        double[] y = Metodos.euler(f("1/x"), 0, 1, 0.1, 5);
        assertTrue(Double.isInfinite(y[1]), "1/0 debe dar infinito y propagarse");
    }

    @Test
    @DisplayName("Una EDO explosiva se detecta como no finita")
    void divergenciaDetectada() {
        double[] y = Metodos.rk4(f("y^2"), 0, 10, 0.5, 40);
        assertTrue(Analisis.hayNoFinitos(y), "y'=y^2 con y0=10 debe desbordar");
    }

    @Test
    @DisplayName("y0 = 0 en y' = y se queda en 0")
    void solucionCero() {
        double[] y = Metodos.rk4(f("y"), 0, 0, 0.1, 20);
        for (double v : y) {
            assertEquals(0.0, v, 0.0);
        }
    }

    // ------------------------------------------------------------------
    //  Formato de números
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Los formatos no dependen del idioma del sistema")
    void formatosConPuntoDecimal() {
        assertEquals("0.500000", Metodos.fmt(0.5));
        assertEquals("0.5", Metodos.num(0.5));
        assertEquals("0", Metodos.num(-0.0));
        assertEquals("NaN", Metodos.fmt(Double.NaN));
        assertEquals("Infinito", Metodos.fmt(Double.POSITIVE_INFINITY));
        assertEquals("-Infinito", Metodos.fmt(Double.NEGATIVE_INFINITY));
        assertTrue(Metodos.fmt(1e12).contains("e"));
        assertTrue(Metodos.fmtError(1e-9).contains("e"));
    }

    // ------------------------------------------------------------------
    //  Auxiliares
    // ------------------------------------------------------------------

    private static double[] referencia(Expresion exacta, double x0, double xf, double h) {
        int n = Metodos.pasosPara(x0, xf, h);
        double[] ref = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            ref[i] = exacta.evaluar(x0 + i * h, Double.NaN);
        }
        return ref;
    }
}
