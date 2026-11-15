package com.cesar.ahorros

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.ui.CatalogoTemas
import com.cesar.ahorros.ui.PantallaAjustes
import com.cesar.ahorros.ui.PantallaDeudaDetalle
import com.cesar.ahorros.ui.TemaAhorros
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

/**
 * Capturas de las pantallas nuevas: la **cuota de cobro** de las deudas y el
 * selector de **ícono** de la app.
 *
 * Mismo método que `CapturaPantallasTest` (Robolectric con gráficos nativos
 * dibuja los píxeles de verdad en el PC, sin emulador ni teléfono), pero
 * **montando la pantalla directo** con los datos en memoria en vez de sembrar
 * los archivos de la app: así las capturas no dependen del orden en que corren
 * los otros tests y siempre sale lo mismo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h1900dp-xhdpi")
class CapturaNuevasPantallasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val hoy: LocalDate get() = LocalDate.now()

    // =====================================================================
    // Utilidades de captura
    // =====================================================================

    private val carpetaDeSalida: File by lazy {
        val ruta = System.getProperty("ahorros.capturas.dir")
            ?: "C:\\Users\\Usuario\\Documents\\deepseek-harness\\default-workspace\\03-previews\\ahorros"
        File(ruta).also { it.mkdirs() }
    }

    private fun guardar(nombre: String, bitmap: Bitmap): File {
        val destino = File(carpetaDeSalida, nombre)
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }
        println("CAPTURA $nombre -> ${destino.absolutePath}")
        return destino
    }

    private fun dibujar(vista: View): Bitmap {
        if (vista.width <= 0 || vista.height <= 0) {
            val metricas = RuntimeEnvironment.getApplication().resources.displayMetrics
            vista.measure(
                View.MeasureSpec.makeMeasureSpec(metricas.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metricas.heightPixels, View.MeasureSpec.EXACTLY)
            )
            vista.layout(0, 0, metricas.widthPixels, metricas.heightPixels)
        }
        val bitmap = Bitmap.createBitmap(
            max(vista.width, 1), max(vista.height, 1), Bitmap.Config.ARGB_8888
        )
        vista.draw(Canvas(bitmap))
        return bitmap
    }

    private fun capturarPantalla(): Bitmap =
        dibujar(composeTestRule.activity.window.decorView)

    // =====================================================================
    // La deuda con CUOTA DE COBRO (el caso de Cesar)
    // =====================================================================

    private fun ts(diasAtras: Long, hora: LocalTime): Long =
        hoy.minusDays(diasAtras).atTime(hora).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

    /**
     * El caso real de Cesar: el banner costó 42,80 y son 23 personas, pero se
     * cobra **1,90** a cada una (no los 1,86 que salen de dividir).
     *
     * - 19 personas pagaron 1,90: quedan al día (antes salía "A favor $0,04").
     * - 4 personas pagaron 1,20: deben 1,90 − 1,20 = **0,70** cada una.
     * - Lo que falta por cobrar: 23 × 1,90 − 40,90 = **2,80**, que es
     *   exactamente 4 × 0,70. Antes el encabezado decía 1,90 y la lista 2,64.
     */
    private fun deudaConCuota(): Deuda {
        val personas = (1..23).map { PersonaDeuda("p$it", "Persona $it") }
        val pagos = mutableListOf<Pago>()
        for (i in 1..19) {
            pagos += Pago(
                id = "pg$i",
                personaId = "p$i",
                monto = 1.90,
                metodo = MetodoPago.EFECTIVO,
                nota = "",
                comprobante = null,
                ts = ts(i.toLong(), LocalTime.of(9, 30))
            )
        }
        for (i in 20..23) {
            pagos += Pago(
                id = "pg$i",
                personaId = "p$i",
                monto = 1.20,
                metodo = MetodoPago.YAPI,
                nota = "pago parcial",
                comprobante = null,
                ts = ts(2, LocalTime.of(16, 0))
            )
        }
        return Deuda(
            id = "d1",
            nombre = "banner 11G",
            modo = ModoDeuda.META,
            total = 42.80,
            montoPorPersona = 0.0,
            cuotaPersonalizada = 1.90,
            personasQuePagan = 23,
            personas = personas,
            pagos = pagos,
            saldada = false,
            ts = ts(5, LocalTime.of(8, 0))
        )
    }

    @Test
    fun `08 detalle de deuda con cuota de cobro`() {
        val deuda = deudaConCuota()
        composeTestRule.setContent {
            TemaAhorros {
                PantallaDeudaDetalle(
                    deuda = deuda,
                    onVolver = { },
                    onAgregarPersona = { },
                    onEditarPersona = { },
                    onEliminarPersona = { },
                    onRegistrarPago = { },
                    onEliminarPago = { },
                    onAlternarSaldada = { },
                    onEditar = { },
                    onEliminar = { },
                    onVerComprobante = { },
                    onAdjuntarPrueba = { }
                )
            }
        }
        composeTestRule.waitForIdle()

        // El encabezado tiene que decir $2,80 (43,70 − 40,90) y no $1,90.
        composeTestRule.onNodeWithText("$2,80").assertExists()
        guardar("08-detalle-cuota-cobro.png", capturarPantalla())

        // Y abajo, las 4 personas que pagaron 1,20: deben $0,70 cada una
        // (1,90 − 1,20), no los $0,66 que decía antes.
        val listas = composeTestRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
        if (listas.isNotEmpty()) {
            composeTestRule.onAllNodes(hasScrollAction())[0].performScrollToIndex(19)
            composeTestRule.waitForIdle()
            // Exactamente 4 personas deben $0,70: las que pagaron 1,20.
            assertEquals(
                4,
                composeTestRule.onAllNodesWithText("Debe: $0,70").fetchSemanticsNodes().size
            )
            guardar("08b-detalle-cuota-cobro-deben.png", capturarPantalla())
        }
    }

    // =====================================================================
    // El selector de icono
    // =====================================================================

    @Test
    fun `09 ajustes con el selector de icono`() {
        composeTestRule.setContent {
            TemaAhorros {
                PantallaAjustes(
                    versionApp = "1.11",
                    temas = CatalogoTemas.todos,
                    temaActualId = "clasico",
                    onCambiarTema = { },
                    iconoActualId = "v1",
                    onCambiarIcono = { },
                    onVolver = { },
                    onExportar = { },
                    onRestaurar = { },
                    onCerrar = { }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Órbita").assertExists()
        composeTestRule.onNodeWithText("Chispa").assertExists()
        composeTestRule.onNodeWithText("Corona").assertExists()
        composeTestRule.onNodeWithText("Creciente").assertExists()
        guardar("09-ajustes-iconos.png", capturarPantalla())
    }
}
