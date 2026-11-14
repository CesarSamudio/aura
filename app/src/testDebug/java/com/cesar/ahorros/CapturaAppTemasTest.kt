package com.cesar.ahorros

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.ui.App
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

/**
 * Genera una captura PNG REAL de la app COMPLETA (la pestaña de Ahorros con
 * datos de ejemplo) en cada uno de los 7 temas.
 *
 * Sirve para ver cómo queda la app de verdad con cada tema, y no solo la
 * pantalla de Ajustes.
 *
 * Es el mismo patrón que `CapturaPantallasTest` y `CapturaTemasTest`:
 * Robolectric con `@GraphicsMode(NATIVE)` dibuja los píxeles de verdad y la
 * ventana se rasteriza a mano (en Robolectric `captureToImage()` no funciona).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h1200dp-xhdpi")
class CapturaAppTemasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun dejarElTemaPorDefecto() {
        TemaActual.aplicar("clasico")
    }

    // =====================================================================
    // 1) Datos de ejemplo
    // =====================================================================

    private val hoy: LocalDate get() = LocalDate.now()

    private fun archivoDeDatos(): File =
        File(ApplicationProvider.getApplicationContext<Application>().filesDir, "movimientos.json")

    private fun escapar(texto: String): String =
        texto.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun movimientoJson(
        id: String,
        tipo: Tipo,
        monto: Double,
        categoria: String,
        nota: String,
        diasAtras: Long,
        hora: LocalTime
    ): String {
        val fecha = hoy.minusDays(diasAtras)
        val ts = fecha.atTime(hora).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return """{"id":"$id","tipo":"${tipo.name}","monto":$monto,"categoria":"${escapar(categoria)}",""" +
            """"nota":"${escapar(nota)}","ts":$ts}"""
    }

    private fun sembrarEjemplo() {
        val items = listOf(
            movimientoJson("a1", Tipo.AHORRO, 50.0, "Físico", "Ahorro de la semana", 0, LocalTime.of(9, 30)),
            movimientoJson("a2", Tipo.AHORRO, 120.5, "Digital", "Transferencia del sueldo", 1, LocalTime.of(18, 45)),
            movimientoJson("a3", Tipo.AHORRO, 30.0, "Físico", "Ladrillera del cole", 3, LocalTime.of(12, 0)),
            movimientoJson("g1", Tipo.GASTO, 8.5, "Comida", "Almuerzo del cole", 0, LocalTime.of(12, 30)),
            movimientoJson("g2", Tipo.GASTO, 25.0, "Transporte", "Bono del bus", 0, LocalTime.of(19, 10)),
            movimientoJson("g3", Tipo.GASTO, 45.9, "Entretenimiento", "Cine con amigos", 2, LocalTime.of(21, 0)),
            movimientoJson("g4", Tipo.GASTO, 15.0, "Comida", "Pizza", 4, LocalTime.of(13, 15)),
            movimientoJson("g5", Tipo.GASTO, 60.0, "Necesidad", "Zapatillas nuevas", 6, LocalTime.of(10, 0)),
            movimientoJson("g6", Tipo.GASTO, 12.0, "Otros", "Cosas para el cole", 9, LocalTime.of(16, 45)),
            movimientoJson("g7", Tipo.GASTO, 33.5, "Comida", "Cena familiar", 13, LocalTime.of(20, 30)),
            movimientoJson("g8", Tipo.GASTO, 70.0, "Transporte", "Bici", 18, LocalTime.of(8, 20))
        )
        archivoDeDatos().writeText("[" + items.joinToString(",") + "]", Charsets.UTF_8)
    }

    // ---------------------------------------------------------------------
    // Las deudas también se siembran.
    //
    // En Robolectric la carpeta de datos de la app es la MISMA para todas las
    // clases de test que corren en la misma JVM. Si esta clase montara la app
    // sin deudas, dejaría la lista vacía y rompería las capturas de deudas
    // de `CapturaPantallasTest`. Con los mismos datos de ejemplo, las dos
    // clases dejan siempre lo mismo.
    // ---------------------------------------------------------------------

    private fun archivoDeDeudas(): File =
        File(ApplicationProvider.getApplicationContext<Application>().filesDir, "deudas.json")

    private fun pagoJson(
        id: String,
        personaId: String,
        monto: Double,
        metodo: String,
        nota: String,
        diasAtras: Long,
        hora: LocalTime
    ): String {
        val fecha = hoy.minusDays(diasAtras)
        val ts = fecha.atTime(hora).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return """{"id":"$id","personaId":"$personaId","monto":$monto,"metodo":"$metodo",""" +
            """"nota":"${escapar(nota)}","comprobante":null,"ts":$ts}"""
    }

    private fun personaJson(id: String, nombre: String, cuotaPropia: Double? = null): String {
        val cuota = if (cuotaPropia == null) "null" else cuotaPropia.toString()
        return """{"id":"$id","nombre":"${escapar(nombre)}","cuotaPropia":$cuota}"""
    }

    private fun deudaJson(
        id: String,
        nombre: String,
        modo: String,
        total: Double,
        montoPorPersona: Double,
        personasQuePagan: Int,
        personas: List<String>,
        pagos: List<String>,
        saldada: Boolean,
        diasAtras: Long
    ): String {
        val fecha = hoy.minusDays(diasAtras)
        val ts = fecha.atTime(9, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val listaPersonas = if (personas.isEmpty()) "[]" else "[" + personas.joinToString(",") + "]"
        val listaPagos = if (pagos.isEmpty()) "[]" else "[" + pagos.joinToString(",") + "]"
        return """{"id":"$id","nombre":"${escapar(nombre)}","modo":"$modo","total":$total,""" +
            """"montoPorPersona":$montoPorPersona,"personasQuePagan":$personasQuePagan,""" +
            """"personas":$listaPersonas,"pagos":$listaPagos,"saldada":$saldada,"ts":$ts}"""
    }

    private fun sembrarDeudas() {
        val items = listOf(
            deudaJson(
                "d1", "banner", "META", 60.0, 0.0, personasQuePagan = 4,
                personas = listOf(
                    personaJson("p1", "Ana", 12.0),
                    personaJson("p2", "Luis"),
                    personaJson("p3", "Kenneth Yassir Alvarado Hurtado")
                ),
                pagos = listOf(
                    pagoJson("pg1", "p1", 12.0, "EFECTIVO", "", 0, LocalTime.of(11, 30)),
                    pagoJson("pg2", "p2", 16.0, "YAPI", "Transferencia por YAPI", 1, LocalTime.of(17, 0))
                ),
                saldada = false,
                diasAtras = 3
            ),
            deudaJson(
                "d2", "pintura del mural", "LIBRE", 0.0, 40.0, personasQuePagan = 2,
                personas = listOf(personaJson("p4", "Sofía")),
                pagos = listOf(pagoJson("pg3", "p4", 40.0, "EFECTIVO", "", 6, LocalTime.of(10, 15))),
                saldada = true,
                diasAtras = 9
            )
        )
        archivoDeDeudas().writeText("[" + items.joinToString(",") + "]", Charsets.UTF_8)
    }

    // =====================================================================
    // 2) Utilidades de captura
    // =====================================================================

    private val carpetaDeSalida: File by lazy {
        val base = System.getProperty("ahorros.capturas.dir")
            ?: "C:\\Users\\Usuario\\Documents\\deepseek-harness\\default-workspace\\03-previews\\ahorros"
        File(base, "temas").also { it.mkdirs() }
    }

    private fun guardar(nombre: String, bitmap: Bitmap): File {
        val destino = File(carpetaDeSalida, nombre)
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)
        }
        println("CAPTURA $nombre -> ${destino.absolutePath} (${destino.length() / 1024} KB)")
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
    // 3) La captura de cada tema
    // =====================================================================

    private fun capturarTema(temaId: String) {
        sembrarEjemplo()
        sembrarDeudas()
        TemaActual.aplicar(temaId)
        composeTestRule.setContent {
            TemaAhorros {
                App()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ahorros").assertExists()

        val archivo = guardar("app-$temaId.png", capturarPantalla())
        assertTrue(
            "La captura de la app con el tema «$temaId» salió vacía (${archivo.length()} bytes)",
            archivo.length() > 3000
        )
    }

    @Test
    fun `app con el tema clasico`() = capturarTema("clasico")

    @Test
    fun `app con el tema osmo`() = capturarTema("osmo")

    @Test
    fun `app con el tema caldera`() = capturarTema("caldera")

    @Test
    fun `app con el tema safepal`() = capturarTema("safepal")

    @Test
    fun `app con el tema boost`() = capturarTema("boost")

    @Test
    fun `app con el tema mode`() = capturarTema("mode")

    @Test
    fun `app con el tema trunk`() = capturarTema("trunk")
}
