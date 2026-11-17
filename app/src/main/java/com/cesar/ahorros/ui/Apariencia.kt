package com.cesar.ahorros.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cesar.ahorros.data.AjustesAnimacion
import com.cesar.ahorros.data.AjustesApariencia
import com.cesar.ahorros.data.AjustesFondo
import com.cesar.ahorros.data.ColorTema
import com.cesar.ahorros.data.ColorTexto
import com.cesar.ahorros.data.EstiloGraficas
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.EstiloNumeros
import com.cesar.ahorros.data.FormaDegradado
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * La capa de **apariencia**: los colores que el usuario cambia a mano, el
 * fondo y las animaciones.
 *
 * Está separada en dos partes:
 *  - `data/Apariencia.kt` tiene los catálogos (los nombres, las
 *    explicaciones) y el formato en que se guarda todo.
 *  - Este archivo tiene lo que se ve: los colores ya convertidos, el estado de
 *    Compose y las reglas para aplicarlos sobre el tema.
 *
 * La idea es que la app "de fábrica" se vea bien sola (los 7 temas con sus
 * contrastes medidos), pero que cualquiera pueda corregirla: cambiar el
 * verde de los ahorros, poner un fondo con una foto, o bajar la velocidad de
 * las animaciones a cero.
 */

// ---------------------------------------------------------------------------
// Los colores
// ---------------------------------------------------------------------------

/**
 * Los colores que el usuario cambió.
 *
 * Un mapa vacío significa "no toques nada": el color sale del tema. Solo se
 * guardan los que el usuario eligió, así que cambiar de tema base sigue
 * funcionando (los colores propios se quedan, los demás se mueven con el
 * tema).
 */
data class PaletaPersonalizada(
    val tema: Map<ColorTema, Color> = emptyMap(),
    val textos: Map<ColorTexto, Color> = emptyMap()
) {

    /** ¿El usuario cambió algo? */
    val vacia: Boolean get() = tema.isEmpty() && textos.isEmpty()

    /** Cuántos colores se cambiaron en total. */
    val cantidad: Int get() = tema.size + textos.size

    /**
     * Una huella corta de la paleta.
     *
     * Sirve para las claves de `remember`: si el usuario cambia un color, la
     * huella cambia y lo que dependía de los colores se vuelve a dibujar.
     */
    val huella: String
        get() = buildString {
            tema.forEach { (slot, color) -> append(slot.id).append('=').append(color.aHex()).append(';') }
            textos.forEach { (slot, color) -> append(slot.id).append('=').append(color.aHex()).append(';') }
        }

    /** El color de un slot del tema, o `null` si el usuario no lo cambió. */
    fun colorTema(slot: ColorTema): Color? = tema[slot]

    /** El color de un slot de texto, o `null` si el usuario no lo cambió. */
    fun colorTexto(slot: ColorTexto): Color? = textos[slot]

    /** Devuelve la paleta con un color del tema cambiado (o quitado). */
    fun conTema(slot: ColorTema, color: Color?): PaletaPersonalizada {
        val copia = tema.toMutableMap()
        if (color == null) copia.remove(slot) else copia[slot] = color
        return copy(tema = copia)
    }

    /** Devuelve la paleta con un color de texto cambiado (o quitado). */
    fun conTexto(slot: ColorTexto, color: Color?): PaletaPersonalizada {
        val copia = textos.toMutableMap()
        if (color == null) copia.remove(slot) else copia[slot] = color
        return copy(textos = copia)
    }

    companion object {
        /** La paleta vacía: todo lo que se ve sale del tema. */
        val VACIA = PaletaPersonalizada()
    }
}

