@file:OptIn(ExperimentalMaterial3Api::class)

package com.cesar.ahorros.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.CategoriaGasto
import com.cesar.ahorros.data.FuenteAhorro
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Alta o edición de un movimiento. Mismo formulario para ahorro y gasto:
 * monto, categoría, nota, fecha y hora exactas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoMovimiento(
    tipo: Tipo,
    inicial: Movimiento?,
    onGuardar: (Movimiento) -> Unit,
    onCerrar: () -> Unit
) {
    val esAhorro = tipo == Tipo.AHORRO

    var montoTexto by remember { mutableStateOf(inicial?.let { formatearMontoInicial(it.monto) } ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: if (esAhorro) FuenteAhorro.FISICO.etiqueta else CategoriaGasto.COMIDA.etiqueta) }
    var nota by remember { mutableStateOf(inicial?.nota ?: "") }
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now()) }
    var hora by remember { mutableStateOf(inicial?.momento?.toLocalTime() ?: LocalTime.now().withSecond(0)) }
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }

    // Los estados de los selectores se declaran siempre (fuera del if) para no
    // romper las reglas de composición de Compose.
    val estadoFecha = rememberDatePickerState(
        initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val estadoHora = rememberTimePickerState(
        initialHour = hora.hour,
        initialMinute = hora.minute,
        is24Hour = true
    )

    val categorias = if (esAhorro) FuenteAhorro.entries.map { it.etiqueta } else CategoriaGasto.entries.map { it.etiqueta }
    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val valido = monto != null && monto > 0.0

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                text = when {
                    inicial != null && esAhorro -> "Editar ahorro"
                    inicial != null -> "Editar gasto"
                    esAhorro -> "Nuevo ahorro"
                    else -> "Nuevo gasto"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { entrada ->
                        // Solo se admiten dígitos, punto y coma.
                        if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                            montoTexto = entrada
                        }
                    },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    singleLine = true,
                    isError = montoTexto.isNotEmpty() && !valido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (esAhorro) "Origen del ahorro" else "Categoría",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categorias.forEach { opcion ->
                        FilterChip(
                            selected = categoria == opcion,
                            onClick = { categoria = opcion },
                            label = { Text(opcion) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nota,
                    onValueChange = { if (it.length <= 80) nota = it },
                    label = { Text(if (esAhorro) "Nota (opcional)" else "Qué compraste") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { mostrarFecha = true }, modifier = Modifier.weight(1f)) {
                        Text("Fecha: ${fecha.fechaCorta()}")
                    }
                    OutlinedButton(onClick = { mostrarHora = true }, modifier = Modifier.weight(1f)) {
                        Text("Hora: ${"%02d:%02d".format(hora.hour, hora.minute)}")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Se guarda con hora, minuto y segundo del momento en que lo registras.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    val ts = fecha.atTime(hora).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onGuardar(
                        Movimiento(
                            id = inicial?.id ?: "",
                            tipo = tipo,
                            monto = monto!!,
                            categoria = categoria,
                            nota = nota.trim(),
                            ts = ts
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )

    if (mostrarFecha) {
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { millis ->
                        fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (mostrarHora) {
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            title = { Text("Hora del movimiento") },
            text = {
                Column {
                    TimeInput(state = estadoHora)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "El segundo se registra automáticamente al guardar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hora = LocalTime.of(estadoHora.hour, estadoHora.minute)
                    mostrarHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Confirmación antes de borrar (o antes de algo que replaces los datos). */
@Composable
fun DialogoConfirmarBorrar(
    texto: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit,
    titulo: String = "¿Eliminar?"
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Eliminar", color = ColorGasto)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

private fun formatearMontoInicial(monto: Double): String =
    if (monto % 1.0 == 0.0) monto.toLong().toString() else monto.toString()
