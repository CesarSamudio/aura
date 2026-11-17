package com.cesar.ahorros.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.FondosApp
import com.cesar.ahorros.data.FormaDegradado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// El fondo de la app.
//
// Son dos cosas que se pueden poner detrás de todo:
//
//  1. Una FOTO del teléfono: la elige el usuario en Ajustes y la app la
//     guarda dentro de su almacenamiento interno (data/FondosApp.kt).
//  2. Un DEGRADADO ANIMADO: un juego de luces de colores que se mueve
//     despacio. Los colores salen de los acentos del tema activo, así que
//     cambia de tema y el fondo cambia con él.
//
// Todo se dibuja con `Canvas` y `Brush`: cero librerías de imágenes (la app
// no tiene ni Coil ni Glide) y nada de API 31 (el difuminado se hace a mano
// con un kernel gaussiano, que funciona desde Android 7).
// ---------------------------------------------------------------------------

/** Cuánto dura un ciclo completo del degradado (a velocidad 1x). */
private const val CICLO_DEL_DEGRADADO = 12_000

/**
 * El tiempo que se cuenta como máximo entre un fotograma y el siguiente.
 *
 * Un fotograma normal dura 16 ms. Si entre dos pasan 100 ms o más (la app
 * estuvo en segundo plano, el teléfono se quedó quieto, o el sistema
 * anduvo ocupado), no se cuenta todo: el fondo sigue donde estaba en vez
 * de dar un salto gigante.
 */
private const val SEGUNDO_MAXIMO = 0.1f

/**
 * El lado más largo (en px) de la imagen chiquita donde se desenfoca.
 *
 * Se desenfoca una imagen de 256 px y después se estira a la pantalla: el
 * resultado se ve igual de suave y el trabajo cuesta 64 veces menos (una
 * foto de 2048 px son 4 millones de píxeles; una de 256, 65 mil).
 */
private const val LADO_REDUCIDA = 256

/**
 * El radio máximo del kernel gaussiano (en píxeles de la imagen chiquita).
 *
 * 8 píxeles de la imagen de 256 son unos 34 px de desenfoque en la
 * pantalla: de lente, suave.
 */
private const val RADIO_MAXIMO = 8

/**
 * El fondo de la app: la imagen del teléfono (con su opacidad y su
 * difuminado) y el degradado animado. Se dibuja DETRÁS de todo.
 */
@Composable
fun FondoApp(modifier: Modifier = Modifier) {
    val pintura = rememberPinturaFondo()

    Canvas(modifier = modifier.fillMaxSize()) {
        dibujarFondo(pintura)
    }
}

/**
 * Una vista previa chica del fondo, para la pantalla de Ajustes.
 *
 * Es el mismo fondo que se ve en la app, pero en un recuadro chico: así el
 * usuario puede ver lo que está poniendo sin tener que salir de Ajustes.
 */
@Composable
fun VistaPreviaFondo(
    modifier: Modifier = Modifier,
    alto: Int = 120
) {
    val pintura = rememberPinturaFondo()

    // El alto va DESPUÉS del modifier que recibe la pantalla: si quien llama
    // ya puso un alto (por ejemplo 120.dp), ese manda; si no, se usa este.
    Canvas(modifier = modifier.height(alto.dp)) {
        dibujarFondo(pintura)
    }
}

// ---------------------------------------------------------------------------
// Todo lo que hay que saber para dibujar el fondo
// ---------------------------------------------------------------------------

/**
 * Los datos del fondo que hay que tener para dibujarlo.
 *
 * `fase` es un `State` y no un número a propósito: leerlo DENTRO del
 * `Canvas` hace que solo se redibuje la pantalla, sin recomponer nada.
 */
private data class PinturaFondo(
    val imagen: ImageBitmap?,
    val difuminada: ImageBitmap?,
    val opacidadImagen: Float,
    val forma: FormaDegradado,
    val fase: State<Float>,
    val colores: List<Color>,
    val opacidadDegradado: Float,
    val colorFondo: Color
)

