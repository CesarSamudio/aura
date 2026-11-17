package com.cesar.ahorros

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.ui.CatalogoTemas
import com.cesar.ahorros.ui.PantallaAjustes
import com.cesar.ahorros.ui.TemaActual
import com.cesar.ahorros.ui.TemaAhorros
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Genera una captura PNG REAL de la pantalla de Ajustes > Apariencia por cada
 * uno de los 7 temas de la app.
 *
 * Es el mismo patrón que `CapturaPantallasTest`: Robolectric levanta la app en
 * una JVM normal y, con `@GraphicsMode(NATIVE)`, dibuja los píxeles de verdad
 * (no un canvas vacío). Se monta `TemaAhorros { PantallaAjustes(...) }` con el
 * catálogo real y después se rasteriza la ventana a un bitmap.
 *
 * Nota sobre `captureToImage()`: en Compose 1.7 ese helper usa `PixelCopy` y
 * espera un "frame commit" real, algo que en Robolectric nunca ocurre (se
 * queda esperando 2 s y revienta con `ComposeTimeoutException`). Por eso las
 * capturas se hacen dibujando la ventana a mano (`View.draw(Canvas)`), que en
 * modo NATIVE sí produce los píxeles reales.
 *
 * Para cambiar el tema se usa `TemaActual.aplicar(id)` ANTES de montar: el
 * tema activo es estado global de Compose y la pantalla se pinta con él.
 * Al terminar se deja el tema por defecto ("clasico") para no romper nada.
 *
 * Las capturas van a su propia carpeta: `<carpeta de capturas>/temas/`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h800dp-xhdpi")
class CapturaTemasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun dejarElTemaPorDefecto() {
        // El tema activo es estado global: se vuelve al de por defecto.
        TemaActual.aplicar(CatalogoTemas.porDefecto.id)
    }

    // =====================================================================
    // 1) Carpeta de salida: las de temas van en su propia subcarpeta
    // =====================================================================

    /** Carpeta donde se guardan las capturas de los temas. */
    private val carpetaDeSalida: File by lazy {
        val base = System.getProperty("ahorros.capturas.dir")
            ?: "C:\\Users\\Usuario\\Documents\\deepseek-harness\\default-workspace\\03-previews\\ahorros"
        File(base, "temas").also { it.mkdirs() }
    }

    // =====================================================================
    // 2) Utilidades de captura
    // =====================================================================

    private fun guardar(nombre: String, bitmap: Bitmap): File {
        val destino = File(carpetaDeSalida, nombre)
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }
        println(
            "CAPTURA $nombre -> ${destino.absolutePath} " +
                "(${destino.length() / 1024} KB, ${bitmap.width}x${bitmap.height}, " +
                "colores=${coloresDistintos(bitmap)})"
        )
        return destino
    }

    /** Cuenta cuántos colores distintos hay: sirve para detectar renders vacíos. */
    private fun coloresDistintos(bitmap: Bitmap): Int {
        val vistos = HashSet<Int>()
        var y = 0
        while (y < bitmap.height && vistos.size < 40) {
            var x = 0
            while (x < bitmap.width && vistos.size < 40) {
                vistos.add(bitmap.getPixel(x, y))
                x += 3
            }
            y += 3
        }
        return vistos.size
    }

    /** Rasteriza una vista (la ventana) a un bitmap real. */
    private fun dibujar(vista: View, etiqueta: String): Bitmap {
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
        println("CAPTURA dibujada '$etiqueta': ${bitmap.width}x${bitmap.height}")
        return bitmap
    }

    /**
     * Captura la pantalla completa.
     *
     * `captureToImage()` de Compose 1.7 usa `PixelCopy` + "frame commit" real
     * (`WindowCapture`): en Robolectric ese callback nunca llega y la llamada
     * revienta con `ComposeTimeoutException` tras 2 s. Por eso la captura se
     * hace rasterizando la ventana de la activity a mano, que en modo NATIVE
     * sí devuelve los píxeles reales.
     */
    private fun capturarPantalla(): Bitmap {
        return dibujar(composeTestRule.activity.window.decorView, "activity")
    }

    // =====================================================================
    // 3) Montar la pantalla de Ajustes con un tema
    // =====================================================================

    /**
     * Monta la pantalla de Ajustes con el tema pedido ya activo.
     *
     * El tema se aplica ANTES de montar: `TemaAhorros` lee el tema activo al
     * componerse, así que la pantalla entera sale pintada con ese tema.
     */
    private fun montarAjustes(temaId: String) {
        TemaActual.aplicar(temaId)
        composeTestRule.setContent {
            TemaAhorros {
                PantallaAjustes(
                    versionApp = "1.8",
                    temas = CatalogoTemas.todos,
                    temaActualId = temaId,
                    onCambiarTema = { },
                    onElegirFondo = { },
                    onQuitarFondo = { },
                    onVolver = { },
                    onExportar = { },
                    onRestaurar = { },
                    onCerrar = { }
                )
            }
        }
        composeTestRule.waitForIdle()

        // La sección de temas viene cerrada: hay que abrirla para que se vean
        // los temas (y para que la captura sea del tema que se pidió).
        try {
            composeTestRule.onNodeWithText("Temas").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            println("CAPTURA no se pudo abrir la sección de temas: ${e.message}")
        }
    }

    /**
     * Baja hasta la sección de temas.
     *
     * El título de la sección puede no llamarse exactamente "APARIENCIA": si
     * no aparece, se busca el nombre de un tema (que solo sale en el selector)
     * y, si tampoco aparece, la captura se hace igual con lo que haya en
     * pantalla. El test NO falla por esto: lo que importa es la captura.
     */
    private fun desplazarHastaLosTemas() {
        val candidatos = listOf(
            "APARIENCIA",
            "Apariencia",
            "Osmo",
            "Caldera",
            "SafePal",
            "Boost",
            "Mode",
            "Trunk"
        )
        for (texto in candidatos) {
            val nodos = composeTestRule.onAllNodesWithText(texto)
            if (nodos.fetchSemanticsNodes().isEmpty()) continue
            try {
                nodos[0].performScrollTo()
                composeTestRule.waitForIdle()
                return
            } catch (e: Exception) {
                println("CAPTURA no se pudo desplazar hasta '$texto': ${e.message}")
            }
        }
        println("CAPTURA no se encontró la sección de temas: se captura el inicio de la pantalla")
    }

    /**
     * Captura la pantalla de Ajustes con un tema.
     *
     * El nombre del archivo sale del id del tema: `tema-clasico.png`,
     * `tema-osmo.png`, `tema-caldera.png`, `tema-safepal.png`,
     * `tema-boost.png`, `tema-mode.png`, `tema-trunk.png`.
     */
    private fun capturarTema(temaId: String) {
        montarAjustes(temaId)

        // La fila del tema que se está capturing tiene que salir marcada como
        // activa: si no, la captura no es del tema que se pidió.
        composeTestRule.onNodeWithContentDescription("Tema activo").assertExists()

        desplazarHastaLosTemas()
        val archivo = guardar("tema-$temaId.png", capturarPantalla())
        assertTrue(
            "La captura del tema «$temaId» salió vacía (${archivo.length()} bytes)",
            archivo.length() > 3000
        )
    }

    // =====================================================================
    // 4) Una captura por tema
    // =====================================================================

    @Test
    fun `tema clasico`() {
        capturarTema("clasico")
    }

    @Test
    fun `tema osmo`() {
        capturarTema("osmo")
    }

    @Test
    fun `tema caldera`() {
        capturarTema("caldera")
    }

    @Test
    fun `tema safepal`() {
        capturarTema("safepal")
    }

    @Test
    fun `tema boost`() {
        capturarTema("boost")
    }

    @Test
    fun `tema mode`() {
        capturarTema("mode")
    }

    @Test
    fun `tema trunk`() {
        capturarTema("trunk")
    }
}
