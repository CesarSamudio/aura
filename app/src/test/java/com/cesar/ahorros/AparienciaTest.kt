package com.cesar.ahorros

import androidx.compose.ui.graphics.Color
import com.cesar.ahorros.data.AjustesApariencia
import com.cesar.ahorros.data.ColorTema
import com.cesar.ahorros.data.ColorTexto
import com.cesar.ahorros.data.EstiloGraficas
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.EstiloNumeros
import com.cesar.ahorros.ui.AparienciaActual
import com.cesar.ahorros.ui.CatalogoTemas
import com.cesar.ahorros.ui.Contraste
import com.cesar.ahorros.ui.PaletaPersonalizada
import com.cesar.ahorros.ui.TemaActual
import com.cesar.ahorros.ui.aAjustes
import com.cesar.ahorros.ui.aHex
import com.cesar.ahorros.ui.aPaleta
import com.cesar.ahorros.ui.conPaleta
import com.cesar.ahorros.ui.hexAColor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de la **personalización** de la app: los colores que el usuario
 * cambia a mano, el contraste que se le avisa y el interruptor de las
 * animaciones.
 *
 * Son tests JVM puros: no dibujan nada. Lo que se comprueba es la lógica:
 * que el color del usuario quede ENCIMA del tema, que se pueda quitar, que
 * lo que se guarda vuelve igual, y que el interruptor maestro apague de
 * verdad las tres animaciones.
 */
class AparienciaTest {

    @Before
    fun antesDeCadaTest() {
        AparienciaActual.paleta = PaletaPersonalizada.VACIA
        AparienciaActual.animaciones = com.cesar.ahorros.data.AjustesAnimacion()
        AparienciaActual.fondo = com.cesar.ahorros.data.AjustesFondo()
        TemaActual.aplicar(CatalogoTemas.porDefecto.id)
    }

    @After
    fun despuesDeCadaTest() {
        AparienciaActual.paleta = PaletaPersonalizada.VACIA
        AparienciaActual.animaciones = com.cesar.ahorros.data.AjustesAnimacion()
        AparienciaActual.fondo = com.cesar.ahorros.data.AjustesFondo()
        TemaActual.aplicar(CatalogoTemas.porDefecto.id)
    }

    // ---------------------------------------------------------------------
    // La paleta encima del tema
    // ---------------------------------------------------------------------

    @Test
    fun `una paleta vacia deja el tema tal cual`() {
        val tema = CatalogoTemas.CLASICO
        assertEquals(tema, tema.conPaleta(PaletaPersonalizada.VACIA))
    }

    @Test
    fun `un color cambiado queda encima del del tema`() {
        val tema = CatalogoTemas.CLASICO
        val nuevo = Color(0xFF00FF00)
        val conPersonalizacion = tema.conPaleta(
            PaletaPersonalizada().conTema(ColorTema.ACENTO_MENTA, nuevo)
        )

        assertEquals(
            "El acento verde tiene que ser el que eligió el usuario",
            nuevo,
            conPersonalizacion.acentoMenta
        )
        assertEquals(
            "Los demás colores no se tocan",
            tema.acentoCalido,
            conPersonalizacion.acentoCalido
        )
    }

    @Test
    fun `los textos se cambian por separado de los colores de la app`() {
        val tema = CatalogoTemas.CLASICO
        val conPaleta = tema.conPaleta(
            PaletaPersonalizada().conTema(ColorTema.ACENTO_MARCA, Color(0xFF123456))
        )
        val conTextos = conPaleta.conPaleta(
            PaletaPersonalizada().conTexto(ColorTexto.TEXTO_PRIMARIO, Color(0xFFABCDEF))
        )

        assertEquals(
            "El texto principal tiene que ser el nuevo",
            Color(0xFFABCDEF),
            conTextos.textoPrimario
        )
        assertEquals(
            "El acento de la app no se mueve por cambiar un texto",
            Color(0xFF123456),
            conTextos.acentoCalido
        )
    }

