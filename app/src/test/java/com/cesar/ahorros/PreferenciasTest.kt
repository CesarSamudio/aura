package com.cesar.ahorros

import com.cesar.ahorros.data.AjustesAnimacion
import com.cesar.ahorros.data.AjustesApariencia
import com.cesar.ahorros.data.AjustesFondo
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
        // El tema, la apariencia y las listas borradas viven en el MISMO
        // archivo: guardar uno no puede borrar lo que ya estaba.
        val preferencias = Preferencias(carpeta.root)
        preferencias.guardarApariencia(
            AjustesApariencia(
                coloresTema = mapOf("ACENTO_MARCA" to "#123456")
            )
        )
        preferencias.guardarListaBorrada("salon11g")
        preferencias.guardarTema("osmo")

        val otra = Preferencias(carpeta.root)
        assertEquals("osmo", otra.temaId())
        assertEquals(
            "#123456",
            otra.apariencia().coloresTema["ACENTO_MARCA"]
        )
        assertEquals(listOf("salon11g"), otra.listasBorradas())
    }

    @Test
    fun `sin tocar nada la apariencia sale de fabrica`() {
        val preferencias = Preferencias(carpeta.root)
        assertTrue(
            "La paleta sale vacía",
            preferencias.apariencia().coloresTema.isEmpty()
        )
        assertTrue(
            "Las animaciones salen prendidas",
            preferencias.apariencia().animaciones.activas
        )
        assertNull(
            "No hay imagen de fondo",
            preferencias.apariencia().fondo.imagen
        )
    }

    @Test
    fun `la apariencia completa se guarda y se vuelve a leer`() {
        val preferencias = Preferencias(carpeta.root)
        val ajustes = AjustesApariencia(
            coloresTema = mapOf("ACENTO_MARCA" to "#FF5A00", "FONDO" to "#101010"),
            coloresTexto = mapOf("TEXTO_PRIMARIO" to "#FFFFFF"),
            animaciones = AjustesAnimacion(
                activas = false,
                velocidad = 1.5f,
                graficas = "DIBUJAR",
                numeros = "FUNDIDO",
                interfaz = "ESCALAR"
            ),
            fondo = AjustesFondo(
                imagen = "fondo.jpg",
                opacidadImagen = 0.6f,
                difuminadoImagen = 0.3f,
                degradadoActivo = true,
                formaDegradado = "AURORA",
                opacidadDegradado = 0.4f,
                velocidadDegradado = 2f,
                intensidadDegradado = 0.7f
            )
        )
        preferencias.guardarApariencia(ajustes)

        val otra = Preferencias(carpeta.root)
        assertEquals(ajustes, otra.apariencia())
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

    @Test
    fun `el archivo de ajustes viejo se sigue leyendo al actualizar la app`() {
        // Cuando se actualiza la app, el archivo de ajustes que quedó del
        // teléfono es el viejo: sin la apariencia y sin las listas borradas.
        // Tiene que seguir cargando (el tema intacto) y los campos nuevos
        // salen vacíos.
        val archivo = File(carpeta.root, Preferencias.NOMBRE_ARCHIVO)
        archivo.writeText("""{"tema":"osmo"}""")

        val preferencias = Preferencias(carpeta.root)
        assertEquals("osmo", preferencias.temaId())
        assertTrue(
            "Las listas borradas salen vacías",
            preferencias.listasBorradas().isEmpty()
        )
        assertTrue(
            "La apariencia sale vacía",
            preferencias.apariencia().coloresTema.isEmpty()
        )
    }

    @Test
    fun `un archivo de ajustes con el icono viejo se sigue leyendo`() {
        // La v1.13 guardaba el ícono de la app en este mismo archivo. Ese
        // campo ya no existe (la app tiene un solo ícono), pero el archivo
        // viejo tiene que seguir cargando sin romperse.
        val archivo = File(carpeta.root, Preferencias.NOMBRE_ARCHIVO)
        archivo.writeText("""{"tema":"osmo","icono":"v3"}""")

        val preferencias = Preferencias(carpeta.root)
        assertEquals("osmo", preferencias.temaId())
    }
}
