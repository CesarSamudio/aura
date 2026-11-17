package com.cesar.ahorros

import androidx.compose.ui.graphics.Color
import com.cesar.ahorros.ui.aHsv
import com.cesar.ahorros.ui.hsvAColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del motor de color del selector (`ui/SelectorColor.kt`).
 *
 * Son tests JVM puros: no dibujan nada, no usan Compose ni Android. Solo
 * comprueban las dos conversiones de HSV, que son las que mueven el dial.
 *
 * El matiz va normalizado (0..1) y no en grados (0..360): el 0 es el rojo,
 * el 1/6 es el amarillo, el 0.5 es el cian y así hasta el vuelta completa.
 *
 * La tolerancia no es cero a propósito: `Color` guarda los canales en 8
 * bits, así que un color que se calcula y se vuelve a calcular puede
 *_VARIAR en un canal por el redondeo (menos de 1/255).
 */
class SelectorColorTest {

    // ---------------------------------------------------------------------
    // Valores conocidos
    // ---------------------------------------------------------------------

    @Test
    fun `el rojo es matiz cero saturacion uno y valor uno`() {
        val (matiz, saturacion, valor) = Color(0xFFFF0000).aHsv()
        assertEquals("El rojo arranca en el matiz 0", 0f, matiz, 0.001f)
        assertEquals("El rojo es totalmente saturado", 1f, saturacion, 0.001f)
        assertEquals("El rojo vale 1", 1f, valor, 0.001f)
    }

    @Test
    fun `el blanco no tiene saturacion`() {
        val (matiz, saturacion, valor) = Color(0xFFFFFFFF).aHsv()
        assertEquals("El blanco no tiene saturación", 0f, saturacion, 0.001f)
        assertEquals("El blanco vale 1", 1f, valor, 0.001f)
        assertEquals("El blanco no tiene matiz", 0f, matiz, 0.001f)
    }

    @Test
    fun `el negro tiene valor cero`() {
        val (matiz, saturacion, valor) = Color(0xFF000000).aHsv()
        assertEquals("El negro vale 0", 0f, valor, 0.001f)
        assertEquals("El negro no tiene saturación", 0f, saturacion, 0.001f)
        assertEquals("El negro no tiene matiz", 0f, matiz, 0.001f)
    }

    @Test
    fun `el amarillo esta en el segundo sector del arcoiris`() {
        val (matiz, saturacion, valor) = Color(0xFFFFFF00).aHsv()
        assertEquals("El amarillo está a 60 grados", 1f / 6f, matiz, 0.001f)
        assertEquals("El amarillo es totalmente saturado", 1f, saturacion, 0.001f)
        assertEquals("El amarillo vale 1", 1f, valor, 0.001f)
    }

    @Test
    fun `el cian esta en la mitad del arcoiris`() {
        val (matiz, _, _) = Color(0xFF00FFFF).aHsv()
        assertEquals("El cian está a 180 grados", 0.5f, matiz, 0.001f)
    }

    @Test
    fun `el azul esta en el cuarto sector del arcoiris`() {
        val (matiz, _, _) = Color(0xFF0000FF).aHsv()
        assertEquals("El azul está a 240 grados", 4f / 6f, matiz, 0.001f)
    }

    @Test
    fun `el magenta esta en el ultimo sector del arcoiris`() {
        val (matiz, _, _) = Color(0xFFFF00FF).aHsv()
        assertEquals("El magenta está a 300 grados", 5f / 6f, matiz, 0.001f)
    }

    @Test
    fun `un gris no tiene matiz ni saturacion`() {
        val (matiz, saturacion, valor) = Color(0xFF808080).aHsv()
        assertEquals("El gris no tiene matiz", 0f, matiz, 0.001f)
        assertEquals("El gris no tiene saturación", 0f, saturacion, 0.001f)
        assertEquals("El gris vale lo que vale su canal", 0.502f, valor, 0.01f)
    }

    // ---------------------------------------------------------------------
    // Ida y vuelta
    // ---------------------------------------------------------------------

    @Test
    fun `los colores vuelven iguales cuando se van a hsv y se vuelven`() {
        for (color in COLORES_DE_PRUEBA) {
            val (m, s, v) = color.aHsv()
            val vuelta = hsvAColor(m, s, v)
            assertMismoColor(
                "El color ${colorAHex(color)} tiene que volver igual",
                color,
                vuelta
            )
        }
    }

