package com.cesar.ahorros.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Los ajustes que la app recuerda entre sesiones.
 *
 * Por ahora hay dos: **el tema elegido** y **el ícono de la app**. Se guardan
 * en un archivo JSON dentro del almacenamiento interno (como los movimientos y
 * las deudas), así que entra también en la copia de seguridad: si respaldás y
 * restaurás, el tema y el ícono vuelven con los datos.
 *
 * Sin base de datos, sin SharedPreferences y sin permisos: es un archivo de
 * una línea dentro de la carpeta de la app.
 */
class Preferencias(private val carpeta: File) {

    /** Constructor de conveniencia para producción: guarda en `context.filesDir`. */
    constructor(context: Context) : this(context.filesDir)

    private val archivo = File(carpeta, NOMBRE_ARCHIVO)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** El id del tema elegido; `null` si todavía no se eligió ninguno. */
    fun temaId(): String? = leer()?.tema

    /** El id del ícono elegido; `null` si todavía no se eligió ninguno. */
    fun iconoId(): String? = leer()?.icono

    /**
     * Las listas que vienen con la app (la del salón del 11G) que el usuario
     * borró.
     *
     * La lista del salón no está en `listas.json` (viene con el código), así
     * que para que no vuelva a salir en cada apertura hay que anotar acá que
     * el usuario la quitó.
     */
    fun listasBorradas(): List<String> = leer()?.listasBorradas ?: emptyList()

    /** Anota que una lista que viene con la app fue borrada. */
    fun guardarListaBorrada(id: String) {
        val actual = leer() ?: AjustesLocales()
        if (id in actual.listasBorradas) return
        escribir(actual.copy(listasBorradas = actual.listasBorradas + id))
    }

    /** Saca la anotación: la lista vuelve a salir. */
    fun restaurarLista(id: String) {
        val actual = leer() ?: AjustesLocales()
        if (id !in actual.listasBorradas) return
        escribir(actual.copy(listasBorradas = actual.listasBorradas - id))
    }

    /** Guarda el tema elegido (el ícono se queda como está). */
    fun guardarTema(id: String) {
        escribir((leer() ?: AjustesLocales()).copy(tema = id))
    }

    /** Guarda el ícono elegido (el tema se queda como está). */
    fun guardarIcono(id: String) {
        escribir((leer() ?: AjustesLocales()).copy(icono = id))
    }

    private fun leer(): AjustesLocales? = try {
        if (archivo.exists()) {
            json.decodeFromString<AjustesLocales>(archivo.readText())
        } else {
            null
        }
    } catch (e: Exception) {
        // Un archivo roto no puede romper la app: se empieza de cero.
        null
    }

    private fun escribir(valor: AjustesLocales) {
        val temporal = File(carpeta, NOMBRE_ARCHIVO + ".tmp")
        try {
            temporal.writeText(json.encodeToString(valor))
            // Camino ideal: rename atómico (nunca se ve un archivo a medias).
            if (archivo.exists()) archivo.delete()
            if (!temporal.renameTo(archivo)) {
                // El rename puede estar bloqueado: se cae a copia.
                temporal.copyTo(archivo, overwrite = true)
            }
        } catch (e: Exception) {
            // Si no se puede guardar, la app sigue funcionando con el tema actual.
        } finally {
            if (temporal.exists()) temporal.delete()
        }
    }

    companion object {
        /** Nombre del archivo de ajustes dentro de la carpeta de la app. */
        const val NOMBRE_ARCHIVO = "ajustes.json"
    }
}

/** El contenido del archivo de ajustes. */
@Serializable
data class AjustesLocales(
    /** Id del tema elegido (ver `CatalogoTemas`). */
    val tema: String? = null,
    /** Id del ícono de la app elegido (ver `CatalogoIconos`). */
    val icono: String? = null,
    /** Ids de las listas que vienen con la app y el usuario borró. */
    val listasBorradas: List<String> = emptyList()
)
