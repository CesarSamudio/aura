package com.cesar.ahorros

import com.cesar.ahorros.data.CategoriaGasto
import com.cesar.ahorros.data.FuenteAhorro
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.dominio.Calculos
import com.cesar.ahorros.dominio.GestorMovimientos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests de la lógica del dinero. Corren en el PC (JVM puro), sin emulador
 * ni teléfono: `gradlew test` o `gradlew testDebugUnitTest`.
 *
 * Cubren: totales y balance, altas, ediciones, borrados, agrupación por día,
 * acumulado día por día, totales por categoría, orden de la lista y casos borde.
 */
class LogicaDineroTest {

    private val hoy: LocalDate = LocalDate.of(2026, 11, 13)

    /** Tolerancia de un centavo: los montos son Double y pueden sumar 0.30000000000000004. */
    private val centavo = 0.0001

    private fun ahorro(
        id: String,
        monto: Double,
        dia: LocalDate = hoy,
        hora: Int = 10,
        minuto: Int = 30,
        segundo: Int = 45,
        origen: String = FuenteAhorro.FISICO.etiqueta
    ) = Movimiento(
        id = id,
        tipo = Tipo.AHORRO,
        monto = monto,
        categoria = origen,
        nota = "",
        ts = dia.atTime(hora, minuto, segundo).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    private fun gasto(
        id: String,
        monto: Double,
        dia: LocalDate = hoy,
        hora: Int = 12,
        minuto: Int = 0,
        segundo: Int = 0,
        categoria: String = CategoriaGasto.COMIDA.etiqueta
    ) = Movimiento(
        id = id,
        tipo = Tipo.GASTO,
        monto = monto,
        categoria = categoria,
        nota = "Almuerzo",
        ts = dia.atTime(hora, minuto, segundo).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // ------------------------------------------------------------------
    // 1. Totales y balance
    // ------------------------------------------------------------------

    @Test
    fun `sin movimientos el balance es cero`() {
        val gestor = GestorMovimientos()
        assertEquals(0.0, gestor.balance(), centavo)
        assertEquals(0.0, gestor.resumen().ahorro, centavo)
        assertEquals(0.0, gestor.resumen().gasto, centavo)
    }

    @Test
    fun `el resumen separa el total de ahorro del total de gasto`() {
        val movimientos = listOf(
            ahorro("a1", 100.0),
            ahorro("a2", 25.5),
            gasto("g1", 30.0),
            gasto("g2", 9.99)
        )
        val resumen = Calculos.resumen(movimientos)
        assertEquals(125.5, resumen.ahorro, centavo)
        assertEquals(39.99, resumen.gasto, centavo)
        assertEquals(125.5, Calculos.totalAhorro(movimientos), centavo)
        assertEquals(39.99, Calculos.totalGasto(movimientos), centavo)
    }

    @Test
    fun `el balance es el total de ahorro menos el total de gasto`() {
        val movimientos = listOf(ahorro("a1", 100.0), gasto("g1", 30.0))
        val resumen = Calculos.resumen(movimientos)
        assertEquals(resumen.ahorro - resumen.gasto, resumen.balance, centavo)
        assertEquals(70.0, resumen.balance, centavo)
    }

    @Test
    fun `los ahorros se acumulan en la cuenta total`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        gestor.agregar(ahorro("a2", 15.50, dia = hoy.minusDays(1)))
        gestor.agregar(ahorro("a3", 4.25, dia = hoy.minusDays(2)))
        assertEquals(29.75, gestor.resumen().ahorro, centavo)
        assertEquals(29.75, gestor.balance(), centavo)
    }

    @Test
    fun `un gasto se descuenta de los ahorros`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))
        assertEquals(100.0, gestor.resumen().ahorro, centavo)
        assertEquals(30.0, gestor.resumen().gasto, centavo)
        assertEquals(70.0, gestor.balance(), centavo)
    }

    @Test
    fun `el balance se recalcula en cada alta edicion y borrado`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        assertEquals(100.0, gestor.balance(), centavo)
        gestor.agregar(gasto("g1", 25.0))
        assertEquals(75.0, gestor.balance(), centavo)
        gestor.actualizar(gestor.buscar("g1")!!.copy(monto = 50.0))
        assertEquals(50.0, gestor.balance(), centavo)
        gestor.eliminar("g1")
        assertEquals(100.0, gestor.balance(), centavo)
        gestor.eliminar("a1")
        assertEquals(0.0, gestor.balance(), centavo)
    }

    // ------------------------------------------------------------------
    // 2. Altas
    // ------------------------------------------------------------------

    @Test
    fun `agregar varios ahorros suma en la cuenta total`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        gestor.agregar(ahorro("a2", 15.5))
        gestor.agregar(ahorro("a3", 4.25))
        gestor.agregar(ahorro("a4", 0.25))
        assertEquals(30.0, gestor.resumen().ahorro, centavo)
        assertEquals(30.0, gestor.balance(), centavo)
        assertEquals(4, gestor.movimientos.size)
    }

    @Test
    fun `agregar un gasto descuenta del balance`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))
        gestor.agregar(gasto("g2", 12.5))
        assertEquals(57.5, gestor.balance(), centavo)
        assertEquals(42.5, gestor.resumen().gasto, centavo)
    }

    @Test
    fun `un gasto mayor que el ahorro deja el balance en negativo`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 20.0))
        gestor.agregar(gasto("g1", 50.0))
        assertTrue(gestor.balance() < 0)
        assertEquals(-30.0, gestor.balance(), centavo)
    }

    // ------------------------------------------------------------------
    // 3. Ediciones
    // ------------------------------------------------------------------

    @Test
    fun `editar un gasto actualiza el balance y los ahorros`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))
        assertEquals(70.0, gestor.balance(), centavo)

        // El usuario se equivocó: eran 50, no 30.
        gestor.actualizar(gestor.buscar("g1")!!.copy(monto = 50.0))
        assertEquals(50.0, gestor.resumen().gasto, centavo)
        assertEquals(50.0, gestor.balance(), centavo)
    }

    @Test
    fun `editar el monto de un ahorro recalcula el balance al instante`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 10.0))
        assertEquals(90.0, gestor.balance(), centavo)

        gestor.actualizar(gestor.buscar("a1")!!.copy(monto = 250.0))
        assertEquals(250.0, gestor.resumen().ahorro, centavo)
        assertEquals(240.0, gestor.balance(), centavo)
    }

    @Test
    fun `editar la fecha de un movimiento lo mueve de dia`() {
        val gestor = GestorMovimientos()
        gestor.agregar(gasto("g1", 30.0, dia = hoy))
        assertEquals(1, Calculos.porDia(gestor.movimientos).size)

        // Editar reemplaza el movimiento (mismo id): no lo duplica, lo muda de día.
        gestor.actualizar(gestor.buscar("g1")!!.copy(ts = gasto("x", 30.0, dia = hoy.minusDays(3)).ts))
        val porDia = Calculos.porDia(gestor.movimientos)
        assertEquals(1, porDia.size)
        assertEquals(hoy.minusDays(3), porDia[0].fecha)
        assertEquals(1, gestor.movimientos.size)
        assertEquals(hoy.minusDays(3), gestor.buscar("g1")!!.fecha)
    }

    @Test
    fun `editar la fecha recalcula los totales por dia`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0, dia = hoy))
        gestor.agregar(gasto("g1", 30.0, dia = hoy))
        assertEquals(1, Calculos.porDia(gestor.movimientos).size)

        gestor.actualizar(gestor.buscar("g1")!!.copy(ts = gasto("x", 30.0, dia = hoy.minusDays(3)).ts))
        val porDia = Calculos.porDia(gestor.movimientos)
        assertEquals(2, porDia.size)
        assertEquals(hoy, porDia[0].fecha)
        assertEquals(100.0, porDia[0].ahorro, centavo)
        assertEquals(0.0, porDia[0].gasto, centavo)
        assertEquals(hoy.minusDays(3), porDia[1].fecha)
        assertEquals(0.0, porDia[1].ahorro, centavo)
        assertEquals(30.0, porDia[1].gasto, centavo)
    }

    @Test
    fun `editar la categoria recalcula los totales por categoria`() {
        val gestor = GestorMovimientos()
        gestor.agregar(gasto("g1", 10.0, categoria = "Comida"))
        gestor.agregar(gasto("g2", 20.0, categoria = "Comida"))
        gestor.agregar(gasto("g3", 5.0, categoria = "Transporte"))

        val antes = Calculos.porCategoria(gestor.movimientos)
        val comidaAntes = antes.first { it.categoria == "Comida" }
        assertEquals(2, comidaAntes.movimientos)
        assertEquals(-30.0, comidaAntes.total, centavo)

        gestor.actualizar(gestor.buscar("g1")!!.copy(categoria = "Transporte"))
        val despues = Calculos.porCategoria(gestor.movimientos)
        val comida = despues.first { it.categoria == "Comida" }
        val transporte = despues.first { it.categoria == "Transporte" }
        assertEquals(1, comida.movimientos)
        assertEquals(-20.0, comida.total, centavo)
        assertEquals(2, transporte.movimientos)
        assertEquals(-15.0, transporte.total, centavo)
    }

    @Test
    fun `editar un movimiento deja intacto el resto de la lista`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        gestor.agregar(ahorro("a2", 20.0, dia = hoy.minusDays(1)))
        gestor.agregar(gasto("g1", 5.0))
        gestor.agregar(gasto("g2", 7.0, dia = hoy.minusDays(2)))
        val antes = gestor.movimientos.associateBy { it.id }

        gestor.actualizar(gestor.buscar("g1")!!.copy(monto = 50.0, categoria = "Transporte", nota = "Corregido"))

        val despues = gestor.movimientos.associateBy { it.id }
        assertEquals(4, despues.size)
        assertEquals(antes["a1"], despues["a1"])
        assertEquals(antes["a2"], despues["a2"])
        assertEquals(antes["g2"], despues["g2"])
        assertEquals(50.0, despues["g1"]!!.monto, centavo)
    }

    @Test
    fun `editar un movimiento conserva su id y no lo duplica`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        gestor.agregar(gasto("g1", 30.0))

        gestor.actualizar(gestor.buscar("g1")!!.copy(monto = 40.0))
        assertEquals(2, gestor.movimientos.size)
        assertEquals(1, gestor.movimientos.count { it.id == "g1" })
        assertEquals(40.0, gestor.buscar("g1")!!.monto, centavo)
        assertEquals(Tipo.GASTO, gestor.buscar("g1")!!.tipo)
        assertEquals("Almuerzo", gestor.buscar("g1")!!.nota)
    }

    @Test
    fun `actualizar un id inexistente no altera la lista`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))

        gestor.actualizar(ahorro("fantasma", 999.0))
        assertEquals(2, gestor.movimientos.size)
        assertEquals(70.0, gestor.balance(), centavo)
        assertNull(gestor.buscar("fantasma"))
    }

    // ------------------------------------------------------------------
    // 4. Borrados
    // ------------------------------------------------------------------

    @Test
    fun `borrar un gasto devuelve el dinero a los ahorros`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))
        assertEquals(70.0, gestor.balance(), centavo)

        // Lo metió por error y lo borra: el saldo vuelve a la normalidad.
        gestor.eliminar("g1")
        assertEquals(100.0, gestor.balance(), centavo)
        assertEquals(0.0, gestor.resumen().gasto, centavo)
    }

    @Test
    fun `borrar un gasto devuelve exactamente su monto al balance`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))
        gestor.agregar(gasto("g2", 20.0))
        assertEquals(50.0, gestor.balance(), centavo)

        gestor.eliminar("g1")
        assertEquals(80.0, gestor.balance(), centavo)
        assertEquals(20.0, gestor.resumen().gasto, centavo)
    }

    @Test
    fun `borrar un ahorro recalcula el balance a la baja`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(ahorro("a2", 20.0))
        gestor.agregar(gasto("g1", 10.0))
        assertEquals(110.0, gestor.balance(), centavo)

        gestor.eliminar("a2")
        assertEquals(90.0, gestor.balance(), centavo)
    }

    @Test
    fun `borrar un ahorro resta exactamente su monto del balance`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(ahorro("a2", 25.0))
        gestor.agregar(gasto("g1", 5.0))
        assertEquals(120.0, gestor.balance(), centavo)

        gestor.eliminar("a2")
        assertEquals(95.0, gestor.balance(), centavo)
        assertEquals(100.0, gestor.resumen().ahorro, centavo)
    }

    @Test
    fun `borrar un movimiento que no existe no cambia nada`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 100.0))
        gestor.agregar(gasto("g1", 30.0))

        gestor.eliminar("no-existe")
        assertEquals(2, gestor.movimientos.size)
        assertEquals(70.0, gestor.balance(), centavo)
    }

    @Test
    fun `borrar deja la lista ordenada por fecha descendente`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("nuevo", 1.0, dia = hoy))
        gestor.agregar(ahorro("medio", 1.0, dia = hoy.minusDays(2)))
        gestor.agregar(ahorro("viejo", 1.0, dia = hoy.minusDays(5)))

        gestor.eliminar("medio")
        assertEquals(listOf("nuevo", "viejo"), gestor.movimientos.map { it.id })
    }

    // ------------------------------------------------------------------
    // 5. Groupación por día
    // ------------------------------------------------------------------

    @Test
    fun `los movimientos se agrupan por dia con sus totales`() {
        val movimientos = listOf(
            ahorro("a1", 10.0, dia = hoy),
            gasto("g1", 4.0, dia = hoy),
            ahorro("a2", 7.0, dia = hoy.minusDays(1))
        )
        val dias = Calculos.porDia(movimientos)
        assertEquals(2, dias.size)
        assertEquals(hoy, dias[0].fecha)
        assertEquals(10.0, dias[0].ahorro, centavo)
        assertEquals(4.0, dias[0].gasto, centavo)
        assertEquals(7.0, dias[1].ahorro, centavo)
        assertEquals(0.0, dias[1].gasto, centavo)
    }

    @Test
    fun `por dia ordena de la fecha mas reciente a la mas antigua`() {
        val movimientos = listOf(
            ahorro("a1", 10.0, dia = hoy.minusDays(5)),
            ahorro("a2", 20.0, dia = hoy),
            ahorro("a3", 30.0, dia = hoy.minusDays(2))
        )
        val dias = Calculos.porDia(movimientos)
        assertEquals(3, dias.size)
        assertEquals(hoy, dias[0].fecha)
        assertEquals(hoy.minusDays(2), dias[1].fecha)
        assertEquals(hoy.minusDays(5), dias[2].fecha)
    }

    @Test
    fun `por dia agrupa varios movimientos del mismo dia en un solo punto`() {
        val movimientos = listOf(
            ahorro("a1", 10.0, dia = hoy),
            ahorro("a2", 5.0, dia = hoy),
            gasto("g1", 3.0, dia = hoy)
        )
        val dias = Calculos.porDia(movimientos)
        assertEquals(1, dias.size)
        assertEquals(15.0, dias[0].ahorro, centavo)
        assertEquals(3.0, dias[0].gasto, centavo)
    }

    @Test
    fun `por dia separa el ahorro y el gasto del mismo dia`() {
        val movimientos = listOf(
            ahorro("a1", 10.0, dia = hoy),
            gasto("g1", 4.0, dia = hoy)
        )
        val dias = Calculos.porDia(movimientos)
        assertEquals(1, dias.size)
        assertEquals(10.0, dias[0].ahorro, centavo)
        assertEquals(4.0, dias[0].gasto, centavo)
    }

    @Test
    fun `por dia con lista vacia devuelve lista vacia`() {
        assertTrue(Calculos.porDia(emptyList()).isEmpty())
    }

    // ------------------------------------------------------------------
    // 6. Acumulado día por día
    // ------------------------------------------------------------------

    @Test
    fun `el acumulado por dia suma y resta en orden`() {
        val movimientos = listOf(
            ahorro("a1", 100.0, dia = hoy.minusDays(2)),
            gasto("g1", 25.0, dia = hoy.minusDays(1)),
            ahorro("a2", 10.0, dia = hoy)
        )
        val serie = Calculos.acumuladoPorDia(movimientos, hoy.minusDays(2), hoy)
        assertEquals(3, serie.size)
        assertEquals(100.0, serie[0], centavo)
        assertEquals(75.0, serie[1], centavo)
        assertEquals(85.0, serie[2], centavo)
    }

    @Test
    fun `el acumulado con lista vacia devuelve una serie de ceros`() {
        val serie = Calculos.acumuladoPorDia(emptyList(), hoy.minusDays(2), hoy)
        assertEquals(3, serie.size)
        assertEquals(0.0, serie[0], centavo)
        assertEquals(0.0, serie[1], centavo)
        assertEquals(0.0, serie[2], centavo)
    }

    @Test
    fun `el acumulado mantiene el saldo en los dias sin movimientos`() {
        val movimientos = listOf(
            ahorro("a1", 100.0, dia = hoy.minusDays(3)),
            gasto("g1", 25.0, dia = hoy.minusDays(1))
        )
        val serie = Calculos.acumuladoPorDia(movimientos, hoy.minusDays(3), hoy)
        assertEquals(4, serie.size)
        assertEquals(100.0, serie[0], centavo)  // día -3: entra el ahorro
        assertEquals(100.0, serie[1], centavo)  // día -2: sin movimientos, no se rompe
        assertEquals(75.0, serie[2], centavo)   // día -1: sale el gasto
        assertEquals(75.0, serie[3], centavo)   // día  0: sin movimientos
    }

    @Test
    fun `el acumulado puede quedar negativo cuando se gasta mas de lo que se ahorra`() {
        val movimientos = listOf(
            ahorro("a1", 20.0, dia = hoy.minusDays(1)),
            gasto("g1", 50.0, dia = hoy)
        )
        val serie = Calculos.acumuladoPorDia(movimientos, hoy.minusDays(1), hoy)
        assertEquals(2, serie.size)
        assertEquals(20.0, serie[0], centavo)
        assertEquals(-30.0, serie[1], centavo)
    }

    @Test
    fun `el acumulado con un rango de un solo dia devuelve un solo valor`() {
        val serie = Calculos.acumuladoPorDia(listOf(ahorro("a1", 50.0, dia = hoy)), hoy, hoy)
        assertEquals(1, serie.size)
        assertEquals(50.0, serie[0], centavo)
    }

    @Test
    fun `el acumulado ignora los movimientos fuera del rango pedido`() {
        val movimientos = listOf(
            ahorro("a1", 100.0, dia = hoy.minusDays(10)),
            ahorro("a2", 20.0, dia = hoy)
        )
        val serie = Calculos.acumuladoPorDia(movimientos, hoy.minusDays(1), hoy)
        assertEquals(2, serie.size)
        assertEquals(0.0, serie[0], centavo)
        assertEquals(20.0, serie[1], centavo)
    }

    @Test
    fun `un rango invertido no rompe la serie`() {
        val serie = Calculos.acumuladoPorDia(
            listOf(ahorro("a1", 10.0, dia = hoy)),
            hoy,
            hoy.minusDays(3)
        )
        assertTrue(serie.isNotEmpty())
        assertTrue(serie.all { it.isFinite() })
    }

    // ------------------------------------------------------------------
    // 7. Totales por categoría
    // ------------------------------------------------------------------

    @Test
    fun `el resumen por categoria ordena de mayor a menor`() {
        val movimientos = listOf(
            gasto("g1", 30.0, categoria = "Comida"),
            gasto("g2", 80.0, categoria = "Transporte"),
            gasto("g3", 10.0, categoria = "Comida")
        )
        val categorias = Calculos.porCategoria(movimientos)
        assertEquals(2, categorias.size)
        assertEquals("Transporte", categorias[0].categoria)
        assertEquals(2, categorias[1].movimientos)
        assertEquals(-40.0, categorias[1].total, centavo)
    }

    @Test
    fun `los totales por categoria cuentan los movimientos de cada categoria`() {
        val movimientos = listOf(
            gasto("g1", 30.0, categoria = "Comida"),
            gasto("g2", 80.0, categoria = "Transporte"),
            gasto("g3", 10.0, categoria = "Comida")
        )
        val categorias = Calculos.porCategoria(movimientos)
        val comida = categorias.first { it.categoria == "Comida" }
        val transporte = categorias.first { it.categoria == "Transporte" }
        assertEquals(2, comida.movimientos)
        assertEquals(-40.0, comida.total, centavo)
        assertEquals(1, transporte.movimientos)
        assertEquals(-80.0, transporte.total, centavo)
    }

    @Test
    fun `los totales por categoria usan signo positivo en el ahorro y negativo en el gasto`() {
        val movimientos = listOf(
            ahorro("a1", 100.0, origen = "Físico"),
            ahorro("a2", 15.0, origen = "Digital"),
            gasto("g1", 40.0, categoria = "Comida")
        )
        val categorias = Calculos.porCategoria(movimientos)
        val fisico = categorias.first { it.categoria == "Físico" }
        val digital = categorias.first { it.categoria == "Digital" }
        val comida = categorias.first { it.categoria == "Comida" }
        assertEquals(100.0, fisico.total, centavo)
        assertEquals(15.0, digital.total, centavo)
        assertEquals(-40.0, comida.total, centavo)
        // Orden de mayor a menor (por magnitud): Físico (100), Comida (40), Digital (15).
        assertEquals(listOf("Físico", "Comida", "Digital"), categorias.map { it.categoria })
    }

    @Test
    fun `por categoria con lista vacia devuelve lista vacia`() {
        assertTrue(Calculos.porCategoria(emptyList()).isEmpty())
    }

    // ------------------------------------------------------------------
    // 8. Casos borde
    // ------------------------------------------------------------------

    @Test
    fun `un movimiento con monto cero no cambia el balance`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 0.0))
        gestor.agregar(gasto("g1", 0.0))
        assertEquals(0.0, gestor.resumen().ahorro, centavo)
        assertEquals(0.0, gestor.resumen().gasto, centavo)
        assertEquals(0.0, gestor.balance(), centavo)
        assertEquals(2, gestor.movimientos.size)
    }

    @Test
    fun `un saldo negativo se mantiene negativo`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 20.0))
        gestor.agregar(gasto("g1", 50.0))
        assertTrue(gestor.balance() < 0)
        assertEquals(-30.0, gestor.balance(), centavo)
    }

    @Test
    fun `los decimales se suman con tolerancia de un centavo`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 0.1))
        gestor.agregar(ahorro("a2", 0.2))
        assertEquals(0.3, gestor.resumen().ahorro, centavo)
        assertEquals(0.3, gestor.balance(), centavo)
    }

    @Test
    fun `los decimales se restan bien contra un gasto`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 0.3))
        gestor.agregar(gasto("g1", 0.1))
        assertEquals(0.2, gestor.balance(), centavo)
    }

    @Test
    fun `los montos muy grandes se suman sin perder precision`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 1000000.50))
        gestor.agregar(ahorro("a2", 1000000.50))
        gestor.agregar(gasto("g1", 500000.25))
        assertEquals(2000001.00, gestor.resumen().ahorro, centavo)
        assertEquals(500000.25, gestor.resumen().gasto, centavo)
        assertEquals(1500000.75, gestor.balance(), centavo)
    }

    @Test
    fun `los montos muy grandes con centavos siguen siendo exactos`() {
        val gestor = GestorMovimientos()
        for (indice in 1..3) {
            gestor.agregar(ahorro("a$indice", 1000000.50))
        }
        assertEquals(3000001.50, gestor.resumen().ahorro, centavo)
        assertEquals(3000001.50, gestor.balance(), centavo)
    }

    // ------------------------------------------------------------------
    // 9. Orden de la lista
    // ------------------------------------------------------------------

    @Test
    fun `la lista se ordena por fecha y hora descendente`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("viejo", 1.0, dia = hoy.minusDays(5)))
        gestor.agregar(ahorro("nuevo", 2.0, dia = hoy))
        gestor.agregar(gasto("medio", 3.0, dia = hoy.minusDays(2)))
        assertEquals(listOf("nuevo", "medio", "viejo"), gestor.movimientos.map { it.id })
    }

    @Test
    fun `dentro del mismo dia manda la hora mas reciente primero`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("manana", 1.0, dia = hoy, hora = 6))
        gestor.agregar(ahorro("tarde", 2.0, dia = hoy, hora = 20))
        gestor.agregar(ahorro("medianoche", 3.0, dia = hoy, hora = 0))
        assertEquals(listOf("tarde", "manana", "medianoche"), gestor.movimientos.map { it.id })
    }

    @Test
    fun `agregar un movimiento mas viejo lo coloca al final de la lista`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("nuevo", 1.0, dia = hoy))
        gestor.agregar(ahorro("viejo", 2.0, dia = hoy.minusDays(3)))
        assertEquals(listOf("nuevo", "viejo"), gestor.movimientos.map { it.id })
    }

    @Test
    fun `la lista inicial se ordena al construir el gestor`() {
        val gestor = GestorMovimientos(
            listOf(
                ahorro("a", 1.0, dia = hoy.minusDays(1)),
                ahorro("b", 2.0, dia = hoy),
                ahorro("c", 3.0, dia = hoy.minusDays(4))
            )
        )
        assertEquals(listOf("b", "a", "c"), gestor.movimientos.map { it.id })
    }

    @Test
    fun `editar la fecha deja la lista ordenada otra vez`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("nuevo", 1.0, dia = hoy))
        gestor.agregar(ahorro("medio", 2.0, dia = hoy.minusDays(2)))
        gestor.agregar(ahorro("viejo", 3.0, dia = hoy.minusDays(5)))

        // "nuevo" pasa a ser el movimiento más antiguo.
        gestor.actualizar(
            gestor.buscar("nuevo")!!.copy(ts = ahorro("x", 1.0, dia = hoy.minusDays(9)).ts)
        )
        assertEquals(listOf("medio", "viejo", "nuevo"), gestor.movimientos.map { it.id })
    }

    // ------------------------------------------------------------------
    // 10. Modelo y búsqueda
    // ------------------------------------------------------------------

    @Test
    fun `se guarda la hora minuto y segundo del movimiento`() {
        val movimiento = ahorro("a1", 5.0, hora = 23, minuto = 59, segundo = 58)
        assertEquals(23, movimiento.momento.hour)
        assertEquals(59, movimiento.momento.minute)
        assertEquals(58, movimiento.momento.second)
    }

    @Test
    fun `la fecha del movimiento sale de su marca de tiempo`() {
        val movimiento = ahorro("a1", 5.0, dia = hoy.minusDays(3))
        assertEquals(hoy.minusDays(3), movimiento.fecha)
    }

    @Test
    fun `un movimiento de madrugada pertenece al mismo dia`() {
        val movimiento = gasto("g1", 5.0, dia = hoy, hora = 0, minuto = 5)
        assertEquals(hoy, movimiento.fecha)
        assertEquals(0, movimiento.momento.hour)
    }

    @Test
    fun `un movimiento de la noche pertenece al mismo dia`() {
        val movimiento = gasto("g1", 5.0, dia = hoy, hora = 23, minuto = 59, segundo = 59)
        assertEquals(hoy, movimiento.fecha)
    }

    @Test
    fun `buscar devuelve el movimiento con ese id`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        gestor.agregar(gasto("g1", 30.0))

        val encontrado = gestor.buscar("g1")
        assertNotNull(encontrado)
        assertEquals(30.0, encontrado!!.monto, centavo)
        assertEquals(Tipo.GASTO, encontrado.tipo)
    }

    @Test
    fun `buscar un id que no existe devuelve null`() {
        val gestor = GestorMovimientos()
        gestor.agregar(ahorro("a1", 10.0))
        assertNull(gestor.buscar("no-existe"))
    }
}