/** Junta lo que hace falta para dibujar el fondo, con lo que eligió el usuario. */
@Composable
private fun rememberPinturaFondo(): PinturaFondo {
    val ajustes = AparienciaActual.fondo
    val tema = TemaActual.tema
    val colorFondo = Fondo

    val imagen = rememberFondoBitmap(ajustes.imagen)
    val difuminada = rememberVersionDifuminada(imagen, ajustes.difuminadoImagen)
    val fase = rememberFaseDelDegradado(ajustes.degradadoActivo, ajustes.velocidadDegradado)

    // Los colores del degradado salen de los ACENTOS del tema: por eso el
    // fondo cambia de color cuando el usuario cambia de tema.
    val colores = remember(tema, colorFondo, ajustes.intensidadDegradado, ajustes.opacidadDegradado) {
        val intensidad = ajustes.intensidadDegradado.coerceIn(0f, 1f)
        val opacidad = ajustes.opacidadDegradado.coerceIn(0f, 1f)
        listOf(
            tema.acentoCalido,
            tema.acentoVioleta,
            tema.acentoHielo,
            tema.acentoMenta
        ).map { acento ->
            // Con poca intensidad el color se acerca al del fondo: el
            // degradado se nota, pero no tapa la app.
            mezclarConFondo(acento, colorFondo, (1f - intensidad) * 0.8f).copy(alpha = opacidad)
        }
    }

    return PinturaFondo(
        imagen = imagen,
        difuminada = difuminada,
        opacidadImagen = ajustes.opacidadImagen,
        forma = AparienciaActual.formaDegradado,
        fase = fase,
        colores = colores,
        opacidadDegradado = ajustes.opacidadDegradado,
        colorFondo = colorFondo
    )
}

/**
 * Carga la imagen del fondo desde el almacenamiento interno de la app.
 *
 * La lectura va en `Dispatchers.IO`: abrir una imagen de 2 MB en el hilo
 * principal dejaría la pantalla congelada un instante.
 */
@Composable
private fun rememberFondoBitmap(nombre: String?): ImageBitmap? {
    val contexto = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, nombre, contexto) {
        value = null
        if (nombre.isNullOrBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            leerFondo(contexto, nombre)
        }
    }.value
}

