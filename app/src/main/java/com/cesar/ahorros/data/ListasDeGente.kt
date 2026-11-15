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
     * Las listas que vienen con la app (la del 11G) no se guardan en el
     * archivo del usuario: se ponen solas cada vez que se abre la app.
     *
     * Ojo: eso NO significa que no se puedan borrar. Si el usuario borra una,
     * queda anotada en los ajustes (`ajustes.json`) para que no vuelva a
     * salir sola, y se puede devolver desde la pantalla de "Añadir personas".
     * Lo que este dato marca es de dónde viene la lista, no si se puede
     * borrar.
     */
    val bloqueada: Boolean = false
)
