package edo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de extremo a extremo sobre el catálogo de ejemplos: es la misma
 * ruta que sigue la interfaz (texto -> Expresion -> Metodos -> Analisis).
 */
class IntegracionTest {

    @Test
    @DisplayName("El catálogo tiene 8 entradas y 7 con ecuación")
    void catalogoCompleto() {
        Ejemplo[] e = Ejemplo.catalogo();
        assertEquals(8, e.length);
        int conEcuacion = 0;
        for (Ejemplo ej : e) {
            assertNotNull(ej.nombre);
            if (ej.f != null) {
                conEcuacion++;
            }
        }
        assertEquals(7, conEcuacion);
    }

    @Test
    @DisplayName("Todos los ejemplos resuelven sin excepción y RK4 gana a Euler")
    void todosLosEjemplos() {
        for (Ejemplo ej : Ejemplo.catalogo()) {
            if (ej.f == null) {
                continue;
            }
            int n = Metodos.pasosPara(ej.x0, ej.xf, ej.h);
            double[] ye = Metodos.euler(new Expresion(ej.f), ej.x0, ej.y0, ej.h, n);
            double[] yr = Metodos.rk4(new Expresion(ej.f), ej.x0, ej.y0, ej.h, n);
            assertFalse(Analisis.hayNoFinitos(ye, yr), "Diverge: " + ej.nombre);

            if (!ej.exacta.isEmpty()) {
                Expresion ex = new Expresion(ej.exacta);
                double[] ref = new double[n + 1];
                for (int i = 0; i <= n; i++) {
                    ref[i] = ex.evaluar(ej.x0 + i * ej.h, Double.NaN);
                }
                double eE = Analisis.errorMaximo(ref, ye);
                double eR = Analisis.errorMaximo(ref, yr);
                assertTrue(eR < eE, "RK4 debería ser mejor en: " + ej.nombre);
                // Se compara el error RELATIVO: un error absoluto de 0.02 es
                // enorme si y ~ 1 y despreciable si y ~ 2000.
                double escala = 0;
                for (double v : ref) {
                    escala = Math.max(escala, Math.abs(v));
                }
                double rel = eR / Math.max(1e-12, escala);
                assertTrue(rel < 1e-3, "RK4 debería ser preciso en: " + ej.nombre
                        + " (error relativo=" + rel + ")");
            }
        }
    }

    @Test
    @DisplayName("La solución exacta de cada ejemplo cumple y(x0) = y0")
    void condicionInicialCoherente() {
        for (Ejemplo ej : Ejemplo.catalogo()) {
            if (ej.f == null || ej.exacta.isEmpty()) {
                continue;
            }
            double v = new Expresion(ej.exacta).evaluar(ej.x0, Double.NaN);
            assertEquals(ej.y0, v, 1e-9, "Condición inicial incoherente en " + ej.nombre);
        }
    }

    @Test
    @DisplayName("Ejemplo del cuaderno completo con h = 0.1")
    void ejemploCuadernoCompleto() {
        Ejemplo ej = Ejemplo.catalogo()[0];
        int n = Metodos.pasosPara(ej.x0, ej.xf, ej.h);
        assertEquals(20, n);
        double[] yr = Metodos.rk4(new Expresion(ej.f), ej.x0, ej.y0, ej.h, n);
        assertEquals(1.270671, yr[n], 1e-6);
    }

    @Test
    @DisplayName("Una referencia fina con RK4 sirve para medir el error de Euler")
    void referenciaFina() {
        Expresion f = new Expresion("x - y");
        int n = 20;
        int m = 20;
        double[] fino = Metodos.rk4(f, 0, 1, 0.1 / m, n * m);
        double[] ref = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            ref[i] = fino[i * m];
        }
        double[] ye = Metodos.euler(f, 0, 1, 0.1, n);
        double err = Analisis.errorMaximo(ref, ye);
        assertEquals(3.84e-2, err, 1e-3);
    }

    @Test
    @DisplayName("Un ejemplo con datos inválidos se rechaza al construirlo")
    void ejemploInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ejemplo("malo", "x", "", 0, 1, 0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Ejemplo("malo", "x", "", 2, 1, 0.1, 2));
    }
}
