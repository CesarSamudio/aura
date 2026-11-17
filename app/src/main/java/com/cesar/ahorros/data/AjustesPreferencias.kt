package com.cesar.ahorros.data

import kotlinx.serialization.Serializable

/**
 * Las **preferencias** de la app: cómo se relacionan los ahorros con las
 * deudas y con los gastos.
 *
 * Son distintas de la apariencia: la apariencia es de **VER** (colores, fondo,
 * animaciones) y esto es de **CÓMO FUNCIONA**. Por eso viven en su propio
 * apartado de los ajustes y en su propio campo del archivo.
 *
 * Las dos vienen **apagadas**: la app se comporta como siempre hasta que el
 * usuario las enciende. Nadie quiere que sus números cambien de la noche a la
 * mañana sin que él lo pida.
 */
@Serializable
data class AjustesPreferencias(
    /**
     * Si el dinero que se cobra de una deuda también entra a los ahorros.
     *
     * Antes el pago vivía solo dentro de la deuda: se marcaba "ya pagó" y ya.
     * Con esto prendido, marcar a alguien como pagado **suma el monto a los
     * ahorros** (como un depósito más, con la nota de a qué deuda pertenece).
     *
     * Si después se borra ese pago, el depósito se borra también: los dos
     * van y vienen juntos.
     */
    val pagosDeDeudasEnAhorros: Boolean = false,
    /**
     * Si los gastos bajan el dinero de los ahorros.
     *
     * Con esto apagado (como siempre), los gastos y los ahorros son dos
     * montones aparte: lo que gastás no toca lo que ahorraste. Con esto
     * prendido, los dos se **combinan**: el dinero que tenés es lo que metiste
     * menos lo que gastaste, así que al gastar, los ahorros bajan.
     */
    val combinarAhorrosYGastos: Boolean = false
)
