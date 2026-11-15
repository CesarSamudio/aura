package com.cesar.ahorros

import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.ListaSalon
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.dominio.CalculosDeudas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests de la lógica de Deudas. Corren en el PC (JVM puro), sin emulador.
 *
 * El modelo, como lo corrigió Cesar:
 * - **META** ("sé el total"): la meta se reparte entre los que pagan, y el
 *   monto especial de uno **redistribuye**: los demás suben para llegar a la meta.
 * - **META con cuota personalizada**: si Cesar pone una cuota a mano, ESA es
 *   la que se cobra a cada uno y el total a cobrar sale de multiplicarla por
 *   la gente. El costo real se conserva aparte.
 * - **LIBRE** ("cuánto paga cada uno"): el monto de cada uno es el que dijo
 *   Cesar; los montos especiales no cambian el de los demás, cambian el total.
 * - La lista de personas NO divide nada: es a quién se le cobra.
 * - Todos los montos se redondean a 2 decimales.
 */
class LogicaDeudasTest {

    private val hoy: LocalDate = LocalDate.of(2026, 11, 14)

    /** Tolerancia de un centavo. */
    private val centavo = 0.0001

    private fun persona(id: String, nombre: String, cuotaPropia: Double? = null) =
        PersonaDeuda(id, nombre, cuotaPropia)