/** Lee el archivo del fondo y lo devuelve como mapa de bits de Compose. */
private fun leerFondo(contexto: Context, nombre: String): ImageBitmap? {
    return try {
        val archivo = File(File(contexto.filesDir, FondosApp.CARPETA), nombre)
        if (!archivo.exists()) return null
        // El archivo ya viene reescalado (2048 px como máximo): entra bien
        // en memoria y no hace falta muestrear.
        BitmapFactory.decodeFile(archivo.absolutePath)?.asImageBitmap()
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}

/**
 * La versión DESENFOCADA de la foto, que es la que se dibuja cuando el
 * usuario pide difuminado.
 *
 * Antes esto era un truco: achicar la foto y estirarla. Se veía "a píxeles",
 * porque una foto chiquita tiene los bordes cuadrados. Ahora es un
 * desenfoque gaussiano de verdad (ver [desenfoqueGaussiano]): la imagen se
 * reduce a 256 px, se le pasa el kernel y después se estira. El resultado es
 * el de una lente: degradados limpios, sin bloques.
 *
 * El radio sale del deslizador, pero se REDONDEA a píxeles enteros: así, al
 * mover el deslizador un poquito no se recalcula el desenfoque entero (el
 * `produceState` solo vuelve a trabajar cuando el radio cambia de verdad).
 *
 * El trabajo va en `Dispatchers.IO`: es cálculo puro y no puede tapar el
 * hilo principal.
 */
@Composable
private fun rememberVersionDifuminada(imagen: ImageBitmap?, difuminado: Float): ImageBitmap? {
    val valor = difuminado.coerceIn(0f, 1f)
    // Con difuminado 0 se dibuja la foto original, tal cual: nítida.
    if (imagen == null || valor <= 0.01f) return null
    val radio = (valor * RADIO_MAXIMO).roundToInt()

    return produceState<ImageBitmap?>(initialValue = null, imagen, radio) {
        value = withContext(Dispatchers.IO) {
            try {
                val original = imagen.asAndroidBitmap()
                val reducida = reducirParaDesenfoque(original, ladoReducidoPara(radio))
                val suave = desenfoqueGaussiano(reducida, radio)
                // La imagen chiquita es temporal: se suelta cuando ya se
                // dibujó (el desenfoque devuelve una copia nueva).
                if (reducida !== original && suave !== reducida) reducida.recycle()
                suave.asImageBitmap()
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
    }.value
}

/**
 * A qué tamaño se reduce la foto antes de desenfocarla, según el radio.
 *
 * Con radio 0 no se reduce nada: la foto queda nítida (es lo que espera
 * quien pone el difuminado en 0). Con radios chicos se reduce a la mitad de
 * lado (un apenas borroso) y con radios grandes se usa la imagen chiquita de
 * [LADO_REDUCIDA], que es la que da el desenfoque de lente.
 */
private fun ladoReducidoPara(radio: Int): Int = when {
    radio <= 0 -> 0
    radio <= 2 -> LADO_REDUCIDA * 2
    else -> LADO_REDUCIDA
}

/**
 * La fase del degradado animado: va de 0 a 1 y vuelve a empezar.
 *
 * ## Por qué el reloj es propio
 *
 * Antes usaba `rememberInfiniteTransition`: su `animationSpec` se guarda en
 * un `remember` SIN claves, así que la transición se queda con la del
 * PRIMER fotograma para siempre. Si el usuario movía el deslizador de
 * velocidad, el fondo seguía moviéndose a la velocidad vieja (ni en la
 * vista previa de Ajustes ni en el fondo real) hasta salirse de Ajustes y
 * volver a entrar: ahí el composable se recreaba y sí cogía la nueva.
 *
 * Con el reloj de abajo la velocidad se aplica AL INSTANTE, y además la
 * fase NO se reinicia: el fondo sigue donde estaba y solo cambia lo
 * rápido que avanza. Tocar el deslizador no da ningún salto.
 *
 * ## Cuando el degradado está apagado
 *
 * No hay reloj corriendo: el degradado se dibuja pero quieto y la app no
 * gasta CPU. Si el usuario apagó todas las animaciones, pasa lo mismo.
 *
 * (No es `private` a propósito: los tests la usan para comprobar que la
 * velocidad se aplica al instante, sin tener que salir de la pantalla.)
 */
@Composable
internal fun rememberFaseDelDegradado(activo: Boolean, velocidad: Float): State<Float> {
    if (!activo || !AparienciaActual.animacionesActivas) {
        return remember { mutableStateOf(0f) }
    }

    val fase = remember { mutableFloatStateOf(0f) }

    // La velocidad se lee en CADA fotograma: si el usuario la cambia a
    // mitad de camino, el cambio se nota al instante (y sin salto).
    val velocidadActual = rememberUpdatedState(velocidad)

    LaunchedEffect(Unit) {
        var anterior = withFrameNanos { it }
        while (true) {
            val ahora = withFrameNanos { it }
            val segundos = (ahora - anterior) / 1_000_000_000f
            anterior = ahora
            fase.floatValue = avanzarFase(fase.floatValue, segundos, velocidadActual.value)
        }
    }

    return fase
}

/**
 * Cómo avanza la fase con el tiempo.
 *
 * `segundos` es lo que pasó desde el fotograma anterior. La fase da una
 * vuelta completa cada [CICLO_DEL_DEGRADADO] milisegundos a velocidad 1x;
 * la velocidad que eligió el usuario multiplica ese avance.
 *
 * El tiempo se TOPA en [SEGUNDO_MAXIMO]: si la app estuvo en segundo plano
 * (o el teléfono se quedó quieto), entre un fotograma y el otro pasa un
 * montón de tiempo. Sin tope, el fondo pegaría un salto gigante al
 * volver; con el tope, sigue donde estaba.
 *
 * El resultado siempre queda entre 0 y 1. Al pasar de 1 a 0 no se ve
 * ningún corte porque el dibujo es CONTINUO: cada forma termina el ciclo
 * EXACTAMENTE donde empezó (ver [dibujarDegradado]).
 */
internal fun avanzarFase(fase: Float, segundos: Float, velocidad: Float): Float {
    val contados = segundos.coerceAtMost(SEGUNDO_MAXIMO)
    val vuelta = contados * velocidad.coerceIn(0.25f, 3f) * 1000f / CICLO_DEL_DEGRADADO
    val resultado = (fase + vuelta) % 1f
    return if (resultado < 0f) resultado + 1f else resultado
}

// ---------------------------------------------------------------------------
// El dibujo
// ---------------------------------------------------------------------------

/**
 * Dibuja el fondo completo: el color del tema, la foto y el degradado.
 *
 * El color del tema va SIEMPRE primero: si la foto tiene transparencia o
 * el degradado está apagado, lo que se ve es el tema (nunca un hueco).
 */
private fun DrawScope.dibujarFondo(pintura: PinturaFondo) {
    // 1. El tema, debajo de todo.
    drawRect(pintura.colorFondo)

    // 2. La foto del teléfono.
    val opacidad = pintura.opacidadImagen.coerceIn(0f, 1f)
    if (pintura.imagen != null && opacidad > 0.01f) {
        val aDibujar = pintura.difuminada ?: pintura.imagen
        // Se estira hasta COBRAR el espacio (no se deforma ni se deja
        // franjas): es lo que hace una foto de fondo.
        val escala = max(
            size.width / aDibujar.width.toFloat(),
            size.height / aDibujar.height.toFloat()
        )
        val ancho = aDibujar.width * escala
        val alto = aDibujar.height * escala
        drawImage(
            image = aDibujar,
            dstOffset = IntOffset(
                ((size.width - ancho) / 2f).roundToInt(),
                ((size.height - alto) / 2f).roundToInt()
            ),
            dstSize = IntSize(ancho.roundToInt(), alto.roundToInt()),
            alpha = opacidad,
            // La foto nítida se estira con bilineal (rápido). La que ya
            // viene desenfocada se estira con filtro bueno: al ser tan
            // chiquita, el filtro hace la diferencia entre un degradado
            // limpio y uno "a cuadritos".
            filterQuality = if (pintura.difuminada != null) FilterQuality.Medium else FilterQuality.Low
        )
    }

    // 3. El degradado animado (si está prendido).
    if (pintura.colores.isNotEmpty() && pintura.opacidadDegradado > 0.01f) {
        dibujarDegradado(pintura.forma, pintura.fase.value, pintura.colores)
    }
}

/**
 * Las cinco formas del degradado.
 *
 * Todas usan la fase (0 a 1) y el seno del ángulo. El truco para que el
 * fondo NO dé un salto al terminar el ciclo es que todas las frecuencias
 * sean ENTERAS: con la fase en 1, sin(2π·k) vale 0 y cos(2π·k) vale 1,
 * exactamente igual que con la fase en 0. Así el ciclo cierra solo: el
 * movimiento es infinito de verdad y no "vuelve al principio" de golpe.
 *
 * (El barrido es la excepción, y a propósito: su banda sale de la
 * pantalla por un lado antes de volver a entrar por el otro, así que el
 * reinicio del ciclo pasa fuera de la vista.)
 *
 * (No es `private` a propósito: las capturas de prueba la llaman con una
 * fase exacta para ver fotograma a fotograma cómo se mueve el fondo.)
 */
internal fun DrawScope.dibujarDegradado(
    forma: FormaDegradado,
    fase: Float,
    colores: List<Color>
) {
    if (colores.isEmpty()) return

    val centro = Offset(size.width / 2f, size.height / 2f)

    when (forma) {

        // Un halo radial en el centro que respira: crece, se achica y sube
        // y baja la luz.
        FormaDegradado.AUREOLA -> {
            val halo = haloAureola(size.minDimension, fase)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colores[0].copy(alpha = colores[0].alpha * halo.brillo),
                        colores[1 % colores.size].copy(alpha = colores[1 % colores.size].alpha * 0.45f),
                        Color.Transparent
                    ),
                    center = centro,
                    radius = halo.radio
                ),
                radius = halo.radio,
                center = centro
            )
        }

        // Tres o cuatro manchas grandes que se deslizan en direcciones
        // distintas y se mezclan entre sí (el modo "Plus" las suma: por eso
        // se mezclan como la aurora del norte).
        FormaDegradado.AURORA -> {
            val radio = size.minDimension * 0.8f
            val puntos = manchasAurora(size.width, size.height, fase)
            puntos.forEachIndexed { indice, centroMancha ->
                val color = colores[indice % colores.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = color.alpha * 0.55f),
                            Color.Transparent
                        ),
                        center = centroMancha,
                        radius = radio
                    ),
                    radius = radio,
                    center = centroMancha,
                    blendMode = BlendMode.Plus
                )
            }
        }

        // Una banda de luz que cruza la pantalla de lado a lado.
        FormaDegradado.BARRIDO -> {
            val ancho = size.width * 0.6f
            // La banda entra por la izquierda y sale por la derecha: en los
            // extremos está fuera de la pantalla, así que cuando el ciclo
            // vuelve a empezar el salto no se ve.
            val (x, x2) = bandasBarrido(size.width, ancho, fase)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, colores[0], Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + ancho, 0f)
                ),
                size = this.size
            )
            // Una segunda banda, más fina y del otro color, da profundidad.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, colores[1 % colores.size], Color.Transparent),
                    start = Offset(x2, 0f),
                    end = Offset(x2 + ancho * 0.5f, 0f)
                ),
                size = this.size
            )
        }

        // Un abanico de color girando alrededor de un punto.
        FormaDegradado.CONICO -> {
            rotate(degrees = giroConico(fase), pivot = centro) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        // El primer color se repite al final: si no, al girar
                        // se ve un corte.
                        colors = colores + colores.first(),
                        center = centro
                    ),
                    radius = max(size.width, size.height) * 0.8f,
                    center = centro,
                    blendMode = BlendMode.Plus
                )
            }
            // El centro, un poco más brillante.
            val radioCentro = size.minDimension * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    center = centro,
                    radius = radioCentro
                ),
                radius = radioCentro,
                center = centro
            )
        }

        // Cuatro focos de luz en las esquinas que se cruzan: cada uno va de
        // su esquina a la de enfrente y vuelve, y al cruzarse en el medio
        // tejen el fondo.
        FormaDegradado.MALLA -> {
            val radio = size.minDimension * 0.7f
            val focos = focosMalla(size.width, size.height, fase)
            focos.forEachIndexed { indice, centroFoco ->
                val color = colores[indice % colores.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = color.alpha * 0.6f),
                            Color.Transparent
                        ),
                        center = centroFoco,
                        radius = radio
                    ),
                    radius = radio,
                    center = centroFoco,
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ayuditas
// ---------------------------------------------------------------------------

