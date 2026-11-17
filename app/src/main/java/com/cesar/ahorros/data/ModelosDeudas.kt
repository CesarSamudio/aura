package com.cesar.ahorros.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Modelo de la pestaña "Deudas".
 *
 * Una deuda NO es ingreso ni gasto: se maneja aparte. Es el dinero que Cesar
 * puso para comprar algo (un banner, por ejemplo) y que le van devolviendo
 * las personas que deben.
 *
 * Hay dos formas de armar una deuda:
 * - [ModoDeuda.META]: César sabe cuánto cuesta la cosa (la "meta"). La cuota
 *   sale de dividir la meta entre las personas que la pagan.
 * - [ModoDeuda.LIBRE]: César no sabe el total, pero sí cuánto va a cobrar a
 *   cada quien. El total se calcula sumando.
 *
 * En los dos modos:
 * - La deuda dice **cuántas personas tienen que pagarlo**
 *   ([Deuda.personasQuePagan]). Ese campo es **opcional**: si no se dice
 *   cuántos van a pagar, la cuota sale de la gente que se vaya añadiendo a la
 *   lista ([Deuda.cuotaAutomatica]).
 * - Con el número a mano, la lista de personas NO divide nada: es a quién se
 *   le está cobrando.
 * - Cada persona puede tener un **monto propio** (el "caso especial"): unos
 *   pagan 4 y otros 2.
 * - En modo META, el monto propio **redistribuye**: los que pagan la cuota
 *   normal suben para que entre todos se llegue a la meta. En modo LIBRE los
 *   demás no cambian de monto y el total se ajusta.
 * - Lo que cada uno ya pagó NO se toca: se conserva, y lo que falta por pagar
 *   es su cuota menos lo pagado.
 * - Si alguien pagó de más, el excedente se conserva (queda a favor).
 * - "Saldada" se marca a mano con un botón, no sola.
 */

/** Cómo se armaron los números de la deuda. */
@Serializable
enum class ModoDeuda(val etiqueta: String) {
    /** César sabe el total (el costo de la cosa) y se divide entre los que pagan. */
    META("Sé el total"),

    /** César sabe cuánto cobra a cada uno y el total se calcula. */
    LIBRE("Cuánto paga cada uno")
}

/** Cómo pagó la persona. YAPI es la única forma de "mandar" el comprobante. */
@Serializable
enum class MetodoPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    YAPI("YAPI")
}

/**
 * Una persona a la que se le cobra parte de la deuda.
 *
 * - [cuotaPropia] es el "caso especial": el monto que debe pagar esta persona.
 *   Si es `null`, paga la cuota normal (la que sale del modo de la deuda).
 * - [metodoPago] es información adicional: cómo paga esta persona (efectivo o
 *   YAPI). El comprobante solo tiene sentido con YAPI: es la única forma de
 *   mandar el archivo.
 * - [pruebas] son los comprobantes adjuntos de esta persona: el nombre de los
 *   archivos dentro de la carpeta interna de la app (no la ruta completa).
 */
@Serializable
data class PersonaDeuda(
    val id: String,
    val nombre: String,
    val cuotaPropia: Double? = null,
    val metodoPago: MetodoPago? = null,
    val pruebas: List<String> = emptyList()
)

/**
 * Un pago de una persona.
 *
 * [comprobante] es el NOMBRE del archivo de la imagen dentro de la carpeta
 * interna de comprobantes (no la ruta completa), para que el JSON siga siendo
 * válido si la app cambia de carpeta.
 */
@Serializable
data class Pago(
    val id: String,
    val personaId: String,
    val monto: Double,
    val metodo: MetodoPago,
    val nota: String = "",
    val comprobante: String? = null,
    /**
     * El movimiento de ahorro que creó este pago.
     *
     * Solo existe si la preferencia de "los pagos de las deudas van a los
     * ahorros" está **prendida**. Se guarda el id para que, si después se
     * borra el pago, se borre también el depósito: los dos van y vienen
     * juntos (si no, el dinero se quedaría en los ahorros para siempre).
     */
    val movimientoId: String? = null,
    val ts: Long
) {
    val momento: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())

    val fecha: LocalDate
        get() = momento.toLocalDate()
}

