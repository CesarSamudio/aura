package com.cesar.ahorros

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.ui.App
import com.cesar.ahorros.ui.DialogoMovimiento
import com.cesar.ahorros.ui.TemaAhorros
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

/**
 * Genera capturas PNG REALES de la app "Ahorros" sin emulador y sin teléfono.
 *
 * Cómo funciona: Robolectric levanta la app en una JVM normal y, con
 * `@GraphicsMode(NATIVE)`, dibuja los píxeles de verdad (no un canvas vacío).
 * El `createComposeRule()` monta la pantalla real (`TemaAhorros { App() }`) y
 * después se rasteriza la ventana a un bitmap.
 *
 * Nota sobre `captureToImage()`: en Compose 1.7 ese helper usa `PixelCopy` y
 * espera un "frame commit" real, algo que en Robolectric nunca ocurre (se
 * queda esperando 2 s y revienta con `ComposeTimeoutException`). Por eso las
 * capturas se hacen dibujando la ventana a mano (`View.draw(Canvas)`), que en
 * modo NATIVE sí produce los píxeles reales.
 *
 * Los datos de ejemplo se inyectan por la vía que usa la app de verdad: se
 * escribe `movimientos.json` en el almacenamiento interno antes de que el
 * `AppViewModel` lo lea, así que no se toca ni una línea de código de
 * producción.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w360dp-h800dp-xhdpi")
class CapturaPantallasTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =====================================================================
    // 1) Datos de ejemplo (se escriben en el archivo que lee la app)
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

    /** Escribe los movimientos de ejemplo que usa la app. */
    private fun sembrarEjemplo() {
        val items = listOf(
            // ---- Ahorros (uno de hoy, uno de ayer, uno de hace 3 días) ----
            movimientoJson("a1", Tipo.AHORRO, 50.0, "Físico", "Ahorro de la semana", 0, LocalTime.of(9, 30)),
            movimientoJson("a2", Tipo.AHORRO, 120.5, "Digital", "Transferencia del sueldo", 1, LocalTime.of(18, 45)),
            movimientoJson("a3",Tipo.AHORRO, 30.0, "Físico", "Ladrillera del cole", 3, LocalTime.of(12, 0)),

            // ---- Gastos (repartidos por el mes para que las gráficas tengan datos) ----
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
        println("CAPTURA datos sembrados hoy=$hoy archivo=${archivoDeDatos().absolutePath}")
    }

    // =====================================================================
    // 1b) Datos de ejemplo de las DEUDAS (archivo aparte: deudas.json)
    // =====================================================================

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
        return """{"id":"$id","nombre":"${escapar(nombre)}","modo":"$modo",""" +
            """"total":$total,"montoPorPersona":$montoPorPersona,""" +
            """"personasQuePagan":$personasQuePagan,"personas":$listaPersonas,""" +
            """"pagos":$listaPagos,"saldada":$saldada,"ts":$ts}"""
    }

    /** Escribe las deudas de ejemplo que usa la app. */
    private fun sembrarDeudas() {
        val items = listOf(
            // Modo META: meta de $60 para 4 personas. Ana tiene monto especial
            // de $12, así que los otros tres pagan (60 − 12) ÷ 3 = $16 cada uno.
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
            // Modo LIBRE: cada uno paga $40 y son 2, el total se calcula ($80).
            // Ya está saldada a mano.
            deudaJson(
                "d2", "pintura del mural", "LIBRE", 0.0, 40.0, personasQuePagan = 2,
                personas = listOf(personaJson("p4", "Sofía")),
                pagos = listOf(pagoJson("pg3", "p4", 40.0, "EFECTIVO", "", 6, LocalTime.of(10, 15))),
                saldada = true,
                diasAtras = 9
            )
        )
        archivoDeDeudas().writeText("[" + items.joinToString(",") + "]", Charsets.UTF_8)
        println("CAPTURA deudas sembradas archivo=${archivoDeDeudas().absolutePath}")
    }

    // =====================================================================
    // 2) Utilidades de captura
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

    /**
     * Captura la ventana del diálogo y la cierra.
     *
     * El cierre es obligatorio: si el test termina con la ventana del diálogo
     * abierta, Espresso revisa al terminar que la app esté en reposo y tira
     * `AppNotIdleException` (con un diálogo abierto nunca lo está).
     */
    private fun capturarVentanaDelDialogo(): Bitmap {
        val ventana = ShadowDialog.getLatestDialog()
            ?: throw AssertionError("No hay ninguna ventana de diálogo abierta")
        val decor = ventana.window?.decorView
            ?: throw AssertionError("La ventana del diálogo no tiene decor view")
        val bitmap = dibujar(decor, "dialogo")
        ventana.dismiss()
        return bitmap
    }

    /**
     * Captura el diálogo abierto por encima de la app: se dibuja la pantalla
     * de fondo, el velo oscuro que pone Android y encima la ventana del
     * diálogo, tal como se ve en el teléfono.
     */
    private fun capturarDialogo(nombre: String, fondo: Bitmap) {
        val dialogo = capturarVentanaDelDialogo()

        val resultado = Bitmap.createBitmap(fondo.width, fondo.height, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(resultado)
        lienzo.drawBitmap(fondo, 0f, 0f, null)
        lienzo.drawColor(Color.argb(110, 0, 0, 0)) // velo del diálogo
        val x = ((fondo.width - dialogo.width) / 2f).coerceAtLeast(0f)
        val y = ((fondo.height - dialogo.height) / 2f).coerceAtLeast(0f)
        lienzo.drawBitmap(dialogo, x, y, null)

        guardar(nombre, resultado)
    }

    private fun montarApp() {
        composeTestRule.setContent {
            TemaAhorros {
                App()
            }
        }
        composeTestRule.waitForIdle()
    }

    // =====================================================================
    // 3) Las capturas
    // =====================================================================

    @Test
    fun `01 pantalla de ahorros`() {
        sembrarEjemplo()
        montarApp()
        composeTestRule.onNodeWithText("Ahorros").assertExists()
        guardar("01-ahorros.png", capturarPantalla())
    }

    @Test
    fun `02 pantalla de gastos`() {
        sembrarEjemplo()
        montarApp()
        composeTestRule.onNodeWithText("Gastos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("GASTOS").assertExists()
        guardar("02-gastos.png", capturarPantalla())
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h1500dp-xhdpi")
    fun `03 pantalla de balance`() {
        sembrarEjemplo()
        montarApp()
        composeTestRule.onNodeWithText("Balance").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("BALANCE").assertExists()
        guardar("03-balance.png", capturarPantalla())
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h1500dp-xhdpi")
    fun `04 pantalla de deudas`() {
        sembrarDeudas()
        montarApp()
        composeTestRule.onNodeWithText("Deudas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DEUDAS").assertExists()
        composeTestRule.onNodeWithText("banner").assertExists()
        guardar("04-deudas.png", capturarPantalla())
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h1900dp-xhdpi")
    fun `05 detalle de una deuda`() {
        sembrarDeudas()
        montarApp()
        composeTestRule.onNodeWithText("Deudas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("banner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("REPARTO").assertExists()
        // "Ana" sale dos veces: en el reparto y en la lista de pagos.
        composeTestRule.onAllNodesWithText("Ana")[0].assertExists()
        guardar("05-detalle-deuda.png", capturarPantalla())
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h1900dp-xhdpi")
    fun `06 pantalla de agregar personas con la lista del salon`() {
        sembrarDeudas()
        montarApp()
        composeTestRule.onNodeWithText("Deudas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("banner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Añadir personas").performClick()
        composeTestRule.waitForIdle()
        // La lista del salón solo existe en la versión del 11G. En la versión
        // general (la de cualquiera) lo que tiene que haber es el botón para
        // crear una lista propia.
        val hayListaDelSalon = composeTestRule
            .onAllNodesWithText("Salón 11G", substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
        val hayBotonDeNuevaLista = composeTestRule
            .onAllNodesWithText("Nueva lista", substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
        assertTrue(
            "En la pantalla de añadir personas no se ve ni la lista del salón " +
                "ni el botón de crear una lista nueva",
            hayListaDelSalon || hayBotonDeNuevaLista
        )
        guardar("06-agregar-personas.png", capturarPantalla())
    }

    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h1900dp-xhdpi")
    fun `07 pantalla de ajustes`() {
        sembrarEjemplo()
        montarApp()
        composeTestRule.onNodeWithText("Balance").performClick()
        composeTestRule.waitForIdle()
        // El engranaje de arriba a la derecha abre los ajustes.
        composeTestRule.onNodeWithContentDescription("Ajustes").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("COPIA DE SEGURIDAD").assertExists()
        guardar("07-ajustes.png", capturarPantalla())
    }

    // Omitidos: con una ventana de diálogo de Compose abierta, Robolectric +
    // Espresso nunca ven el hilo principal en reposo y el test revienta con
    // AppNotIdleException al terminar (ya se probó cerrar la ventana). El
    // diálogo se valida en el emulador / el teléfono real, donde sí funciona.
    @Ignore("El dialogo abre otra ventana y Espresso nunca queda en reposo")
    @Test
    fun `04 dialogo de alta de ahorro`() {
        // Sin click ni waitForIdle(): con una ventana de dialogo abierta, ni
        // Espresso ni Compose ven el hilo principal en reposo
        // (AppNotIdleException). Se frena el reloj y se avanza a mano.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TemaAhorros {
                DialogoMovimiento(
                    tipo = Tipo.AHORRO,
                    inicial = null,
                    onGuardar = {},
                    onCerrar = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(600)
        val archivo = guardar("06-dialogo-alta.png", capturarVentanaDelDialogo())
        assertTrue(
            "La captura del dialogo salio vacia (${archivo.length()} bytes)",
            archivo.length() > 5000
        )
    }

    @Ignore("El dialogo abre otra ventana y Espresso nunca queda en reposo")
    @Test
    fun `05 dialogo de alta de gasto`() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TemaAhorros {
                DialogoMovimiento(
                    tipo = Tipo.GASTO,
                    inicial = null,
                    onGuardar = {},
                    onCerrar = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(600)
        val archivo = guardar("07-dialogo-gasto.png", capturarVentanaDelDialogo())
        assertTrue(
            "La captura del dialogo salio vacia (${archivo.length()} bytes)",
            archivo.length() > 5000
        )
    }
}