/**
 * El movimiento de cada forma, en funciones PURAS (sin dibujar nada).
 *
 * Están aparte del dibujo por dos razones:
 *
 *  1. Se pueden **probar**: los tests comprueban que, al terminar el
 *     ciclo, cada forma vuelve EXACTAMENTE al punto donde empezó (que es
 *     lo que hace que el fondo no dé un salto).
 *  2. Se ven de un vistazo: todas las frecuencias son enteras y eso es
 *     justamente lo que hace que el ciclo cierre.
 */

/** El halo de la aureola: qué tan grande es y qué tan brillante. */
internal data class Halo(val radio: Float, val brillo: Float)

/**
 * El halo de la aureola, con la fase de este momento.
 *
 * Con la fase en 0 y con la fase en 1 da EXACTAMENTE lo mismo (el seno
 * vuelve al mismo punto): por eso el ciclo cierra sin que se note.
 */
internal fun haloAureola(ladoCorto: Float, fase: Float): Halo {
    val ondas = fase * 2f * PI.toFloat()
    return Halo(
        radio = ladoCorto * (0.42f + 0.16f * sin(ondas)),
        brillo = 0.75f + 0.25f * sin(ondas)
    )
}

/**
 * Las cuatro manchas de la aurora, con la fase de este momento.
 *
 * Las frecuencias son ENTERAS (1, 2 y 3) a propósito: con la fase en 1 el
 * seno y el coseno valen exactamente lo mismo que con la fase en 0, así
 * que el ciclo cierra EXACTO y el fondo no pega un salto al terminar.
 *
 * Antes eran decimales (0.8, 1.1, 0.7…): con esos, al terminar el ciclo
 * las manchas quedaban en otro sitio y el fondo cortaba de golpe. Era el
 * único corte que se veía, y justo en la forma que más se nota.
 */