/**
 * Una deuda completa: qué se compró, cuánto costó, cuántas personas lo van a
 * pagar, a quién se le cobra y qué pagó cada uno.
 *
 * - [total]: la meta, cuando el modo es [ModoDeuda.META]. En modo LIBRE se
 *   calcula y este valor no se usa.
 * - [montoPorPersona]: lo que paga cada uno por defecto, cuando el modo es
 *   [ModoDeuda.LIBRE]. En modo META se calcula y este valor no se usa.
 * - [cuotaPersonalizada]: la cuota fija que Cesar cobra a cada uno, cuando no
 *   quiere que el costo se divida. Es opcional: si no está, la cuota sale
 *   de dividir como siempre.
 * - [personasQuePagan]: cuántos van a pagar. Es **opcional**: si no se dice,
 *   la cuota sale de la lista de personas ([cuotaAutomatica]).
 */
@Serializable
data class Deuda(
    val id: String,
    val nombre: String,
    val modo: ModoDeuda = ModoDeuda.META,
    val total: Double = 0.0,
    val montoPorPersona: Double = 0.0,
    /**
     * La cuota que César cobra a cada uno cuando no quiere dividir el total.
     *
     * Si es `null` (lo normal), la cuota sale de dividir el total: el
     * comportamiento de siempre. Si tiene valor, ESA cuota manda y el total a
     * cobrar sale de multiplicarla por la gente, no de dividir el costo.
     */
    val cuotaPersonalizada: Double? = null,
    /**
     * Cuántas personas tienen que pagar este total. De ahí sale la cuota.
     *
     * Es **opcional**: `null` = no se dijo cuántos van a pagar. En ese caso la
     * cuota se calcula con la gente que está en la lista (ver
     * [cuotaAutomatica]): si se añaden 4 personas, la cuota es la cuarta parte
     * del total, sin que haga falta saber de antemano cuántos son.
     *
     * Los archivos viejos del teléfono (que sí traen el número) se leen sin
     * problema: el valor por defecto solo se usa cuando el campo no está.
     */
    val personasQuePagan: Int? = null,
    /**
     * Marca de **cuota automática**: la cuota se calcula con la gente que está
     * en la lista y no con [personasQuePagan].
     *
     * Sirve para distinguir dos cosas que se ven igual en el archivo:
     * - "el usuario no dijo cuántos van a pagar" (deuda nueva en automático:
     *   se deja el número en `null` y se marca acá).
     * - "este archivo es viejo y le falta el campo" (eso se repara al cargar,
     *   en [RepositorioDeudas]).
     *
     * El valor por defecto sale del propio [personasQuePagan]: si no hay
     * número, la cuota es automática. Si al editar una deuda el número y la
     * marca quedan distintos, es el [RepositorioDeudas] el que los deja de
     * nuevo en sincronía antes de guardar el archivo.
     */
    val cuotaAutomatica: Boolean = personasQuePagan == null,
    val personas: List<PersonaDeuda> = emptyList(),
    val pagos: List<Pago> = emptyList(),
    val saldada: Boolean = false,
    /**
     * Si la deuda es **individual**: le debe a una sola persona.
     *
     * En ese caso no hay lista de personas que añadir: la deuda tiene una
     * sola (la que le debe) y su nombre ES el nombre de la deuda. Todo lo
     * demás sigue igual: el modo, los montos, los pagos y el comprobante.
     *
     * Los archivos viejos del teléfono no tienen este campo: sale `false`
     * (una deuda normal, como siempre).
     */
    val individual: Boolean = false,
    val ts: Long
)
