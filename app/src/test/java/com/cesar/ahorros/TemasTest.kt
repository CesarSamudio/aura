package com.cesar.ahorros

import com.cesar.ahorros.ui.CatalogoTemas
import com.cesar.ahorros.ui.TemaActual
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del CATÁLOGO DE TEMAS (la parte de la app que no necesita Android).
 *
 * Corren en el PC (JVM puro), sin emulador ni teléfono: `gradlew test` o
 * `gradlew testReleaseUnitTest`.
 *
 * Se vigilan tres cosas:
 *  - que el catálogo tenga los 7 temas y que cada id sea único (el id es lo
 *    que se guarda en el teléfono para recordar el tema elegido),
 *  - que buscar un tema que no existe devuelva el de por defecto en vez de
 *    reventar (un tema borrado en una versión nueva no puede tumbar la app),
 *  - que dentro de un tema los tres niveles de texto se distingan entre sí.
 *
 * `TemaActual` es estado GLOBAL de la app: al terminar cada test se deja el
 * tema por defecto, así ningún otro test queda con un tema cambiado.
 */
class TemasTest {

    @Before
    fun preparar() {
        // El tema puede haber quedado cambiado por otro test: se vuelve al de
        // por defecto para que cada test arranque siempre igual.
        TemaActual.aplicar(CatalogoTemas.porDefecto.id)
    }

    @After
    fun dejarElTemaPorDefecto() {
        // OJO: `TemaActual` es estado global. Si un test lo deja en otro tema,
        // los tests que vengan después ven colores que no corresponden.
        TemaActual.aplicar(CatalogoTemas.porDefecto.id)
    }

    // ---------------------------------------------------------------------
    // El catálogo
    // ---------------------------------------------------------------------

    @Test
    fun `el catalogo tiene siete temas`() {
        assertEquals("La app debe tener 7 temas", 7, CatalogoTemas.todos.size)
    }

    @Test
    fun `no hay dos temas con el mismo id`() {
        val ids = CatalogoTemas.todos.map { it.id }
        assertEquals(
            "Hay dos temas con el mismo id: ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}",
            ids.size,
            ids.distinct().size
        )
    }

    @Test
    fun `el tema por defecto es el clasico`() {
        assertEquals("El tema por defecto debe ser el «clasico»", "clasico", CatalogoTemas.porDefecto.id)
        assertEquals("El tema por defecto se llama «Clásico»", "Clásico", CatalogoTemas.porDefecto.nombre)
    }

    @Test
    fun `buscar un tema que no existe devuelve el tema por defecto`() {
        assertEquals(
            "Un id nulo debe devolver el tema por defecto",
            CatalogoTemas.porDefecto.id,
            CatalogoTemas.porId(null).id
        )
        assertEquals(
            "Un id que no existe debe devolver el tema por defecto",
            CatalogoTemas.porDefecto.id,
            CatalogoTemas.porId("no-existe").id
        )
        assertEquals(
            "Un id vacío debe devolver el tema por defecto",
            CatalogoTemas.porDefecto.id,
            CatalogoTemas.porId("").id
        )
    }

    @Test
    fun `buscar un tema que existe devuelve ese tema`() {
        for (tema in CatalogoTemas.todos) {
            assertEquals("Busqué «${tema.id}» y no lo encontré", tema.id, CatalogoTemas.porId(tema.id).id)
        }
    }

    // ---------------------------------------------------------------------
    // Los colores de cada tema
    // ---------------------------------------------------------------------

    @Test
    fun `los tres niveles de texto de cada tema se distinguen entre si`() {
        for (tema in CatalogoTemas.todos) {
            val textos = listOf(tema.textoPrimario, tema.textoSecundario, tema.textoTercero)
            assertEquals(
                "En el tema «${tema.id}» hay dos niveles de texto con el mismo color",
                3,
                textos.distinct().size
            )
        }
    }

    @Test
    fun `cada tema tiene seis colores en la paleta`() {
        for (tema in CatalogoTemas.todos) {
            assertEquals("La paleta del tema «${tema.id}» debe tener 6 colores", 6, tema.paleta.size)
            assertEquals(
                "En la paleta del tema «${tema.id}» hay colores repetidos",
                6,
                tema.paleta.distinct().size
            )
        }
    }

    @Test
    fun `cada tema tiene un fondo que se distingue de su superficie`() {
        for (tema in CatalogoTemas.todos) {
            assertTrue(
                "En el tema «${tema.id}» el fondo y la superficie son el mismo color",
                tema.fondo != tema.superficie
            )
        }
    }

    // ---------------------------------------------------------------------
    // El tema activo
    // ---------------------------------------------------------------------

    @Test
    fun `aplicar un tema cambia el tema activo`() {
        assertTrue(
            "Aplicar «osmo» debería cambiar el tema activo",
            TemaActual.aplicar("osmo")
        )
        assertEquals("El tema activo debe ser «osmo»", "osmo", TemaActual.tema.id)
    }

    @Test
    fun `aplicar el tema que ya esta activo no cambia nada`() {
        TemaActual.aplicar("osmo")
        assertFalse(
            "Aplicar «osmo» dos veces no debería cambiar el tema",
            TemaActual.aplicar("osmo")
        )
        assertEquals("El tema activo debe seguir siendo «osmo»", "osmo", TemaActual.tema.id)
    }

    @Test
    fun `aplicar un tema que no existe deja el tema por defecto`() {
        // Primero se sale del tema por defecto: si no, aplicar un id
        // inexistente no cambiaría nada (caería en el tema que ya está).
        TemaActual.aplicar("osmo")
        assertTrue(
            "Aplicar «no-existe» debería caer en el tema por defecto",
            TemaActual.aplicar("no-existe")
        )
        assertEquals(
            "El tema activo debe ser el por defecto",
            CatalogoTemas.porDefecto.id,
            TemaActual.tema.id
        )
    }
}
