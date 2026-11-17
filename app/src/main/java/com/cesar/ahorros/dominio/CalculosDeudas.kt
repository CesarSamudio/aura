package com.cesar.ahorros.dominio

import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import java.time.LocalDate

/**
 * Cálculos de la pestaña "Deudas". Kotlin puro, sin Android: se prueban en el
 * PC con tests de JVM.
 *
 * **Todos los montos se redondean a 2 decimales** (centavos): los números con
 * many decimales (3.333333333333334) no sirven ni para leer ni para cobrar.
 *
 * Los dos modos:
 * - [ModoDeuda.META]: se sabe la meta (cuánto cuesta la cosa). La cuota sale
 *   de dividir la meta entre los que pagan, **descontando los montos propios**:
 *   si uno paga $2 especial y son 3 personas con meta de $10, los otros dos
 *   pagan (10 − 2) ÷ 2 = $4 cada uno. El monto especial **redistribuye**.
 * - [ModoDeuda.LIBRE]: se sabe cuánto cobra a cada uno. Los montos propios no
 *   cambian el monto de los demás: lo que cambia es el total, que se calcula.
 *
 * En modo META hay una tercera opción: la **cuota personalizada**. Cuando
 * César pone una cuota a mano (por ejemplo, $1,90 en vez de los $1,86 que
 * salen de dividir), ESA cuota es la que cobra cada uno y el total a cobrar
 * sale de multiplicarla por la gente. El costo real se conserva aparte: no
 * se borra, se muestra como dato aparte. Sin cuota personalizada (o sea, en
 * `null`, que es lo normal) nada de esto cambia: se divide como siempre.
 *
 * El **número de personas que pagan es opcional**: si César no lo dice (o no
 * lo sabe), la cuota sale de la gente que se fue añadiendo a la lista. Eso es
 * lo que hace [pagadoresDe]: devuelve cuántos pagan ahora mismo, y en modo
 * automático son los que están en la lista.
 */
object CalculosDeudas {

    /**
     * Redondea a centavos. Todo lo que se muestra y lo que se cobra pasa por
     * acá: nadie necesita ver 3.333333333333334.
     */
    fun redondear(monto: Double): Double = Math.round(monto * 100.0) / 100.0

    /** Suma de los montos propios (los "casos especiales") de la lista. */
    private fun sumaPropios(deuda: Deuda): Double =
        deuda.personas.sumOf { it.cuotaPropia ?: 0.0 }

    /** Cuántas personas de la lista tienen monto propio. */
    private fun cantidadPropios(deuda: Deuda): Int =
        deuda.personas.count { it.cuotaPropia != null }

    /**
     * Cuántas personas pagan la deuda ahora mismo.
     *
     * - Si hay un número a mano ([Deuda.personasQuePagan]), manda ESE número:
     *   la lista es a quién se le cobra, no quién divide.
     * - Si NO se dijo (la deuda quedó en automática, o es un archivo viejo sin
     *   reparar), manda la **gente de la lista**: la cuota sale de los que se
     *   fueron añadiendo. Por eso no hace falta saber de antemano cuántos
     *   son: con 60 personas en la lista, la cuota es el total ÷ 60.
     *
     * Nunca da cero (si todavía no hay nadie en la lista se cuenta como 1)
     * para no dividir por cero.
     */
    fun pagadoresDe(deuda: Deuda): Int {
        val declarados = deuda.personasQuePagan
        return if (declarados != null) declarados else maxOf(1, deuda.personas.size)
    }

    /**
     * True si la deuda está en **cuota automática**: la cuota sale de la gente
     * de la lista porque no se dijo cuántos van a pagar.
     *
     * Se pregunta por el número y no por la marca ([Deuda.cuotaAutomatica])
     * porque el número es lo que el usuario tiene delante: si puso uno, el
     * número manda. La marca sirve para el archivo (ver
     * [com.cesar.ahorros.data.RepositorioDeudas]).
     */
    fun esAutomatica(deuda: Deuda): Boolean = deuda.personasQuePagan == null