    @Test
    fun `el matiz vuelve igual en los colores que tienen saturacion`() {
        for (color in COLORES_DE_PRUEBA) {
            val (m, s, v) = color.aHsv()
            // En los grises el matiz no existe: puede dar cualquier valor y
            // el color sigue siendo el mismo.
            if (s < 0.2f) continue
            val vuelta = hsvAColor(m, s, v).aHsv().first
            assertEquals(
                "El matiz de ${colorAHex(color)} tiene que volver igual",
                m,
                vuelta,
                0.02f
            )
        }
    }

    @Test
    fun `un color sin saturacion vuelve siendo el mismo gris`() {
        val gris = Color(0xFF404040)
        val (m, s, v) = gris.aHsv()
        assertEquals("El gris no tiene matiz", 0f, m, 0.001f)
        assertEquals("El gris no tiene saturación", 0f, s, 0.001f)
        assertMismoColor("El gris tiene que volver igual", gris, hsvAColor(m, s, v))
    }

    // ---------------------------------------------------------------------
    // Los valores fuera de rango se recortan
    // ---------------------------------------------------------------------

    @Test
    fun `el matiz de mas de uno se recorta a la mitad`() {
        val recortado = hsvAColor(1.5f, 1f, 1f)
        assertMismoColor(
            "Un matiz de 1.5 tiene que ser el 0.5",
            hsvAColor(0.5f, 1f, 1f),
            recortado
        )
        // Y el 0.5 del arcoíris es el cian.
        assertEquals("El cian no tiene rojo", 0f, recortado.red, 0.001f)
        assertEquals("El cian tiene verde", 1f, recortado.green, 0.001f)
        assertEquals("El cian tiene azul", 1f, recortado.blue, 0.001f)
    }

    @Test
    fun `la saturacion negativa se recorta a cero`() {
        val gris = hsvAColor(0.5f, -1f, 0.8f)
        assertEquals("Sin saturación los tres canales son iguales", 0.8f, gris.red, 0.01f)
        assertEquals("Sin saturación los tres canales son iguales", 0.8f, gris.green, 0.01f)
        assertEquals("Sin saturación los tres canales son iguales", 0.8f, gris.blue, 0.01f)
    }

    @Test
    fun `el valor mayor que uno se recorta a uno`() {
        val color = hsvAColor(0f, 1f, 2f)
        assertEquals("Con valor 1 el rojo vale 1", 1f, color.red, 0.001f)
        assertEquals("Con valor 1 el verde vale 0", 0f, color.green, 0.001f)
        assertEquals("Con valor 1 el azul vale 0", 0f, color.blue, 0.001f)
    }

    @Test
    fun `el editor nunca devuelve un color fuera de rango`() {
        val matices = listOf(-1f, 0f, 0.25f, 0.5f, 0.99f, 1f, 2f)
        val saturaciones = listOf(-0.5f, 0f, 0.5f, 1f, 3f)
        val valores = listOf(-1f, 0f, 0.5f, 1f, 5f)
        for (m in matices) {
            for (s in saturaciones) {
                for (v in valores) {
                    val color = hsvAColor(m, s, v)
                    assertTrue("El rojo no puede salirse", color.red in 0f..1f)
                    assertTrue("El verde no puede salirse", color.green in 0f..1f)
                    assertTrue("El azul no puede salirse", color.blue in 0f..1f)
                    assertTrue("El alfa no puede salirse", color.alpha in 0f..1f)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    /** Falla el test si los dos colores no son el mismo (con tolerancia). */
    private fun assertMismoColor(mensaje: String, esperado: Color, obtenido: Color) {
        assertEquals("$mensaje (rojo)", esperado.red, obtenido.red, 0.02f)
        assertEquals("$mensaje (verde)", esperado.green, obtenido.green, 0.02f)
        assertEquals("$mensaje (azul)", esperado.blue, obtenido.blue, 0.02f)
    }

    /** El color en `#RRGGBB`, para que el fallo diga cuál es. */
    private fun colorAHex(color: Color): String {
        val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    private companion object {
        /**
         * Los colores con los que se prueba la ida y vuelta.
         *
         * Son los acentos de la app y un par de grises: los saturados
         * comprueban el matiz y los grises comprueban que el matiz no se
         * inventa.
         */
        val COLORES_DE_PRUEBA = listOf(
            Color(0xFFFF5A00), // naranja de marca
            Color(0xFF9E7BF0), // violeta
            Color(0xFFB8FEFF), // cian
            Color(0xFF91F58E), // menta
            Color(0xFFFFC24D), // ámbar
            Color(0xFFFD542D), // rojo
            Color(0xFF3898EC), // azul
            Color(0xFF1A1A1A), // casi negro
            Color(0xFF7F7F7F), // gris
            Color(0xFFFFFFFF)  // blanco
        )
    }
}
