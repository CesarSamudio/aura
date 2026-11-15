package com.cesar.ahorros

import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.dominio.CalculosDeudas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Validación del caso REAL de Cesar con la cuota redondeada hacia arriba.
 *
 * Qué pasó en la app: la deuda está en modo META con total de $42.80 para 23
 * personas. La cuota que sale de dividir es $1.86 (42.80 ÷ 23 = 1.8608...),
 * pero Cesar cobra **$1.90** a cada uno (redondea hacia arriba). Con eso:
 *
 * - 19 personas pagaron los $1.90 completos.
 * - 4 personas pagaron $1.20 (pago parcial al principio).
 * - Total pagado: 19 × 1.90 + 4 × 1.20 = 40.90.
 *
 * Lo que la app mostraba: el encabezado decía "falta por cobrar $1.90" (el
 * saldo por dinero: 42.80 − 40.90) pero en la lista las 4 personas que
 * quedaron debiendo aparecían con $0.66 (1.86 − 1.20). O sea: **la lista y el
 * saldo no cuadraban** (4 × 0.66 = 2.64 contra un saldo de 1.90).
 *
 * Estos tests documentan los números reales:
 * - 1) El caso ANTIGUO (modo META sin cuota personalizada): el desajuste de
 *    1.90 contra 2.64 sigue ahí, porque el comportamiento de siempre no se
 *    toca.
 * - 2) La hipótesis del modo LIBRE con $1.90 por persona: el saldo vuelve a
 *    cuadrar con la lista (total 43.70, saldo 2.80, las 4 personas debiendo
 *    0.70 cada una).
 * - 3) El caso NUEVO: la misma deuda en modo META pero con **cuota
 *    personalizada** de $1.90. Ahora la cuota es la que cobra Cesar, el
 *    total a cobrar es 43.70 y el saldo (2.80) vuelve a ser exactamente la
 *    suma de lo que deben los de la lista (4 × 0.70). El costo real de 42.80
 *    se conserva guardado aparte.
 *
 * Corren en el PC (JVM puro), sin emulador.
 */
class ValidacionCuotaRedondeoTest {

    private val hoy: LocalDate = LocalDate.of(2026, 11, 14)

    /** Tolerancia de un centavo. */
    private val centavo = 0.0001

    // ------------------------------------------------------------------
    // Helpers (copiados de LogicaDeudasTest: los de allá son privados)
    // ------------------------------------------------------------------

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
    // El caso de Cesar
    // ------------------------------------------------------------------

    /** Las 23 personas de la lista de Cesar (p1..p23), sin monto especial. */
    private fun listaDe23(): List<PersonaDeuda> =
        List(23) { i -> persona("p${i + 1}", "Persona ${i + 1}") }

    /**
     * Los pagos de Cesar: 19 personas pagaron los $1.90 completos y 4
     * pagaron $1.20 (pago parcial del principio).
     */
    private fun pagosDeCesar(): List<Pago> =
        List(23) { i ->
            val numero = i + 1
            val monto = if (numero <= 19) 1.90 else 1.20
            pago("pg$numero", "p$numero", monto)
        }

    /** La deuda de Cesar tal como está en la app: modo META, meta de $42.80. */
    private fun deudaDeCesarEnMeta() = deuda(
        id = "deuda-banner",
        nombre = "banner del salon",
        modo = ModoDeuda.META,
        total = 42.80,
        personasQuePagan = 23,
        personas = listaDe23(),
        pagos = pagosDeCesar()
    )

    /** La MISMA deuda, pero en modo LIBRE cobrando $1.90 a cada uno. */
    private fun deudaDeCesarEnLibre() = deuda(
        id = "deuda-banner",
        nombre = "banner del salon",
        modo = ModoDeuda.LIBRE,
        total = 0.0,
        montoPorPersona = 1.90,
        personasQuePagan = 23,
        personas = listaDe23(),
        pagos = pagosDeCesar()
    )

    /**
     * La MISMA deuda, en modo META pero con la **cuota personalizada** de
     * $1.90: es lo que ahora puede poner Cesar en el diálogo de la deuda.
     */
    private fun deudaDeCesarConCuota() = deuda(
        id = "deuda-banner",
        nombre = "banner del salon",
        modo = ModoDeuda.META,
        total = 42.80,
        personasQuePagan = 23,
        personas = listaDe23(),
        pagos = pagosDeCesar(),
        cuotaPersonalizada = 1.90
    )

