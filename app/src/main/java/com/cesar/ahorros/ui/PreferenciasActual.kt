package com.cesar.ahorros.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cesar.ahorros.data.AjustesPreferencias

/**
 * Las **preferencias** que están activas ahora mismo.
 *
 * Son estado de Compose (como [AparienciaActual]): cuando el usuario mueve
 * un interruptor en Ajustes, el cambio se ve **al instante** en toda la app,
 * sin reiniciar nada.
 *
 * ## Qué hay acá
 *
 * - [pagosDeDeudasEnAhorros]: el dinero que se cobra de una deuda
 *   también entra a los ahorros.
 * - [combinarAhorrosYGastos]: al gastar, los ahorros bajan (los dos montos
 *   se combinan en uno).
 */
object PreferenciasActual {

    /** Las preferencias completas, tal como están ahora. */
    var preferencias by mutableStateOf(AjustesPreferencias())

    /** Carga las preferencias guardadas (al abrir la app). */
    fun cargar(ajustes: AjustesPreferencias) {
        preferencias = ajustes
    }

    /**
     * Si el dinero que se cobra de una deuda también entra a los ahorros.
     *
     * Con esto apagado, el pago vive solo dentro de la deuda: se marca "ya
     * pagó" y los ahorros no se tocan.
     */
    val pagosDeDeudasEnAhorros: Boolean
        get() = preferencias.pagosDeDeudasEnAhorros

    /**
     * Si los gastos bajan el dinero de los ahorros.
     *
     * Con esto apagado, lo que gastás no toca lo que ahorraste: son dos
     * montones aparte.
     */
    val combinarAhorrosYGastos: Boolean
        get() = preferencias.combinarAhorrosYGastos
}
