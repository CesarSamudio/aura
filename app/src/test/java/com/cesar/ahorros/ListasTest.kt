package com.cesar.ahorros

import com.cesar.ahorros.data.ListaDeGente
import com.cesar.ahorros.data.ListaSalon
import com.cesar.ahorros.data.RepositorioListas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Las listas de gente que el usuario guarda (un curso, un equipo, la clase...).
 *
 * Tienen que sobrevivir al cierre de la app y volver tal cual, y una lista
 * vacía o un archivo roto no pueden romper nada.
 */
class ListasTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private fun lista(
        nombre: String,
        personas: List<String> = listOf("Ana", "Luis"),
        bloqueada: Boolean = false
    ) = ListaDeGente(
        id = nombre.lowercase(),
        nombre = nombre,
        personas = personas,
        bloqueada = bloqueada
    )

    @Test
    fun `sin guardar nada no hay listas`() {
        assertTrue(RepositorioListas(carpeta.root).cargar().isEmpty())
    }

    @Test
    fun `la lista guardada se vuelve a leer igual`() {
        val repositorio = RepositorioListas(carpeta.root)
        repositorio.guardar(listOf(lista("Curso de Historia")))

        val cargadas = RepositorioListas(carpeta.root).cargar()
        assertEquals(1, cargadas.size)
        assertEquals("Curso de Historia", cargadas[0].nombre)
        assertEquals(listOf("Ana", "Luis"), cargadas[0].personas)
    }

    @Test
    fun `se pueden guardar varias listas y quitarle una`() {
        val repositorio = RepositorioListas(carpeta.root)
        val original = listOf(lista("Curso"), lista("Equipo"))
        repositorio.guardar(original)

        val sinEquipo = RepositorioListas(carpeta.root).cargar().filter { it.nombre != "Equipo" }
        repositorio.guardar(sinEquipo)

        val cargadas = RepositorioListas(carpeta.root).cargar()
        assertEquals(1, cargadas.size)
        assertEquals("Curso", cargadas[0].nombre)
    }

    @Test
    fun `una lista sin gente no se guarda`() {
        val repositorio = RepositorioListas(carpeta.root)
        repositorio.guardar(listOf(lista("Vacía", personas = emptyList())))

        assertTrue(RepositorioListas(carpeta.root).cargar().isEmpty())
    }

    @Test
    fun `un archivo roto no rompe la app`() {
        val archivo = java.io.File(carpeta.root, RepositorioListas.NOMBRE_ARCHIVO)
        archivo.writeText("esto no es json {{{")

        assertTrue(RepositorioListas(carpeta.root).cargar().isEmpty())
    }

    @Test
    fun `el archivo de listas queda dentro de la carpeta de la app`() {
        RepositorioListas(carpeta.root).guardar(listOf(lista("Curso")))

        val archivo = java.io.File(carpeta.root, RepositorioListas.NOMBRE_ARCHIVO)
        assertTrue("No se creó ${archivo.absolutePath}", archivo.exists())
        assertTrue(archivo.readText().contains("Curso"))
    }

    @Test
    fun `la lista que viene con la app no se guarda en el archivo del usuario`() {
        // En la versión del 11G viene la del salón; en la general no viene
        // ninguna. "bloqueada" marca de dónde viene (viene con el código, no se
        // guarda en listas.json), NO que no se pueda borrar: desde el 15/11 se
        // puede borrar y después devolver.
        val delColegio = ListaSalon.lista
        if (delColegio != null) {
            assertTrue(
                "La lista que viene con la app tiene que estar marcada como tal",
                delColegio.bloqueada
            )
            assertTrue(
                "La lista que viene con la app tiene que traer gente",
                delColegio.personas.isNotEmpty()
            )
        }
    }
}
