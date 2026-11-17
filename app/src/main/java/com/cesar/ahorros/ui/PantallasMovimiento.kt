package com.cesar.ahorros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.dominio.Calculos
import java.time.LocalDate

private data class GrupoDia(
    val fecha: LocalDate,
    val movimientos: List<Movimiento>
) {
    val ahorro: Double get() = movimientos.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto }
    val gasto: Double get() = movimientos.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
}

/**
 * Pantalla de Ahorros o Gastos. Comparten estructura: encabezado con el
 * total y lista agrupada por día, con el encabezado de cada día plegable.
 */
@Composable
fun PantallaMovimiento(
    tipo: Tipo,
    movimientos: List<Movimiento>,
    balance: Double,
    colapsados: Set<String>,
    onAlternarDia: (String) -> Unit,
    onEditar: (Movimiento) -> Unit,
    onEliminar: (Movimiento) -> Unit
) {
    val esAhorro = tipo == Tipo.AHORRO
    val propios = movimientos.filter { it.tipo == tipo }
    // "Combinar ahorros con gastos": el dinero de los ahorros pasa a ser lo
    // que QUEDA (lo depositado menos lo gastado), así que al gastar, los
    // ahorros bajan. Sin la preferencia, son dos montones aparte.
    val combinar = PreferenciasActual.combinarAhorrosYGastos
    val total = if (esAhorro && combinar) {
        Calculos.totalAhorro(movimientos, combinarConGastos = true)
    } else {
        propios.sumOf { it.monto }
    }
    // Lo depositado sin descontar: cuando se combinan, se muestra aparte
    // (el número de arriba ya es el balance).
    val depositado = if (esAhorro) Calculos.totalAhorro(movimientos) else 0.0
    val colorPrincipal = if (esAhorro) AcentoMenta else ColorGasto

    val grupos = remember(movimientos, tipo) {
        // Cada pestaña muestra SOLO lo suyo (ahorros o gastos). El libro
        // completo del día, mezclado, se ve en la pestaña Balance.
        movimientos.filter { it.tipo == tipo }
            .groupBy { it.fecha }
            .map { (fecha, lista) -> GrupoDia(fecha, lista.sortedByDescending { it.ts }) }
            .sortedByDescending { it.fecha }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "encabezado") {
            EncabezadoPantalla(
                etiqueta = if (esAhorro) "AHORROS" else "GASTOS",
                total = total,
                color = colorPrincipal,
                pie = when {
                    // Con la preferencia prendida, el número ya viene restado:
                    // se dice de dónde sale.
                    esAhorro && combinar ->
                        "Después de tus gastos · ${propios.size} depósitos"
                    esAhorro ->
                        "Total depositado · ${propios.size} movimientos"
                    else ->
                        "Total gastado · ${propios.size} movimientos"
                },
                balance = balance,
                depositado = depositado,
                combinar = esAhorro && combinar,
                esAhorro = esAhorro
            )
            Spacer(Modifier.height(18.dp))
        }

        if (propios.isEmpty()) {
            item(key = "vacio") {
                EstadoVacio(
                    titulo = if (esAhorro) "Aún no tienes ahorros" else "Aún no tienes gastos",
                    mensaje = if (esAhorro) {
                        "Toca el botón + para registrar tu primer depósito."
                    } else {
                        "Toca el botón + para registrar tu primera compra."
                    }
                )
            }
        }

        grupos.forEach { grupo ->
            val clave = grupo.fecha.toString()
            val expandido = clave !in colapsados
            item(key = "dia-$clave") {
                Spacer(Modifier.height(10.dp))
                EncabezadoDia(
                    fecha = grupo.fecha,
                    ahorro = grupo.ahorro,
                    gasto = grupo.gasto,
                    expandido = expandido,
                    onAlternar = { onAlternarDia(clave) }
                )
            }
            if (expandido) {
                items(grupo.movimientos, key = { it.id }) { movimiento ->
                    Row(modifier = Modifier.padding(start = 6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Borde.copy(alpha = 0.7f))
                        )
                        Spacer(Modifier.width(10.dp))
                        FilaMovimiento(
                            movimiento = movimiento,
                            onEditar = { onEditar(movimiento) },
                            onEliminar = { onEliminar(movimiento) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoPantalla(
    etiqueta: String,
    total: Double,
    color: Color,
    pie: String,
    balance: Double,
    depositado: Double,
    combinar: Boolean,
    esAhorro: Boolean
) {
    Column {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = total.dineroSimbolo(),
            style = MaterialTheme.typography.displaySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = pie,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(SuperficieAlta, Superficie)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    // Con "combinar" el número de arriba ya es el balance:
                    // abajo se muestra lo depositado sin restar, para no ver
                    // el mismo número dos veces.
                    combinar -> "Total depositado"
                    esAhorro -> "Balance actual"
                    else -> "Te queda disponible"
                },
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = (if (combinar) depositado else balance).dineroSimbolo(),
                style = MaterialTheme.typography.titleMedium,
                color = if (balance < 0) ColorGasto else AcentoMenta,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
