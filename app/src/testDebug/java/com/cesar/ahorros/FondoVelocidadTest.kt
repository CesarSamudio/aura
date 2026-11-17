package com.cesar.ahorros

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.ui.rememberFaseDelDegradado
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Test del **reloj del fondo animado**.
 *
 * Esto es lo que el usuario(reportó): al mover el deslizador de velocidad,
 * el fondo —y su vista previa— seguían a la velocidad VIEJA. Solo se
 * notaba el cambio al salirse de Ajustes y volver a entrar, porque ahí el
 * composable se recreaba y cogía la velocidad nueva.
 *
 * El test comprueba las dos cosas:
 *
 *  1. La velocidad se aplica **al instante**, sin salir de la pantalla.
 *  2. La fase **no se reinicia** al cambiarla: el fondo sigue donde
 *     estaba (si se reiniciara, el avance sería una vuelta entera y el
 *     fondo pegaría un salto).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class FondoVelocidadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `la velocidad se aplica al instante y la fase no se reinicia`() {
        var velocidad by mutableStateOf(1f)
        var fase: State<Float>? = null

        // A partir de acá el reloj solo avanza cuando lo decimos nosotros.
        // Tiene que ser ANTES de `setContent`: con el reloj automático, el
        // fondo animado nunca deja de pedir fotogramas y la app nunca queda
        // quieta (Espresso se quejaría con `AppNotIdleException`).
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            fase = rememberFaseDelDegradado(activo = true, velocidad = velocidad)
        }

        // Un fotograma para que el reloj del fondo se arme.
        composeTestRule.mainClock.advanceTimeBy(16)
        val antes = fase!!.value

        // Un segundo de fotogramas de verdad (16 ms cada uno): una
        // doceava parte de la vuelta (el ciclo dura doce segundos).
        repetirFotogramas(63)
        val normal = fase!!.value
        assertEquals(1f / 12f, normal - antes, 0.02f)

        // El usuario sube la velocidad a 3x. Un fotograma para que el
        // cambio llegue al reloj del fondo.
        velocidad = 3f
        composeTestRule.mainClock.advanceTimeBy(16)
        val trasElCambio = fase!!.value

        // Un segundo a 3x: tiene que avanzar TRES veces más que antes, sin
        // haber salido de la pantalla.
        repetirFotogramas(63)
        val rapida = fase!!.value
        assertEquals(3f / 12f, rapida - trasElCambio, 0.02f)
    }

    /** Pasa `cantidad` fotogramas de 16 ms (uno cada frame). */
    private fun repetirFotogramas(cantidad: Int) {
        repeat(cantidad) {
            composeTestRule.mainClock.advanceTimeBy(16)
        }
    }
}
