package com.cesar.ahorros.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val es = Locale("es")

private fun simbolos() = DecimalFormatSymbols(es)

/** $1,234.56 — dos decimales siempre. */
fun Double.dinero(): String {
    val df = DecimalFormat("#,##0.00", simbolos())
    return df.format(this)
}

/** $1,235 — para ejes de gráficas donde el centavo no aporta. */
fun Double.dineroCorto(): String {
    val df = DecimalFormat("#,##0", simbolos())
    return df.format(this)
}

private val fmtFechaLarga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
private val fmtFechaCorta = DateTimeFormatter.ofPattern("dd MMM", es)
private val fmtDiaSemana = DateTimeFormatter.ofPattern("EEEE", es)
private val fmtHora = DateTimeFormatter.ofPattern("HH:mm:ss", es)
private val fmtMesAnio = DateTimeFormatter.ofPattern("MMMM yyyy", es)
private val fmtSoloDia = DateTimeFormatter.ofPattern("d 'de' MMMM", es)

fun LocalDate.fechaLarga(): String = fmtFechaLarga.format(this).replaceFirstChar { it.uppercase() }

/** Sin el día de la semana: "13 de noviembre". */
fun LocalDate.fechaSinDia(): String = fmtSoloDia.format(this).replaceFirstChar { it.uppercase() }

fun LocalDate.fechaCorta(): String = fmtFechaCorta.format(this).replaceFirstChar { it.uppercase() }

fun LocalDate.diaSemana(): String = fmtDiaSemana.format(this).replaceFirstChar { it.uppercase() }

fun LocalDate.mesAnio(): String = fmtMesAnio.format(this).replaceFirstChar { it.uppercase() }

fun LocalDateTime.hora(): String = fmtHora.format(this)

fun LocalDateTime.fechaYHora(): String = "${toLocalDate().fechaCorta()} · ${hora()}"

/**
 * Importe con el símbolo antes del número: "$1,234.56" o "−$69,40".
 * El signo va adelante (con el signo tipográfico U+2212, no un guion).
 */
fun Double.dineroSimbolo(): String {
    val texto = dinero()
    return if (texto.startsWith("-")) "−$${texto.substring(1)}" else "$$texto"
}