    /**
     * El total real de la deuda.
     * - META: la meta que puso Cesar. Si hay cuota personalizada, el total que
     *   se COBRA es la cuota × la gente (más el ajuste de los montos
     *   especiales): el costo real queda guardado aparte, en la deuda.
     * - LIBRE: lo que se cobra sumando (base × gente, más o menos los casos
     *   especiales).
     */
    fun totalDe(deuda: Deuda): Double = when (deuda.modo) {
        ModoDeuda.META -> {
            val cuota = deuda.cuotaPersonalizada
            if (cuota != null) {
                // Con cuota fija el total a cobrar sale de multiplicar, igual
                // que en modo libre: el costo real no se reparte.
                val base = redondear(cuota)
                val n = pagadoresDe(deuda)
                // Los que tienen monto propio pagan su monto en vez de la base.
                val ajuste = sumaPropios(deuda) - base * cantidadPropios(deuda)
                redondear(base * n + ajuste)
            } else {
                redondear(deuda.total)
            }
        }
        ModoDeuda.LIBRE -> {
            val base = redondear(deuda.montoPorPersona)
            // En modo libre no se divide: el total sale de multiplicar por la
            // gente. Con la cuota automática, esa gente es la de la lista.
            val n = pagadoresDe(deuda)
            // Los que tienen monto propio pagan su monto en vez de la base.
            val ajuste = sumaPropios(deuda) - base * cantidadPropios(deuda)
            redondear(base * n + ajuste)
        }
    }

    /**
     * Lo que paga cada uno por defecto (la "cuota normal").
     * - META: (meta − montos propios) ÷ (personas que pagan − las de monto
     *   propio). El monto especial redistribuye: los demás suben para llegar
     *   a la meta. Si hay **cuota personalizada**, esa manda: es la que se
     *   cobra a cada uno y el monto especial de uno NO redistribuye (igual
     *   que en modo libre).
     * - LIBRE: el monto que puso Cesar, sin redistribuir.
     *
     * Con la cuota automática (no se dijo cuántos pagan), los que pagan son
     * los de la lista: si todavía no hay nadie, la cuota es 0 (no hay a quién
     * cobrarle) y se calcula sola cuando se añada gente.
     */
    fun montoBaseDe(deuda: Deuda): Double = when (deuda.modo) {
        ModoDeuda.META -> {
            val cuota = deuda.cuotaPersonalizada
            if (cuota != null) {
                redondear(cuota)
            } else if (esAutomatica(deuda) && deuda.personas.isEmpty()) {
                // Todavía no hay nadie en la lista: no hay cuota que repartir.
                0.0
            } else {
                val restantes = pagadoresDe(deuda) - cantidadPropios(deuda)
                if (restantes <= 0) 0.0 else redondear((deuda.total - sumaPropios(deuda)) / restantes)
            }
        }
        ModoDeuda.LIBRE -> redondear(deuda.montoPorPersona)
    }

    /** Lo que le toca a esta persona: su monto propio o la cuota normal. */
    fun cuotaDe(deuda: Deuda, persona: PersonaDeuda): Double =
        persona.cuotaPropia ?: montoBaseDe(deuda)

    /** Suma de lo que pagó cada persona. */
    fun pagadoPorPersona(deuda: Deuda): Map<String, Double> =
        deuda.pagos.groupBy { it.personaId }
            .mapValues { (_, pagos) -> redondear(pagos.sumOf { it.monto }) }

    fun resumenPersona(deuda: Deuda, personaId: String): ResumenPersona? {
        val persona = deuda.personas.firstOrNull { it.id == personaId } ?: return null
        val pagado = pagadoPorPersona(deuda)[personaId] ?: 0.0
        return construirResumen(persona, cuotaDe(deuda, persona), pagado)
    }

    private fun construirResumen(persona: PersonaDeuda, cuota: Double, pagado: Double) =
        ResumenPersona(
            persona = persona,
            cuota = redondear(cuota),
            pagado = redondear(pagado),
            pendiente = redondear((cuota - pagado).coerceAtLeast(0.0)),
            excedente = redondear((pagado - cuota).coerceAtLeast(0.0)),
            esCuotaPropia = persona.cuotaPropia != null
        )