/** Convierte un color a `#RRGGBB` (sin alfa). */
fun Color.aHex(): String {
    val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/** Convierte `#RRGGBB` (o `#AARRGGBB`) a un color; `null` si no es válido. */
fun hexAColor(hex: String): Color? {
    val limpio = hex.trim().removePrefix("#")
    if (limpio.length != 6 && limpio.length != 8) return null
    val valor = limpio.toLongOrNull(16) ?: return null
    return if (limpio.length == 6) {
        Color(0xFF000000L or valor)
    } else {
        Color(valor)
    }
}

/**
 * Le pone la paleta del usuario encima de un tema.
 *
 * Devuelve el tema YA con los colores cambiados: el resto de la app sigue
 * usando los tokens (`Fondo`, `AcentoCalido`…) y no sabe nada de esto.
 */
fun TemaVisual.conPaleta(paleta: PaletaPersonalizada): TemaVisual {    if (paleta.vacia) return this
    return copy(
        fondo = paleta.colorTema(ColorTema.FONDO) ?: fondo,
        superficie = paleta.colorTema(ColorTema.SUPERFICIE) ?: superficie,
        superficieAlta = paleta.colorTema(ColorTema.SUPERFICIE_ALTA) ?: superficieAlta,
        superficieMaxima = paleta.colorTema(ColorTema.SUPERFICIE_MAXIMA) ?: superficieMaxima,
        borde = paleta.colorTema(ColorTema.BORDE) ?: borde,
        textoPrimario = paleta.colorTexto(ColorTexto.TEXTO_PRIMARIO) ?: textoPrimario,
        textoSecundario = paleta.colorTexto(ColorTexto.TEXTO_SECUNDARIO) ?: textoSecundario,
        textoTercero = paleta.colorTexto(ColorTexto.TEXTO_TERCERO) ?: textoTercero,
        acentoCalido = paleta.colorTema(ColorTema.ACENTO_MARCA) ?: acentoCalido,
        acentoMenta = paleta.colorTema(ColorTema.ACENTO_MENTA) ?: acentoMenta,
        acentoAzul = paleta.colorTema(ColorTema.ACENTO_AZUL) ?: acentoAzul,
        acentoVioleta = paleta.colorTema(ColorTema.ACENTO_VIOLETA) ?: acentoVioleta,
        acentoHielo = paleta.colorTema(ColorTema.ACENTO_HIELO) ?: acentoHielo,
        colorGasto = paleta.colorTema(ColorTema.COLOR_GASTO) ?: colorGasto,
        colorAviso = paleta.colorTema(ColorTema.COLOR_AVISO) ?: colorAviso
    )
}

/**
 * Convierte la paleta de la app (colores) a la que se guarda (texto).
 *
 * Lo que no es un hexadecimal válido se pierde: es preferible no guardar
 * un color raro que romper la app al abrirla.
 */
fun PaletaPersonalizada.aAjustes(): AjustesApariencia = AjustesApariencia(
    // La clave es el id del color (un texto), no el enum: así el archivo de
    // ajustes se puede leer y editar a mano.
    coloresTema = tema.mapKeys { (slot, _) -> slot.id }
        .mapValues { (_, color) -> color.aHex() },
    coloresTexto = textos.mapKeys { (slot, _) -> slot.id }
        .mapValues { (_, color) -> color.aHex() }
)

/** Lee la paleta de los ajustes guardados. Lo que no se reconoce se ignora. */
fun AjustesApariencia.aPaleta(): PaletaPersonalizada {
    val delTema = mutableMapOf<ColorTema, Color>()
    coloresTema.forEach { (id, hex) ->
        val slot = ColorTema.porId(id) ?: return@forEach
        val color = hexAColor(hex) ?: return@forEach
        delTema[slot] = color
    }
    val deTextos = mutableMapOf<ColorTexto, Color>()
    coloresTexto.forEach { (id, hex) ->
        val slot = ColorTexto.porId(id) ?: return@forEach
        val color = hexAColor(hex) ?: return@forEach
        deTextos[slot] = color
    }
    return PaletaPersonalizada(tema = delTema, textos = deTextos)
}

// ---------------------------------------------------------------------------
// El contraste (WCAG 2.2)
// ---------------------------------------------------------------------------

/**
 * El contraste entre dos colores, según WCAG 2.2.
 *
 * Vive en la app (no solo en los tests) porque el selector de color le dice
 * al usuario, en vivo, si el color que está choosing se lee o no.
 */
object Contraste {

    /** WCAG 1.4.3 — texto normal. */
    const val UMBRAL_TEXTO = 4.5

    /** WCAG 1.4.11 — texto grande y elementos de UI (bordes, íconos, gráficas). */
    const val UMBRAL_NO_TEXTO = 3.0

    /** La luminancia relativa de un color (0 = negro, 1 = blanco). */
    fun luminancia(color: Color): Double {
        fun canal(componente: Float): Double {
            val c = componente.coerceIn(0f, 1f).toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = canal(color.red)
        val g = canal(color.green)
        val b = canal(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** La razón de contraste entre dos colores (1 = iguales, 21 = blanco y negro). */
    fun ratio(primero: Color, segundo: Color): Double {
        val l1 = luminancia(primero)
        val l2 = luminancia(segundo)
        val claro = max(l1, l2)
        val oscuro = min(l1, l2)
        return (claro + 0.05) / (oscuro + 0.05)
    }

    /** ¿El texto se lee sobre ese fondo? (WCAG AA) */
    fun textoSeLee(texto: Color, fondo: Color): Boolean =
        ratio(texto, fondo) + 1e-9 >= UMBRAL_TEXTO
}

// ---------------------------------------------------------------------------
// El estado de la apariencia
// ---------------------------------------------------------------------------

/**
 * El color que tiene ahora un slot del tema (con la personalización puesta).
 *
 * Sirve para el selector de colores: hay que mostrar el color que se ve, no
 * el del catálogo.
 */
fun TemaVisual.colorDe(slot: ColorTema): Color = when (slot) {
    ColorTema.FONDO -> fondo
    ColorTema.SUPERFICIE -> superficie
    ColorTema.SUPERFICIE_ALTA -> superficieAlta
    ColorTema.SUPERFICIE_MAXIMA -> superficieMaxima
    ColorTema.BORDE -> borde
    ColorTema.ACENTO_MARCA -> acentoCalido
    ColorTema.ACENTO_MENTA -> acentoMenta
    ColorTema.ACENTO_AZUL -> acentoAzul
    ColorTema.ACENTO_VIOLETA -> acentoVioleta
    ColorTema.ACENTO_HIELO -> acentoHielo
    ColorTema.COLOR_GASTO -> colorGasto
    ColorTema.COLOR_AVISO -> colorAviso
}

/** El color que tiene ahora un slot de texto (con la personalización puesta). */
fun TemaVisual.colorDe(slot: ColorTexto): Color = when (slot) {
    ColorTexto.TEXTO_PRIMARIO -> textoPrimario
    ColorTexto.TEXTO_SECUNDARIO -> textoSecundario
    ColorTexto.TEXTO_TERCERO -> textoTercero
}

/**
 * Los ajustes de apariencia que están **activos ahora mismo**.
 *
 * Es estado de Compose: cuando algo cambia, todo lo que usa un color o una
 * animación se redibuja solo, sin tocar las pantallas.
 */
object AparienciaActual {

    /** Los colores que el usuario cambió. */
    var paleta by mutableStateOf(PaletaPersonalizada.VACIA)

    /** Las animaciones: el interruptor, la velocidad y el estilo de cada cosa. */
    var animaciones by mutableStateOf(AjustesAnimacion())

    /** El fondo: la imagen del teléfono y el degradado animado. */
    var fondo by mutableStateOf(AjustesFondo())

    /**
     * Carga los ajustes guardados (al abrir la app).
     *
     * La paleta se le pasa aparte porque el tema tiene que aplicarla.
     */
    fun cargar(ajustes: AjustesApariencia) {
        paleta = ajustes.aPaleta()
        animaciones = ajustes.animaciones
        fondo = ajustes.fondo
    }

    /** ¿Hay algo puesto detrás de la app? */
    val hayFondo: Boolean
        get() = fondo.imagen != null || fondo.degradadoActivo

    /** ¿Las animaciones están prendidas? */
    val animacionesActivas: Boolean
        get() = animaciones.activas

    /** El estilo de las gráficas (con el interruptor maestro aplicado). */
    val estiloGraficas: EstiloGraficas
        get() = if (animaciones.activas) EstiloGraficas.porId(animaciones.graficas) else EstiloGraficas.NINGUNA

    /** El estilo de los números (con el interruptor maestro aplicado). */
    val estiloNumeros: EstiloNumeros
        get() = if (animaciones.activas) EstiloNumeros.porId(animaciones.numeros) else EstiloNumeros.NINGUNA

    /** El estilo de la interfaz (con el interruptor maestro aplicado). */
    val estiloInterfaz: EstiloInterfaz
        get() = if (animaciones.activas) EstiloInterfaz.porId(animaciones.interfaz) else EstiloInterfaz.NINGUNA

    /** La forma del degradado (con el interruptor maestro aplicado). */
    val formaDegradado: FormaDegradado
        get() = FormaDegradado.porId(fondo.formaDegradado)

    /**
     * Cuánto dura una animación, según la velocidad que eligió el usuario.
     *
     * `base` es la duración "normal" en milisegundos. La velocidad divide:
     * más velocidad, menos tiempo.
     */
    fun duracion(base: Int): Int {
        if (!animaciones.activas) return 0
        val factor = animaciones.velocidad.coerceIn(0.25f, 4f)
        return (base / factor).toInt().coerceAtLeast(60)
    }
}
