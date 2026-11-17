package com.cesar.ahorros.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.cesar.ahorros.data.EstiloGraficas
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.EstiloNumeros

/**
 * # El motor de animaciones de la app
 *
 * Acá viven las tres animaciones que la app usa (los números, las gráficas y
 * la entrada de la interfaz) y el interruptor maestro que las apaga a todas.
 *
 * La idea es que **el resto de la app no sepa cómo se anima**: pide un
 * progreso o un número y lo dibuja como le digan. Así el estilo que eligió el
 * usuario se aplica en un solo lugar, y apagar las animaciones es una sola
 * decisión en un solo archivo.
 *
 * ## Cómo lo usa el resto de la app
 *
 * - **Los números** — `rememberNumeroAnimado(valor)` devuelve el número que
 *   se está mostrando. El que lo llama solo lo formatea:
 *   `Text(animado.dineroSimbolo())`.
 *
 * - **Las gráficas** — `progresoDeGraficas()` devuelve un progreso (0..1).
 *   Como las gráficas se dibujan con `Canvas`, ese progreso se aplica
 *   **multiplicando los valores que se dibujan** (la altura de la barra, el
 *   ángulo del arco, el largo de la línea), nunca con un `graphicsLayer`
 *   sobre el `Canvas`: el `Canvas` dibuja píxeles, no transformaciones.
 *
 * - **La interfaz** — `progresoDeEntrada()` devuelve un progreso (0..1) para
 *   la entrada de un bloque. Se aplica con `alpha`, `scale` u `offset`:
 *   nunca con `width`, `height`, `top` ni `padding`, porque animar el layout
 *   es justo lo que hace que un teléfono de colegio tartamudee.
 *
 * ## El interruptor maestro manda
 *
 * `AparienciaActual.animaciones.activas` es el equivalente de
 * `prefers-reduced-motion`: si está apagado, **nada** se mueve (el progreso
 * vale 1f y el número vale su valor final, desde el primer fotograma). Por
 * eso los estilos de `AparienciaActual` (`estiloGraficas`, `estiloNumeros`,
 * `estiloInterfaz`) ya devuelven `NINGUNA` cuando el interruptor está
 * apagado: acá solo hay que respetarlos.
 *
 * ## La velocidad
 *
 * Todas las duraciones salen de `AparienciaActual.duracion(base)`: la
 * velocidad que eligió el usuario divide la duración base. El motor no tiene
 * tiempos propios.
 */

/** Las duraciones base, en milisegundos. */
object Duraciones {
    const val NUMEROS = 900
    const val GRAFICAS = 700
    const val INTERFAZ = 260
}

/**
 * El número que se está mostrando, animado según el estilo elegido.
 * Devuelve el valor actual (0..valor) para que el que lo llama lo formatee.
 * Si las animaciones están apagadas o el estilo es NINGUNA, vale `valor`.
 *
 * ## Qué hace cada estilo
 *
 * - **CONTAR**: la cifra sube desde cero hasta su valor real. Es una entrada
 *   que **desacelera** (`FastOutSlowInEasing`): arranca rápido y frena, así
 *   se puede leer la cifra mientras sube.
 * - **FUNDIDO**: la cifra no se cuenta, aparece en su valor final con un
 *   cambio de opacidad; el fundido lo aplica quien la usa.
 * - **NINGUNA** (o el interruptor apagado): la cifra vale su valor final.
 *
 * ## Lo importante: de dónde arranca
 *
 * Si el valor cambia mientras la cifra está subiendo, el tramo nuevo **no
 * vuelve a empezar desde cero**: sigue desde lo que se está viendo. Así una
 * cifra que cambia cada segundo se lee como un contador vivo y no como una
 * animación que se reinicia. La única excepción es cuando el número cruza el
 * cero de negativo a positivo: ahí sí vuelve a contar desde cero, porque
 * contar hacia arriba desde cero se entiende mejor.
 *
 * El número se lleva en un tramo (de qué valor a qué valor) y un progreso
 * (0..1): así el tramo puede cambiar a mitad de camino sin que la cifra
 * salte, y la cuenta es exacta al final (nunca queda un céntimo de diferencia).
 */
