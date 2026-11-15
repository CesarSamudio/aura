package com.cesar.ahorros

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.ui.App
import com.cesar.ahorros.ui.TemaAhorros
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * El botón "atrás" del dispositivo (lo de la flecha del celular).
 *
 * Lo que pidió Cesar (15/11):
 * - En las pantallas de adentro (Ajustes, el detalle de una deuda, agregar
 *   personas) el botón atrás **vuelve** a la pantalla de al lado: no cierra la
 *   app.
 * - En las 4 pantallas principales (Ahorros, Gastos, Deudas, Balance) **sí
 *   sale** de la app, pero hay que apretarlo **dos veces** para no cerrarla
 *   sin querer.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h800dp-xhdpi")
class NavegacionAtrasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun montarApp() {
        composeTestRule.setContent {
            TemaAhorros {
                App()
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Aprieta el botón atrás del dispositivo. */
    private fun apretarAtras() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `atras en los ajustes vuelve a la pantalla de al lado y no cierra la app`() {
        montarApp()
        composeTestRule.onNodeWithText("Balance").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Ajustes").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("COPIA DE SEGURIDAD").assertExists()

        apretarAtras()

        // Se cerrado los ajustes y se volvió al Balance.
        composeTestRule.onNodeWithText("BALANCE").assertExists()
        composeTestRule.onNodeWithText("COPIA DE SEGURIDAD").assertDoesNotExist()
        assert(!composeTestRule.activity.isFinishing) {
            "La app se cerró al apretar atrás en los ajustes"
        }
    }

    @Test
    fun `atras en una pantalla principal avisa y no cierra la app`() {
        montarApp()
        composeTestRule.onNodeWithText("Ahorros").assertExists()

        apretarAtras()

        // El aviso de "presioná de nuevo" y la app sigue abierta.
        composeTestRule.onNodeWithText("Presioná de nuevo para salir.").assertExists()
        assert(!composeTestRule.activity.isFinishing) {
            "La app se cerró con un solo apretón de atrás"
        }
    }

    @Test
    fun `atras dos veces en una pantalla principal si cierra la app`() {
        montarApp()

        apretarAtras()
        apretarAtras()

        assert(composeTestRule.activity.isFinishing) {
            "La app no se cerró con dos apretones de atrás"
        }
    }
}
