package com.cesar.ahorros.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class Tipo { AHORRO, GASTO }

/**
 * Un movimiento es un ingreso (ahorro) o un gasto.
 * Se guarda con marca de tiempo epoch para conservar hora, minuto y segundo.
 */
@Serializable
data class Movimiento(
    val id: String,
    val tipo: Tipo,
    val monto: Double,
    val categoria: String,
    val nota: String,
    val ts: Long
) {
    val momento: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())

    val fecha: LocalDate
        get() = momento.toLocalDate()
}

/** De dónde viene el ahorro: físico (en alcancía/bolsillo) o digital (banco, billetera). */
enum class FuenteAhorro(val etiqueta: String) {
    FISICO("Físico"),
    DIGITAL("Digital")
}

/** Categoría de un gasto. */
enum class CategoriaGasto(val etiqueta: String) {
    COMIDA("Comida"),
    TRANSPORTE("Transporte"),
    ENTRETENIMIENTO("Entretenimiento"),
    NECESIDAD("Necesidad"),
    TECNOLOGICO("Tecnológico"),
    OTROS("Otros")
}
