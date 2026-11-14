package com.cesar.ahorros

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.cesar.ahorros.ui.CatalogoTemas
import com.cesar.ahorros.ui.TemaVisual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Vigila la accesibilidad de color de LOS 7 TEMAS de la app contra WCAG 2.2 AA.
 *
 * Los colores NO están escritos a mano en este test: se leen de los objetos
 * REALES de producción (`CatalogoTemas.todos`). Antes se parseaba el código
 * fuente de `ui/Theme.kt` con expresiones regulares, pero con 7 paletas eso
 * ya no decía nada: ahora se usa la app de verdad.
 *
 * Para pasar un `Color` de Compose a ARGB (0xAARRGGBB) se usa la extensión
 * `androidx.compose.ui.graphics.toArgb()`.
 *
 * Fórmulas (WCAG 2.x):
 *  - Luminancia relativa sRGB:
 *      c = canal / 255
 *      c <= 0.03928  ->  c / 12.92
 *      c >  0.03928  ->  ((c + 0.055) / 1.055) ^ 2.4
 *      L = 0.2126 R + 0.7152 G + 0.0722 B
 *  - Razón de contraste: (L1 + 0.05) / (L2 + 0.05), con L1 = color más claro.
 *
 * Umbrales:
 *  - 4.5:1  texto normal (1.4.3 Contrast (Minimum), AA).
 *  - 3.0:1  texto grande (>= 18 sp, o >= 14 sp en negrita) y elementos de UI:
 *            bordes, iconos y series de gráficas (1.4.11 Non-text Contrast).
 *
 * En la práctica se exige 4.5:1 para TODO el texto (aunque sea grande) para
 * no depender del tamaño real de cada rótulo, y 3.0:1 para los elementos no
 * textuales.
 *
 * Qué se mira en CADA tema:
 *  a) texto y colores de marca (textoPrimario, textoSecundario, textoTercero,
 *     acentoCalido, acentoMenta, acentoAzul, acentoVioleta, acentoHielo,
 *     colorGasto, colorAviso) contra fondo, superficie y superficieAlta: 4.5:1.
 *  b) el borde contra fondo, superficie y superficieAlta: 3.0:1.
 *  c) el "onPrimary" implícito: el fondo contra el acento cálido, porque el
 *     fondo es el color del contenido que va ENCIMA del acento (botón, ícono,
 *     barra de navegación): 4.5:1.
 */
class ContrasteTest {

    // ---------------------------------------------------------------------
    // WCAG 2.2
    // ---------------------------------------------------------------------

