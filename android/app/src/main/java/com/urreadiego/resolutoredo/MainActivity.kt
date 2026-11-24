package com.urreadiego.resolutoredo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// Guarda todo lo calculado para mostrarlo en la pantalla
private data class SolucionUi(
    val xs: DoubleArray,
    val yEuler: DoubleArray,
    val yRk4: DoubleArray,
    val yRef: DoubleArray,
    val pasoTexto: String,
    val resumenTexto: String,
    val convergenciaTexto: String
)

// Actividad principal de la app en Android
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ResolutorEdoScreen()
                }
            }
        }
    }
}

@Composable
private fun ResolutorEdoScreen() {
    val ejemplos = remember { Ejemplo.catalogo() }
    val inicial = ejemplos.first()

    // Estados de los campos de entrada
    var ecText by remember { mutableStateOf(inicial.f ?: "x - y") }
    var exactaText by remember { mutableStateOf(inicial.exacta) }
    var x0Text by remember { mutableStateOf(Metodos.num(inicial.x0)) }
    var y0Text by remember { mutableStateOf(Metodos.num(inicial.y0)) }
    var hText by remember { mutableStateOf(Metodos.num(inicial.h)) }
    var xfText by remember { mutableStateOf(Metodos.num(inicial.xf)) }

    // Estados de pestañas, errores y resultados
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var pasoIdx by remember { mutableIntStateOf(0) }
    var solucion by remember { mutableStateOf<SolucionUi?>(null) }

    // Lee los datos, corre Euler y RK4 y arma los textos/gráfica
    fun calcularTodo() {
        try {
            val f = Expresion(ecText)
            val x0 = Expresion(x0Text.replace(',', '.')).evaluar(0.0, 0.0)
            val y0 = Expresion(y0Text.replace(',', '.')).evaluar(0.0, 0.0)
            val h = Expresion(hText.replace(',', '.')).evaluar(0.0, 0.0)
            val xf = Expresion(xfText.replace(',', '.')).evaluar(0.0, 0.0)
            val n = Metodos.pasosPara(x0, xf, h)

            val exExpr = if (exactaText.isNotBlank()) Expresion(exactaText) else null
            require(exExpr?.dependeDeY() != true) { "La exacta solo debe usar x." }

            val xs = DoubleArray(n + 1) { i -> x0 + i * h }
            val ye = Metodos.euler(f, x0, y0, h, n)
            val yr = Metodos.rk4(f, x0, y0, h, n)

            // Si no hay fórmula exacta, usamos RK4 con paso más fino como referencia
            val ref = if (exExpr != null) {
                DoubleArray(n + 1) { i -> exExpr.evaluar(xs[i], Double.NaN) }
            } else {
                val m = 10
                val fino = Metodos.rk4(f, x0, y0, h / m, n * m)
                DoubleArray(n + 1) { i -> fino[i * m] }
            }

            val pIdx = pasoIdx.coerceIn(0, (n - 1).coerceAtLeast(0))
            val pasoTxt = "${Metodos.detalleEuler(f, xs[pIdx], ye[pIdx], h, pIdx)}\n" +
                    Metodos.detalleRK4(f, xs[pIdx], yr[pIdx], h, pIdx)

            val errFE = Analisis.errorFinal(ref, ye)
            val errFR = Analisis.errorFinal(ref, yr)
            val resumenTxt = buildString {
                append("Pasos n=$n, h=${Metodos.num(h)}\n")
                append("Err final Euler: ${Metodos.fmtError(errFE)}\n")
                append("Err final RK4:   ${Metodos.fmtError(errFR)}\n")
                val fac = Analisis.factorMejora(errFE, errFR)
                if (Metodos.finito(fac)) append("Mejora RK4 vs Euler: ${Metodos.num(fac)}x")
            }

            // Estimación rápida del orden dividiendo h entre 2
            val e1 = Analisis.errorFinal(ref, ye)
            val e2 = Analisis.errorFinal(
                DoubleArray(n * 2 + 1) { i -> exExpr?.evaluar(x0 + i * (h / 2.0), Double.NaN) ?: 0.0 },
                Metodos.euler(f, x0, y0, h / 2.0, n * 2)
            )
            val r1 = Analisis.errorFinal(ref, yr)
            val r2 = Analisis.errorFinal(
                DoubleArray(n * 2 + 1) { i -> exExpr?.evaluar(x0 + i * (h / 2.0), Double.NaN) ?: 0.0 },
                Metodos.rk4(f, x0, y0, h / 2.0, n * 2)
            )
            val convTxt = buildString {
                append("Al reducir h a la mitad (h -> h/2):\n")
                append("Orden empírico Euler: p ≈ ${Metodos.num(Analisis.ordenEmpirico(e1, e2, 2.0))} (teoría 1)\n")
                append("Orden empírico RK4:   p ≈ ${Metodos.num(Analisis.ordenEmpirico(r1, r2, 2.0))} (teoría 4)")
            }

            solucion = SolucionUi(xs, ye, yr, ref, pasoTxt, resumenTxt, convTxt)
            errorMsg = null
        } catch (ex: Exception) {
            errorMsg = ex.message ?: "Error al calcular"
        }
    }

    // Calcula el primer ejemplo al abrir la pantalla
    LaunchedEffect(Unit) { calcularTodo() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Resolutor de EDO (Euler y RK4)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        // Fila para escribir la ecuación y resolver
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ecText,
                onValueChange = { ecText = it },
                label = { Text("dy/dx = f(x, y)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = { calcularTodo() }) {
                Text("Resolver")
            }
        }

        Spacer(Modifier.height(6.dp))

        // Campos de x0, y0, h y xf
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(x0Text, { x0Text = it }, label = { Text("x0") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(y0Text, { y0Text = it }, label = { Text("y0") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(hText, { hText = it }, label = { Text("h") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(xfText, { xfText = it }, label = { Text("xf") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Spacer(Modifier.height(6.dp))

        // Campo opcional para solución exacta
        OutlinedTextField(
            value = exactaText,
            onValueChange = { exactaText = it },
            label = { Text("y(x) exacta (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMsg != null) {
            Spacer(Modifier.height(6.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        // Pestañas de resultados
        ScrollableTabRow(selectedTabIndex = tabIndex) {
            listOf("Gráfica", "Tabla", "Paso a paso", "Convergencia").forEachIndexed { i, title ->
                Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(title) })
            }
        }

        Spacer(Modifier.height(8.dp))
        val sol = solucion
        if (sol != null) {
            when (tabIndex) {
                0 -> {
                    Text(sol.resumenTexto, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    GraficaCompose(
                        xs = sol.xs,
                        yEuler = sol.yEuler,
                        yRk4 = sol.yRk4,
                        yRef = sol.yRef,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
                1 -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(sol.xs.toList()) { i, xVal ->
                            val rErr = abs(sol.yRef[i] - sol.yRk4[i])
                            Text(
                                "i=$i x=${Metodos.num(xVal)} | Eu=${Metodos.fmt(sol.yEuler[i])} | RK4=${Metodos.fmt(sol.yRk4[i])} | eR=${Metodos.fmtError(rErr)}",
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (pasoIdx > 0) { pasoIdx--; calcularTodo() } }) { Text("Paso -") }
                        Text("i = $pasoIdx", modifier = Modifier.padding(top = 10.dp))
                        Button(onClick = { if (pasoIdx < sol.xs.size - 2) { pasoIdx++; calcularTodo() } }) { Text("Paso +") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(sol.pasoTexto, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }
                3 -> {
                    Text(sol.convergenciaTexto, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Dibuja las curvas de referencia, Euler y RK4 en pantalla
@Composable
private fun GraficaCompose(
    xs: DoubleArray,
    yEuler: DoubleArray,
    yRk4: DoubleArray,
    yRef: DoubleArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color.White)) {
        if (xs.size < 2) return@Canvas
        val xmin = xs.first()
        val xmax = xs.last()
        val finitos = (yRef.toList() + yRk4.toList() + yEuler.toList()).filter { Metodos.finito(it) }
        if (finitos.isEmpty() || xmax <= xmin) return@Canvas
        val ymin = finitos.minOrNull()!!
        val ymax = finitos.maxOrNull()!!
        val ySpan = (ymax - ymin).takeIf { it > 1e-12 } ?: 1.0

        fun dibujarLinea(ys: DoubleArray, color: Color, strokeWidth: Float) {
            val path = Path()
            var started = false
            for (i in ys.indices) {
                val v = ys[i]
                if (!Metodos.finito(v)) {
                    started = false
                    continue
                }
                val px = (((xs[i] - xmin) / (xmax - xmin)) * size.width).toFloat()
                val py = (size.height - (((v - ymin) / ySpan) * size.height)).toFloat()
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            }
            drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
        }

        dibujarLinea(yRef, Color(0xFF78C88C), 10f)
        dibujarLinea(yEuler, Color(0xFFD65428), 4f)
        dibujarLinea(yRk4, Color(0xFF1E64C8), 5f)
    }
}