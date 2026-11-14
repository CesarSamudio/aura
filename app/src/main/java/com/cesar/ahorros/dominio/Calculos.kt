package com.cesar.ahorros.dominio

import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Tipo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Toda la lógica del dinero vive acá, en Kotlin puro (sin Android).
 * Así se puede probar en el PC con tests de JVM, sin emulador ni teléfono.
 */
data class Resumen(val ahorro: Double, val gasto: Double) {
    val balance: Double get() = ahorro - gasto
}

data class PuntoDia(
    val fecha: LocalDate,
    val ahorro: Double,
    val gasto: Double
)

data class PuntoCategoria(
    val categoria: String,
    val movimientos: Int,
    val total: Double
)

object Calculos {

    fun totalAhorro(movimientos: List<Movimiento>): Double =
        movimientos.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto }

    fun totalGasto(movimientos: List<Movimiento>): Double =
        movimientos.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }

    fun resumen(movimientos: List<Movimiento>): Resumen =
        Resumen(totalAhorro(movimientos), totalGasto(movimientos))

    /** Agrupa por día, del más reciente al más viejo. */
    fun porDia(movimientos: List<Movimiento>): List<PuntoDia> =
        movimientos.groupBy { it.fecha }
            .map { (fecha, lista) ->
                PuntoDia(
                    fecha = fecha,
                    ahorro = lista.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto },
                    gasto = lista.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
                )
            }
            .sortedByDescending { it.fecha }

    /**
     * Saldo acumulado día por día entre [desde] y [hasta] (ambos inclusive).
     * Cada gasto resta, cada ahorro suma: es el "balance" que ve el usuario.
     */
    fun acumuladoPorDia(
        movimientos: List<Movimiento>,
        desde: LocalDate,
        hasta: LocalDate
    ): List<Double> {
        val dias = ChronoUnit.DAYS.between(desde, hasta).toInt().coerceAtLeast(0)
        var acumulado = 0.0
        return (0..dias).map { indice ->
            val fecha = desde.plusDays(indice.toLong())
            val delDia = movimientos.filter { it.fecha == fecha }
            acumulado += delDia.sumOf { if (it.tipo == Tipo.AHORRO) it.monto else -it.monto }
            acumulado
        }
    }

    /** Totales por categoría, de mayor a menor. */
    fun porCategoria(movimientos: List<Movimiento>): List<PuntoCategoria> =
        movimientos.groupBy { it.categoria }
            .map { (categoria, lista) ->
                PuntoCategoria(
                    categoria = categoria,
                    movimientos = lista.size,
                    total = lista.sumOf { if (it.tipo == Tipo.AHORRO) it.monto else -it.monto }
                )
            }
            .sortedByDescending { kotlin.math.abs(it.total) }
}

/**
 * Gestor de la lista de movimientos. Todas las altas, ediciones y borrados
 * pasan por acá, así el balance siempre se recalcula desde la fuente de verdad.
 */
class GestorMovimientos(inicial: List<Movimiento> = emptyList()) {

    var movimientos: List<Movimiento> = inicial.sortedByDescending { it.ts }
        private set

    fun agregar(movimiento: Movimiento) {
        movimientos = (movimientos + movimiento).sortedByDescending { it.ts }
    }

    fun actualizar(movimiento: Movimiento) {
        movimientos = movimientos
            .map { if (it.id == movimiento.id) movimiento else it }
            .sortedByDescending { it.ts }
    }

    fun eliminar(id: String) {
        movimientos = movimientos.filterNot { it.id == id }
    }

    fun buscar(id: String): Movimiento? = movimientos.firstOrNull { it.id == id }

    fun resumen(): Resumen = Calculos.resumen(movimientos)

    fun balance(): Double = resumen().balance
}