internal fun manchasAurora(ancho: Float, alto: Float, fase: Float): List<Offset> {
    val ondas = fase * 2f * PI.toFloat()
    return listOf(
        Offset(
            x = ancho * (0.20f + 0.22f * sin(ondas)),
            y = alto * (0.26f + 0.16f * cos(ondas))
        ),
        Offset(
            x = ancho * (0.80f + 0.20f * cos(ondas)),
            y = alto * (0.42f + 0.20f * sin(2f * ondas))
        ),
        Offset(
            x = ancho * (0.50f + 0.26f * sin(ondas + 1.6f)),
            y = alto * (0.74f + 0.14f * cos(2f *ondas))
        ),
        Offset(
            x = ancho * (0.30f + 0.18f * sin(2f * ondas + 3.0f)),
            y = alto * (0.56f + 0.22f * cos(3f * ondas))
        )
    )
}

/**
 * La posición de las dos bandas del barrido.
 *
 * La primera entra por la izquierda y sale por la derecha: cuando el
 * ciclo vuelve a empezar, la banda está FUERA de la pantalla, así que el
 * salto no se ve. La segunda es más fina y va del otro color.
 */
internal fun bandasBarrido(anchoPantalla: Float, ancho: Float, fase: Float): Pair<Float, Float> {
    val recorrido = anchoPantalla + ancho * 2f
    val x = -ancho + recorrido * fase
    val x2 = -ancho + recorrido * ((fase + 0.5f) % 1f)
    return x to x2
}

