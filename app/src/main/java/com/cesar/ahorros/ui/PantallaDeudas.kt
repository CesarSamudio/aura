package com.cesar.ahorros.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.dominio.CalculosDeudas

/**
 * Pestaña "Deudas": la lista de todas las deudas.
 *
 * Una deuda NO es ingreso ni gasto, así que aquí no se suma a ningún total de
 * Ahorros, Gastos ni Balance: es dinero que Cesar puso y que le van
 * devolviendo las personas que deben.
 *
 * Hay dos formas de armar una deuda:
 * - [ModoDeuda.META]: César sabe el costo de la cosa y se divide entre los que
 *   la pagan.
 * - [ModoDeuda.LIBRE]: César sabe cuánto cobra a cada uno y el total sale de
 *   sumar.
 *
 * En los dos modos, lo que define la cuota es la DEUDA (el número de personas
 * que la pagan), nunca la lista de personas: la lista es a quién se le cobra.
 *
 * El total que se muestra arriba es lo que le deben a Cesar ahora mismo: la
 * suma del saldo de las deudas que todavía NO están saldadas. Las saldadas se
 * pueden marcar a mano, nunca se marcan solas.
 */
@Composable
fun PantallaDeudas(
    deudas: List<Deuda>,
    onAbrir: (String) -> Unit,
    onEditar: (Deuda) -> Unit,
    onEliminar: (Deuda) -> Unit
) {
    val totalPorCobrar = remember(deudas) {
        deudas.filterNot { it.saldada }.sumOf { CalculosDeudas.saldo(it) }
    }
    val sinSaldar = remember(deudas) { deudas.count { !it.saldada } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "encabezado") {
            Column {
                Text(
                    text = "DEUDAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = totalPorCobrar.dineroSimbolo(),
                    style = MaterialTheme.typography.displaySmall,
                    color = AcentoCalido,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (sinSaldar == 0) {
                        "No te deben nada por ahora"
                    } else {
                        "Te deben en total · $sinSaldar ${if (sinSaldar == 1) "deuda sin saldar" else "deudas sin saldar"}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Una deuda no es ingreso ni gasto: es dinero que te van devolviendo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }

        if (deudas.isEmpty()) {
            item(key = "vacio") {
                EstadoVacio(
                    titulo = "Sin deudas",
                    mensaje = "Toca + para registrar la primera."
                )
            }
        }

        items(deudas, key = { it.id }) { deuda ->
            TarjetaDeuda(
                deuda = deuda,
                onAbrir = { onAbrir(deuda.id) },
                onEditar = { onEditar(deuda) },
                onEliminar = { onEliminar(deuda) }
            )
        }
    }
}

/** Tarjeta de una deuda: nombre, costo, cuántas personas lo pagan y cuánto falta. */
@Composable
private fun TarjetaDeuda(
    deuda: Deuda,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val resumen = CalculosDeudas.resumenParaMostrar(deuda)

    Card(
        onClick = onAbrir,
        shape = RoundedCornerShape(RadioTarjeta),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deuda.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deuda.saldada) {
                        Spacer(Modifier.height(8.dp))
                        EtiquetaSaldadaDeuda()
                    }
                }
                IconButton(onClick = onEditar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = TextoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = ColorGasto,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = textoTotalDeuda(deuda),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )

            Spacer(Modifier.height(8.dp))
            // La línea clave: cuántas personas pagan el total y cuánto les toca.
            // Cambia según el modo de la deuda.
            Surface(
                color = AcentoCalido.copy(alpha = 0.14f),
                shape = RoundedCornerShape(RadioChip),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = textoRepartoDeuda(deuda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AcentoCalido,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Falta:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextoSecundario
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = resumen.saldo.dineroSimbolo(),
                    style = EstiloDinero,
                    color = if (deuda.saldada) AcentoMenta else ColorGasto
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Ya pagado: ${resumen.totalPagado.dineroSimbolo()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

/** Etiqueta verde "Saldada". La comparten la lista y el detalle. */
@Composable
internal fun EtiquetaSaldadaDeuda() {
    Surface(
        color = AcentoMenta.copy(alpha = 0.16f),
        shape = RoundedCornerShape(RadioChip),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "Saldada",
            style = MaterialTheme.typography.labelSmall,
            color = AcentoMenta,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * El total de la deuda, con la etiqueta que corresponde al modo.
 *
 * - META: es la meta (el costo de la cosa).
 * - LIBRE: es lo que sale de sumar (César no lo puso, la app lo calculó).
 */
internal fun textoTotalDeuda(deuda: Deuda): String {
    val total = CalculosDeudas.totalDe(deuda).dineroSimbolo()
    return when (deuda.modo) {
        ModoDeuda.META -> "Costo total: $total"
        ModoDeuda.LIBRE -> "Total a cobrar: $total"
    }
}

/**
 * La línea clave de la deuda, según el modo:
 * - META: "4 personas deben pagarlo · $15,00 cada uno"
 * - LIBRE: "4 personas · $4,00 cada uno · total $16,00"
 *
 * Si la deuda está marcada a mano como saldada, se le quita el "deben
 * pagarlo": la deuda está cerrada y no se contradice el estado verde.
 */
internal fun textoRepartoDeuda(deuda: Deuda): String {
    val personas = deuda.personasQuePagan
    if (personas <= 0) return "Falta decir cuántas personas lo pagan"

    val base = CalculosDeudas.montoBaseDe(deuda).dineroSimbolo()
    val total = CalculosDeudas.totalDe(deuda).dineroSimbolo()

    return when {
        // Deuda cerrada a mano: sin "deben pagarlo".
        deuda.saldada && deuda.modo == ModoDeuda.META ->
            if (personas == 1) "1 persona · $base" else "$personas personas · $base cada uno"

        deuda.saldada ->
            "$personas personas · $base cada uno · total $total"

        deuda.modo == ModoDeuda.META ->
            if (personas == 1) {
                "1 persona debe pagarlo · $base"
            } else {
                "$personas personas deben pagarlo · $base cada uno"
            }

        else -> "$personas personas · $base cada uno · total $total"
    }
}

/**
 * La nota que explica el efecto de los montos especiales. Null si no hay
 * ninguno, para no meter texto de más.
 *
 * - META: el monto especial **redistribuye**: los que pagan la cuota normal
 *   suben para que entre todos se llegue a la meta.
 * - LIBRE: el monto especial no cambia lo que pagan los demás; cambia el
 *   total.
 */
internal fun textoRedistribucion(deuda: Deuda): String? {
    val propios = deuda.personas.filter { it.cuotaPropia != null }
    if (propios.isEmpty()) return null

    val nombres = propios.joinToString(", ") { it.nombre }
    val base = CalculosDeudas.montoBaseDe(deuda).dineroSimbolo()
    val total = CalculosDeudas.totalDe(deuda).dineroSimbolo()

    return when (deuda.modo) {
        ModoDeuda.META -> {
            val prefijo = if (propios.size == 1) {
                "Con el monto especial de"
            } else {
                "Con los montos especiales de"
            }
            "$prefijo $nombres, los demás pagan $base cada uno."
        }
        ModoDeuda.LIBRE ->
            "Los montos especiales de $nombres no cambian lo que pagan los demás: " +
                "cambian el total ($total)."
    }
}
