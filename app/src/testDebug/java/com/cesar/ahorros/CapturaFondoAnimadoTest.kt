package com.cesar.ahorros

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas as Lienzo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.data.FormaDegradado
import com.cesar.ahorros.ui.TemaActual
import com.cesar.ahorros.ui.dibujarDegradado
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max

/**
 * Genera la **tira de fotogramas del fondo animado** y comprueba que el
 * ciclo cierra sin salto.
 *
 * Lo que el usuario reportó: "la animación lo que hace es que se reinicia,
 * osea, cuando llega como al final, hace un corte y vuelve al inicio". Con
 * esta tira se ve (y se comprueba) que ya no pasa: la última columna es el
 * final del ciclo (fase 1) y tiene que ser la MISMA imagen que la primera
 * (fase 0).
 *
 * La tira sale de los 7 fotogramas de un ciclo completo, uno por cada
 * forma del degradado (una fila por forma). Se guarda como PNG para
 * poder mirarla.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h360dp-xhdpi")
class CapturaFondoAnimadoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Cuántos fotogramas tiene un ciclo (uno cada 1/6 de vuelta). */
    private val fotogramas = 7

    private var forma by mutableStateOf(FormaDegradado.AURORA)
    private var fase by mutableStateOf(0f)

    private val carpetaDeSalida: File by lazy {
        val ruta = System.getProperty("ahorros.capturas.dir")
            ?: "C:\\Users\\Usuario\\Documents\\deepseek-harness\\default-workspace\\03-previews\\ahorros"
        File(ruta).also { it.mkdirs() }
    }

    // =====================================================================
    // La tira
    // =====================================================================

    @Test
    fun `tira de fotogramas del degradado en todas sus formas`() {
        TemaActual.aplicar("clasico")
        val tema = TemaActual.tema
        val colores = listOf(
            tema.acentoCalido.copy(alpha = 0.55f),
            tema.acentoVioleta.copy(alpha = 0.55f),
            tema.acentoHielo.copy(alpha = 0.55f),
            tema.acentoMenta.copy(alpha = 0.55f)
        )

        composeTestRule.setContent {
            Lienzo(modifier = Modifier.fillMaxSize()) {
                drawRect(tema.fondo)
                dibujarDegradado(forma, fase, colores)
            }
        }

        val lado = 200
        val formas = FormaDegradado.entries.toList()
        val tira = Bitmap.createBitmap(lado * fotogramas, lado * formas.size, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(tira)

        formas.forEachIndexed { fila, formaActual ->
            forma = formaActual
            val imagenes = (0 until fotogramas).map { indice ->
                // La fase va de 0 a 1 en 7 pasos: el último fotograma es el
                // final del ciclo (fase 1), que es donde antes cortaba.
                fase = indice.toFloat() / (fotogramas - 1)
                composeTestRule.waitForIdle()
                capturar()
            }

            // La última imagen (final del ciclo) tiene que ser la misma que
            // la primera: si no, el fondo pega un salto al terminar.
            val diferencias = diferencia(imagenes.first(), imagenes.last())
            val porcentaje = 100f * diferencias.toFloat() / (imagenes.first().width * imagenes.first().height)
            println(
                "FONDO ${formaActual.id}: el final del ciclo difiere de " +
                    "el principio en $diferencias px ($porcentaje %)"
            )
            assertTrue(
                "la forma ${formaActual.id} da un salto al terminar el ciclo " +
                    "($porcentaje % de píxeles distintos)",
                porcentaje < 2f
            )

            imagenes.forEachIndexed { columna, imagen ->
                lienzo.drawBitmap(
                    Bitmap.createScaledBitmap(imagen, lado, lado, true),
                    (columna * lado).toFloat(),
                    (fila * lado).toFloat(),
                    null
                )
            }
        }

        guardar("fondo-ciclo-completo.png", tira)
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Rasteriza la ventana a un bitmap real. */
    private fun capturar(): Bitmap {
        val vista: View = composeTestRule.activity.window.decorView
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

    /** Cuántos píxeles se ven distintos entre dos imágenes. */
    private fun diferencia(primera: Bitmap, segunda: Bitmap): Int {
        val ancho = minOf(primera.width, segunda.width)
        val alto = minOf(primera.height, segunda.height)
        var distintos = 0
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                val a = primera.getPixel(x, y)
                val b = segunda.getPixel(x, y)
                val cambio = max(
                    max(abs(Color.red(a) - Color.red(b)), abs(Color.green(a) - Color.green(b))),
                    abs(Color.blue(a) - Color.blue(b))
                )
                if (cambio > 12) distintos++
            }
        }
        return distintos
    }

    private fun guardar(nombre: String, bitmap: Bitmap): File {
        val destino = File(carpetaDeSalida, nombre)
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }
        println("CAPTURA $nombre -> ${destino.absolutePath} (${destino.length() / 1024} KB)")
        return destino
    }
}
