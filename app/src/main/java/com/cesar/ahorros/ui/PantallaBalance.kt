package com.cesar.ahorros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.R
import java.time.LocalDate

enum class Periodo(val etiqueta: String, val dias: Int) {
    SEMANA("7 días", 7),
    MES("30 días", 30),
    TRIMESTRE("90 días", 90)
}

/**
 * Balance: savings + expenses together with charts and statistics.
 * Style: dashboard empresarial (tarjetas KPI, barras, dona, área y tabla).
 */
@Composable
fun PantallaBalance(movimientos: List<Movimiento>, onAjustes: () -> Unit) {
    // El periodo con el que se abre el Balance: 7 días. Lo que la persona
    // mira casi siempre es "cómo venía la semana", y además son las gráficas
    // más legibles (7 barras, no 30).
    var periodo by remember { mutableStateOf(Periodo.SEMANA) }

    val totalAhorros = remember(movimientos) {
        movimientos.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto }
    }
    val totalGastos = remember(movimientos) {
        movimientos.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
    }
    val neto = totalAhorros - totalGastos

    val enPeriodo = remember(movimientos, periodo) {
        val desde = LocalDate.now().minusDays(periodo.dias - 1L)
        movimientos.filter { !it.fecha.isBefore(desde) }
    }

    // La cifra grande cuenta hasta su valor (si el usuario dejó las
    // animaciones prendidas).
    val netoAnimado = rememberNumeroAnimado(neto)

    val puntosBarra = remember(enPeriodo, periodo) { construirBarras(enPeriodo, periodo) }
    val acumulado = remember(enPeriodo, periodo) { construirAcumulado(enPeriodo, periodo) }
    // La paleta de la dona depende del tema: si cambia, hay que volver a
    // construirla (por eso la clave del tema entra en el remember: la clave
    // incluye los colores que el usuario personalizó, no solo el tema).
    val porCategoria = remember(enPeriodo, TemaActual.clave) { construirCategorias(enPeriodo) }
    val resumenTabla = remember(enPeriodo) { construirResumen(enPeriodo) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "encabezado") {
            // El engranaje (ajustes) va arriba a la derecha, junto al título.
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                    Text(
                        text = netoAnimado.value.dineroSimbolo(),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (neto < 0) ColorGasto else AcentoMenta,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (neto < 0) {
                            "Estás gastando más de lo que ahorrás"
                        } else {
                            "Ahorro menos gastos, acumulado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                IconButton(onClick = onAjustes, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ajustes),
                        contentDescription = "Ajustes",
                        tint = TextoSecundario,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item(key = "kpis") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TarjetaKpi(
                    titulo = "Ahorros",
                    valor = totalAhorros.dineroSimbolo(),
                    colorValor = AcentoMenta,
                    pie = "depositado",
                    modifier = Modifier.weight(1f),
                    valorAnimado = totalAhorros,
                    clave = periodo
                )
                TarjetaKpi(
                    titulo = "Gastos",
                    valor = totalGastos.dineroSimbolo(),
                    colorValor = ColorGasto,
                    pie = "consumido",
                    modifier = Modifier.weight(1f),
                    valorAnimado = totalGastos,
                    clave = periodo
                )
            }
        }

        item(key = "selector-periodo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Periodo.entries.forEach { opcion ->
                    FilterChip(
                        selected = periodo == opcion,
                        onClick = { periodo = opcion },
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }
        }

        item(key = "grafica-barras") {
            TarjetaPanel(titulo = "Ahorros vs gastos · ${periodo.etiqueta}", clave = periodo) {
                GraficaBarras(puntos = puntosBarra, clave = periodo, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                LeyendaGrafica(
                    elementos = listOf(
                        Triple("↑ AHORRO", AcentoMenta, "Entradas"),
                        Triple("↓ GASTO", ColorGasto, "Salidas")
                    )
                )
            }
        }

        item(key = "grafica-area") {
            TarjetaPanel(titulo = "Balance acumulado", clave = periodo) {
                GraficaArea(valores = acumulado, clave = periodo, modifier = Modifier.fillMaxWidth())
            }
        }

        item(key = "grafica-dona") {
            TarjetaPanel(titulo = "Gastos por categoría", clave = periodo) {
                if (porCategoria.isEmpty()) {
                    Text(
                        text = "Sin gastos registrados en este periodo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GraficaDona(
                            puntos = porCategoria,
                            clave = periodo,
                            modifier = Modifier.size(116.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        LeyendaDona(
                            puntos = porCategoria,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item(key = "tabla") {
            TarjetaPanel(titulo = "Detalle por categoría", clave = periodo) {
                if (resumenTabla.isEmpty()) {
                    Text(
                        text = "Todavía no hay movimientos en este periodo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                } else {
                    TablaResumen(resumenTabla, clave = periodo)
                }
            }
        }
    }
}

private data class FilaResumen(
    val categoria: String,
    val movimientos: Int,
    val ahorro: Double,
    val gasto: Double
)

private fun construirBarras(movimientos: List<Movimiento>, periodo: Periodo): List<PuntoBarra> {
    val hoy = LocalDate.now()
    val buckets = mutableListOf<Pair<LocalDate, String>>()
    if (periodo == Periodo.TRIMESTRE) {
        val desde = hoy.minusDays(periodo.dias - 1L)
        var cursor = desde
        while (!cursor.isAfter(hoy)) {
            buckets += cursor to "%02d/%02d".format(cursor.dayOfMonth, cursor.monthValue)
            cursor = cursor.plusWeeks(1)
        }
    } else {
        val desde = hoy.minusDays(periodo.dias - 1L)
        for (i in 0 until periodo.dias) {
            val fecha = desde.plusDays(i.toLong())
            buckets += fecha to fecha.dayOfMonth.toString()
        }
    }
    return buckets.map { (fecha, etiqueta) ->
        val delDia = movimientos.filter { it.fecha == fecha }
        PuntoBarra(
            etiqueta = etiqueta,
            ahorro = delDia.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto },
            gasto = delDia.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
        )
    }
}

private fun construirAcumulado(movimientos: List<Movimiento>, periodo: Periodo): List<Double> {
    val hoy = LocalDate.now()
    val desde = hoy.minusDays(periodo.dias - 1L)
    var acumulado = 0.0
    val serie = mutableListOf<Double>()
    if (periodo == Periodo.TRIMESTRE) {
        var cursor = desde
        while (!cursor.isAfter(hoy)) {
            val deLaSemana = movimientos.filter { !it.fecha.isBefore(cursor) && it.fecha.isBefore(cursor.plusWeeks(1)) }
            acumulado += deLaSemana.sumOf { if (it.tipo == Tipo.AHORRO) it.monto else -it.monto }
            serie += acumulado
            cursor = cursor.plusWeeks(1)
        }
    } else {
        for (i in 0 until periodo.dias) {
            val fecha = desde.plusDays(i.toLong())
            val delDia = movimientos.filter { it.fecha == fecha }
            acumulado += delDia.sumOf { if (it.tipo == Tipo.AHORRO) it.monto else -it.monto }
            serie += acumulado
        }
    }
    return serie
}

private fun construirCategorias(movimientos: List<Movimiento>): List<PuntoDona> {
    val gastos = movimientos.filter { it.tipo == Tipo.GASTO }
    if (gastos.isEmpty()) return emptyList()
    return gastos.groupBy { it.categoria }
        .map { (categoria, lista) -> PuntoDona(categoria, lista.sumOf { it.monto }, Color(0xFF000000)) }
        .sortedByDescending { it.valor }
        .mapIndexed { indice, punto ->
            punto.copy(color = PaletaCategorica[indice % PaletaCategorica.size])
        }
}

private fun construirResumen(movimientos: List<Movimiento>): List<FilaResumen> {
    return movimientos.groupBy { it.categoria }
        .map { (categoria, lista) ->
            FilaResumen(
                categoria = categoria,
                movimientos = lista.size,
                ahorro = lista.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto },
                gasto = lista.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
            )
        }
        .sortedByDescending { it.ahorro + it.gasto }
}

@Composable
private fun TarjetaPanel(titulo: String, clave: Any? = null, contenido: @Composable () -> Unit) {
    // La entrada del panel: cómo se asienta, se desliza o se funde depende
    // del estilo que eligió el usuario en Ajustes. La clave reinicia la
    // animación cuando cambian los datos (al cambiar de periodo, el panel se
    // vuelve a asentar en vez de cambiar de golpe).
    val entrada = progresoDeEntrada(clave)
    val estilo = AparienciaActual.estiloInterfaz

    Card(
        colors = CardDefaults.cardColors(containerColor = Superficie),
        shape = RoundedCornerShape(RadioTarjeta),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (estilo == EstiloInterfaz.ESCALAR) 1f else entrada
                val escala = if (estilo == EstiloInterfaz.ESCALAR) {
                    0.94f + 0.06f * entrada
                } else {
                    1f
                }
                scaleX = escala
                scaleY = escala
                if (estilo == EstiloInterfaz.DESLIZAR) {
                    translationY = (1f - entrada) * 18f
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Spacer(Modifier.height(14.dp))
            contenido()
        }
    }
}

@Composable
private fun LeyendaGrafica(elementos: List<Triple<String, Color, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        elementos.forEach { (nombre, color, _) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Composable
private fun TablaResumen(filas: List<FilaResumen>, clave: Any? = null) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CATEGORÍA",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = "Nº",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.End
            )
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(8.dp))
        filas.forEachIndexed { indice, fila ->
            val total = fila.ahorro - fila.gasto
            // Las filas entran una detrás de otra (escalonado): así se nota
            // que la tabla cambió y no que saltó de golpe.
            val entrada = progresoDeFila(indice, clave)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entrada
                        translationX = (1f - entrada) * 24f
                    }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (fila.ahorro > 0) AcentoMenta else ColorGasto
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fila.categoria,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoPrimario,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = fila.movimientos.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.weight(0.5f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = kotlin.math.abs(total).dineroSimbolo(),
                    style = EstiloDinero,
                    color = if (total < 0) ColorGasto else AcentoMenta,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
