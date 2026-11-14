package com.cesar.ahorros

import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Repositorio
import com.cesar.ahorros.data.Tipo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tests de la PERSISTENCIA (archivo JSON) de la app "Ahorros".
 *
 * Corren en el PC (JVM puro), sin emulador ni teléfono: `gradlew test` o
 * `gradlew testReleaseUnitTest`.
 *
 * Para poder probar en JVM puro el repositorio se construye con la CARPETA
 * donde se guardan los datos (`Repositorio(File)`); en la app se sigue usando
 * el constructor con `Context`, que internamente usa `context.filesDir`.
 */
class PersistenciaTest {

    private lateinit var carpeta: File
    private lateinit var repositorio: Repositorio

    @Before
    fun preparar() {
        // Directorio temporal real: se crea de verdad y se borra al terminar.
        carpeta = Files.createTempDirectory("ahorros-persistencia").toFile()
        repositorio = Repositorio(carpeta)
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun archivoDatos(): File = File(carpeta, "movimientos.json")

    private fun movimiento(
        id: String,
        tipo: Tipo = Tipo.GASTO,
        monto: Double = 10.0,
        categoria: String = "Comida",
        nota: String = "",
        ts: Long = 1_700_000_000_000L
    ) = Movimiento(id = id, tipo = tipo, monto = monto, categoria = categoria, nota = nota, ts = ts)

    @Test
    fun `guardar y volver a cargar devuelve exactamente los mismos movimientos`() {
        val lista = listOf(
            movimiento("a1", Tipo.AHORRO, 150.0, "Físico", "Ahorro del mes", 1_700_000_000_000L),
            movimiento("g1", Tipo.GASTO, 42.75, "Comida", "Almuerzo con amigos", 1_700_086_400_000L),
            movimiento("g2", Tipo.GASTO, 8.5, "Transporte", "Paseo", 1_700_172_800_000L)
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(3, cargados.size)
        assertEquals(lista, cargados)
        assertEquals("a1", cargados[0].id)
        assertEquals(Tipo.AHORRO, cargados[0].tipo)
        assertEquals(150.0, cargados[0].monto, 0.0001)
        assertEquals("Físico", cargados[0].categoria)
        assertEquals("Ahorro del mes", cargados[0].nota)
        assertEquals(1_700_000_000_000L, cargados[0].ts)
        assertEquals("g2", cargados[2].id)
        assertEquals(1_700_172_800_000L, cargados[2].ts)
    }

    @Test
    fun `guardar una lista vacia y cargarla devuelve lista vacia`() {
        repositorio.guardar(emptyList())

        val cargados = repositorio.cargar()
        assertTrue(cargados.isEmpty())
    }

    @Test
    fun `cargar cuando el archivo no existe devuelve lista vacia sin lanzar excepcion`() {
        assertFalse(archivoDatos().exists())

        val cargados = repositorio.cargar()

        assertTrue(cargados.isEmpty())
    }

    @Test
    fun `un archivo corrupto devuelve lista vacia y no rompe la app`() {
        // Basura total: ni siquiera es JSON.
        archivoDatos().writeText("esto no es json {{{ \u00F1 \uD83D\uDE00 <<<")

        val cargados = repositorio.cargar()

        assertTrue(cargados.isEmpty())
    }

    @Test
    fun `un archivo truncado a mitad de camino devuelve lista vacia y no rompe la app`() {
        // JSON válido al principio pero cortado: es lo que queda si la app
        // muere mientras escribe.
        archivoDatos().writeText("""[{ "id": "a1", "tipo": "AHORRO", "monto": 10.0,""")

        val cargados = repositorio.cargar()

        assertTrue(cargados.isEmpty())
    }

    @Test
    fun `el json con campos desconocidos se carga sin fallar`() {
        archivoDatos().writeText(
            """
            [
              {
                "id": "a1",
                "tipo": "AHORRO",
                "monto": 25.0,
                "categoria": "Digital",
                "nota": "Transferencia",
                "ts": 1700000000000,
                "campoDesconocido": "debería ignorarse",
                "otroCampo": { "anidado": true }
              }
            ]
            """.trimIndent()
        )

        val cargados = repositorio.cargar()

        assertEquals(1, cargados.size)
        assertEquals("a1", cargados[0].id)
        assertEquals(Tipo.AHORRO, cargados[0].tipo)
        assertEquals(25.0, cargados[0].monto, 0.0001)
        assertEquals("Digital", cargados[0].categoria)
        assertEquals("Transferencia", cargados[0].nota)
        assertEquals(1_700_000_000_000L, cargados[0].ts)
    }

    @Test
    fun `los caracteres especiales sobreviven al round trip`() {
        val lista = listOf(
            movimiento(
                "e1",
                categoria = "Café ☕",
                nota = "Añadir ñandú, águila, piñata — 100% «sí» ¿¡? & #\$ @ \\ /"
            ),
            movimiento("e2", nota = "🎉🥳 Emoji: 💰🏦 ¡Vamos! 😀")
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(2, cargados.size)
        assertEquals(lista, cargados)
        assertEquals("Café ☕", cargados[0].categoria)
        assertEquals(
            "Añadir ñandú, águila, piñata — 100% «sí» ¿¡? & #\$ @ \\ /",
            cargados[0].nota
        )
        assertEquals("🎉🥳 Emoji: 💰🏦 ¡Vamos! 😀", cargados[1].nota)
    }

    @Test
    fun `los montos con decimales y negativos se guardan y vuelven identicos`() {
        val lista = listOf(
            movimiento("d1", monto = 12.34),
            movimiento("d2", monto = -45.67),
            movimiento("d3", monto = 0.01),
            movimiento("d4", monto = 100.0),
            movimiento("d5", monto = -0.05)
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(5, cargados.size)
        assertEquals(lista, cargados)
        assertEquals(12.34, cargados[0].monto, 0.0)
        assertEquals(-45.67, cargados[1].monto, 0.0)
        assertEquals(0.01, cargados[2].monto, 0.0)
        assertEquals(100.0, cargados[3].monto, 0.0)
        assertEquals(-0.05, cargados[4].monto, 0.0)
    }

    @Test
    fun `guardar no deja archivos tmp huerfanos y deja un json valido`() {
        repositorio.guardar(listOf(movimiento("a1"), movimiento("g1")))

        val archivos = carpeta.listFiles().orEmpty().map { it.name }
        assertFalse(
            "No debe quedar un .tmp huérfano, se encontraron: $archivos",
            archivos.any { it.endsWith(".tmp") }
        )
        assertTrue(archivos.contains("movimientos.json"))

        // El contenido tiene que ser JSON válido de verdad.
        val elemento = Json.parseToJsonElement(archivoDatos().readText())
        assertTrue(elemento is JsonArray)
        assertEquals(2, (elemento as JsonArray).size)
    }

    @Test
    fun `guardar dos veces deja la segunda lista y no una mezcla`() {
        val primera = listOf(movimiento("a1"), movimiento("g1"), movimiento("g2"))
        val segunda = listOf(movimiento("b1", nota = "solo esto"))

        repositorio.guardar(primera)
        repositorio.guardar(segunda)

        val cargados = repositorio.cargar()
        assertEquals(1, cargados.size)
        assertEquals(segunda, cargados)
        assertEquals("b1", cargados[0].id)

        // El archivo no puede contener los ids de la lista anterior.
        val texto = archivoDatos().readText()
        assertFalse(texto.contains("a1"))
        assertFalse(texto.contains("g2"))
        assertNotNull(Json.parseToJsonElement(texto))
    }

    @Test
    fun `el archivo de datos se llama movimientos json`() {
        repositorio.guardar(listOf(movimiento("a1")))

        assertTrue(archivoDatos().exists())
        assertEquals("movimientos.json", Repositorio.NOMBRE_ARCHIVO)
    }

    @Test
    fun `un monto no numerico no rompe la app ni borra los datos ya guardados`() {
        val buena = listOf(movimiento("a1", monto = 10.0))
        repositorio.guardar(buena)

        // NaN no es un JSON válido: el guardado debe fallar sin reventar nada.
        repositorio.guardar(listOf(movimiento("raro", monto = Double.NaN)))

        // La app sigue viva: lo que había antes sigue en el archivo.
        assertEquals(buena, repositorio.cargar())
    }
}