    @Test
    fun `quitar un color devuelve el del tema`() {
        val tema = CatalogoTemas.CLASICO
        val paleta = PaletaPersonalizada().conTema(
            ColorTema.COLOR_GASTO,
            Color(0xFFFF0000)
        )
        val conRojo = tema.conPaleta(paleta)
        val sinRojo = tema.conPaleta(paleta.conTema(ColorTema.COLOR_GASTO, null))

        assertEquals(
            "Con el color puesto sale el del usuario",
            Color(0xFFFF0000),
            conRojo.colorGasto
        )
        assertEquals(
            "Sin personalización el rojo vuelve a ser el del tema",
            tema.colorGasto,
            sinRojo.colorGasto
        )
    }

    @Test
    fun `la paleta dice cuantos colores cambio el usuario`() {
        var paleta = PaletaPersonalizada.VACIA
        assertTrue("Una paleta nueva está vacía", paleta.vacia)

        paleta = paleta.conTema(ColorTema.FONDO, Color(0xFF101010))
        paleta = paleta.conTexto(ColorTexto.TEXTO_PRIMARIO, Color(0xFFFFFFFF))

        assertFalse("Ahora tiene colores", paleta.vacia)
        assertEquals("Cambió dos colores", 2, paleta.cantidad)
    }

    @Test
    fun `la huella cambia cuando cambia un color`() {
        val uno = PaletaPersonalizada().conTema(ColorTema.FONDO, Color(0xFF101010))
        val otro = uno.conTema(ColorTema.FONDO, Color(0xFF202020))

        assertNotEquals(
            "Dos colores distintos tienen que dar huellas distintas",
            uno.huella,
            otro.huella
        )
        assertEquals(
            "La misma paleta tiene la misma huella",
            uno.huella,
            uno.conTema(ColorTema.FONDO, Color(0xFF101010)).huella
        )
    }

    // ---------------------------------------------------------------------
    // El hexadecimal
    // ---------------------------------------------------------------------

    @Test
    fun `el hexadecimal ida y vuelta`() {
        val color = Color(0xFF5A00FF)
        assertEquals("#5A00FF", color.aHex())
        assertEquals(color, hexAColor("#5A00FF"))
        assertEquals(color, hexAColor("5A00FF"))
    }

    @Test
    fun `un hexadecimal que no es un color se rechaza`() {
        assertNull("No es un color", hexAColor("hola"))
        assertNull("Le falta un dígito", hexAColor("#12345"))
        assertNull("No son números", hexAColor("#GGGGGG"))
    }

    // ---------------------------------------------------------------------
    // Guardar y volver a leer
    // ---------------------------------------------------------------------

    @Test
    fun `la paleta guardada y leida es la misma`() {
        val paleta = PaletaPersonalizada()
            .conTema(ColorTema.ACENTO_MARCA, Color(0xFFFF5A00))
            .conTexto(ColorTexto.TEXTO_SECUNDARIO, Color(0xFFA1A1AA))

        val ajustes = paleta.aAjustes()
        assertEquals(
            "El acento tiene que quedar guardado con su id",
            "#FF5A00",
            ajustes.coloresTema[ColorTema.ACENTO_MARCA.id]
        )

        val leida = ajustes.aPaleta()
        assertEquals(
            "El color tiene que volver igual",
            Color(0xFFFF5A00),
            leida.colorTema(ColorTema.ACENTO_MARCA)
        )
        assertEquals(
            "El texto tiene que volver igual",
            Color(0xFFA1A1AA),
            leida.colorTexto(ColorTexto.TEXTO_SECUNDARIO)
        )
    }

    @Test
    fun `un color que no se reconoce en los ajustes se ignora`() {
        val ajustes = AjustesApariencia(
            coloresTema = mapOf(
                "ACENTO_MARCA" to "#FF5A00",
                "NO_EXISTE" to "#123456"
            )
        )

        val paleta = ajustes.aPaleta()
        assertEquals("Solo se lee el color que se conoce", 1, paleta.tema.size)
        assertEquals(
            Color(0xFFFF5A00),
            paleta.colorTema(ColorTema.ACENTO_MARCA)
        )
    }

    @Test
    fun `un color raro guardado no rompe la app`() {
        val ajustes = AjustesApariencia(
            coloresTema = mapOf("FONDO" to "no-es-un-color")
        )

        val paleta = ajustes.aPaleta()
        assertTrue("El color inválido se ignora", paleta.vacia)
    }

