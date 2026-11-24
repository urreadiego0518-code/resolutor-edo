package com.urreadiego.resolutoredo

import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

// Guarda un problema listo para cargarlo con un toque
data class Ejemplo(
    val nombre: String,
    val f: String?,
    val exacta: String,
    val x0: Double,
    val y0: Double,
    val h: Double,
    val xf: Double
) {
    init {
        require(nombre.isNotBlank()) { "El ejemplo necesita un nombre." }
        require(Metodos.finito(x0) && Metodos.finito(y0) && Metodos.finito(xf)) {
            "Datos no finitos en el ejemplo $nombre"
        }
        require(Metodos.finito(h) && h > 0.0) { "h inválida en el ejemplo $nombre" }
        require(xf > x0) { "El intervalo del ejemplo $nombre es inválido" }
    }

    override fun toString(): String = nombre

    companion object {
        // Lista de ejemplos del cuaderno y casos clásicos
        fun catalogo(): List<Ejemplo> = listOf(
            Ejemplo("Cuaderno: y' + y = x", "x - y", "x - 1 + 2*exp(-x)", 0.0, 1.0, 0.1, 2.0),
            Ejemplo("Enfriamiento de Newton", "-0.5*(y - 20)", "20 + 70*exp(-0.5*x)", 0.0, 90.0, 0.5, 10.0),
            Ejemplo("Crecimiento poblacional", "0.3*y", "100*exp(0.3*x)", 0.0, 100.0, 0.5, 10.0),
            Ejemplo("Logístico (límite 100)", "0.5*y*(1 - y/100)", "100/(1 + 9*exp(-0.5*x))", 0.0, 10.0, 1.0, 15.0),
            Ejemplo("Paracaidista", "9.81 - 0.25*y", "39.24*(1 - exp(-0.25*x))", 0.0, 0.0, 1.0, 20.0),
            Ejemplo("Circuito RC", "(12 - y)/2", "12*(1 - exp(-x/2))", 0.0, 0.0, 1.0, 10.0),
            Ejemplo("Oscilante: y' = y*cos(x)", "y*cos(x)", "exp(sin(x))", 0.0, 1.0, 0.5, 10.0),
            Ejemplo("Personalizada", null, "", 0.0, 1.0, 0.1, 2.0)
        )
    }
}

// Convierte un texto como "x - y" o "2x + sin(x)" en una función evaluable
class Expresion(rawTexto: String) {
    companion object {
        const val MAX_PROFUNDIDAD = 200
    }

    private fun interface Nodo {
        fun ev(x: Double, y: Double): Double
    }

    val texto: String
    private val raiz: Nodo
    private var pos = 0
    private var profundidad = 0
    private var usaX = false
    private var usaY = false

    init {
        require(rawTexto.isNotBlank()) { "La expresión está vacía." }
        // Pasamos todo a minúsculas para aceptar SIN, Cos, etc.
        texto = rawTexto.trim().lowercase(Locale.ROOT)
        raiz = parseExpresion()
        saltarEspacios()
        if (pos < texto.length) {
            throw error("Carácter inesperado '${texto[pos]}'")
        }
    }

    // Evalúa la ecuación en un punto (x, y)
    fun evaluar(x: Double, y: Double): Double = raiz.ev(x, y)
    fun dependeDeX(): Boolean = usaX
    fun dependeDeY(): Boolean = usaY

    // Resuelve sumas y restas
    private fun parseExpresion(): Nodo {
        profundidad++
        if (profundidad > MAX_PROFUNDIDAD) {
            throw error("Demasiados paréntesis anidados (máximo $MAX_PROFUNDIDAD)")
        }
        var izq = parseTermino()
        while (true) {
            saltarEspacios()
            when {
                consumir('+') -> {
                    val a = izq
                    val b = parseTermino()
                    izq = Nodo { x, y -> a.ev(x, y) + b.ev(x, y) }
                }
                consumir('-') -> {
                    val a = izq
                    val b = parseTermino()
                    izq = Nodo { x, y -> a.ev(x, y) - b.ev(x, y) }
                }
                else -> {
                    profundidad--
                    return izq
                }
            }
        }
    }