    private fun pago(
        id: String,
        personaId: String,
        monto: Double,
        dia: LocalDate = hoy,
        hora: Int = 10,
        metodo: MetodoPago = MetodoPago.EFECTIVO,
        comprobante: String? = null
    ) = Pago(
        id = id,
        personaId = personaId,
        monto = monto,
        metodo = metodo,
        nota = "",
        comprobante = comprobante,
        ts = dia.atTime(hora, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
        saldada: Boolean = false,
        cuotaPersonalizada: Double? = null
    ) = Deuda(
        id = id,
        nombre = nombre,
        modo = modo,
        total = total,
        montoPorPersona = montoPorPersona,
        cuotaPersonalizada = cuotaPersonalizada,
        personasQuePagan = personasQuePagan,
        personas = personas,
        pagos = pagos,
        saldada = saldada,
        ts = hoy.atTime(9, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // ------------------------------------------------------------------
    // Modo META: sé el total
    // ------------------------------------------------------------------

    @Test
    fun `en modo meta la cuota es el total dividido entre los que pagan`() {
        val d = deuda(modo = ModoDeuda.META, total = 60.0, personasQuePagan = 4)
        assertEquals(15.0, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(60.0, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `el monto especial hace que los demas paguen mas para llegar a la meta`() {
        // El ejemplo de Cesar: 3 personas, meta de 10, una paga 2 de especial
        // → los otros dos pagan 4 cada uno.
        val d = deuda(
            modo = ModoDeuda.META,
            total = 10.0,
            personasQuePagan = 3,
            personas = listOf(
                persona("p1", "Ana", 2.0),
                persona("p2", "Luis"),
                persona("p3", "Carlos")
            )
        )
        assertEquals(4.0, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(2.0, CalculosDeudas.cuotaDe(d, d.personas[0]), centavo)
        assertEquals(4.0, CalculosDeudas.cuotaDe(d, d.personas[1]), centavo)
        assertEquals(4.0, CalculosDeudas.cuotaDe(d, d.personas[2]), centavo)
        // Con todo junto se llega a la meta.
        assertEquals(10.0, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `varios montos especiales se descuentan todos de la meta`() {
        val d = deuda(
            modo = ModoDeuda.META,
            total = 12.0,
            personasQuePagan = 4,
            personas = listOf(
                persona("p1", "Ana", 2.0),
                persona("p2", "Luis", 3.0),
                persona("p3", "Carlos"),
                persona("p4", "Sofía")
            )
        )
        // (12 − 2 − 3) ÷ (4 − 2) = 3.50 cada uno
        assertEquals(3.5, CalculosDeudas.montoBaseDe(d), centavo)
    }

    @Test
    fun `agregar personas a la lista no cambia la cuota`() {
        val d = deuda(modo = ModoDeuda.META, total = 30.0, personasQuePagan = 3, personas = listOf(persona("p1", "Cesar")))
        assertEquals(10.0, CalculosDeudas.montoBaseDe(d), centavo)

        val mas = d.copy(personas = d.personas + List(20) { persona("x$it", "Extra $it") })
        assertEquals(10.0, CalculosDeudas.montoBaseDe(mas), centavo)
    }

    @Test
    fun `sin personas que pagarlo la cuota es cero`() {
        assertEquals(0.0, CalculosDeudas.montoBaseDe(deuda(personasQuePagan = 0)), centavo)
    }

    @Test
    fun `si todos tienen monto propio no se puede repartir`() {
        val d = deuda(
            modo = ModoDeuda.META,
            total = 10.0,
            personasQuePagan = 2,
            personas = listOf(persona("p1", "Ana", 5.0), persona("p2", "Luis", 5.0))
        )
        assertEquals(0.0, CalculosDeudas.montoBaseDe(d), centavo)
    }

    // ------------------------------------------------------------------
    // Cuota personalizada: cuando César NO quiere que se divida el costo
    // ------------------------------------------------------------------

    @Test
    fun `la cuota personalizada manda sobre la division`() {
        // El caso de Cesar: el costo de 42.80 entre 23 da 1.86, pero él cobra
        // 1.90 a cada uno. Con cuota personalizada, la que manda es esa.
        val d = deuda(
            modo = ModoDeuda.META,
            total = 42.80,
            personasQuePagan = 23,
            cuotaPersonalizada = 1.90
        )
        assertEquals(1.90, CalculosDeudas.montoBaseDe(d), centavo)
        // Y la cuota es la que le toca a cada persona de la lista.
        assertEquals(1.90, CalculosDeudas.cuotaDe(d, d.personas[0]), centavo)
    }

    @Test
    fun `el total a cobrar sale de multiplicar la cuota por la gente`() {
        val d = deuda(
            modo = ModoDeuda.META,
            total = 42.80,
            personasQuePagan = 23,
            cuotaPersonalizada = 1.90
        )
        // 23 × 1.90 = 43.70
        assertEquals(43.70, CalculosDeudas.totalDe(d), centavo)
        // El costo real NO se borra: sigue guardado en la deuda.
        assertEquals(42.80, d.total, centavo)
    }

    @Test
    fun `sin cuota personalizada no cambia nada de lo de siempre`() {
        // Mismo caso, sin cuota: el total se divide como siempre.
        val d = deuda(
            modo = ModoDeuda.META,
            total = 42.80,
            personasQuePagan = 23
        )
        assertEquals(1.86, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(42.80, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `con cuota personalizada los montos especiales no reparten`() {
        // Ana paga 5 de especial: con cuota fija, los demás siguen pagando
        // 1.90 (lo que cambia es el total, no lo que pagan los demás).
        val d = deuda(
            modo = ModoDeuda.META,
            total = 42.80,
            personasQuePagan = 4,
            personas = listOf(
                persona("p1", "Ana", 5.0),
                persona("p2", "Luis"),
                persona("p3", "Carlos"),
                persona("p4", "Sofía")
            ),
            cuotaPersonalizada = 1.90
        )
        assertEquals(1.90, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(5.0, CalculosDeudas.cuotaDe(d, d.personas[0]), centavo)
        assertEquals(1.90, CalculosDeudas.cuotaDe(d, d.personas[1]), centavo)
        // 1.90 × 4 + (5.00 − 1.90) = 10.70
        assertEquals(10.70, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `el caso completo de cesar con cuota personalizada si cuadra`() {
        // 23 personas, cuota de 1.90: 19 pagaron los 1.90 completos y 4
        // pagaron 1.20 (parcial). Con la cuota a mano, la lista y el saldo
        // dan el mismo número: 4 × 0.70 = 2.80.
        val lista = List(23) { i -> persona("p${i + 1}", "Persona ${i + 1}") }
        val pagos = List(23) { i ->
            val numero = i + 1
            pago("pg$numero", "p$numero", if (numero <= 19) 1.90 else 1.20)
        }
        val d = deuda(
            modo = ModoDeuda.META,
            total = 42.80,
            personasQuePagan = 23,
            personas = lista,
            pagos = pagos,
            cuotaPersonalizada = 1.90
        )

        val resumen = CalculosDeudas.resumen(d)
        val personas = CalculosDeudas.personas(d)
        val las19 = personas.filter { it.persona.id in (1..19).map { n -> "p$n" } }
        val las4 = personas.filter { it.persona.id in (20..23).map { n -> "p$n" } }

        // El total a cobrar es 43.70 (23 × 1.90), no el costo de 42.80.
        assertEquals(43.70, resumen.total, centavo)
        assertEquals(40.90, resumen.totalPagado, centavo)
        // El saldo: 43.70 − 40.90 = 2.80
        assertEquals(2.80, resumen.saldo, centavo)

        // Las 19 que pagaron los 1.90: cuadradas exactas.
        assertEquals(19, las19.size)
        las19.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.0, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Las 4 que pagaron 1.20: deben 1.90 − 1.20 = 0.70 cada una.
        assertEquals(4, las4.size)
        las4.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.70, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Y lo importante: la lista y el saldo son el MISMO número.
        val sumaPendientes = personas.sumOf { it.pendiente }
        assertEquals(2.80, sumaPendientes, centavo)
        assertEquals(resumen.saldo, sumaPendientes, centavo)
    }

    // ------------------------------------------------------------------
    // Modo LIBRE: cuánto paga cada uno
    // ------------------------------------------------------------------

    @Test
    fun `en modo libre el monto de cada uno es el que puso cesar`() {
        val d = deuda(
            modo = ModoDeuda.LIBRE,
            total = 0.0,
            montoPorPersona = 4.0,
            personasQuePagan = 4,
            personas = listOf(persona("p1", "Ana"), persona("p2", "Luis"))
        )
        assertEquals(4.0, CalculosDeudas.montoBaseDe(d), centavo)
        // El total se calcula: 4 personas × $4.
        assertEquals(16.0, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `en modo libre el monto especial no cambia el monto de los demas`() {
        // El ejemplo de Cesar: cobra 4 a los que no hicieron nada y 2 a los que
        // expusieron. El total se calcula, los demas siguen pagando 4.
        val d = deuda(
            modo = ModoDeuda.LIBRE,
            montoPorPersona = 4.0,
            personasQuePagan = 4,
            personas = listOf(
                persona("p1", "Ana", 2.0),
                persona("p2", "Luis", 2.0),
                persona("p3", "Carlos"),
                persona("p4", "Sofía")
            )
        )
        assertEquals(4.0, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(2.0, CalculosDeudas.cuotaDe(d, d.personas[0]), centavo)
        assertEquals(4.0, CalculosDeudas.cuotaDe(d, d.personas[2]), centavo)
        // 2 × 2 + 2 × 4 = 12
        assertEquals(12.0, CalculosDeudas.totalDe(d), centavo)
    }

    @Test
    fun `en modo libre el total sube cuando alguien tiene monto especial mayor`() {
        val d = deuda(
            modo = ModoDeuda.LIBRE,
            montoPorPersona = 4.0,
            personasQuePagan = 2,
            personas = listOf(persona("p1", "Ana", 10.0))
        )
        // 4 + 10 = 14
        assertEquals(14.0, CalculosDeudas.totalDe(d), centavo)
    }

    // ------------------------------------------------------------------
    // Redondeo: nada de 3.333333333333334
    // ------------------------------------------------------------------

    @Test
    fun `todos los montos se redondean a dos decimales`() {
        val d = deuda(modo = ModoDeuda.META, total = 10.0, personasQuePagan = 3)
        assertEquals(3.33, CalculosDeudas.montoBaseDe(d), centavo)

        val conPendiente = d.copy(pagos = listOf(pago("pg1", "p1", 1.0)))
        assertEquals(2.33, CalculosDeudas.pendienteDe(conPendiente, "p1"), centavo)
        assertEquals(9.0, CalculosDeudas.saldo(conPendiente), centavo)
    }

    @Test
    fun `el redondeo se aplica tambien al total y a lo pagado`() {
        val d = deuda(
            modo = ModoDeuda.LIBRE,
            montoPorPersona = 3.333,
            personasQuePagan = 3,
            pagos = listOf(pago("pg1", "p1", 0.125))
        )
        assertEquals(3.33, CalculosDeudas.montoBaseDe(d), centavo)
        assertEquals(9.99, CalculosDeudas.totalDe(d), centavo)
        // 0.125 se redondea a 0.13 (se redondea hacia arriba).
        assertEquals(0.13, CalculosDeudas.resumen(d).totalPagado, centavo)
    }

    @Test
    fun `redondear deja siempre dos decimales`() {
        assertEquals(3.33, CalculosDeudas.redondear(3.3333333333333335), centavo)
        assertEquals(0.0, CalculosDeudas.redondear(0.001), centavo)
        assertEquals(10.0, CalculosDeudas.redondear(10.0), centavo)
        assertEquals(2.5, CalculosDeudas.redondear(2.5), centavo)
    }

    // ------------------------------------------------------------------
    // Lo que pagó cada uno
    // ------------------------------------------------------------------

    @Test
    fun `lo pagado es la suma de los pagos de cada persona`() {
        val d = deuda(
            pagos = listOf(
                pago("pg1", "p1", 10.0),
                pago("pg2", "p1", 5.0),
                pago("pg3", "p2", 7.5)
            )
        )
        val pagado = CalculosDeudas.pagadoPorPersona(d)
        assertEquals(15.0, pagado["p1"]!!, centavo)
        assertEquals(7.5, pagado["p2"]!!, centavo)
    }

    @Test
    fun `lo que falta por pagar es la cuota menos lo pagado`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 2,
            personas = listOf(persona("p1", "A"), persona("p2", "B")),
            pagos = listOf(pago("pg1", "p1", 10.0))
        )
        val resumen = CalculosDeudas.resumenPersona(d, "p1")!!
        assertEquals(15.0, resumen.cuota, centavo)
        assertEquals(10.0, resumen.pagado, centavo)
        assertEquals(5.0, resumen.pendiente, centavo)
        assertEquals(0.0, resumen.excedente, centavo)
        assertFalse(resumen.estaSaldada)
    }

    @Test
    fun `al cambiar la meta cambia la cuota pero no lo ya pagado`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 3,
            personas = listOf(persona("p1", "Cesar")),
            pagos = listOf(pago("pg1", "p1", 10.0))
        )
        assertEquals(10.0, CalculosDeudas.montoBaseDe(d), centavo)

        // Ahora son 6 los que pagan: la cuota baja a 5 y el que pagó 10 queda
        // con 5 de crédito.
        val seis = d.copy(personasQuePagan = 6)
        assertEquals(5.0, CalculosDeudas.montoBaseDe(seis), centavo)
        val resumen = CalculosDeudas.resumenPersona(seis, "p1")!!
        assertEquals(10.0, resumen.pagado, centavo)
        assertEquals(0.0, resumen.pendiente, centavo)
        assertEquals(5.0, resumen.excedente, centavo)
    }

    @Test
    fun `si alguien pago de mas el excedente queda a favor y no debe nada`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 2,
            personas = listOf(persona("p1", "A"), persona("p2", "B")),
            pagos = listOf(pago("pg1", "p1", 20.0))
        )
        val resumen = CalculosDeudas.resumenPersona(d, "p1")!!
        assertEquals(15.0, resumen.cuota, centavo)
        assertEquals(20.0, resumen.pagado, centavo)
        assertEquals(0.0, resumen.pendiente, centavo)
        assertEquals(5.0, resumen.excedente, centavo)
        assertTrue(resumen.estaSaldada)
    }

    @Test
    fun `nadie debe nunca una cantidad negativa`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 2,
            personas = listOf(persona("p1", "A"), persona("p2", "B")),
            pagos = listOf(pago("pg1", "p1", 100.0))
        )
        assertEquals(0.0, CalculosDeudas.pendienteDe(d, "p1"), centavo)
        assertEquals(15.0, CalculosDeudas.pendienteDe(d, "p2"), centavo)
        assertTrue(CalculosDeudas.personas(d).all { it.pendiente >= 0.0 })
    }

    // ------------------------------------------------------------------
    // Resumen de la deuda
    // ------------------------------------------------------------------

    @Test
    fun `el resumen cuenta solo a quienes deben algo`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 3,
            personas = listOf(persona("p1", "A"), persona("p2", "B"), persona("p3", "C")),
            pagos = listOf(pago("pg1", "p1", 10.0))
        )
        val resumen = CalculosDeudas.resumen(d)
        assertEquals(10.0, resumen.montoBase, centavo)
        assertEquals(30.0, resumen.total, centavo)
        assertEquals(10.0, resumen.totalPagado, centavo)
        assertEquals(2, resumen.personasDeben)
        assertEquals(3, resumen.personasEnLista)
        assertEquals(20.0, resumen.saldo, centavo)
    }

    @Test
    fun `el saldo es lo que falta por cobrar`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 1,
            personas = listOf(persona("p1", "A")),
            pagos = listOf(pago("pg1", "p1", 12.0))
        )
        assertEquals(18.0, CalculosDeudas.saldo(d), centavo)
    }

    @Test
    fun `una deuda pagada de mas no da saldo negativo`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 1,
            personas = listOf(persona("p1", "A")),
            pagos = listOf(pago("pg1", "p1", 40.0))
        )
        assertEquals(0.0, CalculosDeudas.saldo(d), centavo)
    }

    // ------------------------------------------------------------------
    // Saldada: a mano
    // ------------------------------------------------------------------

    @Test
    fun `saldada se marca a mano y no depende de los pagos`() {
        val pagada = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 1,
            personas = listOf(persona("p1", "A")),
            pagos = listOf(pago("pg1", "p1", 30.0))
        )
        assertFalse(pagada.saldada)

        val marcada = pagada.copy(saldada = true)
        assertTrue(marcada.saldada)
        assertEquals(0.0, CalculosDeudas.saldo(marcada), centavo)
    }

    @Test
    fun `una deuda saldada a mano se muestra cerrada aunque falte dinero`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 3,
            personas = listOf(persona("p1", "A")), saldada = true
        )
        assertEquals(30.0, CalculosDeudas.resumen(d).saldo, centavo)
        assertEquals(1, CalculosDeudas.resumen(d).personasDeben)

        val mostrado = CalculosDeudas.resumenParaMostrar(d)
        assertEquals(0.0, mostrado.saldo, centavo)
        assertEquals(0, mostrado.personasDeben)
    }

    @Test
    fun `una deuda sin marcar se muestra con lo que realmente falta`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 30.0, personasQuePagan = 2,
            personas = listOf(persona("p1", "A"), persona("p2", "B")), saldada = false
        )
        val mostrado = CalculosDeudas.resumenParaMostrar(d)
        assertEquals(30.0, mostrado.saldo, centavo)
        assertEquals(2, mostrado.personasDeben)
    }

    // ------------------------------------------------------------------
    // Pagos agrupados por día
    // ------------------------------------------------------------------

    @Test
    fun `los pagos se agrupan por dia del mas reciente al mas viejo`() {
        val d = deuda(
            pagos = listOf(
                pago("pg1", "p1", 5.0, hoy.minusDays(2)),
                pago("pg2", "p2", 5.0, hoy),
                pago("pg3", "p1", 5.0, hoy)
            )
        )
        val grupos = CalculosDeudas.pagosPorDia(d)
        assertEquals(2, grupos.size)
        assertEquals(hoy, grupos[0].fecha)
        assertEquals(2, grupos[0].pagos.size)
        assertEquals(hoy.minusDays(2), grupos[1].fecha)
    }

    @Test
    fun `los pagos de una persona salen del mas nuevo al mas viejo`() {
        val d = deuda(
            pagos = listOf(
                pago("pg1", "p1", 5.0, hoy.minusDays(1), hora = 9),
                pago("pg2", "p1", 5.0, hoy, hora = 9),
                pago("pg3", "p2", 5.0, hoy, hora = 9)
            )
        )
        val deAna = CalculosDeudas.pagosDe(d, "p1")
        assertEquals(2, deAna.size)
        assertEquals("pg2", deAna[0].id)
    }

    // ------------------------------------------------------------------
    // Casos borde
    // ------------------------------------------------------------------

    @Test
    fun `una deuda sin pagos no debe nada pero el saldo es el total`() {
        val d = deuda(
            modo = ModoDeuda.META, total = 50.0, personasQuePagan = 2,
            personas = listOf(persona("p1", "A"), persona("p2", "B"))
        )
        val resumen = CalculosDeudas.resumen(d)
        assertEquals(0.0, resumen.totalPagado, centavo)
        assertEquals(50.0, resumen.saldo, centavo)
        assertEquals(2, resumen.personasDeben)
    }

    @Test
    fun `el resumen de una persona que no existe es null`() {
        assertEquals(null, CalculosDeudas.resumenPersona(deuda(), "no-existe"))
    }

    // ------------------------------------------------------------------
    // La lista del salón se prueba en su propio archivo de test
    // (src/testG11): esa lista solo existe en la versión del 11G.
    // ------------------------------------------------------------------
}
