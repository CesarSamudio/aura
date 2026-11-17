package com.cesar.ahorros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import java.time.LocalDate

/**
 * Tarjeta de métrica: etiqueta pequeña, cifra grande y nota al pie.
 *
 * `valorAnimado` es el número con el que se cuenta: si viene, la cifra sube
 * desde cero hasta ese valor (el estilo "Contar" de las animaciones). Si no
 * viene, se muestra el texto tal cual.
 */
@Composable
fun TarjetaKpi(
    titulo: String,
    valor: String,
    colorValor: Color,
    pie: String? = null,
    modifier: Modifier = Modifier,
    valorAnimado: Double? = null
) {
    val numero = rememberNumeroAnimado(valorAnimado ?: 0.0)
    val texto = if (valorAnimado != null) numero.value.dineroSimbolo() else valor

    // La entrada de la tarjeta: el patrón "asentamiento con resorte" cuando
    // el usuario elige "Asentar", un deslizamiento corto cuando elige
    // "Deslizar" y un simple fundido cuando elige "Fundido".
    val entrada = progresoDeEntrada()
    val estilo = AparienciaActual.estiloInterfaz

    Card(
        modifier = modifier.graphicsLayer {
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
        },
        colors = CardDefaults.cardColors(containerColor = SuperficieAlta),
        shape = RoundedCornerShape(RadioTarjeta)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.titleLarge,
                color = colorValor,
                fontWeight = FontWeight.ExtraBold
            )
            if (pie != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pie,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

/** Encabezado de un día: fecha, totales del día y plegable. */
@Composable
fun EncabezadoDia(
    fecha: LocalDate,
    ahorro: Double,
    gasto: Double,
    expandido: Boolean,
    onAlternar: () -> Unit
) {
    Surface(
        color = SuperficieAlta,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAlternar)
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
            if (ahorro > 0.0) {
                Text(
                    text = "+${ahorro.dineroSimbolo()}",
                    style = EstiloDinero,
                    color = AcentoMenta
                )
                Spacer(Modifier.width(10.dp))
            }
            if (gasto > 0.0) {
                Text(
                    text = "−${gasto.dineroSimbolo()}",
                    style = EstiloDinero,
                    color = ColorGasto
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = if (expandido) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expandido) "Plegar" else "Desplegar",
                tint = TextoSecundario
            )
        }
    }
}

/** Una fila de movimiento con acciones de editar y eliminar. */
@Composable
fun FilaMovimiento(
    movimiento: Movimiento,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val esAhorro = movimiento.tipo == Tipo.AHORRO
    val color = if (esAhorro) AcentoMenta else ColorGasto
    val signo = if (esAhorro) "+" else "−"
    val flecha = if (esAhorro) "↑" else "↓"

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
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = flecha,
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movimiento.categoria,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (movimiento.nota.isBlank()) movimiento.momento.hora() else movimiento.nota,
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$signo${movimiento.monto.dineroSimbolo()}",
                style = EstiloDinero,
                color = color
            )
            Text(
                text = movimiento.momento.hora(),
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
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
}

/** Estado vacío de una lista. */
@Composable
fun EstadoVacio(titulo: String, mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = TextoSecundario
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
    }
}

/** Título de sección del Balance. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextoSecundario,
        modifier = modifier.padding(bottom = 8.dp)
    )
}