    // Resuelve multiplicaciones, divisiones y multiplicación pegada (ej. 2x)
    private fun parseTermino(): Nodo {
        var izq = parseUnario()
        while (true) {
            saltarEspacios()
            when {
                consumir('*') -> {
                    val a = izq
                    val b = parseUnario()
                    izq = Nodo { x, y -> a.ev(x, y) * b.ev(x, y) }
                }
                consumir('/') -> {
                    val a = izq
                    val b = parseUnario()
                    izq = Nodo { x, y -> a.ev(x, y) / b.ev(x, y) }
                }
                iniciaFactor() -> {
                    // Aquí detectamos cosas como 2x o 3(x+1) sin poner *
                    val a = izq
                    val b = parseUnario()
                    izq = Nodo { x, y -> a.ev(x, y) * b.ev(x, y) }
                }
                else -> return izq
            }
        }
    }

    // Revisa signos + o - al inicio de un factor
    private fun parseUnario(): Nodo {
        saltarEspacios()
        if (consumir('-')) {
            val a = parseUnario()
            return Nodo { x, y -> -a.ev(x, y) }
        }
        if (consumir('+')) {
            return parseUnario()
        }
        return parsePotencia()
    }

    // Resuelve potencias con ^
    private fun parsePotencia(): Nodo {
        val base = parsePrimario()
        saltarEspacios()
        if (consumir('^')) {
            val expNode = parseUnario()
            return Nodo { x, y -> base.ev(x, y).pow(expNode.ev(x, y)) }
        }
        return base
    }

    // Lee números, variables, funciones o expresiones entre paréntesis
    private fun parsePrimario(): Nodo {
        saltarEspacios()
        if (pos >= texto.length) throw error("La expresión termina de forma inesperada")
        val c = texto[pos]
        if (consumir('(')) {
            val n = parseExpresion()
            saltarEspacios()
            if (!consumir(')')) throw error("Falta cerrar un paréntesis ')'")
            return n
        }
        if (c.isDigit() || c == '.') return parseNumero()
        if (c.isLetter()) return parseIdentificador()
        throw error("Carácter inesperado '$c'")
    }

    // Lee un número decimal o con notación científica (como 1e-3)
    private fun parseNumero(): Nodo {
        val ini = pos
        while (pos < texto.length && (texto[pos].isDigit() || texto[pos] == '.')) {
            pos++
        }
        if (pos < texto.length && texto[pos] == 'e') {
            var p = pos + 1
            if (p < texto.length && (texto[p] == '+' || texto[p] == '-')) p++
            if (p < texto.length && texto[p].isDigit()) {
                while (p < texto.length && texto[p].isDigit()) p++
                pos = p
            }
        }
        val s = texto.substring(ini, pos)
        val valor = s.toDoubleOrNull() ?: throw error("Número inválido '$s'")
        return Nodo { _, _ -> valor }
    }

    // Lee funciones (sin, cos, exp...), variables (x, t, y) o constantes (pi, e)
    private fun parseIdentificador(): Nodo {
        val ini = pos
        while (pos < texto.length && texto[pos].isLetter()) pos++
        val nombre = texto.substring(ini, pos)
        val fn = funcion(nombre)
        if (fn != null) {
            saltarEspacios()
            if (!consumir('(')) throw error("La función '$nombre' necesita paréntesis")
            val arg = parseExpresion()
            saltarEspacios()
            if (!consumir(')')) throw error("Falta cerrar el paréntesis de $nombre(...)")
            return Nodo { x, y -> fn(arg.ev(x, y)) }
        }
        return when (nombre) {
            "x", "t" -> {
                usaX = true
                Nodo { x, _ -> x }
            }
            "y" -> {
                usaY = true
                Nodo { _, y -> y }
            }
            "pi" -> Nodo { _, _ -> Math.PI }
            "e" -> Nodo { _, _ -> Math.E }
            else -> throw error("No reconozco '$nombre'. Variables: x (o t) y y.")
        }
    }

