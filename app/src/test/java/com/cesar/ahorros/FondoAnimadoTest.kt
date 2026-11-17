package com.cesar.ahorros

import com.cesar.ahorros.ui.avanzarFase
import com.cesar.ahorros.ui.bandasBarrido
import com.cesar.ahorros.ui.focosMalla
import com.cesar.ahorros.ui.giroConico
import com.cesar.ahorros.ui.haloAureola
import com.cesar.ahorros.ui.manchasAurora
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Tests del **fondo animado**.
 *
 * Hay dos cosas que el usuario notó y que se comprueban acá:
 *
 *  1. **El ciclo no puede cortar.** Cuando la fase vuelve a 0, el fondo
 *     tiene que quedar EXACTAMENTE como estaba: si no, pega un salto
 *     ("hace un corte y vuelve al inicio"). Eso se comprueba con cada
 *     forma del degradado.
 *  2. **La velocidad manda de verdad.** La fase tiene que avanzar más
 *     rápido con más velocidad, y siempre quedar entre 0 y 1.
 *
 * Son tests JVM puros: no dibujan nada, solo comprueban las cuentas del
 * movimiento.
 */
class FondoAnimadoTest {

    /** Una pantalla de teléfono normal (1080 x 2340). */
    private val ancho = 1080f
    private val alto = 2340f

    /** Lo que puede moverse de golpe sin que se note (en píxeles). */
    private val TOLERANCIA = 40f

    // ---------------------------------------------------------------------
    // La fase y la velocidad
    // ---------------------------------------------------------------------

    @Test
    fun `la fase da una vuelta completa en doce segundos a velocidad normal`() {
        // Doce segundos es un ciclo entero: la fase vuelve al punto de
        // partida (no a 1, que ya sería el vuelta siguiente).
        assertEquals(0f, avanzarFase(0f, 12f, 1f), 0.0001f)
        assertEquals(0f, avanzarFase(0.5f, 6f, 1f), 0.0001f)
    }

    @Test
    fun `la fase avanza mas rapido con mas velocidad`() {
        val unSegundo = 1f
        val normal = avanzarFase(0f, unSegundo, 1f)
        val rapida = avanzarFase(0f, unSegundo, 2f)
        val lentisima = avanzarFase(0f, unSegundo, 0.25f)

        assertEquals(normal * 2f, rapida, 0.0001f)
        assertEquals(normal / 4f, lentisima, 0.0001f)
    }

    @Test
    fun `la fase siempre esta entre cero y uno`() {
        // Aunque el reloj dé un salto enorme (la app estuvo en segundo
        // plano y volvió), la fase no se sale del rango.
        listOf(0.25f, 1f, 2f, 3f, 10f).forEach { velocidad ->
            listOf(0f, 0.37f, 0.99f).forEach { inicio ->
                val resultado = avanzarFase(inicio, 999f, velocidad)
                assertTrue(
                    "la fase se salió del rango: $resultado",
                    resultado >= 0f && resultado < 1f
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // El ciclo cierra: ninguna forma da un salto
    // ---------------------------------------------------------------------

    @Test
    fun `la aurora vuelve al mismo punto al terminar el ciclo`() {
        val inicio = manchasAurora(ancho, alto, 0f)
        val fin = manchasAurora(ancho, alto, 1f)

        assertEquals(4, inicio.size)
        inicio.forEachIndexed { indice, punto ->
            assertEquals("x de la mancha $indice", punto.x, fin[indice].x, 0.001f)
            assertEquals("y de la mancha $indice", punto.y, fin[indice].y, 0.001f)
        }
    }

    @Test
    fun `la aurora no pega un salto al final del ciclo`() {
        // Un paso de fase antes del reinicio y otro después: si el ciclo
        // cerrara con un corte, las manchas darían un salto enorme.
        val antes = manchasAurora(ancho, alto, 0.999f)
        val despues = manchasAurora(ancho, alto, 0.001f)

        antes.forEachIndexed { indice, punto ->
            val salto = abs(punto.x - despues[indice].x) + abs(punto.y - despues[indice].y)
            assertTrue(
                "la mancha $indice se movió $salto px de golpe",
                salto < TOLERANCIA
            )
        }
    }

    @Test
    fun `la aureola vuelve al mismo radio y al mismo brillo`() {
        val inicio = haloAureola(ancho, 0f)
        val fin = haloAureola(ancho, 1f)

        assertEquals(inicio.radio, fin.radio, 0.001f)
        assertEquals(inicio.brillo, fin.brillo, 0.001f)
    }

    @Test
    fun `la malla vuelve a los mismos focos`() {
        val inicio = focosMalla(ancho, alto, 0f)
        val fin = focosMalla(ancho, alto, 1f)

        assertEquals(4, inicio.size)
        inicio.forEachIndexed { indice, foco ->
            assertEquals("x del foco $indice", foco.x, fin[indice].x, 0.001f)
            assertEquals("y del foco $indice", foco.y, fin[indice].y, 0.001f)
        }
    }

    @Test
    fun `el barrido sale de la pantalla por los dos lados`() {
        // La banda del barrido no vuelve al mismo sitio (no hace falta):
        // entra por la izquierda y sale por la derecha, así que el salto
        // del ciclo pasa FUERA de la vista.
        val anchoBanda = ancho * 0.6f
        val (xInicio, x2Inicio) = bandasBarrido(ancho, anchoBanda, 0f)
        val (xFin, x2Fin) = bandasBarrido(ancho, anchoBanda, 1f)

        assertTrue("la banda se ve al empezar", xInicio + anchoBanda <= 0f)
        assertTrue("la banda se ve al terminar", xFin >= ancho)

        // La segunda banda (la fina) sí vuelve al mismo sitio.
        assertEquals(x2Inicio, x2Fin, 0.001f)
    }

    @Test
    fun `el conico gira una vuelta entera`() {
        // 360° es lo mismo que 0°: el abanico cierra el ciclo sin corte.
        assertEquals(0f, giroConico(0f), 0.001f)
        assertEquals(360f, giroConico(1f), 0.001f)
    }
}
