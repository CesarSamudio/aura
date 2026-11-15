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
 * - La deuda dice **cuántas personas tienen que pagarlo** ([Deuda.personasQuePagan]).
 * - La lista de personas NO divide nada: es a quién se le está cobrando.
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
     * SIN valor por defecto a propósito: si el archivo viejo no lo tiene, la
     * carga falla y [RepositorioDeudas] lo repara poniéndole la cantidad de
     * personas que había en la lista.
     */
    val personasQuePagan: Int,
    val personas: List<PersonaDeuda> = emptyList(),
    val pagos: List<Pago> = emptyList(),
    val saldada: Boolean = false,
    val ts: Long
)
