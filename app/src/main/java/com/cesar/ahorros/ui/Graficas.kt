package com.cesar.ahorros.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cesar.ahorros.data.EstiloGraficas
import kotlin.math.max
import kotlin.math.min

data class PuntoBarra(val etiqueta: String, val ahorro: Double, val gasto: Double)

data class PuntoDona(val etiqueta: String, val valor: Double, val color: Color)

/**
 * Barras agrupadas: Ahorros vs Gastos por periodo.
 *
 * No se distinguen SOLO por color: ademas del tono, la barra de ahorro tiene
 * las esquinas SUPERIORES redondeadas y la de gasto las INFERIORES. Es la doble
 * codificacion que exige WCAG 1.4.1 (el color nunca puede ser la unica
 * senal). Se dibuja con Canvas nativo: cero librerias, render a 60 fps.
 */
@Composable
fun GraficaBarras(
    puntos: List<PuntoBarra>,
    modifier: Modifier = Modifier
) {
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = TextoSecundario)

    // El progreso con el que se dibuja la gráfica ahora mismo. Con las
    // animaciones apagadas vale 1: se ve todo de una, como siempre.
    val progreso = progresoDeGraficas()
    val estilo = AparienciaActual.estiloGraficas

    Canvas(
        modifier = modifier
            .height(190.dp)
            // El estilo "Suave" no mueve nada: solo aparece.
            .graphicsLayer {
                if (estilo == EstiloGraficas.SUAVE) alpha = progreso
            }
    ) {
        if (puntos.isEmpty()) return@Canvas

        val maximo = (puntos.maxOf { max(it.ahorro, it.gasto) }).coerceAtLeast(1.0)
        val margenIzq = 36.dp.toPx()
        val margenInf = 20.dp.toPx()
        val anchoUtil = size.width - margenIzq
        val altoUtil = size.height - margenInf
        val anchoGrupo = anchoUtil / puntos.size
        val anchoBarra = min(anchoGrupo * 0.3f, 13.dp.toPx())
        val separacion = 2.dp.toPx()
        val radio = 5.dp.toPx()

        // Cuánto crecimiento tienen las barras. En el estilo "Suave" y en
        // "Ninguna" las barras ya están completas: solo se funden.
        val crecimiento = when (estilo) {
            EstiloGraficas.CRECER -> progreso
            else -> 1f
        }

        // Rejilla + valores del eje Y
        for (i in 0..3) {
            val y = altoUtil * i / 3f
            drawLine(
                color = Hairline,
                start = Offset(margenIzq, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            val valor = maximo * (3 - i) / 3.0
            val medicion = medidor.measure(AnnotatedString(valor.dineroCorto()), style = estiloEje)
            drawText(
                medicion,
                topLeft = Offset(
                    margenIzq - medicion.size.width - 5.dp.toPx(),
                    y - medicion.size.height / 2f
                )
            )
        }

        puntos.forEachIndexed { indice, punto ->
            val centro = margenIzq + anchoGrupo * indice + anchoGrupo / 2f
            val alturaAhorro = (punto.ahorro / maximo * altoUtil * crecimiento).toFloat()
            val alturaGasto = (punto.gasto / maximo * altoUtil * crecimiento).toFloat()

            // Ahorro: redondeo arriba (decrece hacia abajo)
            if (alturaAhorro > 0f) {
                drawPath(
                    barraRedondeada(
                        x = centro - anchoBarra - separacion,
                        y = altoUtil - alturaAhorro,
                        ancho = anchoBarra,
                        alto = alturaAhorro,
                        radioSuperior = radio,
                        radioInferior = 0f
                    ),
                    brush = Brush.verticalGradient(
                        listOf(AcentoMenta, AcentoMenta.copy(alpha = 0.72f)),
                        startY = altoUtil - alturaAhorro,
                        endY = altoUtil
                    )
                )
            }
            // Gasto: redondeo abajo
            if (alturaGasto > 0f) {
                drawPath(
                    barraRedondeada(
                        x = centro + separacion,
                        y = altoUtil - alturaGasto,
                        ancho = anchoBarra,
                        alto = alturaGasto,
                        radioSuperior = 0f,
                        radioInferior = radio
                    ),
                    color = ColorGasto
                )
            }
        }

        // Etiqueta del eje X: se omiten las que no entran, para que no se amontonen.
        var derechaAnterior = -1f
        puntos.forEachIndexed { indice, punto ->
            val centro = margenIzq + anchoGrupo * indice + anchoGrupo / 2f
            val etiqueta = medidor.measure(AnnotatedString(punto.etiqueta), style = estiloEje)
            val xTexto = (centro - etiqueta.size.width / 2f).coerceIn(0f, size.width - etiqueta.size.width)
            if (xTexto >= derechaAnterior + 6.dp.toPx()) {
                drawText(etiqueta, topLeft = Offset(xTexto, altoUtil + 4.dp.toPx()))
                derechaAnterior = xTexto + etiqueta.size.width
            }
        }
    }
}

/** Barra con esquinas redondeadas solo arriba o solo abajo. */
private fun barraRedondeada(
    x: Float,
    y: Float,
    ancho: Float,
    alto: Float,
    radioSuperior: Float,
    radioInferior: Float
): Path {
    val rs = min(radioSuperior, min(ancho / 2f, alto))
    val ri = min(radioInferior, min(ancho / 2f, alto))
    return Path().apply {
        moveTo(x, y + rs)
        lineTo(x, y + alto - ri)
        if (ri > 0f) quadraticTo(x, y + alto, x + ri, y + alto) else lineTo(x, y + alto)
        lineTo(x + ancho - ri, y + alto)
        if (ri > 0f) quadraticTo(x + ancho, y + alto, x + ancho, y + alto - ri) else lineTo(x + ancho, y + alto)
        lineTo(x + ancho, y + rs)
        if (rs > 0f) quadraticTo(x + ancho, y, x + ancho - rs, y) else lineTo(x + ancho, y)
        lineTo(x + rs, y)
        if (rs > 0f) quadraticTo(x, y, x, y + rs) else lineTo(x, y)
        close()
    }
}

/**
 * Dona de categorias con el total al centro.
 * Los segmentos se separan con 2dp del color del fondo: sobre fondo oscuro
 * es matematicamente imposible que 6 categorias sean 3:1 entre si, asi que
 * el separador garantiza el contraste de cada segmento con su entorno.
 */
@Composable
fun GraficaDona(
    puntos: List<PuntoDona>,
    modifier: Modifier = Modifier
) {
    val total = puntos.sumOf { it.valor }
    val progreso = progresoDeGraficas()
    val estilo = AparienciaActual.estiloGraficas

    Box(
        modifier = modifier.graphicsLayer {
            if (estilo == EstiloGraficas.SUAVE) alpha = progreso
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (puntos.isEmpty() || total <= 0.0) return@Canvas
            val grosor = 20.dp.toPx()
            val separador = 2.dp.toPx()
            val diametro = min(size.width, size.height) - grosor
            val topLeft = Offset((size.width - diametro) / 2f, (size.height - diametro) / 2f)
            var angulo = -90f
            puntos.forEach { punto ->
                val barrido = (punto.valor / total * 360.0).toFloat()
                // Con el estilo "Crecer" la dona se abre como un abanico:
                // el barrido crece con el progreso.
                val avance = if (estilo == EstiloGraficas.CRECER) barrido * progreso else barrido
                if (avance <= 0f) return@forEach
                // Separador: se dibuja el mismo arco con el color del fondo, mas grueso.
                drawArc(
                    color = Fondo,
                    startAngle = angulo,
                    sweepAngle = avance,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diametro, diametro),
                    style = Stroke(width = grosor + separador)
                )
                drawArc(
                    color = punto.color,
                    startAngle = angulo,
                    sweepAngle = avance,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diametro, diametro),
                    style = Stroke(width = grosor)
                )
                angulo += avance
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GASTOS",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Text(
                text = total.dinero(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum"
                ),
                color = TextoPrimario
            )
        }
    }
}

