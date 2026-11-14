package com.cesar.ahorros

import com.cesar.ahorros.data.ListaSalon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La lista del salón del 11G.
 *
 * Este archivo de test vive en `src/testG11`: corre **solo** en la versión del
 * 11G (la que trae la lista escrita). En la versión general no hay lista del
 * salón, y eso se prueba en `ListasTest`.
 */
class ListaSalonTest {

    @Test
    fun `la lista del salon tiene a los 24 alumnos`() {
        val lista = ListaSalon.lista
        assertTrue("La versión del 11G tiene que traer la lista del salón", lista != null)
        val personas = lista!!.personas

        // Son 24: en la lista original el número 8 no viene (salta del 7 al 9).
        assertEquals(24, personas.size)
        assertEquals("Kenneth Yassir Alvarado Hurtado", personas.first())
        assertEquals("LUIS CALDERON", personas.last())
        assertTrue(personas.any { it == "Cesar Antonio Samudio Chavarria" })
    }

    @Test
    fun `la lista del salon no tiene nombres repetidos`() {
        val personas = ListaSalon.lista!!.personas
        assertEquals(personas.size, personas.distinct().size)
    }

    @Test
    fun `la lista del salon viene bloqueada para que no se pueda borrar`() {
        assertTrue("La lista del salón no se puede borrar", ListaSalon.lista!!.bloqueada)
    }
}