    private fun funcion(n: String): ((Double) -> Double)? = when (n) {
        "sin", "sen" -> ::sin
        "cos" -> ::cos
        "tan" -> ::tan
        "asin" -> ::asin
        "acos" -> ::acos
        "atan" -> ::atan
        "sinh" -> ::sinh
        "cosh" -> ::cosh
        "tanh" -> ::tanh
        "exp" -> ::exp
        "ln" -> ::ln
        "log" -> ::log10
        "sqrt" -> ::sqrt
        "abs" -> ::abs
        else -> null
    }

    private fun iniciaFactor(): Boolean {
        if (pos >= texto.length) return false
        val c = texto[pos]
        return c.isLetter() || c.isDigit() || c == '.' || c == '('
    }

    private fun consumir(c: Char): Boolean =
        if (pos < texto.length && texto[pos] == c) {
            pos++
            true
        } else false

    private fun saltarEspacios() {
        while (pos < texto.length && texto[pos].isWhitespace()) pos++
    }

    private fun error(mensaje: String): IllegalArgumentException {
        val p = (pos + 1).coerceIn(1, texto.length.coerceAtLeast(1))
        return IllegalArgumentException("$mensaje (posición $p en \"$texto\").")
    }
}

// Aquí están los métodos de Euler y RK4 más utilidades de formato
object Metodos {
    const val MAX_PASOS = 50_000

    // Revisa que el número no sea NaN ni infinito
    fun finito(v: Double): Boolean = !v.isNaN() && !v.isInfinite()

    // Valida los parámetros antes de calcular
    fun validar(f: Expresion?, x0: Double, y0: Double, h: Double, n: Int) {
        require(f != null) { "La función f(x, y) es obligatoria." }
        require(finito(x0)) { "x0 debe ser un número finito." }
        require(finito(y0)) { "y0 debe ser un número finito." }
        require(finito(h) && h > 0.0) { "El tamaño de paso h debe ser un número finito mayor que 0." }
        require(n >= 0) { "El número de pasos no puede ser negativo." }
        require(n <= MAX_PASOS) { "Demasiados pasos ($n). El máximo es $MAX_PASOS." }
    }

    // Calcula la solución aproximada con Euler
    fun euler(f: Expresion, x0: Double, y0: Double, h: Double, n: Int): DoubleArray {
        validar(f, x0, y0, h, n)
        val y = DoubleArray(n + 1)
        y[0] = y0
        for (i in 0 until n) {
            val x = x0 + i * h
            y[i + 1] = y[i] + h * f.evaluar(x, y[i])
        }
        return y
    }

    // Calcula la solución aproximada con Runge-Kutta de 4to orden
    fun rk4(f: Expresion, x0: Double, y0: Double, h: Double, n: Int): DoubleArray {
        validar(f, x0, y0, h, n)
        val y = DoubleArray(n + 1)
        y[0] = y0
        for (i in 0 until n) {
            val x = x0 + i * h
            val yi = y[i]
            val k1 = f.evaluar(x, yi)
            val k2 = f.evaluar(x + h / 2.0, yi + k1 * h / 2.0)
            val k3 = f.evaluar(x + h / 2.0, yi + k2 * h / 2.0)
            val k4 = f.evaluar(x + h, yi + k3 * h)
            y[i + 1] = yi + (h / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4)
        }
        return y
    }

    // Calcula cuántos pasos n se necesitan para ir de x0 a xf con paso h
    fun pasosPara(x0: Double, xf: Double, h: Double): Int {
        require(finito(x0) && finito(xf)) { "x0 y x final deben ser números finitos." }
        require(finito(h) && h > 0.0) { "El tamaño de paso h debe ser mayor que 0." }
        require(xf > x0) { "El x final debe ser mayor que x0." }
        val bruto = (xf - x0) / h
        require(finito(bruto)) { "El intervalo es demasiado grande para ese h." }
        require(bruto >= 1.0 - 1e-9) { "h es mayor que el intervalo [x0, x final]." }
        val n = ceil(bruto - 1e-9).toLong()
        require(n <= MAX_PASOS) { "Demasiados pasos ($n). El máximo es $MAX_PASOS." }
        return n.toInt()
    }

