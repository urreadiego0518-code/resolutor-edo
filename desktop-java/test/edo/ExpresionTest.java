package edo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del intérprete de ecuaciones.
 *
 * Se comprueba la jerarquía de operaciones, la multiplicación implícita,
 * las funciones, las constantes, la notación científica y los mensajes de
 * error ante entradas inválidas.
 */
class ExpresionTest {

    private static double ev(String s, double x, double y) {
        return new Expresion(s).evaluar(x, y);
    }

    @Test
    @DisplayName("Operaciones básicas y variables")
    void operacionesBasicas() {
        assertEquals(3.0, ev("x - y", 5, 2), 1e-12);
        assertEquals(10.0, ev("x + y", 5, 5), 1e-12);
        assertEquals(6.0, ev("x*y", 2, 3), 1e-12);
        assertEquals(2.5, ev("x/y", 5, 2), 1e-12);
    }

    @Test
    @DisplayName("Jerarquía: potencias antes que multiplicación y suma")
    void jerarquia() {
        assertEquals(14.0, ev("2 + 3*4", 0, 0), 1e-12);
        assertEquals(20.0, ev("(2 + 3)*4", 0, 0), 1e-12);
        assertEquals(8.0, ev("2^3", 0, 0), 1e-12);
        assertEquals(-4.0, ev("-2^2", 0, 0), 1e-12);
        assertEquals(4.0, ev("(-2)^2", 0, 0), 1e-12);
        assertEquals(512.0, ev("2^3^2", 0, 0), 1e-12);
    }

    @Test
    @DisplayName("Multiplicación implícita: 2x, 3(x+1), 2sin(x)")
    void multiplicacionImplicita() {
        assertEquals(6.0, ev("2x", 3, 0), 1e-12);
        assertEquals(12.0, ev("3(x+1)", 3, 0), 1e-12);
        assertEquals(1.0, ev("2sin(pi/6)", 0, 0), 1e-9);
        assertEquals(6.0, ev("2x y", 3, 1), 1e-12);
    }

    @Test
    @DisplayName("Constantes pi y e")
    void constantes() {
        assertEquals(Math.PI, ev("pi", 0, 0), 1e-15);
        assertEquals(Math.E, ev("e", 0, 0), 1e-15);
        assertEquals(2 * Math.E, ev("2e", 0, 0), 1e-15);
    }

    @Test
    @DisplayName("Notación científica")
    void notacionCientifica() {
        assertEquals(0.001, ev("1e-3", 0, 0), 1e-18);
        assertEquals(15000.0, ev("1.5e4", 0, 0), 1e-9);
    }

    @Test
    @DisplayName("Funciones matemáticas")
    void funciones() {
        assertEquals(1.0, ev("cos(0)", 0, 0), 1e-15);
        assertEquals(1.0, ev("sen(pi/2)", 0, 0), 1e-15);
        assertEquals(2.0, ev("sqrt(4)", 0, 0), 1e-15);
        assertEquals(3.0, ev("abs(-3)", 0, 0), 1e-15);
        assertEquals(1.0, ev("ln(e)", 0, 0), 1e-15);
        assertEquals(2.0, ev("log(100)", 0, 0), 1e-15);
        assertEquals(1.0, ev("tanh(50)", 0, 0), 1e-12);
    }

    @Test
    @DisplayName("La variable t es equivalente a x")
    void variableT() {
        assertEquals(4.0, ev("t^2", 2, 0), 1e-12);
    }

    @Test
    @DisplayName("Mayúsculas y espacios no afectan")
    void mayusculasYEspacios() {
        assertEquals(3.0, ev("  X  -  Y  ", 5, 2), 1e-12);
        assertEquals(3.0, ev("2 * SIN(pi/6) + 2", 0, 0), 1e-9);
    }

    // ------------------------------------------------------------------
    //  Detección de dependencias (se usa para validar la solución exacta)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dependeDeX / dependeDeY detectan las variables usadas")
    void dependencias() {
        assertTrue(new Expresion("x - 1 + 2*exp(-x)").dependeDeX());
        assertFalse(new Expresion("x - 1 + 2*exp(-x)").dependeDeY());
        assertTrue(new Expresion("x - y").dependeDeY());
        assertTrue(new Expresion("t + y").dependeDeX());
        assertFalse(new Expresion("2*pi").dependeDeX());
        assertFalse(new Expresion("2*pi").dependeDeY());
    }

    @Test
    @DisplayName("La expresión es inmutable y se puede evaluar varias veces")
    void reutilizable() {
        Expresion f = new Expresion("x*y");
        assertEquals(6.0, f.evaluar(2, 3), 1e-12);
        assertEquals(20.0, f.evaluar(4, 5), 1e-12);
        assertEquals("x*y", f.texto());
    }

    // ------------------------------------------------------------------
    //  Errores esperados
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Expresión vacía o nula se rechaza")
    void vacia() {
        assertThrows(IllegalArgumentException.class, () -> new Expresion(""));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("   "));
        assertThrows(IllegalArgumentException.class, () -> new Expresion(null));
    }

    @Test
    @DisplayName("Expresión incompleta se rechaza")
    void incompleta() {
        assertThrows(IllegalArgumentException.class, () -> new Expresion("x +"));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("(x"));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("x)"));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("2*"));
    }

    @Test
    @DisplayName("Variable desconocida se rechaza con mensaje útil")
    void variableDesconocida() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Expresion("xy"));
        assertTrue(ex.getMessage().contains("xy"));
        assertTrue(ex.getMessage().contains("*"));
    }

    @Test
    @DisplayName("Función sin paréntesis se rechaza")
    void funcionSinParentesis() {
        assertThrows(IllegalArgumentException.class, () -> new Expresion("sin x"));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("sin(x"));
    }

    @Test
    @DisplayName("Número mal formado se rechaza")
    void numeroMalo() {
        assertThrows(IllegalArgumentException.class, () -> new Expresion("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> new Expresion("x @ y"));
    }

    @Test
    @DisplayName("Demasiados paréntesis anidados no desbordan la pila")
    void profundidadLimitada() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append('(');
        }
        sb.append('x');
        for (int i = 0; i < 300; i++) {
            sb.append(')');
        }
        assertThrows(IllegalArgumentException.class, () -> new Expresion(sb.toString()));
    }

    @Test
    @DisplayName("Anidamiento razonable sí funciona")
    void anidamientoNormal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append('(');
        }
        sb.append("x+1");
        for (int i = 0; i < 20; i++) {
            sb.append(')');
        }
        assertEquals(3.0, ev(sb.toString(), 2, 0), 1e-12);
    }

    @Test
    @DisplayName("División entre cero da infinito, no excepción")
    void divisionEntreCero() {
        assertEquals(Double.POSITIVE_INFINITY, ev("1/0", 0, 0));
        assertTrue(Double.isNaN(ev("sqrt(-1)", 0, 0)));
    }
}
