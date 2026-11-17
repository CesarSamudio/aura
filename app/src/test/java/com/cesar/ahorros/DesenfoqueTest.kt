package com.cesar.ahorros

import com.cesar.ahorros.ui.desenfoqueGaussiano
import com.cesar.ahorros.ui.pesosGaussiano
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del **desenfoque del fondo**.
 *
 * Cesar vio que el desenfoque se veía "a píxeles" (blocky) y pidió un
 * desenfoque de lente. La implementación es un kernel gaussiano de verdad
 * (separable: una pasada horizontal y otra vertical), con pesos enteros
 * normalizados a 65536.
 *
 * Los pesos son enteros a propósito: así un píxel blanco sigue siendo blanco
 * (un kernel con coma flotante rounding para abajo lo ensucia) y el cálculo
 * es idéntico en cualquier teléfono.
 *
 * El núcleo puro (sobre un `IntArray` de ARGB) se prueba en JVM pelado. La
 * versión con `Bitmap` necesita Robolectric (Android no está en un test JVM
 * normal).
 */
class DesenfoqueTest {

    // ---------------------------------------------------------------------
    // Los pesos del kernel
    // ---------------------------------------------------------------------

    @Test
    fun `el kernel gaussiano siempre suma lo mismo`() {
        // 65536 = 2^16: los pesos se pueden sumar con enteros y no hay
        // error de punto flotante.
        for (radio in 0..8) {
            val pesos = pesosGaussiano(radio)
            assertEquals(
                "El kernel de radio $radio tiene que sumar 65536",
                65536,
                pesos.sum()
            )
        }
    }

    @Test
    fun `el kernel de radio cero no toca nada`() {
        assertArrayEquals(
            "Sin radio no hay kernel: el color pasa de largo",
            intArrayOf(65536),
            pesosGaussiano(0)
        )
    }

    @Test
    fun `los pesos del kernel son los del gaussiano`() {
        assertArrayEquals(
            "Radio 1",
            intArrayOf(6980, 51576, 6980),
            pesosGaussiano(1)
        )
        assertArrayEquals(
            "Radio 4",
            intArrayOf(1811, 4344, 8115, 11808, 13380, 11808, 8115, 4344, 1811),
            pesosGaussiano(4)
        )
    }

    @Test
    fun `el kernel es simetrico`() {
        // Un kernel simétrico no desplaza la imagen: el punto de luz queda
        // en el mismo lugar.
        for (radio in 1..6) {
            val pesos = pesosGaussiano(radio)
            for (i in pesos.indices) {
                assertEquals(
                    "El kernel de radio $radio tiene que ser simétrico",
                    pesos[i],
                    pesos[pesos.size - 1 - i]
                )
            }
        }
    }

    @Test
    fun `el kernel crece con el radio`() {
        // A más radio, más pesos (y el centro pesa más).
        assertTrue(pesosGaussiano(2).size > pesosGaussiano(1).size)
        assertTrue(pesosGaussiano(4).size > pesosGaussiano(2).size)
    }

    // ---------------------------------------------------------------------
    // El núcleo puro (JVM, sin Android)
    // ---------------------------------------------------------------------

    @Test
    fun `sin radio los pixeles no cambian`() {
        val pixeles = intArrayOf(0xFF123456.toInt(), 0xFFABCDEF.toInt(), 0xFFFFFFFF.toInt())
        val salida = desenfoqueGaussiano(pixeles, 3, 1, 0)
        assertArrayEquals(
            "Con radio 0 no se toca ningún píxel",
            pixeles,
            salida
        )
    }

    @Test
    fun `un punto blanco se convierte en una mancha suave`() {
        // Una imagen de 9x9 negra con un punto blanco en el medio (4,4).
        val ancho = 9
        val alto = 9
        val pixeles = IntArray(ancho * alto) { 0xFF000000.toInt() }
        pixeles[4 * ancho + 4] = 0xFFFFFFFF.toInt()

        val salida = desenfoqueGaussiano(pixeles, ancho, alto, 4)

        // El centro sigue siendo el punto más claro, pero ya no es blanco:
        // el desenfoque repartió su luz alrededor.
        val centro = salida[4 * ancho + 4] and 0xFFFFFF
        assertEquals("El centro queda en gris claro", 0x0B0B0B, centro)

        // A dos píxeles el gris es más bajo: la mancha se apaga.
        assertTrue(
            "La mancha se apaga lejos del centro",
            (salida[3 * ancho + 3] and 0xFFFFFF) < centro
        )

        // Y en la esquina no queda nada de luz.
        assertEquals(
            "En la esquina no queda luz",
            0x000000,
            salida[0] and 0xFFFFFF
        )
    }

    @Test
    fun `una imagen toda blanca sigue blanca`() {
        // El error clásico de un kernel mal normalizado: el blanco se
        // ennegrece. Con los pesos enteros no pasa.
        val ancho = 8
        val alto = 8
        val pixeles = IntArray(ancho * alto) { 0xFFFFFFFF.toInt() }

        val salida = desenfoqueGaussiano(pixeles, ancho, alto, 4)

        for (i in salida.indices) {
            assertEquals(
                "El píxel $i tiene que seguir siendo blanco",
                0xFFFFFF,
                salida[i] and 0xFFFFFF
            )
        }
    }
}