    /** Luminancia relativa de un color empaquetado 0xAARRGGBB. */
    private fun luminancia(argb: Long): Double {
        fun canal(componente: Int): Double {
            val c = componente / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = canal(((argb shr 16) and 0xFF).toInt())
        val g = canal(((argb shr 8) and 0xFF).toInt())
        val b = canal((argb and 0xFF).toInt())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Razón de contraste entre dos colores empaquetados 0xAARRGGBB. */
    private fun ratio(primero: Long, segundo: Long): Double {
        val l1 = luminancia(primero)
        val l2 = luminancia(segundo)
        val claro = max(l1, l2)
        val oscuro = min(l1, l2)
        return (claro + 0.05) / (oscuro + 0.05)
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    /** ARGB (0xAARRGGBB) de un color real de Compose. */
    private fun argb(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL

    /** El color en hexadecimal, para que el fallo diga exactamente cuál es. */
    private fun hex(color: Color): String = String.format(Locale.ROOT, "#%08X", argb(color))

    /**
     * El color de un token, según el tema.
     *
     * Los nombres son los del `TemaVisual` de producción.
     */
    private fun colorDe(tema: TemaVisual, token: String): Color = when (token) {
        "fondo" -> tema.fondo
        "superficie" -> tema.superficie
        "superficieAlta" -> tema.superficieAlta
        "borde" -> tema.borde
        "textoPrimario" -> tema.textoPrimario
        "textoSecundario" -> tema.textoSecundario
        "textoTercero" -> tema.textoTercero
        "acentoCalido" -> tema.acentoCalido
        "acentoMenta" -> tema.acentoMenta
        "acentoAzul" -> tema.acentoAzul
        "acentoVioleta" -> tema.acentoVioleta
        "acentoHielo" -> tema.acentoHielo
        "colorGasto" -> tema.colorGasto
        "colorAviso" -> tema.colorAviso
        else -> error("El token «$token» no existe en el tema «${tema.id}»")
    }

    /**
     * Falla el test si el par de tokens no llega al mínimo de WCAG.
     * El mensaje dice el tema, el token, el color en hexadecimal y el ratio
     * real, para que se pueda corregir de una.
     */
    private fun exigir(tema: TemaVisual, token: String, fondoToken: String, minimo: Double) {
        val color = colorDe(tema, token)
        val fondo = colorDe(tema, fondoToken)
        val r = ratio(argb(color), argb(fondo))
        val mensaje = String.format(
            Locale.ROOT,
            "Contraste insuficiente en el tema «%s» (%s): %s (%s) sobre %s (%s) = %.2f:1 — " +
                "se exige %.1f:1 (WCAG 2.2 AA)",
            tema.nombre, tema.id, token, hex(color), fondoToken, hex(fondo), r, minimo
        )
        assertTrue(mensaje, r + 1e-9 >= minimo)
    }

    // ---------------------------------------------------------------------
    // a) Texto y colores de marca: 4.5:1 (WCAG 1.4.3)
    // ---------------------------------------------------------------------

    @Test
    fun `el texto de cada tema contrasta con el fondo la superficie y la superficie alta`() {
        for (tema in CatalogoTemas.todos) {
            for (token in TOKENS_DE_TEXTO) {
                for (fondoToken in FONDOS) {
                    exigir(tema, token, fondoToken, UMBRAL_TEXTO)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // b) Bordes de control: 3:1 (WCAG 1.4.11)
    // ---------------------------------------------------------------------

    @Test
    fun `el borde de cada tema contrasta con el fondo la superficie y la superficie alta`() {
        for (tema in CatalogoTemas.todos) {
            for (fondoToken in FONDOS) {
                exigir(tema, "borde", fondoToken, UMBRAL_NO_TEXTO)
            }
        }
    }

    // ---------------------------------------------------------------------
    // c) El contenido que va ENCIMA del acento cálido (el "onPrimary")
    // ---------------------------------------------------------------------

    @Test
    fun `el contenido que va encima del acento calido se lee en cada tema`() {
        for (tema in CatalogoTemas.todos) {
            // El fondo es el color del botón, del ícono y de la barra de
            // navegación que se dibujan ENCIMA del acento cálido.
            exigir(tema, "fondo", "acentoCalido", UMBRAL_TEXTO)
        }
    }

    // ---------------------------------------------------------------------
    // La propia fórmula: si esto está mal, todo lo demás miente.
    // ---------------------------------------------------------------------

    @Test
    fun `la razon de contraste coincide con los valores de referencia`() {
        assertEquals("Blanco sobre negro", 21.0, ratio(0xFFFFFFFF, 0xFF000000), 0.001)
        assertEquals("Un color contra sí mismo", 1.0, ratio(0xFF123456, 0xFF123456), 0.0001)
        assertEquals(
            "El gris de referencia de WCAG sobre blanco",
            4.54, ratio(0xFF767676, 0xFFFFFFFF), 0.01
        )
        assertEquals(
            "El canal alfa no cuenta para el contraste",
            1.0, ratio(0x80123456, 0xFF123456), 0.0001
        )
    }

    // ---------------------------------------------------------------------
    // La paleta de las gráficas: 6 series, siempre en el mismo orden.
    // ---------------------------------------------------------------------

    @Test
    fun `la paleta categorica son las seis series de la grafica en su orden`() {
        for (tema in CatalogoTemas.todos) {
            assertEquals(
                "La paleta del tema «${tema.id}» debe traer las 6 series en su orden",
                listOf(
                    tema.acentoCalido,
                    tema.acentoAzul,
                    tema.acentoMenta,
                    tema.acentoVioleta,
                    tema.colorAviso,
                    tema.colorGasto
                ),
                tema.paleta
            )
        }
    }

    @Test
    fun `la paleta categorica del tema por defecto son seis colores`() {
        val tema = CatalogoTemas.porDefecto
        assertEquals("La paleta debe tener 6 series", 6, tema.paleta.size)
        assertEquals(
            "Las 6 series del tema por defecto deben ser colores distintos",
            6,
            tema.paleta.distinct().size
        )
    }

    // ---------------------------------------------------------------------
    // Constantes
    // ---------------------------------------------------------------------

    private companion object {
        /** WCAG 1.4.3 — texto normal. */
        const val UMBRAL_TEXTO = 4.5

        /** WCAG 1.4.11 — texto grande y elementos de UI (bordes, iconos, gráficas). */
        const val UMBRAL_NO_TEXTO = 3.0

        /** Los tokens que se leen como texto (rótulos, cifras, íconos). */
        val TOKENS_DE_TEXTO = listOf(
            "textoPrimario",
            "textoSecundario",
            "textoTercero",
            "acentoCalido",
            "acentoMenta",
            "acentoAzul",
            "acentoVioleta",
            "acentoHielo",
            "colorGasto",
            "colorAviso"
        )

        /** Las superficies sobre las que se dibuja el texto. */
        val FONDOS = listOf("fondo", "superficie", "superficieAlta")
    }
}