    /** Todas las personas de la lista, con su estado. */
    fun personas(deuda: Deuda): List<ResumenPersona> {
        val pagado = pagadoPorPersona(deuda)
        return deuda.personas.map { persona ->
            construirResumen(persona, cuotaDe(deuda, persona), pagado[persona.id] ?: 0.0)
        }
    }

    /** Resumen general de la deuda. */
    fun resumen(deuda: Deuda): ResumenDeuda {
        val totalPagado = redondear(deuda.pagos.sumOf { it.monto })
        val resúmenes = personas(deuda)
        return ResumenDeuda(
            total = totalDe(deuda),
            montoBase = montoBaseDe(deuda),
            totalPagado = totalPagado,
            personasDeben = resúmenes.count { it.pendiente > 0.0 },
            personasEnLista = deuda.personas.size,
            saldo = redondear((totalDe(deuda) - totalPagado).coerceAtLeast(0.0))
        )
    }

    /**
     * El resumen que se le MUESTRA al usuario.
     *
     * Si la deuda está marcada como saldada a mano, se muestra cerrada: no
     * hay nada que cobrar. La decisión del usuario manda sobre los números.
     * Los números reales quedan intactos en [resumen] y vuelven a verse al
     * reabrir la deuda.
     */
    fun resumenParaMostrar(deuda: Deuda): ResumenDeuda {
        val real = resumen(deuda)
        if (!deuda.saldada) return real
        return real.copy(personasDeben = 0, saldo = 0.0)
    }

    /** Cuánto falta por cobrar en total. */
    fun saldo(deuda: Deuda): Double = resumen(deuda).saldo

    /** Los pagos agrupados por día, del día más reciente al más viejo. */
    fun pagosPorDia(deuda: Deuda): List<GrupoPagos> =
        deuda.pagos.groupBy { it.fecha }
            .map { (fecha, lista) -> GrupoPagos(fecha, lista.sortedByDescending { it.ts }) }
            .sortedByDescending { it.fecha }

    /** Los pagos de una persona, del más nuevo al más viejo. */
    fun pagosDe(deuda: Deuda, personaId: String): List<Pago> =
        deuda.pagos.filter { it.personaId == personaId }.sortedByDescending { it.ts }

    /**
     * Lo que falta por pagar de una persona: su cuota menos lo que ya pagó.
     * Si ya pagó de más, no debe nada (el excedente queda a favor).
     */
    fun pendienteDe(deuda: Deuda, personaId: String): Double =
        resumenPersona(deuda, personaId)?.pendiente ?: 0.0
}

/** Cómo va una persona dentro de la deuda. */
data class ResumenPersona(
    val persona: PersonaDeuda,
    /** Lo que le toca pagar: la cuota normal o su monto propio. */
    val cuota: Double,
    val pagado: Double,
    /** Lo que le falta por pagar: max(0, cuota - pagado). */
    val pendiente: Double,
    /** Lo que pagó de más (queda a favor): max(0, pagado - cuota). */
    val excedente: Double,
    /** True si esta persona tiene un monto propio (el "caso especial"). */
    val esCuotaPropia: Boolean
) {
    val estaSaldada: Boolean get() = pendiente <= 0.0
}

/** Cómo va la deuda completa. */
data class ResumenDeuda(
    /**
     * El total de la deuda: la meta, o lo que sale de sumar. Con cuota
     * personalizada es el total A COBRAR (la cuota × la gente).
     */
    val total: Double,
    /** Lo que paga cada uno por defecto (la cuota normal). */
    val montoBase: Double,
    val totalPagado: Double,
    /** En la lista, cuántas personas todavía deben algo. */
    val personasDeben: Int,
    /** Cuántas personas hay en la lista. */
    val personasEnLista: Int,
    /**
     * Cuánto falta por cobrar en total (el dinero, no la lista). Se mide
     * contra el total a cobrar: por eso, con cuota personalizada, el saldo y
     * la suma de lo que deben los de la lista dan el mismo número.
     */
    val saldo: Double
) {
    val estaSaldada: Boolean get() = saldo <= 0.0
}

/** Un día con sus pagos, para mostrar la lista agrupada por fecha. */
data class GrupoPagos(
    val fecha: LocalDate,
    val pagos: List<Pago>
)
