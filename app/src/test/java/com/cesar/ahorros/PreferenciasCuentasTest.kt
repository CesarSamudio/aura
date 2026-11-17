package com.cesar.ahorros

import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.data.RepositorioDeudas
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.dominio.Calculos
import com.cesar.ahorros.dominio.CalculosDeudas
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tests de lo que cambian las **preferencias** y la **deuda individual**.
 *
 * - "Combinar ahorros con gastos": el dinero de los ahorros pasa a ser lo que
 *   queda (lo depositado menos lo gastado), así que al gastar, baja.
 * - La **deuda individual**: le debe a una sola persona, sin lista de gente
 *   ni cuota que repartir.
 *
 * Son tests JVM puros: no dibujan nada, solo comprueban las cuentas.
 */
class PreferenciasCuentasTest {

    private val centavo = 0.0001

    private lateinit var carpeta: File
    private lateinit var repositorio: RepositorioDeudas

    @Before
    fun preparar() {
        carpeta = Files.createTempDirectory("ahorros-preferencias").toFile()
        repositorio = RepositorioDeudas(carpeta)
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun ahorro(monto: Double) = Movimiento(
        id = "ahorro$monto",
        tipo = Tipo.AHORRO,
        monto = monto,
        categoria = "Digital",
        nota = "",
        ts = 0L
    )

    private fun gasto(monto: Double) = Movimiento(
        id = "gasto$monto",
        tipo = Tipo.GASTO,
        monto = monto,
        categoria = "Comida",
        nota = "",
        ts = 0L
    )

    // ---------------------------------------------------------------------
    // Combinar ahorros con gastos
    // ---------------------------------------------------------------------

    @Test
    fun `sin combinar, los ahorros son lo depositado`() {
        val movimientos = listOf(ahorro(100.0), gasto(30.0))
        assertEquals(100.0, Calculos.totalAhorro(movimientos), centavo)
    }

    @Test
    fun `combinando, los ahorros son lo depositado menos lo gastado`() {
        val movimientos = listOf(ahorro(100.0), gasto(30.0))
        assertEquals(
            70.0,
            Calculos.totalAhorro(movimientos, combinarConGastos = true),
            centavo
        )
    }

    @Test
    fun `al gastar, los ahorros bajan`() {
        val antes = listOf(ahorro(100.0))
        val despues = antes + gasto(30.0)

        assertEquals(
            Calculos.totalAhorro(antes, combinarConGastos = true),
            Calculos.totalAhorro(despues, combinarConGastos = true) + 30.0,
            centavo
        )
    }

    @Test
    fun `combinar no cambia el balance`() {
        // El balance es lo depositado menos lo gastado, siempre. Si al
        // combinar se restara el gasto otra vez, saldría el doble de menos.
        val movimientos = listOf(ahorro(100.0), gasto(30.0))
        val balance = Calculos.totalAhorro(movimientos) - Calculos.totalGasto(movimientos)

        assertEquals(70.0, balance, centavo)
        assertEquals(
            balance,
            Calculos.totalAhorro(movimientos, combinarConGastos = true),
            centavo
        )
    }

    // ---------------------------------------------------------------------
    // La deuda individual
    // ---------------------------------------------------------------------

    private fun deudaIndividual(total: Double = 40.0) = Deuda(
        id = "d1",
        nombre = "Pedro",
        modo = ModoDeuda.META,
        total = total,
        personasQuePagan = 1,
        cuotaAutomatica = false,
        personas = listOf(PersonaDeuda(id = "p1", nombre = "Pedro")),
        individual = true,
        ts = 0L
    )

    @Test
    fun `en una deuda individual le toca todo a la unica persona`() {
        val deuda = deudaIndividual(40.0)

        assertEquals(1, CalculosDeudas.pagadoresDe(deuda))
        assertEquals(40.0, CalculosDeudas.montoBaseDe(deuda), centavo)
        assertEquals(40.0, CalculosDeudas.totalDe(deuda), centavo)
    }

    @Test
    fun `una deuda individual no es de cuota automatica`() {
        // Con una sola persona no hay nada que repartir: la cuota no sale de
        // una lista.
        assertFalse(CalculosDeudas.esAutomatica(deudaIndividual()))
    }

    @Test
    fun `la deuda individual se guarda y se vuelve a leer`() {
        assertTrue(repositorio.guardar(listOf(deudaIndividual(40.0))))

        val leida = repositorio.cargar().first()
        assertTrue("La deuda perdió la marca de individual", leida.individual)
        assertEquals("Pedro", leida.nombre)
        assertEquals("Pedro", leida.personas.first().nombre)
        assertEquals(1, CalculosDeudas.pagadoresDe(leida))
    }

    @Test
    fun `una deuda vieja (sin la marca) sigue siendo normal`() {
        // Los archivos del teléfono no tienen el campo nuevo: la deuda se
        // lee como siempre (una deuda de grupo, con su lista de personas).
        val archivo = File(carpeta, RepositorioDeudas.NOMBRE_ARCHIVO)
        archivo.writeText(
            """[{"id":"d9","nombre":"banner","modo":"META","total":30.0,""" +
                """"personasQuePagan":2,"personas":[{"id":"p1","nombre":"Ana"}],"ts":0}]"""
        )

        val leida = repositorio.cargar().first()
        assertFalse("Una deuda vieja no puede ser individual", leida.individual)
        assertEquals(2, CalculosDeudas.pagadoresDe(leida))
    }
}