/** Leyenda de la dona: punto de color, etiqueta, valor y porcentaje. */
@Composable
fun LeyendaDona(puntos: List<PuntoDona>, modifier: Modifier = Modifier) {
    val total = puntos.sumOf { it.valor }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        puntos.forEach { punto ->
            val porcentaje = if (total > 0.0) (punto.valor / total * 100).toInt() else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(punto.color)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = punto.etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoPrimario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = punto.valor.dineroSimbolo(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = TextoPrimario
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$porcentaje%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

/** Área con línea: evolución acumulada del balance. */
@Composable
fun GraficaArea(
    valores: List<Double>,
    modifier: Modifier = Modifier
) {
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = TextoSecundario)

    val progreso = progresoDeGraficas()
    val estilo = AparienciaActual.estiloGraficas

    // Con el estilo "Dibujar" la línea se escribe de izquierda a derecha:
    // solo se dibuja la parte que ya "llegó".
    val cuantos = when (estilo) {
        EstiloGraficas.DIBUJAR -> (valores.size * progreso).toInt().coerceIn(2, valores.size)
        else -> valores.size
    }
    val visibles = if (estilo == EstiloGraficas.DIBUJAR) valores.take(cuantos) else valores

    Canvas(
        modifier = modifier
            .height(170.dp)
            .graphicsLayer {
                if (estilo == EstiloGraficas.SUAVE) alpha = progreso
            }
    ) {
        if (visibles.size < 2) return@Canvas

        val maximo = valores.max()
        val minimo = valores.min()
        val rango = (maximo - minimo).takeIf { it > 0.0 } ?: 1.0
        val margenIzq = 38.dp.toPx()
        val margenInf = 16.dp.toPx()
        val anchoUtil = size.width - margenIzq
        val altoUtil = size.height - margenInf
        // El eje se mantiene completo: la línea es la que se dibuja.
        val paso = anchoUtil / (valores.size - 1)
        val color = if (valores.last() < 0) ColorGasto else AcentoMenta

        fun yDe(valor: Double): Float =
            altoUtil - (((valor - minimo) / rango) * altoUtil).toFloat()

        for (i in 0..3) {
            val y = altoUtil * i / 3f
            drawLine(
                color = Hairline,
                start = Offset(margenIzq, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            val valor = minimo + rango * (3 - i) / 3.0
            val medicion = medidor.measure(AnnotatedString(valor.dineroCorto()), style = estiloEje)
            drawText(
                medicion,
                topLeft = Offset(
                    margenIzq - medicion.size.width - 5.dp.toPx(),
                    y - medicion.size.height / 2f
                )
            )
        }

        val linea = Path()
        visibles.forEachIndexed { indice, valor ->
            val x = margenIzq + paso * indice
            val y = yDe(valor)
            if (indice == 0) linea.moveTo(x, y) else linea.lineTo(x, y)
        }

        val relleno = Path().apply {
            addPath(linea)
            lineTo(margenIzq + paso * (visibles.size - 1), altoUtil)
            lineTo(margenIzq, altoUtil)
            close()
        }
        drawPath(
            relleno,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = altoUtil
            )
        )
        drawPath(
            linea,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Punto en el ultimo valor: ancla la lectura del grafico.
        val ultimoX = margenIzq + paso * (visibles.size - 1)
        val ultimoY = yDe(visibles.last())
        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(ultimoX, ultimoY))
        drawCircle(color = Fondo, radius = 1.6.dp.toPx(), center = Offset(ultimoX, ultimoY))
    }
}
