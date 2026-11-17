package com.cesar.ahorros

import com.cesar.ahorros.data.FondosApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del **fondo con imagen**: cómo se reescala la foto que el usuario
 * elige para que se vea bien de fondo y no pese demasiado.
 *
 * Son tests JVM puros: las dos funciones que se prueban son aritmética, sin
 * Android. Lo que se vigila es lo que de verdad importa en un teléfono de
 * colegio:
 *  - que la imagen NUNCA se agrande (una foto de 300 px no se estira),
 *  - que el lado más largo no pase de 2048 px (48 MB de memoria si se trae
 *    entera una foto de 12 MP),
 *  - y que la foto se lea en trozos si es muy grande (4 millones de píxeles es
 *    el presupuesto).
 */
class FondosAppTest {

    @Test
    fun `una imagen que ya entra se deja como esta`() {
        assertEquals(
            "300 x 200 no se toca",
            300 to 200,
            FondosApp.escalaParaFondo(300, 200)
        )
    }

    @Test
    fun `una imagen de la resolucion recomendada se reescala para no pasar del tope`() {
        // 1080 x 2340: el lado alto (2340) pasa de 2048, así que se achica
        // en la misma proporción (2048 / 2340 = 0.875).
        assertEquals(
            "1080 x 2340 se deja en 945 x 2048",
            945 to 2048,
            FondosApp.escalaParaFondo(1080, 2340)
        )
    }

    @Test
    fun `una foto de doce megapixeles se reescala proporcionalmente`() {
        assertEquals(
            "4000 x 3000 se deja en 2048 x 1536",
            2048 to 1536,
            FondosApp.escalaParaFondo(4000, 3000)
        )
    }

    @Test
    fun `la imagen nunca se agranda`() {
        val (largo, alto) = FondosApp.escalaParaFondo(120, 90)
        assertTrue("El largo no crece", largo <= 120)
        assertTrue("El alto no crece", alto <= 90)
    }

    @Test
    fun `los dos lados se mantienen en la misma proporcion`() {
        val original = 4000 to 3000
        val (largo, alto) = FondosApp.escalaParaFondo(original.first, original.second)
        val proporcionOriginal = original.first.toDouble() / original.second
        val proporcionNueva = largo.toDouble() / alto
        assertEquals(
            "La foto no se deforma",
            proporcionOriginal,
            proporcionNueva,
            0.02
        )
    }

    @Test
    fun `un tamaño imposible no rompe nada`() {
        assertEquals(
            "Cero a cero",
            0 to 0,
            FondosApp.escalaParaFondo(0, 0)
        )
        assertEquals(
            "Negativo a negativo",
            0 to 0,
            FondosApp.escalaParaFondo(-10, -10)
        )
    }

    @Test
    fun `una foto normal se lee entera`() {
        assertEquals(
            "1080 x 2340 entra de sobra: se lee entera",
            1,
            FondosApp.factorDeMuestreo(1080, 2340)
        )
        assertEquals(
            "Una foto chica se lee entera",
            1,
            FondosApp.factorDeMuestreo(100, 100)
        )
    }

    @Test
    fun `una foto de doce megapixeles se lee en dos`() {
        assertEquals(
            "4000 x 3000 se lee en 2000 x 1500",
            2,
            FondosApp.factorDeMuestreo(4000, 3000)
        )
    }

    @Test
    fun `una foto enorme se lee en trozos mas pequenos`() {
        // 8000 x 6000 = 48 M de píxeles: en 4 quedan 3 M, que entra.
        assertEquals(
            "8000 x 6000 se lee en 2000 x 1500",
            4,
            FondosApp.factorDeMuestreo(8000, 6000)
        )
    }

    @Test
    fun `el factor de muestreo siempre deja la imagen dentro del presupuesto`() {
        val presupuesto = 4_000_000L
        for ((largo, alto) in listOf(100 to 100, 1080 to 2340, 4000 to 3000, 8000 to 6000, 12000 to 9000)) {
            val factor = FondosApp.factorDeMuestreo(largo, alto)
            val pixeles = largo.toLong() * alto.toLong() / (factor.toLong() * factor.toLong())
            assertTrue(
                "La imagen $largo x $alto (en $factor) tiene que entrar en el presupuesto",
                pixeles <= presupuesto
            )
        }
    }

    @Test
    fun `la resolucion que se le sugiere al usuario es la del telefono`() {
        assertEquals(
            "1080 x 2340",
            1080 to 2340,
            FondosApp.RESOLUCION_RECOMENDADA
        )
    }
}
