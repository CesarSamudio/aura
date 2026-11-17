package com.cesar.ahorros.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import kotlin.math.floor

// ---------------------------------------------------------------------------
// El selector de color de la app.
//
// Es la pieza con la que el usuario cambia a mano los colores de la
// apariencia (el acento de marca, el verde de los ahorros, el fondo de las
// tarjetas…).
//
// Son tres cosas:
//  - el motor de color (`aHsv` y `hsvAColor`): convierte los colores a
//    matiz/saturación/valor y de vuelta, que es el modelo con el que se
//    dibuja el dial;
//  - la `MuestraColor`: el cuadrito con el color y el nombre de lo que ese
//    color cambia. NO abre nada: solo avisa con `onClick`;
//  - el `DialogoEditorColor`: el dial en sí, con la barra de matiz, el
//    campo del hexadecimal, los colores sugeridos y el aviso de contraste
//    en vivo.
//
// No hay librerías de color: todo se dibuja con `Canvas` y los degradados
// los hace Compose. Tampoco hay nada nuevo para Android 7: ni `Modifier.blur`
// (que necesita API 31) ni APIs recientes.
//
// Los únicos colores "fijos" del archivo son el blanco y el negro del dial:
// son los extremos de los ejes de saturación y valor, así que usar un token
// del tema ahí haría que el dial mintiera sobre el color que se está
// eligiendo. Todo lo demás usa los tokens del tema.
// ---------------------------------------------------------------------------

/**
 * Los colores que se ofrecen como sugerencia.
 *
 * No es un arcoíris: son los acentos que ya usa la app (los de los 7
 * temas) más unas versiones más claras y más oscuras de esos mismos. Así
 * lo que se elige siempre encaja con el resto de la app.
 */
private val COLORES_SUGERIDOS: List<Color> = listOf(
    Color(0xFFFF5A00), // naranja de marca
    Color(0xFFFFA45C), // naranja claro
    Color(0xFFB53808), // naranja quemado
    Color(0xFFFFC24D), // ámbar
    Color(0xFFFD542D), // rojo
    Color(0xFFFCA5A5), // rosa claro
    Color(0xFF9E7BF0), // violeta
    Color(0xFFC9B6FF), // lila claro
    Color(0xFF6B2FBF), // violeta oscuro
    Color(0xFF3898EC), // azul
    Color(0xFF14509E), // azul oscuro
    Color(0xFF7DD3FC), // azul claro
    Color(0xFF91F58E), // menta
    Color(0xFFC6F24E), // lima
    Color(0xFFB8FEFF), // cian
    Color(0xFF22D3EE)  // turquesa
)

// ---------------------------------------------------------------------------
// El motor de color: HSV
// ---------------------------------------------------------------------------

/**
 * Conversión de color a HSV: (matiz 0..1, saturación 0..1, valor 0..1).
 *
 * El matiz va normalizado (0..1) y no en grados (0..360) porque así lo usa
 * el editor: el 0 es el rojo, el 0.5 es el cian y el 1 vuelve a ser el rojo.
 *
 * Los grises (sin saturación) no tienen matiz: se les da 0, que es el
 * arranque del arcoíris, para que el dial siempre tenga un color puro de
 * fondo que dibujar.
 */
fun Color.aHsv(): Triple<Float, Float, Float> {
    val mayor = maxOf(red, green, blue)
    val menor = minOf(red, green, blue)
    val delta = mayor - menor

    val matizGrados = when {
        // Sin delta no hay matiz: es un gris.
        delta == 0f -> 0f
        // El módulo se hace "matemático" (siempre positivo) porque el
        // numerador puede ser negativo en los colores entre rojo y magenta.
        mayor == red -> (((green - blue) / delta) % 6f + 6f) % 6f
        mayor == green -> (blue - red) / delta + 2f
        else -> (red - green) / delta + 4f
    }

    val saturacion = if (mayor <= 0f) 0f else delta / mayor
    return Triple(matizGrados / 6f, saturacion, mayor)
}

/**
 * De HSV a color.
 *
 * Los valores que se le pasan se recortan a su rango: un matiz de 1.5 se
 * convierte en 0.5, una saturación negativa en 0 y un valor mayor que 1 en
 * 1. Así el editor nunca puede producir un color roto, aunque el cálculo
 * venga de un gesto del dedo.
 */