@Composable
fun rememberNumeroAnimado(valor: Double): State<Double> {
    val estilo = AparienciaActual.estiloNumeros
    val duracion = AparienciaActual.duracion(Duraciones.NUMEROS)

    // El progreso del tramo (0..1) y el tramo que se está contando.
    val progreso = remember { Animatable(0f) }
    val tramo = remember { mutableStateOf(Tramo(0.0, 0.0)) }
    val estadoProgreso = remember { progreso.asState() }

    // El último valor pedido: sirve para saber si el número cruzó el cero.
    val anterior = remember { mutableStateOf<Double?>(null) }

    // El valor tal cual, para cuando no hay conteo (así no hay parpadeo en el
    // primer fotograma: la cifra vale su valor final desde el primer cuadro).
    val directo = remember { mutableStateOf(valor) }

    LaunchedEffect(valor, estilo, duracion) {
        val previo = anterior.value
        anterior.value = valor

        if (estilo != EstiloNumeros.CONTAR) {
            // Sin conteo: la cifra ya está en su sitio y no se mueve.
            tramo.value = Tramo(valor, valor)
            progreso.snapTo(1f)
            directo.value = valor
            return@LaunchedEffect
        }

        // Dónde está la cifra AHORA. Si el valor cambió a mitad de camino,
        // el tramo nuevo tiene que arrancar desde lo que se ve.
        val t = tramo.value
        val enPantalla = t.inicio + (t.destino - t.inicio) * progreso.value

        val inicio = when {
            // Primera vez: cuenta desde cero (es el efecto "contar").
            previo == null -> 0.0
            // Cruzó el cero: vuelve a contar desde cero (se entiende mejor).
            previo < 0.0 && valor > 0.0 -> 0.0
            // Si no, sigue desde donde está: no reinicia el contador.
            else -> enPantalla
        }

        tramo.value = Tramo(inicio, valor)
        progreso.snapTo(0f)
        // Un `tween`, no un resorte: un resorte se pasa del objetivo y
        // mostraría una cifra equivocada un instante. Con `tween` el número
        // llega exacto y se queda ahí.
        progreso.animateTo(
            targetValue = 1f,
            animationSpec = tween<Float>(
                durationMillis = duracion,
                easing = FastOutSlowInEasing
            )
        )
    }

    // El valor que se ve: el tramo interpolado con el progreso.
    val mostrado = remember {
        derivedStateOf {
            val t = tramo.value
            t.inicio + (t.destino - t.inicio) * estadoProgreso.value
        }
    }

    return if (estilo == EstiloNumeros.CONTAR) mostrado else directo
}

/**
 * El progreso (0f..1f) con el que se dibujan las gráficas ahora mismo.
 * Con el estilo CRECER: las barras crecen desde la base y la dona se abre.
 * Con DIBUJAR: la línea del área se dibuja de izquierda a derecha.
 * Con SUAVE: solo un fundido (el progreso va 0..1 igual, el que lo usa decide).
 * Con NINGUNA o animaciones apagadas: 1f siempre (se ve todo de una).
 *
 * ## Cómo se aplica
 *
 * Multiplicando los valores que se dibujan (nunca con `graphicsLayer`):
 *
 * ```kotlin
 * val progreso = progresoDeGraficas()
 * Canvas(modifier) {
 *     val altura = (punto.ahorro / maximo * altoUtil * progreso).toFloat()
 * }
 * ```
 *
 * ## Qué patrón usa cada estilo
 *
 * - **CRECER** — las barras salen disparadas y frenan (`FastOutSlowInEasing`):
 *   se lee de una dónde está el valor más alto.
 * - **DIBUJAR** — la línea se traza a velocidad constante (`LinearEasing`),
 *   como un lápiz que escribe de izquierda a derecha.
 * - **SUAVE** — un fundido parejo, sin que las barras se muevan.
 */
@Composable
fun progresoDeGraficas(): Float {
    val estilo = AparienciaActual.estiloGraficas
    val duracion = AparienciaActual.duracion(Duraciones.GRAFICAS)
    val activo = estilo != EstiloGraficas.NINGUNA

    val especificacion: AnimationSpec<Float> = when (estilo) {
        EstiloGraficas.CRECER -> tween<Float>(duracion, easing = FastOutSlowInEasing)
        EstiloGraficas.DIBUJAR -> tween<Float>(duracion, easing = LinearEasing)
        EstiloGraficas.SUAVE -> tween<Float>(duracion, easing = LinearEasing)
        EstiloGraficas.NINGUNA -> tween<Float>(duracion, easing = LinearEasing)
    }

    return rememberProgreso(activo, duracion, estilo, especificacion)
}