/**
 * Los grados que gira el abanico de color.
 *
 * Con la fase en 1 son 360°, que es lo mismo que 0°: el ciclo cierra sin
 * corte (y el primer color se repite al final del degradado).
 */
internal fun giroConico(fase: Float): Float = fase * 360f

/**
 * Los cuatro focos de la malla, con la fase de este momento.
 *
 * Cada foco va de su esquina a la de enfrente y vuelve (onda triangular):
 * con la fase en 0 y con la fase en 1 están en el mismo sitio.
 */
internal fun focosMalla(ancho: Float, alto: Float, fase: Float): List<Offset> {
    val esquinas = listOf(
        Offset(0f, 0f),
        Offset(ancho, 0f),
        Offset(ancho, alto),
        Offset(0f, alto)
    )
    return esquinas.mapIndexed { indice, esquina ->
        val t = triangular((fase + indice * 0.25f) % 1f)
        val opuesta = esquinas[(indice + 2) % 4]
        Offset(
            x = esquina.x + (opuesta.x - esquina.x) * t,
            y = esquina.y + (opuesta.y - esquina.y) * t
        )
    }
}

/** Mezcla un color con el del fondo: `factor` es cuánto del fondo entra (0..1). */
private fun mezclarConFondo(color: Color, fondo: Color, factor: Float): Color = Color(
    red = color.red + (fondo.red - color.red) * factor,
    green = color.green + (fondo.green - color.green) * factor,
    blue = color.blue + (fondo.blue - color.blue) * factor,
    alpha = 1f
)

/**
 * Una onda triangular: 0 en 0, 1 en 0.5 y otra vez 0 en 1.
 *
 * Sirve para que un foco vaya de un punto a otro y vuelva, sin el tirón
 * que daría ir y volver en línea recta.
 */
private fun triangular(valor: Float): Float = 1f - abs(2f * valor - 1f)
