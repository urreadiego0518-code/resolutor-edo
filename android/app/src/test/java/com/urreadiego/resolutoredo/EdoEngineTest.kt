package com.urreadiego.resolutoredo


import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas sencillas para revisar que el motor de EDO calcule bien
class EdoEngineTest {

    // Revisa el ejemplo del cuaderno y comprueba que RK4 tenga menos error que Euler
    @Test
    fun ejemploCuaderno_rk4EsMasPrecisoQueEuler() {
        val f = Expresion("x - y")
        val n = Metodos.pasosPara(0.0, 2.0, 0.1)
        val ye = Metodos.euler(f, 0.0, 1.0, 0.1, n)
        val yr = Metodos.rk4(f, 0.0, 1.0, 0.1, n)
        val exacta = Expresion("x - 1 + 2*exp(-x)")
        val ref = DoubleArray(n + 1) { i -> exacta.evaluar(0.0 + i * 0.1, Double.NaN) }

        val errE = Analisis.errorFinal(ref, ye)
        val errR = Analisis.errorFinal(ref, yr)

        assertTrue(errR < errE)
        assertTrue(errR < 1e-6)
    }

    // Revisa multiplicación pegada (como 2x) y funciones como sin
    @Test
    fun multiplicacionImplicitaYFunciones() {
        val expr = Expresion("2x + sin(pi/2)")
        assertEquals(5.0, expr.evaluar(2.0, 0.0), 1e-9)
    }

    // Revisa que marque error si h es negativo
    @Test(expected = IllegalArgumentException::class)
    fun rechazaPasoNegativo() {
        Metodos.euler(Expresion("x - y"), 0.0, 1.0, -0.1, 10)
    }
}