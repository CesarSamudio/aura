package com.cesar.ahorros.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Las listas de gente que el usuario guarda, en su propio archivo JSON.
 *
 * Va aparte de `movimientos.json` y `deudas.json` a propósito: son los
 * "borrador" de trabajo (las listas), no datos de la app.
 *
 * Mismo patrón que [Repositorio]: constructor con la CARPETA para poder
 * probarlo en un test de JVM puro, y constructor de conveniencia con Context.
 */
class RepositorioListas(private val carpeta: File) {

    /** Constructor de conveniencia para producción: guarda en `context.filesDir`. */
    constructor(context: Context) : this(context.filesDir)

    private val archivo = File(carpeta, NOMBRE_ARCHIVO)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Las listas guardadas (sin la que viene con la app). */
    fun cargar(): List<ListaDeGente> = try {
        if (archivo.exists()) {
            json.decodeFromString<List<ListaDeGente>>(archivo.readText())
                .filter { it.personas.isNotEmpty() }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Guarda las listas. */
    fun guardar(listas: List<ListaDeGente>): Boolean {
        val temporal = File(carpeta, NOMBRE_ARCHIVO + ".tmp")
        return try {
            temporal.writeText(json.encodeToString(listas))
            if (archivo.exists()) archivo.delete()
            if (!temporal.renameTo(archivo)) {
                temporal.copyTo(archivo, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            if (temporal.exists()) temporal.delete()
        }
    }

    companion object {
        /** Nombre del archivo de listas dentro de la carpeta de la app. */
        const val NOMBRE_ARCHIVO = "listas.json"
    }
}
