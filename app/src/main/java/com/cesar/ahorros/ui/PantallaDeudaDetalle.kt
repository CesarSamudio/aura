@file:OptIn(ExperimentalMaterial3Api::class)

package com.cesar.ahorros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.dominio.CalculosDeudas
import com.cesar.ahorros.dominio.ResumenPersona
import java.time.LocalDate

/**
 * Detalle de una deuda: cómo está repartida y qué pagos lleva.
 *
 * La cuota sale de la DEUDA, no de la lista:
 * - En modo [ModoDeuda.META] (César sabe el costo), la cuota normal es
 *   (meta − montos especiales) ÷ (personas que pagan − los de monto
 *   especial): el monto especial **redistribuye** y los demás suben un poco
 *   para llegar a la meta.
 * - En modo [ModoDeuda.LIBRE] (César sabe cuánto cobra a cada uno), el monto
 *   de cada uno no se redistribuye: lo que cambia es el total.
 *
 * La lista de personas NO divide nada: es a quién se le está cobrando. Lo que
 * cada uno ya pagó no se toca nunca: lo que falta es su cuota menos lo
 * pagado. Si alguien pagó de más, el excedente queda a favor.
 *
 * "Saldada" solo se marca a mano con el botón, nunca sola.
 *
 * Cada persona puede además tener **pruebas** (el comprobante que le mandaron
 * por YAPI): se adjuntan desde la misma fila, sin registrar un pago.
 */
