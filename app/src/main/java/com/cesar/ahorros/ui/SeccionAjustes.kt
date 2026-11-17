package com.cesar.ahorros.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Los piececitos de la pantalla de Ajustes.
 *
 * Ajustes estaba todo tirado en una sola lista y cada vez pesa más. Ahora
 * son **secciones** (Apariencia, Copia de seguridad, Acerca de) y dentro de
 * la primera hay **plegables** (collapses) para no buryar al usuario con 30
 * opciones a la vista.
 *
 * Todos los controles viven acá para que la pantalla sea corta de leer.
 */

/**
 * Un plegable: el título se toca y aparece o desaparece lo de abajo.
 *
 * El título también se puede apretar y soltar (long-press) para abrir la
 * explicación de lo que hay adentro: así el usuario puede preguntar "¿qué
 * es esto?" sin salir de Ajustes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeccionPlegable(
    titulo: String,
    subtitulo: String? = null,
    inicialmenteAbierta: Boolean = false,
    onExplicacion: (() -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    var abierta by rememberSaveable(titulo) { mutableStateOf(inicialmenteAbierta) }

    Surface(
        color = Superficie,
        shape = RoundedCornerShape(RadioTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadioTarjeta))
                    .combinedClickable(
                        onClick = { abierta = !abierta },
                        onLongClick = { onExplicacion?.invoke() }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrimario
                    )
                    if (subtitulo != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }
                Icon(
                    imageVector = if (abierta) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (abierta) "Plegar" else "Desplegar",
                    tint = TextoSecundario,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (abierta) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    contenido()
                }
            }
        }
    }
}

/** Una fila con un interruptor: el título, la explicación y el switch. */
@Composable
fun FilaInterruptor(
    titulo: String,
    descripcion: String? = null,
    valor: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = TextoPrimario
            )
            if (descripcion != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = valor,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Fondo,
                checkedTrackColor = AcentoCalido,
                uncheckedThumbColor = TextoSecundario,
                uncheckedTrackColor = SuperficieAlta,
                uncheckedBorderColor = Borde
            )
        )
    }
}

/**
 * Un deslizador con su nombre y su valor al lado.
 *
 * `valor` va de 0 a 1 (o de `minimo` a `maximo`); lo que se muestra es el
 * texto que le pasa el que lo usa.
 */
@Composable
fun FilaDeslizador(
    titulo: String,
    valor: Float,
    textoValor: String,
    onCambio: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Float = 0f,
    maximo: Float = 1f,
    pasos: Int = 0
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = TextoPrimario,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = textoValor,
                style = MaterialTheme.typography.labelLarge,
                color = AcentoCalido
            )
        }
        Slider(
            value = valor.coerceIn(minimo, maximo),
            onValueChange = onCambio,
            valueRange = minimo..maximo,
            steps = pasos,
            colors = SliderDefaults.colors(
                thumbColor = AcentoCalido,
                activeTrackColor = AcentoCalido,
                inactiveTrackColor = SuperficieAlta
            )
        )
    }
}

/**
 * Una fila de opciones (chips) para elegir UNA.
 *
 * Cada chip se puede apretar y soltar: sale la explicación de esa opción.
 */
@Composable
fun <T> FilaOpciones(
    opciones: List<T>,
    seleccionada: T,
    etiqueta: (T) -> String,
    onElegir: (T) -> Unit,
    onExplicacion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            ChipOpcion(
                texto = etiqueta(opcion),
                seleccionado = opcion == seleccionada,
                onClick = { onElegir(opcion) },
                onLongClick = { onExplicacion(opcion) }
            )
        }
    }
}

/**
 * Un chip propio.
 *
 * Es a mano (y no el `FilterChip` de Material) por una razón: el chip tiene
 * que reaccionar de DOS maneras (apretar lo elige, apretar y soltar lo
 * explica) y el de Material ya trae su propio click.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChipOpcion(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        color = if (seleccionado) AcentoCalido.copy(alpha = 0.22f) else Color.Transparent,
        shape = RoundedCornerShape(RadioChip),
        border = BorderStroke(1.dp, if (seleccionado) AcentoCalido else Borde),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (seleccionado) TextoPrimario else TextoSecundario,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/** El diálogo con la explicación de una opción. */
@Composable
fun DialogoExplicacion(
    titulo: String,
    texto: String,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrimario
            )
        },
        text = {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoTercero
            )
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Entendido", color = AcentoCalido)
            }
        },
        containerColor = Superficie,
        titleContentColor = TextoPrimario,
        textContentColor = TextoTercero
    )
}

/** Una tarjeta con un borde, para los grupos de opciones de Ajustes. */
@Composable
fun TarjetaAjuste(
    modifier: Modifier = Modifier,
    color: Color = SuperficieAlta,
    contenido: @Composable () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(RadioBoton),
        border = BorderStroke(1.dp, Borde.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            contenido()
        }
    }
}

/** Un subtítulo dentro de un plegable: separa un bloque de otro. */
@Composable
fun SubtituloAjuste(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextoSecundario,
        modifier = modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

/** Una línea de "etiqueta: valor". */
@Composable
fun FilaDato(etiqueta: String, valor: String, colorValor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall,
            color = colorValor
        )
    }
}