    // ---------------------------------------------------------------------
    // El contraste
    // ---------------------------------------------------------------------

    @Test
    fun `el contraste coincide con los valores de referencia de WCAG`() {
        assertEquals(
            "Blanco sobre negro",
            21.0,
            Contraste.ratio(Color(0xFFFFFFFF), Color(0xFF000000)),
            0.001
        )
        assertEquals(
            "Un color contra sí mismo",
            1.0,
            Contraste.ratio(Color(0xFF123456), Color(0xFF123456)),
            0.0001
        )
        assertEquals(
            "El gris de referencia de WCAG sobre blanco",
            4.54,
            Contraste.ratio(Color(0xFF767676), Color(0xFFFFFFFF)),
            0.01
        )
    }

    @Test
    fun `el contraste avisa si el texto no se lee`() {
        assertTrue(
            "El blanco sobre el fondo oscuro se lee",
            Contraste.textoSeLee(Color(0xFFFFFFFF), Color(0xFF040308))
        )
        assertFalse(
            "El gris claro sobre el gris oscuro no se lee",
            Contraste.textoSeLee(Color(0xFF9A9A9A), Color(0xFF8A8A8A))
        )
    }

    // ---------------------------------------------------------------------
    // El interruptor maestro de las animaciones
    // ---------------------------------------------------------------------

    @Test
    fun `con las animaciones apagadas los tres estilos se apagan`() {
        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(
            activas = false,
            graficas = EstiloGraficas.CRECER.id,
            numeros = EstiloNumeros.CONTAR.id,
            interfaz = EstiloInterfaz.DESLIZAR.id
        )

        assertEquals(
            "Las gráficas no se animan",
            EstiloGraficas.NINGUNA,
            AparienciaActual.estiloGraficas
        )
        assertEquals(
            "Los números no se animan",
            EstiloNumeros.NINGUNA,
            AparienciaActual.estiloNumeros
        )
        assertEquals(
            "La interfaz no se anima",
            EstiloInterfaz.NINGUNA,
            AparienciaActual.estiloInterfaz
        )
    }

    @Test
    fun `con las animaciones prendidas cada estilo es el que eligio el usuario`() {
        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(
            activas = true,
            graficas = EstiloGraficas.DIBUJAR.id,
            numeros = EstiloNumeros.FUNDIDO.id,
            interfaz = EstiloInterfaz.ESCALAR.id
        )

        assertEquals(EstiloGraficas.DIBUJAR, AparienciaActual.estiloGraficas)
        assertEquals(EstiloNumeros.FUNDIDO, AparienciaActual.estiloNumeros)
        assertEquals(EstiloInterfaz.ESCALAR, AparienciaActual.estiloInterfaz)
    }

    @Test
    fun `la duracion sale de la velocidad que eligio el usuario`() {
        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(
            activas = true,
            velocidad = 1.0f
        )
        val normal = AparienciaActual.duracion(900)

        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(velocidad = 2.0f)
        val rapida = AparienciaActual.duracion(900)

        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(velocidad = 0.5f)
        val lenta = AparienciaActual.duracion(900)

        assertTrue("La rápida dura menos que la normal", rapida < normal)
        assertTrue("La lenta dura más que la normal", lenta > normal)
        assertEquals("La normal dura lo que dice", 900, normal)
    }

    @Test
    fun `con las animaciones apagadas no hay duracion que esperar`() {
        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(activas = false)
        assertEquals(
            "Apagadas no se anima nada",
            0,
            AparienciaActual.duracion(900)
        )
    }

    @Test
    fun `el fondo se sabe si hay algo puesto`() {
        assertFalse(
            "De fábrica no hay fondo",
            AparienciaActual.hayFondo
        )

        AparienciaActual.fondo = AparienciaActual.fondo.copy(imagen = "fondo.jpg")
        assertTrue("Con imagen hay fondo", AparienciaActual.hayFondo)

        AparienciaActual.fondo = AparienciaActual.fondo.copy(
            imagen = null,
            degradadoActivo = true
        )
        assertTrue("Con degradado hay fondo", AparienciaActual.hayFondo)
    }
}