@Composable
fun PantallaDeudaDetalle(
    deuda: Deuda,
    onVolver: () -> Unit,
    onAgregarPersona: () -> Unit,
    onEditarPersona: (PersonaDeuda) -> Unit,
    onEliminarPersona: (PersonaDeuda) -> Unit,
    onRegistrarPago: (PersonaDeuda) -> Unit,
    onEliminarPago: (Pago) -> Unit,
    onAlternarSaldada: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onVerComprobante: (String) -> Unit,
    onAdjuntarPrueba: (PersonaDeuda) -> Unit
) {
    val resumen = CalculosDeudas.resumenParaMostrar(deuda)
    val repartos = CalculosDeudas.personas(deuda)
    val grupos = CalculosDeudas.pagosPorDia(deuda)
    val montoBase = CalculosDeudas.montoBaseDe(deuda)
    val notaRedistribucion = textoRedistribucion(deuda)
    val nombrePorId = remember(deuda.personas) { deuda.personas.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ------------------------------------------------------------------
        // Encabezado
        // ------------------------------------------------------------------
        item(key = "encabezado") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextoPrimario,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
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

                Spacer(Modifier.height(4.dp))
                Text(
                    text = deuda.nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = textoTotalDeuda(deuda),
                    style = EstiloDinero,
                    color = TextoSecundario
                )

                Spacer(Modifier.height(10.dp))
                // La línea grande: cuántas personas pagan el total y cuánto
                // les toca a cada una. Cambia según el modo de la deuda.
                Surface(
                    color = AcentoCalido.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(RadioTarjeta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = textoRepartoDeuda(deuda),
                        style = MaterialTheme.typography.titleMedium,
                        color = AcentoCalido,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // El monto especial cambia el monto de los demás (en modo
                // META) o el total (en modo LIBRE): hay que decirlo.
                if (notaRedistribucion != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = notaRedistribucion,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
        }

        // ------------------------------------------------------------------
        // Franja de saldada
        // ------------------------------------------------------------------
        if (deuda.saldada) {
            item(key = "saldada") {
                Surface(
                    color = AcentoMenta.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(RadioTarjeta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AcentoMenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Esta deuda está saldada",
                            style = MaterialTheme.typography.titleMedium,
                            color = AcentoMenta
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------------------
        // KPIs
        // ------------------------------------------------------------------
        item(key = "kpis") {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Si la deuda está marcada a mano como saldada, no hay nada que
                // cobrar: se muestra cerrada y no se habla de quién debe.
                if (deuda.saldada) {
                    TarjetaKpi(
                        titulo = "Estado",
                        valor = "Saldada",
                        colorValor = AcentoMenta,
                        pie = "marcada a mano",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    TarjetaKpi(
                        titulo = if (deuda.modo == ModoDeuda.META) {
                            "Cuota de cada uno"
                        } else {
                            "Cada uno paga"
                        },
                        valor = montoBase.dineroSimbolo(),
                        colorValor = AcentoCalido,
                        pie = if (deuda.modo == ModoDeuda.META) {
                            "total ÷ ${deuda.personasQuePagan} " +
                                (if (deuda.personasQuePagan == 1) "persona" else "personas")
                        } else {
                            "monto fijo"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(10.dp))
                TarjetaKpi(
                    titulo = "Falta por cobrar",
                    valor = resumen.saldo.dineroSimbolo(),
                    colorValor = if (deuda.saldada) AcentoMenta else ColorGasto,
                    pie = "de ${resumen.total.dineroSimbolo()} en total",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ------------------------------------------------------------------
        // Botón de saldada (manual)
        // ------------------------------------------------------------------
        item(key = "boton-saldada") {
            if (deuda.saldada) {
                OutlinedButton(
                    onClick = onAlternarSaldada,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Reabrir deuda")
                }
            } else {
                Button(
                    onClick = onAlternarSaldada,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcentoMenta,
                        contentColor = Fondo
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Marcar como saldada")
                }
            }
        }

        // ------------------------------------------------------------------
        // Reparto
        // ------------------------------------------------------------------
        item(key = "titulo-reparto") {
            TituloSeccion("Reparto")
        }

        item(key = "subtitulo-reparto") {
            Text(
                text = "${resumen.personasEnLista} " +
                    (if (resumen.personasEnLista == 1) "persona en la lista" else "personas en la lista") +
                    " · ${resumen.personasDeben} " +
                    (if (resumen.personasDeben == 1) "debe" else "deben"),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        if (repartos.isEmpty()) {
            item(key = "reparto-vacio") {
                Text(
                    text = "Todavía no has agregado a nadie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        items(repartos, key = { it.persona.id }) { reparto ->
            FilaPersonaDeuda(
                reparto = reparto,
                saldada = deuda.saldada,
                onPago = { onRegistrarPago(reparto.persona) },
                onEditar = { onEditarPersona(reparto.persona) },
                onQuitar = { onEliminarPersona(reparto.persona) },
                // El 📎 y el chip de pruebas abren el mismo diálogo: ahí se
                // adjunta la prueba nueva y se ven las que ya tiene.
                onAdjuntar = { onAdjuntarPrueba(reparto.persona) },
                onVerPruebas = { onAdjuntarPrueba(reparto.persona) }
            )
        }

        item(key = "agregar-persona") {
            OutlinedButton(
                onClick = onAgregarPersona,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Añadir personas")
            }
        }

        // ------------------------------------------------------------------
        // Pagos
        // ------------------------------------------------------------------
        item(key = "titulo-pagos") {
            TituloSeccion("Pagos")
        }

        if (grupos.isEmpty()) {
            item(key = "pagos-vacios") {
                Text(
                    text = "Todavía no hay pagos registrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        }

        grupos.forEach { grupo ->
            item(key = "dia-${grupo.fecha}") {
                EncabezadoDiaDeudas(
                    fecha = grupo.fecha,
                    total = grupo.pagos.sumOf { it.monto }
                )
            }
            items(grupo.pagos, key = { it.id }) { pago ->
                val comprobante = pago.comprobante
                FilaPagoDeuda(
                    pago = pago,
                    nombrePersona = nombrePorId[pago.personaId]?.nombre ?: "Sin nombre",
                    onVerComprobante = { comprobante?.let { onVerComprobante(it) } },
                    onEliminar = { onEliminarPago(pago) }
                )
            }
        }
    }
}

/**
 * Fila de una persona del reparto: cuota, lo que puso, lo que debe y sus
 * pruebas.
 *
 * La fila se parte en dos líneas de texto y una de botones para que el
 * nombre no se trunque tanto en un teléfono angosto (360 dp): el 📎 para
 * adjuntar una prueba va entre el "+" (pago) y el ✎ (editar).
 */
@Composable
private fun FilaPersonaDeuda(
    reparto: ResumenPersona,
    saldada: Boolean,
    onPago: () -> Unit,
    onEditar: () -> Unit,
    onQuitar: () -> Unit,
    onAdjuntar: () -> Unit,
    onVerPruebas: () -> Unit
) {
    val persona = reparto.persona
    val pruebas = persona.pruebas

    Surface(
        color = Superficie,
        shape = RoundedCornerShape(RadioTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Línea 1: el nombre y las etiquetas de la persona.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = persona.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // El método de pago es información adicional de la persona.
                persona.metodoPago?.let { metodo ->
                    Spacer(Modifier.width(8.dp))
                    EtiquetaMetodoPago(metodo)
                }
                // El "caso especial": esta persona NO paga la cuota normal.
                if (reparto.esCuotaPropia) {
                    Spacer(Modifier.width(8.dp))
                    EtiquetaMontoEspecial()
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Cuota: ${reparto.cuota.dineroSimbolo()}  ·  " +
                    "Puesto: ${reparto.pagado.dineroSimbolo()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            Spacer(Modifier.height(4.dp))
            // Con la deuda saldada a mano no se muestra quién debe: la
            // deuda está cerrada y no hay nada que cobrar.
            if (!saldada) {
                when {
                    reparto.excedente > 0.0 -> Text(
                        text = "A favor ${reparto.excedente.dineroSimbolo()}",
                        style = EstiloDinero,
                        color = AcentoHielo
                    )
                    reparto.pendiente <= 0.0 -> Text(
                        text = "Al día",
                        style = EstiloDinero,
                        color = AcentoMenta
                    )
                    else -> Text(
                        text = "Debe: ${reparto.pendiente.dineroSimbolo()}",
                        style = EstiloDinero,
                        color = ColorGasto
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Línea de abajo: las pruebas que ya tiene y los botones.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pruebas.isNotEmpty()) {
                    ChipPruebas(cantidad = pruebas.size, onClick = onVerPruebas)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPago, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Registrar pago",
                        tint = AcentoMenta,
                        modifier = Modifier.size(20.dp)
                    )
                }
                BotonAdjuntarPrueba(
                    activa = persona.metodoPago == MetodoPago.YAPI,
                    onClick = onAdjuntar
                )
                IconButton(onClick = onEditar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar persona",
                        tint = TextoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onQuitar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Quitar persona",
                        tint = ColorGasto,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Botón de adjuntar una prueba (📎) de una persona.
 *
 * Es un emoji y no un icono de Material porque la librería de iconos
 * extendidos no está en el proyecto: el 📎 es lo que todos entienden como
 * "adjuntar".
 *
 * El botón está siempre en la fila porque es la puerta de entrada al diálogo
 * de pruebas (ahí se cambia el método de pago). Pero solo con YAPI se puede
 * adjuntar de verdad: sin YAPI el botón sale apagado y el diálogo lo explica.
 */
@Composable
private fun BotonAdjuntarPrueba(activa: Boolean, onClick: () -> Unit) {
    val estado = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text("Adjuntar prueba") } },
        state = estado
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            // El emoji es de color y no toma el tinte: el halo es lo que
            // avisa si esta persona puede adjuntar (solo con YAPI).
            Surface(
                color = if (activa) {
                    AcentoHielo.copy(alpha = 0.18f)
                } else {
                    TextoSecundario.copy(alpha = 0.12f)
                },
                shape = CircleShape,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "📎",
                        fontSize = 14.sp,
                        modifier = Modifier.semantics { contentDescription = "Adjuntar prueba" }
                    )
                }
            }
        }
    }
}

/** Chip "📎 2 pruebas": cuántas pruebas tiene esta persona. */
@Composable
private fun ChipPruebas(cantidad: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadioChip))
            .background(AcentoHielo.copy(alpha = 0.14f))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📎", fontSize = 12.sp, color = AcentoHielo)
            Spacer(Modifier.width(5.dp))
            Text(
                text = "$cantidad ${if (cantidad == 1) "prueba" else "pruebas"}",
                style = MaterialTheme.typography.labelMedium,
                color = AcentoHielo
            )
        }
    }
}

/** Etiqueta del método de pago de la persona (información adicional). */
@Composable
private fun EtiquetaMetodoPago(metodo: MetodoPago) {
    val color = if (metodo == MetodoPago.YAPI) AcentoHielo else TextoSecundario
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(RadioChip)
    ) {
        Text(
            text = metodo.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Etiqueta cian "monto especial": esta persona paga un monto propio. */
@Composable
private fun EtiquetaMontoEspecial() {
    Surface(
        color = AcentoHielo.copy(alpha = 0.16f),
        shape = RoundedCornerShape(RadioChip)
    ) {
        Text(
            text = "monto especial",
            style = MaterialTheme.typography.labelSmall,
            color = AcentoHielo,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Encabezado de un día de pagos (mismo aire que el de movimientos). */
@Composable
private fun EncabezadoDiaDeudas(fecha: LocalDate, total: Double) {
    Surface(
        color = SuperficieAlta,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fecha.diaSemana(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = fecha.fechaSinDia(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
            Text(
                text = total.dineroSimbolo(),
                style = EstiloDinero,
                color = AcentoMenta
            )
        }
    }
}

/** Fila de un pago: quién pagó, cuánto, cómo y el comprobante si hay. */
@Composable
private fun FilaPagoDeuda(
    pago: Pago,
    nombrePersona: String,
    onVerComprobante: () -> Unit,
    onEliminar: () -> Unit
) {
    val comprobante = pago.comprobante

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AcentoMenta.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↑",
                color = AcentoMenta,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombrePersona,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${pago.metodo.etiqueta} · ${pago.momento.hora()}",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            if (pago.nota.isNotBlank()) {
                Text(
                    text = pago.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTercero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (comprobante != null) {
                TextButton(
                    onClick = onVerComprobante,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Ver comprobante",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcentoHielo
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = pago.monto.dineroSimbolo(),
                style = EstiloDinero,
                color = AcentoMenta
            )
            IconButton(onClick = onEliminar, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Eliminar pago",
                    tint = ColorGasto,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