fun hsvAColor(matiz: Float, saturacion: Float, valor: Float): Color {
    // El matiz da la vuelta (el arcoíris es redondo): 1.5 es lo mismo que
    // 0.5 y -0.25 es lo mismo que 0.75.
    val h = matiz - floor(matiz)
    val s = saturacion.coerceIn(0f, 1f)
    val v = valor.coerceIn(0f, 1f)

    // Sin saturación no hay matiz: sale un gris del valor pedido.
    if (s <= 0f) return Color(v, v, v)

    val sector = h * 6f
    val entero = floor(sector).toInt()
    val resto = sector - entero

    // p es el "piso" (valor sin saturación), q y t son los dos intermedios
    // que van girando de un sector al siguiente.
    val p = v * (1f - s)
    val q = v * (1f - s * resto)
    val t = v * (1f - s * (1f - resto))

    return when (entero % 6) {
        0 -> Color(v, t, p)
        1 -> Color(q, v, p)
        2 -> Color(p, v, t)
        3 -> Color(p, q, v)
        4 -> Color(t, p, v)
        else -> Color(v, p, q)
    }
}

// ---------------------------------------------------------------------------
// La muestra de color
// ---------------------------------------------------------------------------

/**
 * Una muestra de color (el cuadrito con el color) que al tocarla abre el
 * editor.
 *
 * onClick es el que dispara el editor; esta función NO abre nada sola.
 *
 * @param color el color que se muestra.
 * @param etiqueta el nombre de lo que ese color cambia ("Acento de marca").
 * @param descripcion la explicación de qué se ve distinto al cambiarlo.
 * @param onClick se llama al tocar la muestra: ahí es donde el usuario
 *   decide abrir el editor.
 * @param esPersonalizado si el usuario lo cambió a mano (se marca con un
 *   borde de acento y una etiqueta "A mano").
 * @param colorDeFondo el fondo sobre el que vive la muestra. Sirve para
 *   juzgar el contraste: si el color elegido es casi igual al fondo, la
 *   muestra se volvería invisible, así que se avisa.
 * @param avisaSiEsInvisible si es false, no se avisa aunque el color sea
 *   parecido al fondo. Se usa para los colores que SON el fondo (el fondo,
 *   las tarjetas): ahí el aviso no diría nada útil, solo molestaría.
 * @param onRestablecer si no es null, aparece un botón para volver al color
 *   del tema.
 */
