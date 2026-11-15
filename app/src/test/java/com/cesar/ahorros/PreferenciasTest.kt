package com.cesar.ahorros

import com.cesar.ahorros.data.Preferencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * El tema elegido tiene que sobrevivir:
 *  - cuando la app se cierra y se vuelve a abrir (se guarda en el archivo),
 *  - y cuando el usuario saca una copia de seguridad y la restaura.
 *
 * Es un test de JVM puro: `Preferencias` recibe una carpeta, no un `Context`,
 * así que corre en el PC sin emulador.
 */
class PreferenciasTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    @Test
    fun `sin tocar nada no hay tema guardado`() {
        val preferencias = Preferencias(carpeta.root)
        assertNull("No se eligió ningún tema todavía", preferencias.temaId())
    }

    @Test
    fun `el tema elegido se guarda y se vuelve a leer`() {
        val preferencias = Preferencias(carpeta.root)
        preferencias.guardarTema("boost")

        // Otra instancia, como si la app se hubiera cerrado y abierto de nuevo.
        val otra = Preferencias(carpeta.root)
        assertEquals("boost", otra.temaId())
    }

    @Test
    fun `el archivo de ajustes queda dentro de la carpeta de la app`() {
        Preferencias(carpeta.root).guardarTema("osmo")

        val archivo = File(carpeta.root, Preferencias.NOMBRE_ARCHIVO)
        assertTrue("No se creó ${archivo.absolutePath}", archivo.exists())
        assertTrue(
            "El archivo de ajustes tiene que ser JSON legible",
            archivo.readText().contains("osmo")
        )
    }

    @Test
    fun `un archivo roto no rompe la app`() {
        val archivo = File(carpeta.root, Preferencias.NOMBRE_ARCHIVO)
        archivo.writeText("esto no es json {{{")

        // Devuelve null (el tema por defecto) en vez de reventar.
        assertNull(Preferencias(carpeta.root).temaId())
    }

    @Test
    fun `guardar dos veces deja el ultimo tema`() {
        val preferencias = Preferencias(carpeta.root)
        preferencias.guardarTema("osmo")
        preferencias.guardarTema("trunk")

        assertEquals("trunk", Preferencias(carpeta.root).temaId())
    }

    @Test
    fun `guardar un ajuste no borra los otros`() {
        // El tema, el ícono y las listas borradas viven en el MISMO archivo:
        // guardar uno no puede borrar lo que ya estaba.
        val preferencias = Preferencias(carpeta.root)
        preferencias.guardarIcono("v3")
        preferencias.guardarListaBorrada("salon11g")
        preferencias.guardarTema("osmo")

        val otra = Preferencias(carpeta.root)
        assertEquals("osmo", otra.temaId())
        assertEquals("v3", otra.iconoId())
        assertEquals(listOf("salon11g"), otra.listasBorradas())
    }

    @Test
    fun `una lista que viene con la app se puede borrar y devolver`() {
        // La lista del salón no está en el archivo de listas: para que no
        // vuelva a salir sola, se anota acá que el usuario la borró.
        val preferencias = Preferencias(carpeta.root)
        assertTrue(preferencias.listasBorradas().isEmpty())

        preferencias.guardarListaBorrada("salon11g")
        assertEquals(listOf("salon11g"), Preferencias(carpeta.root).listasBorradas())

        preferencias.restaurarLista("salon11g")
        assertTrue(Preferencias(carpeta.root).listasBorradas().isEmpty())
    }

    @Test
    fun `borrar dos veces la misma lista no la duplica`() {
        val preferencias = Preferencias(carpeta.root)
        preferencias.guardarListaBorrada("salon11g")
        preferencias.guardarListaBorrada("salon11g")

        assertEquals(listOf("salon11g"), Preferencias(carpeta.root).listasBorradas())
    }
}