    // Arma el texto paso a paso de Euler
    fun detalleEuler(f: Expresion, x: Double, y: Double, h: Double, i: Int): String {
        val fv = f.evaluar(x, y)
        val y1 = y + h * fv
        return buildString {
            append("MÉTODO DE EULER (paso i = $i)\n")
            append("  y(i+1) = y(i) + h·f(x(i), y(i))\n")
            append("  x=${num(x)}, y=${num(y)}, h=${num(h)}, f=${num(fv)}\n")
            append("  y(i+1) = ${num(y1)}\n")
        }
    }

    // Arma el texto paso a paso de RK4
    fun detalleRK4(f: Expresion, x: Double, y: Double, h: Double, i: Int): String {
        val k1 = f.evaluar(x, y)
        val k2 = f.evaluar(x + h / 2.0, y + k1 * h / 2.0)
        val k3 = f.evaluar(x + h / 2.0, y + k2 * h / 2.0)
        val k4 = f.evaluar(x + h, y + k3 * h)
        val y1 = y + (h / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4)
        return buildString {
            append("MÉTODO DE RK4 (paso i = $i)\n")
            append("  k1=${num(k1)}, k2=${num(k2)}, k3=${num(k3)}, k4=${num(k4)}\n")
            append("  y(i+1) = ${num(y1)}\n")
        }
    }

    fun fmt(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "Infinito" else "-Infinito"
        val a = abs(v)
        if (a != 0.0 && (a >= 1e9 || a < 1e-6)) return String.format(Locale.US, "%.6e", v)
        return String.format(Locale.US, "%.6f", v)
    }

    fun num(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "Infinito" else "-Infinito"
        val a = abs(v)
        if (a != 0.0 && (a >= 1e9 || a < 1e-6)) return String.format(Locale.US, "%.4e", v)
        var s = String.format(Locale.US, "%.6f", v)
        if ('.' in s) {
            s = s.trimEnd('0').trimEnd('.')
        }
        return if (s == "-0" || s.isEmpty()) "0" else s
    }

    fun fmtError(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return "Infinito"
        return String.format(Locale.US, "%.3e", v)
    }
}

// Calcula errores y orden de convergencia
object Analisis {
    private fun validarSeries(ref: DoubleArray, aprox: DoubleArray) {
        require(ref.size == aprox.size) { "Series de distinto tamaño (${ref.size} vs ${aprox.size})." }
        require(ref.isNotEmpty()) { "Las series están vacías." }
    }

    fun errorMaximo(ref: DoubleArray, aprox: DoubleArray): Double {
        validarSeries(ref, aprox)
        var max = 0.0
        for (i in ref.indices) {
            val e = abs(ref[i] - aprox[i])
            if (!Metodos.finito(e)) return Double.NaN
            if (e > max) max = e
        }
        return max
    }

    fun errorFinal(ref: DoubleArray, aprox: DoubleArray): Double {
        validarSeries(ref, aprox)
        val e = abs(ref.last() - aprox.last())
        return if (Metodos.finito(e)) e else Double.NaN
    }

    fun factorMejora(errorGrueso: Double, errorFino: Double): Double {
        if (!Metodos.finito(errorGrueso) || !Metodos.finito(errorFino) ||
            errorGrueso <= 0.0 || errorFino < 0.0
        ) return Double.NaN
        if (errorFino == 0.0) return Double.POSITIVE_INFINITY
        return errorGrueso / errorFino
    }

    fun ordenEmpirico(errorGrueso: Double, errorFino: Double, factorH: Double): Double {
        if (!Metodos.finito(errorGrueso) || !Metodos.finito(errorFino) ||
            errorGrueso <= 0.0 || errorFino <= 0.0 || !(factorH > 1.0)
        ) return Double.NaN
        return ln(errorGrueso / errorFino) / ln(factorH)
    }

    fun hayNoFinitos(vararg series: DoubleArray?): Boolean =
        series.any { arr -> arr?.any { !Metodos.finito(it) } == true }
}