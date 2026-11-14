package com.cesar.ahorros.data

import kotlinx.serialization.Serializable

/**
 * Una lista de gente que queda guardada para las próximas deudas.
 *
 * Ejemplos: los compañeros de un curso, los del equipo, los de la clase.
 * La idea es no escribir los nombres a mano cada vez: se abre la lista, se
 * marca lo que falta y listo.
 */
@Serializable
data class ListaDeGente(
    /** Identificador único de la lista. */
    val id: String,
    /** Cómo se llama la lista ("Curso de Historia", "Los del 11G"...). */
    val nombre: String,
    /** Los nombres de la gente, en el orden en que se guardaron. */
    val personas: List<String>,
    /**
     * Las listas que vienen con la app (la del 11G) **no se pueden borrar**:
     * vienen con el código y se vuelven a poner cada vez que se abre la app.
     */
    val bloqueada: Boolean = false
)