/**
 * El progreso de entrada (0f..1f) de un bloque de la interfaz.
 * FUNDIDO: solo cambia la opacidad (el que lo usa aplica).
 * DESLIZAR: además se desplaza (el que lo usa aplica).
 * ESCALAR: además escala (el que lo usa aplica).
 * NINGUNA o animaciones apagadas: 1f siempre.
 *
 * ## Qué patrón usa cada estilo
 *
 * - **FUNDIDO** — un `tween` lineal corto: lo más discreto, nada se desliza
 *   de la nada.
 * - **DESLIZAR** — un `tween` con `FastOutSlowInEasing`: entra desde abajo y
 *   frena al acomodarse (llega desacelerando, como una tarjeta que se
 *   acomoda sola en la pantalla).
 * - **ESCALAR** — *asentamiento con resorte*: la tarjeta llega un poco pasada
 *   y vuelve. Por eso, con este estilo, el progreso puede pasar de 1f un
 *   instante: ese rebote es el efecto, no un error. Al final queda en 1f.
 *
 *   Ojo: ese rebote solo tiene sentido en la **escala**. Si lo usás para el
 *   `alpha`, acotalo (`alpha = entrada.coerceIn(0f, 1f)`), porque una opacidad
 *   mayor de 1 no tiene a qué asentarse.
 *
 * ## Cómo se aplica
 *
 * Solo con `alpha`, `scale` y `offset` (nunca con `width`, `height`, `top` ni
 * `padding`: animar el layout es lo que hace que la app tartamudee):
 *
 * ```kotlin
 * val entrada = progresoDeEntrada()
 * Card(
 *     modifier = modifier.graphicsLayer {
 *         alpha = entrada
 *         scaleX = 0.96f + 0.04f * entrada
 *     }
 * ) { ... }
 * ```
 */
@Composable
fun progresoDeEntrada(): Float {
    val estilo = AparienciaActual.estiloInterfaz
    val duracion = AparienciaActual.duracion(Duraciones.INTERFAZ)
    val activo = estilo != EstiloInterfaz.NINGUNA

    val especificacion: AnimationSpec<Float> = when (estilo) {
        EstiloInterfaz.ESCALAR -> resorteAsentamiento(duracion)
        EstiloInterfaz.DESLIZAR -> tween<Float>(duracion, easing = FastOutSlowInEasing)
        EstiloInterfaz.FUNDIDO -> tween<Float>(duracion, easing = LinearEasing)
        EstiloInterfaz.NINGUNA -> tween<Float>(duracion, easing = LinearEasing)
    }

    return rememberProgreso(activo, duracion, estilo, especificacion)
}

// ---------------------------------------------------------------------------
// Lo de adentro
// ---------------------------------------------------------------------------

/**
 * Un tramo del contador: de qué valor a qué valor va.
 *
 * El progreso (0..1) dice en qué parte del tramo estamos; el tramo dice a
 * dónde vamos. Así, si el valor cambia a mitad de camino, se puede arrancar
 * el tramo nuevo desde la cifra que se está viendo sin que esta salte.
 */
private data class Tramo(val inicio: Double, val destino: Double)

/**
 * El resorte del *asentamiento*.
 *
 * Un resorte no tiene duración: tiene rigidez. Por eso la velocidad del
 * usuario se traduce a rigidez (más rápido = más rígido) y no a milisegundos.
 * `dampingRatio = 0.6` deja un rebote apenas perceptible: se siente el peso
 * de la tarjeta sin que parezca un error.
 */
private fun resorteAsentamiento(duracionMs: Int): AnimationSpec<Float> {
    val factor = (Duraciones.INTERFAZ.toFloat() / duracionMs.coerceAtLeast(1))
        .coerceIn(0.25f, 4f)
    return spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 320f * factor
    )
}

/**
 * El motor común de los dos progresos (gráficas e interfaz).
 *
 * Hace siempre lo mismo: un progreso que va de 0 a 1 al entrar. Lo único que
 * cambia es la `especificacion` (con qué curva y a qué velocidad) y el
 * `activo` (si el interruptor maestro está apagado, el progreso es 1f desde
 * el primer fotograma: se ve todo de una y no hay movimiento).
 *
 * - El `Animatable` se recuerda **sin claves**: no depende de nada que se
 *   recree en cada recomposición, así que la animación no se reinicia sola.
 * - La clave del `LaunchedEffect` es lo que realmente puede cambiar: el
 *   interruptor, la velocidad y el estilo. Si cambia la velocidad a mitad de
 *   camino, la animación **sigue** desde donde estaba (no salta a cero).
 */
@Composable
private fun rememberProgreso(
    activo: Boolean,
    duracion: Int,
    clave: Any,
    especificacion: AnimationSpec<Float>
): Float {
    val animable = remember { Animatable(0f) }
    val estado = remember { animable.asState() }

    // Si el interruptor estaba apagado, hay que rearmar el progreso para que
    // vuelva a entrar desde cero cuando se vuelve a prender.
    val estabaApagado = remember { mutableStateOf(false) }

    LaunchedEffect(activo, duracion, clave) {
        if (!activo) {
            animable.snapTo(1f)
            estabaApagado.value = true
        } else {
            if (estabaApagado.value) animable.snapTo(0f)
            estabaApagado.value = false
            animable.animateTo(targetValue = 1f, animationSpec = especificacion)
        }
    }

    // Apagado: 1f siempre. Encendido: el progreso real, fotograma a fotograma.
    return if (activo) estado.value else 1f
}