@Composable
fun MuestraColor(
    color: Color,
    etiqueta: String,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    esPersonalizado: Boolean = false,
    colorDeFondo: Color = Superficie,
    avisaSiEsInvisible: Boolean = true,
    onRestablecer: (() -> Unit)? = null
) {
    val forma = RoundedCornerShape(RadioBoton)

    // Un color casi igual al fondo se pierde: por eso el borde del cuadrito
    // se pone de acento y se avisa con una línea.
    val contrasteConElFondo = Contraste.ratio(color, colorDeFondo)
    val casiInvisible = avisaSiEsInvisible && contrasteConElFondo < 1.3

    Surface(
        color = Superficie,
        shape = forma,
        border = BorderStroke(
            width = if (esPersonalizado) 1.5.dp else 1.dp,
            color = if (esPersonalizado) AcentoCalido else Borde
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .border(
                        width = if (casiInvisible) 2.dp else 1.dp,
                        color = if (casiInvisible) AcentoCalido else Borde,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .semantics { contentDescription = "Editar el color de $etiqueta" }
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrimario
                    )
                    if (esPersonalizado) {
                        Spacer(Modifier.width(8.dp))
                        ChipPequeno(texto = "A mano", color = AcentoCalido)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = colorAHex(color),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoTercero
                )
                if (casiInvisible) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Casi igual al fondo (${ratioEnTexto(contrasteConElFondo)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorAviso
                    )
                }
            }

            if (onRestablecer != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRestablecer) {
                    Text(
                        text = "Restablecer",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoSecundario
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// El editor
// ---------------------------------------------------------------------------

/**
 * El editor de color. Se muestra en un diálogo.
 *
 * onCambio se llama en cada cambio (para que se vea en vivo).
 *
 * [colorInicial] es solo el punto de partida: el editor guarda su propio
 * estado, así que los cambios en vivo no reinician el dial. Por eso el
 * diálogo tiene que crearse al abrirse (dentro de un `if`), no quedarse
 * montado cambiando de color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoEditorColor(
    colorInicial: Color,
    titulo: String,
    onCambio: (Color) -> Unit,
    onCerrar: () -> Unit
) {
    val onCambioActual by rememberUpdatedState(onCambio)

    var color by remember { mutableStateOf(colorInicial) }
    var matiz by remember { mutableStateOf(colorInicial.aHsv().first) }
    var saturacion by remember { mutableStateOf(colorInicial.aHsv().second) }
    var valor by remember { mutableStateOf(colorInicial.aHsv().third) }
    var hexTexto by remember { mutableStateOf(colorAHex(colorInicial)) }

    // Todo cambio pasa por acá: así el hexadecimal, el dial y el aviso de
    // contraste nunca se contradicen entre sí.
    fun aplicarHsv(m: Float, s: Float, v: Float) {
        matiz = m
        saturacion = s
        valor = v
        val nuevo = hsvAColor(m, s, v)
        color = nuevo
        hexTexto = colorAHex(nuevo)
        onCambioActual(nuevo)
    }

    fun aplicar(nuevo: Color) {
        color = nuevo
        val hsv = nuevo.aHsv()
        matiz = hsv.first
        saturacion = hsv.second
        valor = hsv.third
        hexTexto = colorAHex(nuevo)
        onCambioActual(nuevo)
    }

    val ratioFondo = Contraste.ratio(color, Fondo)
    val ratioSuperficie = Contraste.ratio(color, Superficie)
    val seLee = ratioFondo + 1e-9 >= Contraste.UMBRAL_TEXTO

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            color = Superficie,
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrimario
                )

                Spacer(Modifier.height(14.dp))

                // La vista previa y el hexadecimal escrito a mano.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(RadioBoton))
                            .background(color)
                            .border(1.dp, Borde, RoundedCornerShape(RadioBoton))
                            .semantics { contentDescription = "Color elegido" }
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexTexto,
                        onValueChange = { entrada ->
                            hexTexto = entrada
                            // Mientras el hexadecimal no sea válido no se
                            // toca el color: el usuario puede escribir a
                            // medias y no se le pisa lo que puso.
                            val escrito = colorDeHex(entrada)
                            if (escrito != null) aplicar(escrito)
                        },
                        label = { Text("Hexadecimal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // El dial: saturación (a los lados) y valor (arriba y abajo).
                CuadroSaturacionValor(
                    matiz = matiz,
                    saturacion = saturacion,
                    valor = valor,
                    onMover = { s, v -> aplicarHsv(matiz, s, v) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .aspectRatio(1.5f)
                )

                Spacer(Modifier.height(14.dp))

                // La barra de matiz.
                BarraMatiz(
                    matiz = matiz,
                    onMover = { m -> aplicarHsv(m, saturacion, valor) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                // El contraste, en vivo: el usuario ve si lo que está
                // eligiendo se va a leer o no antes de dejarlo.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipPequeno(
                        texto = if (seLee) "Se lee" else "Falta contraste",
                        color = if (seLee) AcentoMenta else ColorAviso
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Fondo ${ratioEnTexto(ratioFondo)} · " +
                            "Tarjetas ${ratioEnTexto(ratioSuperficie)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Se necesita 4.5:1 para que un texto se lea bien.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoTercero
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Sugeridos",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLORES_SUGERIDOS.forEach { sugerido ->
                        MuestraSugerida(
                            color = sugerido,
                            esElegido = sugerido == color,
                            onClick = { aplicar(sugerido) }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (color != colorInicial) {
                        TextButton(onClick = { aplicar(colorInicial) }) {
                            Text(
                                text = "Volver al original",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextoSecundario
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onCerrar,
                        shape = RoundedCornerShape(RadioBoton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcentoCalido,
                            contentColor = Fondo
                        )
                    ) {
                        Text("Listo")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Las piezas del dial
// ---------------------------------------------------------------------------

/**
 * El cuadrado de saturación y valor.
 *
 * El fondo es el color puro del matiz; encima va un degradado de blanco
 * (sube la saturación) y otro de negro (baja el valor). El círculo marca
 * dónde está el punto elegido.
 */
@Composable
private fun CuadroSaturacionValor(
    matiz: Float,
    saturacion: Float,
    valor: Float,
    onMover: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val onMoverActual by rememberUpdatedState(onMover)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadioBoton))
            .pointerInput(Unit) {
                // Un solo gesto sirve para tocar y para arrastrar: se espera
                // el dedo, se marca el punto y se sigue el movimiento.
                // El tamaño se mide en cada gesto: al empezar puede que el
                // dial todavía no tenga tamaño y el cálculo se iría.
                awaitPointerEventScope {
                    while (true) {
                        val ancho = size.width.toFloat()
                        val alto = size.height.toFloat()
                        val abajo = awaitFirstDown(requireUnconsumed = false)
                        fun marcar(pos: Offset) {
                            onMoverActual(
                                (pos.x / ancho).coerceIn(0f, 1f),
                                (1f - pos.y / alto).coerceIn(0f, 1f)
                            )
                        }
                        marcar(abajo.position)
                        abajo.consume()
                        var siguiendo = true
                        while (siguiendo) {
                            val evento = awaitPointerEvent()
                            val cambio = evento.changes.firstOrNull { it.id == abajo.id }
                            if (cambio == null || !cambio.pressed) {
                                siguiendo = false
                            } else {
                                marcar(cambio.position)
                                cambio.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // El color puro del matiz: el punto de partida del dial.
            drawRect(color = hsvAColor(matiz, 1f, 1f))

            // Blanco → transparente de izquierda a derecha: más saturación.
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    startX = 0f,
                    endX = size.width
                )
            )

            // Transparente → negro de arriba abajo: menos valor.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = size.height
                )
            )

            // El punto elegido: un anillo blanco con una línea negra
            // alrededor, para que se vea sobre cualquier color.
            val centro = Offset(
                x = saturacion * size.width,
                y = (1f - valor) * size.height
            )
            drawCircle(
                color = Color.Black,
                radius = 11.dp.toPx(),
                center = centro,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = centro,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * La barra de matiz: el arcoíris horizontal con un marcador.
 *
 * El arcoíris se arma con los seis colores puros del modelo HSV, para que
 * el punto que marca y el color que se ve siempre coincidan.
 */
@Composable
private fun BarraMatiz(
    matiz: Float,
    onMover: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val onMoverActual by rememberUpdatedState(onMover)

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(RadioBoton))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ancho = size.width.toFloat()
                        val abajo = awaitFirstDown(requireUnconsumed = false)
                        fun marcar(pos: Offset) {
                            onMoverActual((pos.x / ancho).coerceIn(0f, 1f))
                        }
                        marcar(abajo.position)
                        abajo.consume()
                        var siguiendo = true
                        while (siguiendo) {
                            val evento = awaitPointerEvent()
                            val cambio = evento.changes.firstOrNull { it.id == abajo.id }
                            if (cambio == null || !cambio.pressed) {
                                siguiendo = false
                            } else {
                                marcar(cambio.position)
                                cambio.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tonos = listOf(0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f)
                .map { hsvAColor(it, 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(tonos))

            // El marcador: una barrita con el matiz elegido en el medio.
            val x = matiz * size.width
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(x - 6.dp.toPx(), 1.dp.toPx()),
                size = Size(12.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            drawRoundRect(
                color = hsvAColor(matiz, 1f, 1f),
                topLeft = Offset(x - 4.dp.toPx(), 3.dp.toPx()),
                size = Size(8.dp.toPx(), size.height - 6.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

/** Un cuadrito de la lista de sugeridos. */
@Composable
private fun MuestraSugerida(
    color: Color,
    esElegido: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .border(
                width = if (esElegido) 3.dp else 1.dp,
                color = if (esElegido) TextoPrimario else Borde,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Color ${colorAHex(color)}" }
    )
}

/** Un chip chico: un rótulo con borde, para marcar estados. */
@Composable
private fun ChipPequeno(texto: String, color: Color) {
    Surface(
        color = SuperficieAlta,
        shape = RoundedCornerShape(RadioChip),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

/** El color en `#RRGGBB`, como se lo muestra al usuario. */
private fun colorAHex(color: Color): String {
    val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
}

/** Convierte `#RRGGBB` (con o sin la almohadilla) en color; `null` si no es válido. */
private fun colorDeHex(hex: String): Color? {
    val limpio = hex.trim().removePrefix("#")
    if (limpio.length != 6) return null
    val valor = limpio.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or valor)
}

/** La razón de contraste escrita como la ve el usuario: "2.8:1". */
private fun ratioEnTexto(ratio: Double): String =
    String.format(Locale.ROOT, "%.1f:1", ratio)
