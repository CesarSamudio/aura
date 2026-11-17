package com.cesar.ahorros

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.ui.desenfoqueGaussiano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests del desenfoque con **Bitmap** (la parte que necesita Android).
 *
 * El núcleo de la matemática está en `DesenfoqueTest` (JVM pelado); acá se
 * vigila que el Bitmap no se rompa: que con radio 0 no se cree una imagen
 * nueva, que una imagen de un solo píxel no reviente, y que el resultado sea
 * el mismo que el del núcleo.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class DesenfoqueBitmapTest {

    @Test
    fun `sin radio se devuelve la misma imagen`() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        assertSame(
            "Con radio 0 no se crea una imagen nueva",
            bitmap,
            desenfoqueGaussiano(bitmap, 0)
        )
    }

    @Test
    fun `una imagen de un solo px no se rompe`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, 0xFF123456.toInt())

        val salida = desenfoqueGaussiano(bitmap, 4)
        assertEquals("El color no se toca", 0xFF123456.toInt(), salida.getPixel(0, 0))
    }

    @Test
    fun `un punto de luz se vuelve una mancha suave`() {
        val bitmap = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888)
        for (x in 0 until 9) {
            for (y in 0 until 9) {
                bitmap.setPixel(x, y, 0xFF000000.toInt())
            }
        }
        bitmap.setPixel(4, 4, 0xFFFFFFFF.toInt())

        val salida = desenfoqueGaussiano(bitmap, 4)

        assertEquals(
            "El centro queda en gris claro (no blanco: la luz se repartió)",
            0x0B0B0B,
            salida.getPixel(4, 4) and 0xFFFFFF
        )
        assertEquals(
            "En la esquina no queda luz",
            0x000000,
            salida.getPixel(0, 0) and 0xFFFFFF
        )
    }

    @Test
    fun `una imagen blanca no se ensucia`() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        for (x in 0 until 8) {
            for (y in 0 until 8) {
                bitmap.setPixel(x, y, 0xFFFFFFFF.toInt())
            }
        }

        val salida = desenfoqueGaussiano(bitmap, 4)

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                assertEquals(
                    "El píxel ($x, $y) tiene que seguir siendo blanco",
                    0xFFFFFF,
                    salida.getPixel(x, y) and 0xFFFFFF
                )
            }
        }
    }
}
