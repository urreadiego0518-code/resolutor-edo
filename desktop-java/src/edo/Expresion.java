package edo;

import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

// Aquí convertimos lo que escribe el usuario (como "x - y") en una función que se puede evaluar
public class Expresion {

    public static final int MAX_PROFUNDIDAD = 200;

    private interface Nodo {
        double ev(double x, double y);
    }

    private final String texto;
    private final Nodo raiz;
    private int pos;
    private int profundidad;
    private boolean usaX;
    private boolean usaY;

    public Expresion(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("La expresión está vacía.");
        }
        // Pasamos todo a minúsculas para que no importe si escriben SIN o sin
        this.texto = texto.trim().toLowerCase(Locale.ROOT);
        this.pos = 0;
        this.profundidad = 0;
        this.usaX = false;
        this.usaY = false;
        this.raiz = parseExpresion();
        saltarEspacios();
        if (pos < this.texto.length()) {
            throw error("Carácter inesperado '" + this.texto.charAt(pos) + "'");
        }
    }

    // Evalúa la ecuación en un punto (x, y)
    public double evaluar(double x, double y) {
        return raiz.ev(x, y);
    }

    public boolean dependeDeX() {
        return usaX;
    }

    public boolean dependeDeY() {
        return usaY;
    }

    public String texto() {
        return texto;
    }

    // Sumas y restas (menor prioridad)
    private Nodo parseExpresion() {
        if (++profundidad > MAX_PROFUNDIDAD) {
            throw error("La expresión tiene demasiados paréntesis anidados (máximo "
                    + MAX_PROFUNDIDAD + ")");
        }
        Nodo izq = parseTermino();
        while (true) {
            saltarEspacios();
            if (consumir('+')) {
                final Nodo a = izq;
                final Nodo b = parseTermino();
                izq = (x, y) -> a.ev(x, y) + b.ev(x, y);
            } else if (consumir('-')) {
                final Nodo a = izq;
                final Nodo b = parseTermino();
                izq = (x, y) -> a.ev(x, y) - b.ev(x, y);
            } else {
                profundidad--;
                return izq;
            }
        }
    }

    // Multiplicaciones, divisiones y multiplicación pegada (como 2x)
    private Nodo parseTermino() {
        Nodo izq = parseUnario();
        while (true) {
            saltarEspacios();
            if (consumir('*')) {
                final Nodo a = izq;
                final Nodo b = parseUnario();
                izq = (x, y) -> a.ev(x, y) * b.ev(x, y);
            } else if (consumir('/')) {
                final Nodo a = izq;
                final Nodo b = parseUnario();
                izq = (x, y) -> a.ev(x, y) / b.ev(x, y);
            } else if (iniciaFactor()) {
                // Aquí detectamos casos como 2x o 3(x+1) sin signo *
                final Nodo a = izq;
                final Nodo b = parseUnario();
                izq = (x, y) -> a.ev(x, y) * b.ev(x, y);
            } else {
                return izq;
            }
        }
    }

    // Signos + o - delante de algo
    private Nodo parseUnario() {
        saltarEspacios();
        if (consumir('-')) {
            final Nodo a = parseUnario();
            return (x, y) -> -a.ev(x, y);
        }
        if (consumir('+')) {
            return parseUnario();
        }
        return parsePotencia();
    }

    // Potencias con ^
    private Nodo parsePotencia() {
        final Nodo base = parsePrimario();
        saltarEspacios();
        if (consumir('^')) {
            final Nodo exp = parseUnario();
            return (x, y) -> Math.pow(base.ev(x, y), exp.ev(x, y));
        }
        return base;
    }

    // Números, variables, funciones o paréntesis
    private Nodo parsePrimario() {
        saltarEspacios();
        if (pos >= texto.length()) {
            throw error("La expresión termina de forma inesperada");
        }
        char c = texto.charAt(pos);
        if (consumir('(')) {
            Nodo n = parseExpresion();
            saltarEspacios();
            if (!consumir(')')) {
                throw error("Falta cerrar un paréntesis ')'");
            }
            return n;
        }
        if (Character.isDigit(c) || c == '.') {
            return parseNumero();
        }
        if (Character.isLetter(c)) {
            return parseIdentificador();
        }
        throw error("Carácter inesperado '" + c + "'");
    }

    // Lee un número (incluyendo decimales o notación científica como 1e-3)
    private Nodo parseNumero() {
        int ini = pos;
        while (pos < texto.length()
                && (Character.isDigit(texto.charAt(pos)) || texto.charAt(pos) == '.')) {
            pos++;
        }
        if (pos < texto.length() && texto.charAt(pos) == 'e') {
            int p = pos + 1;
            if (p < texto.length() && (texto.charAt(p) == '+' || texto.charAt(p) == '-')) {
                p++;
            }
            if (p < texto.length() && Character.isDigit(texto.charAt(p))) {
                while (p < texto.length() && Character.isDigit(texto.charAt(p))) {
                    p++;
                }
                pos = p;
            }
        }
        String s = texto.substring(ini, pos);
        final double valor;
        try {
            valor = Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            throw error("Número inválido '" + s + "'");
        }
        return (x, y) -> valor;
    }

    // Lee funciones (sin, cos, exp...), variables (x, t, y) o constantes (pi, e)
    private Nodo parseIdentificador() {
        int ini = pos;
        while (pos < texto.length() && Character.isLetter(texto.charAt(pos))) {
            pos++;
        }
        String nombre = texto.substring(ini, pos);

        DoubleUnaryOperator fn = funcion(nombre);
        if (fn != null) {
            saltarEspacios();
            if (!consumir('(')) {
                throw error("La función '" + nombre + "' necesita paréntesis, por ejemplo " + nombre + "(x)");
            }
            final Nodo arg = parseExpresion();
            saltarEspacios();
            if (!consumir(')')) {
                throw error("Falta cerrar el paréntesis de " + nombre + "(...)");
            }
            final DoubleUnaryOperator f = fn;
            return (x, y) -> f.applyAsDouble(arg.ev(x, y));
        }

        switch (nombre) {
            case "x":
            case "t":
                usaX = true;
                return (x, y) -> x;
            case "y":
                usaY = true;
                return (x, y) -> y;
            case "pi":
                return (x, y) -> Math.PI;
            case "e":
                return (x, y) -> Math.E;
            default:
                throw error("No reconozco '" + nombre + "'. Variables: x (o t) y y. "
                        + "Si querías multiplicar usa * (ej. x*y)");
        }
    }

    private static DoubleUnaryOperator funcion(String n) {
        switch (n) {
            case "sin":
            case "sen":  return Math::sin;
            case "cos":  return Math::cos;
            case "tan":  return Math::tan;
            case "asin": return Math::asin;
            case "acos": return Math::acos;
            case "atan": return Math::atan;
            case "sinh": return Math::sinh;
            case "cosh": return Math::cosh;
            case "tanh": return Math::tanh;
            case "exp":  return Math::exp;
            case "ln":   return Math::log;
            case "log":  return Math::log10;
            case "sqrt": return Math::sqrt;
            case "abs":  return Math::abs;
            default:     return null;
        }
    }

    private boolean iniciaFactor() {
        if (pos >= texto.length()) {
            return false;
        }
        char c = texto.charAt(pos);
        return Character.isLetter(c) || Character.isDigit(c) || c == '.' || c == '(';
    }

    private boolean consumir(char c) {
        if (pos < texto.length() && texto.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String mensaje) {
        int p = Math.min(pos + 1, Math.max(1, texto.length()));
        return new IllegalArgumentException(mensaje + " (posición " + p
                + " en \"" + texto + "\").");
    }
}