    // ------------------------------------------------------------------
    // 1) Modo META: los números reales (y el desajuste) de la app
    // ------------------------------------------------------------------

    @Test
    fun `en modo meta la cuota de 1_86 no cuadra con el saldo de 1_90`() {
        val d = deudaDeCesarEnMeta()
        val resumen = CalculosDeudas.resumen(d)
        val lista = CalculosDeudas.personas(d)

        // Los que pagaron los 1.90 completos (19) y los que pagaron 1.20 (4).
        val pagaronCompleto = lista.filter { it.persona.id in (1..19).map { n -> "p$n" } }
        val pagaronParcial = lista.filter { it.persona.id in (20..23).map { n -> "p$n" } }

        val sumaPendientes = lista.sumOf { it.pendiente }

        println("=== META (caso real de Cesar) ===")
        println("totalDe      = ${CalculosDeudas.totalDe(d)}")
        println("montoBaseDe  = ${CalculosDeudas.montoBaseDe(d)}")
        println("totalPagado  = ${resumen.totalPagado}")
        println("saldo        = ${resumen.saldo}")
        println("personasDeben= ${resumen.personasDeben} de ${resumen.personasEnLista}")
        println("19 completas -> pendiente=${pagaronCompleto.first().pendiente} " +
            "excedente=${pagaronCompleto.first().excedente}")
        println("4 parciales  -> pendiente=${pagaronParcial.first().pendiente} " +
            "excedente=${pagaronParcial.first().excedente}")
        println("suma pendientes = $sumaPendientes  vs saldo = ${resumen.saldo}")

        // El total y la cuota que muestra la app.
        assertEquals(42.80, CalculosDeudas.totalDe(d), centavo)
        assertEquals(1.86, CalculosDeudas.montoBaseDe(d), centavo)

        // Lo pagado y lo que falta por cobrar (el encabezado de la app).
        assertEquals(40.90, resumen.totalPagado, centavo)
        assertEquals(1.90, resumen.saldo, centavo)

        // Las 19 que pagaron 1.90: no deben nada, pero quedaron 0.04 de más.
        assertEquals(19, pagaronCompleto.size)
        pagaronCompleto.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.0, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.04, it.excedente, centavo)
        }

        // Las 4 que pagaron 1.20: deben 1.86 − 1.20 = 0.66 cada una.
        assertEquals(4, pagaronParcial.size)
        pagaronParcial.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.66, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Y acá está el problema: la lista (2.64) NO es el saldo (1.90).
        assertEquals(2.64, sumaPendientes, centavo)
        assertNotEquals(resumen.saldo, sumaPendientes, centavo)
    }

    // ------------------------------------------------------------------
    // 2) Modo LIBRE: la hipótesis (con 1.90 el saldo sí cuadra)
    // ------------------------------------------------------------------

    @Test
    fun `en modo libre con 1_90 por persona el saldo cuadra con la lista`() {
        val d = deudaDeCesarEnLibre()
        val resumen = CalculosDeudas.resumen(d)
        val lista = CalculosDeudas.personas(d)

        val pagaronCompleto = lista.filter { it.persona.id in (1..19).map { n -> "p$n" } }
        val pagaronParcial = lista.filter { it.persona.id in (20..23).map { n -> "p$n" } }

        val sumaPendientes = lista.sumOf { it.pendiente }

        println("=== LIBRE con montoPorPersona = 1.90 (hipótesis) ===")
        println("totalDe      = ${CalculosDeudas.totalDe(d)}")
        println("montoBaseDe  = ${CalculosDeudas.montoBaseDe(d)}")
        println("totalPagado  = ${resumen.totalPagado}")
        println("saldo        = ${resumen.saldo}")
        println("personasDeben= ${resumen.personasDeben} de ${resumen.personasEnLista}")
        println("19 completas -> pendiente=${pagaronCompleto.first().pendiente} " +
            "excedente=${pagaronCompleto.first().excedente}")
        println("4 parciales  -> pendiente=${pagaronParcial.first().pendiente} " +
            "excedente=${pagaronParcial.first().excedente}")
        println("suma pendientes = $sumaPendientes  vs saldo = ${resumen.saldo}")

        // El total ahora se calcula: 23 × 1.90 = 43.70 (no 42.80).
        assertEquals(43.70, CalculosDeudas.totalDe(d), centavo)
        assertEquals(1.90, CalculosDeudas.montoBaseDe(d), centavo)

        // Lo pagado sigue siendo 40.90, pero el saldo sube a 2.80.
        assertEquals(40.90, resumen.totalPagado, centavo)
        assertEquals(2.80, resumen.saldo, centavo)

        // Las 19 que pagaron 1.90: cuadradas exactas, sin excedente.
        assertEquals(19, pagaronCompleto.size)
        pagaronCompleto.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.0, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Las 4 que pagaron 1.20: deben 1.90 − 1.20 = 0.70 cada una.
        assertEquals(4, pagaronParcial.size)
        pagaronParcial.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.70, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Y acá sí: la lista (2.80) es EXACTAMENTE el saldo (2.80).
        assertEquals(2.80, sumaPendientes, centavo)
        assertEquals(resumen.saldo, sumaPendientes, centavo)
    }

    // ------------------------------------------------------------------
    // 3) El caso NUEVO: modo META con cuota personalizada de $1.90
    // ------------------------------------------------------------------

    @Test
    fun `con cuota personalizada el saldo vuelve a cuadrar con la lista`() {
        val d = deudaDeCesarConCuota()
        val resumen = CalculosDeudas.resumen(d)
        val lista = CalculosDeudas.personas(d)

        val pagaronCompleto = lista.filter { it.persona.id in (1..19).map { n -> "p$n" } }
        val pagaronParcial = lista.filter { it.persona.id in (20..23).map { n -> "p$n" } }

        val sumaPendientes = lista.sumOf { it.pendiente }

        println("=== META con cuota personalizada = 1.90 (caso nuevo) ===")
        println("totalDe      = ${CalculosDeudas.totalDe(d)}")
        println("montoBaseDe  = ${CalculosDeudas.montoBaseDe(d)}")
        println("costo real   = ${d.total}")
        println("totalPagado  = ${resumen.totalPagado}")
        println("saldo        = ${resumen.saldo}")
        println("19 completas -> pendiente=${pagaronCompleto.first().pendiente} " +
            "excedente=${pagaronCompleto.first().excedente}")
        println("4 parciales  -> pendiente=${pagaronParcial.first().pendiente} " +
            "excedente=${pagaronParcial.first().excedente}")
        println("suma pendientes = $sumaPendientes  vs saldo = ${resumen.saldo}")

        // La cuota es la que puso Cesar: 1.90, no la división (1.86).
        assertEquals(1.90, CalculosDeudas.montoBaseDe(d), centavo)

        // El total a cobrar sale de multiplicar: 23 × 1.90 = 43.70.
        assertEquals(43.70, CalculosDeudas.totalDe(d), centavo)
        // El costo real NO se borra: sigue guardado en la deuda.
        assertEquals(42.80, d.total, centavo)

        // Lo pagado sigue siendo 40.90 y el saldo sube a 2.80.
        assertEquals(40.90, resumen.totalPagado, centavo)
        assertEquals(2.80, resumen.saldo, centavo)

        // Las 19 que pagaron los 1.90: cuadradas exactas, sin excedente.
        assertEquals(19, pagaronCompleto.size)
        pagaronCompleto.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.0, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Las 4 que pagaron 1.20: deben 1.90 − 1.20 = 0.70 cada una.
        assertEquals(4, pagaronParcial.size)
        pagaronParcial.forEach {
            assertEquals("pendiente de ${it.persona.id}", 0.70, it.pendiente, centavo)
            assertEquals("excedente de ${it.persona.id}", 0.0, it.excedente, centavo)
        }

        // Y lo que se buscaba: la lista y el saldo son el MISMO número.
        assertEquals(2.80, sumaPendientes, centavo)
        assertEquals(resumen.saldo, sumaPendientes, centavo)
    }
}
