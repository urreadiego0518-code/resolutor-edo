package edo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las medidas de error y del orden de convergencia. */
class AnalisisTest {

    @Test
    @DisplayName("Error máximo y error final")
    void erroresBasicos() {
        double[] ref = {1, 2, 3};
        double[] aprox = {1, 2.5, 2.9};
        assertEquals(0.5, Analisis.errorMaximo(ref, aprox), 1e-15);
        assertEquals(0.1, Analisis.errorFinal(ref, aprox), 1e-15);
    }

    @Test
    @DisplayName("Series idénticas dan error 0")
    void errorCero() {
        double[] s = {1, 2, 3};
        assertEquals(0.0, Analisis.errorMaximo(s, s), 0.0);
        assertEquals(0.0, Analisis.errorFinal(s, s), 0.0);
    }

    @Test
    @DisplayName("Una serie con NaN o infinito da error NaN (no un número falso)")
    void divergencia() {
        double[] ref = {1, 2, 3};
        double[] aprox = {1, Double.NaN, 3};
        assertTrue(Double.isNaN(Analisis.errorMaximo(ref, aprox)));
        assertTrue(Double.isNaN(Analisis.errorFinal(new double[]{1, 2, Double.POSITIVE_INFINITY},
                new double[]{1, 2, 3})));
        assertTrue(Analisis.hayNoFinitos(ref, aprox));
        assertFalse(Analisis.hayNoFinitos(ref));
    }

    @Test
    @DisplayName("Tamaños distintos se rechazan")
    void tamanosDistintos() {
        assertThrows(IllegalArgumentException.class,
                () -> Analisis.errorMaximo(new double[]{1, 2}, new double[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> Analisis.errorFinal(null, new double[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> Analisis.errorMaximo(new double[0], new double[0]));
    }

    @Test
    @DisplayName("Factor de mejora")
    void factorMejora() {
        assertEquals(16.0, Analisis.factorMejora(0.16, 0.01), 1e-12);
        assertTrue(Double.isInfinite(Analisis.factorMejora(0.5, 0)));
        assertTrue(Double.isNaN(Analisis.factorMejora(0, 0.1)));
        assertTrue(Double.isNaN(Analisis.factorMejora(Double.NaN, 0.1)));
        assertTrue(Double.isNaN(Analisis.factorMejora(1, -0.1)));
    }

    @Test
    @DisplayName("Orden empírico: error /16 al dividir h entre 2 implica orden 4")
    void ordenEmpirico() {
        assertEquals(4.0, Analisis.ordenEmpirico(0.16, 0.01, 2), 1e-12);
        assertEquals(1.0, Analisis.ordenEmpirico(0.2, 0.1, 2), 1e-12);
        assertEquals(2.0, Analisis.ordenEmpirico(0.4, 0.1, 2), 1e-12);
        assertTrue(Double.isNaN(Analisis.ordenEmpirico(0, 0.1, 2)));
        assertTrue(Double.isNaN(Analisis.ordenEmpirico(0.1, 0, 2)));
        assertTrue(Double.isNaN(Analisis.ordenEmpirico(0.1, 0.01, 1)));
        assertTrue(Double.isNaN(Analisis.ordenEmpirico(Double.NaN, 0.01, 2)));
    }
}
