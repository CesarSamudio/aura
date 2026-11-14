package com.cesar.ahorros

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Comprueba que la app **abre** en Android viejo.
 *
 * En el teléfono de Cesar (Nougat 64 bits) el APK se instalaba pero no abría:
 * el proceso moría al arrancar. Acá se abre la `MainActivity` real en un
 * Android simulado (API 24, 25 y 26) y, si algo revienta en el arranque,
 * el test falla con el error exacto.
 *
 * API 24 = Android 7.0 Nougat · API 25 = Android 7.1 Nougat.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [24, 25, 26, 30])
class AperturaNougatTest {

    @Test
    fun laAppAbreEnNougat() {
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            assertNotNull("La app no llegó a crear la pantalla", escenario.state)
        }
    }
}
