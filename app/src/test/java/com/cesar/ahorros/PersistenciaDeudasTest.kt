package com.cesar.ahorros

import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.data.Repositorio
import com.cesar.ahorros.data.RepositorioDeudas
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tests de la PERSISTENCIA de las deudas.
 *
 * Las deudas viven en su propio archivo (`deudas.json`), aparte de
 * `movimientos.json`: una deuda no es un ingreso ni un gasto, así que los
 * datos ya guardados no se tocan al usarla.
 */
class PersistenciaDeudasTest {

    private lateinit var carpeta: File
    private lateinit var repositorio: RepositorioDeudas

    @Before
    fun preparar() {
        carpeta = Files.createTempDirectory("ahorros-deudas").toFile()
        repositorio = RepositorioDeudas(carpeta)
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun archivoDeudas(): File = File(carpeta, "deudas.json")

    private fun persona(
        id: String,
        nombre: String,
        cuotaPropia: Double? = null,
        metodoPago: MetodoPago? = null,
        pruebas: List<String> = emptyList()
    ) = PersonaDeuda(id, nombre, cuotaPropia, metodoPago, pruebas)

    private fun pago(
        id: String,
        personaId: String,
        monto: Double,
        metodo: MetodoPago = MetodoPago.EFECTIVO,
        comprobante: String? = null
    ) = Pago(
        id = id,
        personaId = personaId,
        monto = monto,
        metodo = metodo,
        nota = "",
        comprobante = comprobante,
        ts = 1_700_000_000_000L
    )

    private fun deuda(
        id: String = "d1",
        nombre: String = "banner",
        modo: ModoDeuda = ModoDeuda.META,
        total: Double = 30.0,
        montoPorPersona: Double = 0.0,
        personasQuePagan: Int = 3,
        personas: List<PersonaDeuda> = listOf(persona("p1", "Cesar"), persona("p2", "Ana")),
        pagos: List<Pago> = emptyList(),
        saldada: Boolean = false
    ) = Deuda(
        id = id,
        nombre = nombre,
        modo = modo,
        total = total,
        montoPorPersona = montoPorPersona,
        personasQuePagan = personasQuePagan,
        personas = personas,
        pagos = pagos,
        saldada = saldada,
        ts = 1_700_000_000_000L
    )

    @Test
    fun `guardar y volver a cargar devuelve exactamente las mismas deudas`() {
        val lista = listOf(
            deuda(
                "d1", "banner", ModoDeuda.META, 30.0, personasQuePagan = 3,
                personas = listOf(persona("p1", "Cesar"), persona("p2", "Ana", 4.0)),
                pagos = listOf(pago("pg1", "p2", 15.0, MetodoPago.YAPI, "comprobante_1.jpg"))
            ),
            deuda(
                "d2", "pintura", ModoDeuda.LIBRE, 0.0, montoPorPersona = 4.0, personasQuePagan = 2,
                personas = listOf(persona("p1", "Cesar")), saldada = true
            )
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(2, cargados.size)
        assertEquals(lista, cargados)
        assertEquals("banner", cargados[0].nombre)
        assertEquals(ModoDeuda.META, cargados[0].modo)
        assertEquals(30.0, cargados[0].total, 0.0001)
        assertEquals(3, cargados[0].personasQuePagan)
        assertEquals(2, cargados[0].personas.size)
        assertEquals("Ana", cargados[0].personas[1].nombre)
        assertEquals(4.0, cargados[0].personas[1].cuotaPropia!!, 0.0001)
        assertNull(cargados[0].personas[0].cuotaPropia)
        assertEquals(MetodoPago.YAPI, cargados[0].pagos[0].metodo)
        assertEquals("comprobante_1.jpg", cargados[0].pagos[0].comprobante)
        // La segunda es del modo libre: guarda el monto por persona.
        assertEquals(ModoDeuda.LIBRE, cargados[1].modo)
        assertEquals(4.0, cargados[1].montoPorPersona, 0.0001)
        assertTrue(cargados[1].saldada)
    }

    @Test
    fun `guardar una lista vacia y cargarla devuelve lista vacia`() {
        repositorio.guardar(emptyList())
        assertTrue(repositorio.cargar().isEmpty())
    }

    @Test
    fun `cargar cuando el archivo no existe devuelve lista vacia sin lanzar excepcion`() {
        assertFalse(archivoDeudas().exists())
        assertTrue(repositorio.cargar().isEmpty())
    }

    @Test
    fun `un archivo corrupto devuelve lista vacia y no rompe la app`() {
        archivoDeudas().writeText("esto no es json {{{ ñ <<<")
        assertTrue(repositorio.cargar().isEmpty())
    }

    @Test
    fun `un archivo truncado a mitad de camino devuelve lista vacia`() {
        archivoDeudas().writeText("""[{ "id": "d1", "nombre": "banner",""")
        assertTrue(repositorio.cargar().isEmpty())
    }

    @Test
    fun `el json con campos desconocidos se carga sin fallar`() {
        archivoDeudas().writeText(
            """
            [
              {
                "id": "d1",
                "nombre": "banner",
                "total": 30.0,
                "personasQuePagan": 3,
                "personas": [],
                "pagos": [],
                "saldada": false,
                "ts": 1700000000000,
                "campoDesconocido": "debería ignorarse"
              }
            ]
            """.trimIndent()
        )

        val cargados = repositorio.cargar()

        assertEquals(1, cargados.size)
        assertEquals("banner", cargados[0].nombre)
        assertEquals(30.0, cargados[0].total, 0.0001)
        assertEquals(3, cargados[0].personasQuePagan)
    }

    @Test
    fun `los caracteres especiales sobreviven al round trip`() {
        val lista = listOf(
            deuda(nombre = "Café ☕ del cole", total = 12.5),
            deuda(id = "d2", nombre = "Añadir ñandú, águila — 100% «sí» ¿¡?")
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(2, cargados.size)
        assertEquals(lista, cargados)
        assertEquals("Café ☕ del cole", cargados[0].nombre)
        assertEquals("Añadir ñandú, águila — 100% «sí» ¿¡?", cargados[1].nombre)
    }

    @Test
    fun `guardar no deja archivos tmp huerfanos y deja un json valido`() {
        repositorio.guardar(listOf(deuda()))

        val archivos = carpeta.listFiles().orEmpty().map { it.name }
        assertFalse(
            "No debe quedar un .tmp huérfano, se encontraron: $archivos",
            archivos.any { it.endsWith(".tmp") }
        )
        assertTrue(archivos.contains("deudas.json"))

        val elemento = Json.parseToJsonElement(archivoDeudas().readText())
        assertTrue(elemento is JsonArray)
        assertEquals(1, (elemento as JsonArray).size)
    }

    @Test
    fun `las deudas viejas sin el numero de pagadores se reparan y no se pierden`() {
        // Deuda creada con la versión anterior del modelo: la cuota se dividía
        // entre los de la lista y no existía el campo "personasQuePagan".
        archivoDeudas().writeText(
            """
            [
              {
                "id": "d1",
                "nombre": "banner",
                "total": 30.0,
                "personas": [
                  {"id": "p1", "nombre": "Cesar"},
                  {"id": "p2", "nombre": "Ana"}
                ],
                "pagos": [],
                "saldada": false,
                "ts": 1700000000000
              }
            ]
            """.trimIndent()
        )

        val cargados = repositorio.cargar()

        assertEquals(1, cargados.size)
        assertEquals("banner", cargados[0].nombre)
        // Se repara con la cantidad de personas que había en la lista.
        assertEquals(2, cargados[0].personasQuePagan)
        assertEquals(2, cargados[0].personas.size)
    }

    @Test
    fun `las deudas viejas sin el modo se cargan como modo meta`() {
        // La v1.2 no tenía "modo" ni "montoPorPersona": se cargan con los
        // valores por defecto (modo meta).
        archivoDeudas().writeText(
            """
            [
              {
                "id": "d1",
                "nombre": "banner",
                "total": 30.0,
                "personasQuePagan": 3,
                "personas": [{"id": "p1", "nombre": "Cesar"}],
                "pagos": [],
                "saldada": false,
                "ts": 1700000000000
              }
            ]
            """.trimIndent()
        )

        val cargados = repositorio.cargar()

        assertEquals(1, cargados.size)
        assertEquals(ModoDeuda.META, cargados[0].modo)
        assertEquals(30.0, cargados[0].total, 0.0001)
        assertEquals(3, cargados[0].personasQuePagan)
        assertEquals(0.0, cargados[0].montoPorPersona, 0.0001)
    }

    @Test
    fun `el metodo de pago y las pruebas de una persona se guardan y vuelven`() {
        val lista = listOf(
            deuda(
                "d1", "banner",
                personas = listOf(
                    persona("p1", "Ana", metodoPago = MetodoPago.YAPI, pruebas = listOf("prueba_1.png")),
                    persona("p2", "Luis", metodoPago = MetodoPago.EFECTIVO)
                )
            )
        )

        repositorio.guardar(lista)

        val cargados = repositorio.cargar()
        assertEquals(1, cargados.size)
        val ana = cargados[0].personas[0]
        assertEquals(MetodoPago.YAPI, ana.metodoPago)
        assertEquals(listOf("prueba_1.png"), ana.pruebas)
        // La que no tiene pruebas viene vacía, no rota.
        assertEquals(MetodoPago.EFECTIVO, cargados[0].personas[1].metodoPago)
        assertTrue(cargados[0].personas[1].pruebas.isEmpty())
    }

    @Test
    fun `las personas viejas sin metodo ni pruebas se cargan igual`() {
        archivoDeudas().writeText(
            """
            [
              {
                "id": "d1",
                "nombre": "banner",
                "total": 30.0,
                "montoPorPersona": 0.0,
                "personasQuePagan": 2,
                "personas": [{"id": "p1", "nombre": "Cesar"}],
                "pagos": [],
                "saldada": false,
                "ts": 1700000000000
              }
            ]
            """.trimIndent()
        )

        val cargados = repositorio.cargar()

        assertEquals(1, cargados.size)
        assertEquals(null, cargados[0].personas[0].metodoPago)
        assertTrue(cargados[0].personas[0].pruebas.isEmpty())
    }

    @Test
    fun `el archivo de deudas se llama deudas json y va aparte de los movimientos`() {
        repositorio.guardar(listOf(deuda()))
        Repositorio(carpeta).guardar(emptyList())

        assertTrue(archivoDeudas().exists())
        assertTrue(File(carpeta, "movimientos.json").exists())
        assertEquals("deudas.json", RepositorioDeudas.NOMBRE_ARCHIVO)
    }

    @Test
    fun `un pago sin comprobante se guarda con comprobante null`() {
        repositorio.guardar(listOf(deuda(pagos = listOf(pago("pg1", "p1", 10.0)))))

        val cargados = repositorio.cargar()
        assertNull(cargados[0].pagos[0].comprobante)
        assertEquals(MetodoPago.EFECTIVO, cargados[0].pagos[0].metodo)
    }

    @Test
    fun `guardar dos veces deja la segunda lista y no una mezcla`() {
        repositorio.guardar(listOf(deuda("d1"), deuda("d2")))
        repositorio.guardar(listOf(deuda("d3", nombre = "solo esto")))

        val cargados = repositorio.cargar()
        assertEquals(1, cargados.size)
        assertEquals("d3", cargados[0].id)
        assertFalse(archivoDeudas().readText().contains("d1"))
    }
}
