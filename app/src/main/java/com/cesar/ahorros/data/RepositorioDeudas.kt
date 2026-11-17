package com.cesar.ahorros.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Persistencia de las deudas en su propio archivo JSON.
 *
 * Va aparte de `movimientos.json` a propósito: una deuda NO es un movimiento
 * (no es ingreso ni gasto) y así los datos ya guardados nunca se tocan.
 *
 * Mismo patrón que [Repositorio]: constructor con la CARPETA para poder
 * probarlo en un test de JVM puro, y constructor de conveniencia con Context.
 */
class RepositorioDeudas(private val carpeta: File) {

    /** Constructor de conveniencia para producción: guarda en `context.filesDir`. */
    constructor(context: Context) : this(context.filesDir)

    private val archivo = File(carpeta, NOMBRE_ARCHIVO)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun cargar(): List<Deuda> = try {
        if (!archivo.exists()) {
            emptyList()
        } else {
            val texto = archivo.readText()
            // La reparación va SIEMPRE antes de leer, no solo cuando falla:
            // el campo "personasQuePagan" ahora es opcional (puede venir en
            // null, que significa "cuota automática"), así que un archivo
            // viejo se carga sin que salte ningún error.
            json.decodeFromString<List<Deuda>>(reparar(texto))
        }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Deja el archivo al día antes de leerlo.
     *
     * Hay tres casos:
     * - Deuda nueva en **cuota automática** ("cuotaAutomatica": true): no se
     *   toca. Es una deuda en la que el usuario no dijo cuántos pagan y la
     *   cuota sale de la lista.
     * - Deuda vieja que NO tiene "personasQuePagan" (la versión más
     *   anterior del modelo, que dividía entre los de la lista): se repara
     *   como siempre, poniéndole la cantidad de personas que había en la
     *   lista.
     * - Deuda vieja que YA tiene el número: se deja como está, pero se le
     *   marca que NO es automática (para que no se confunda con una deuda
     *   nueva en automática).
     */
    private fun reparar(texto: String): String {
        val elemento = Json.parseToJsonElement(texto) as? JsonArray ?: return texto
        val reparado = elemento.map { deuda ->
            val objeto = deuda.jsonObject
            val cantidad = (objeto["personas"] as? JsonArray)?.size ?: 0
            val esAutomatica = (objeto[CAMPO_AUTOMATICA] as? JsonPrimitive)?.booleanOrNull == true
            val pagadores = objeto[CAMPO_PAGADORES]
            val tieneNumero = pagadores != null && pagadores !is JsonNull
            when {
                // Deuda en automática: no se toca.
                esAutomatica -> objeto
                // Deuda vieja sin el número (o con el número en null): se
                // repara con la cantidad de personas de la lista.
                !tieneNumero -> JsonObject(
                    objeto +
                        (CAMPO_PAGADORES to JsonPrimitive(cantidad)) +
                        (CAMPO_AUTOMATICA to JsonPrimitive(false))
                )
                // Deuda vieja con el número: se le marca que no es automática.
                !objeto.containsKey(CAMPO_AUTOMATICA) ->
                    JsonObject(objeto + (CAMPO_AUTOMATICA to JsonPrimitive(false)))
                else -> objeto
            }
        }
        return JsonArray(reparado).toString()
    }

    /**
     * Deja cada deuda coherente antes de guardarla: la marca de automática y
     * el número de pagadores tienen que decir lo mismo.
     *
     * Hace falta porque al editar una deuda se copia la anterior y la copia
     * conserva la marca vieja: si el usuario dejó el número en blanco
     * (cuota automática) la deuda vieja decía otra cosa, y al revés.
     */
    private fun dejarCoherente(deuda: Deuda): Deuda = when {
        deuda.personasQuePagan == null && !deuda.cuotaAutomatica ->
            deuda.copy(cuotaAutomatica = true)
        deuda.personasQuePagan != null && deuda.cuotaAutomatica ->
            deuda.copy(cuotaAutomatica = false)
        else -> deuda
    }

    fun guardar(lista: List<Deuda>): Boolean {
        val temporal = File(carpeta, NOMBRE_ARCHIVO + ".tmp")
        return try {
            temporal.writeText(json.encodeToString<List<Deuda>>(lista.map { dejarCoherente(it) }))
            if (archivo.exists()) archivo.delete()
            if (temporal.renameTo(archivo)) {
                true
            } else {
                temporal.copyTo(archivo, overwrite = true)
                true
            }
        } catch (e: Exception) {
            false
        } finally {
            if (temporal.exists()) temporal.delete()
        }
    }

    companion object {
        const val NOMBRE_ARCHIVO = "deudas.json"

        /** Carpeta interna donde se guardan las imágenes de los comprobantes. */
        const val CARPETA_COMPROBANTES = "comprobantes"

        /** Carpeta interna donde se guardan las pruebas adjuntas por persona. */
        const val CARPETA_PRUEBAS = "pruebas"

        /** Nombre del campo con cuántas personas pagan la deuda. */
        private const val CAMPO_PAGADORES = "personasQuePagan"

        /** Nombre del campo que marca la deuda como "cuota automática". */
        private const val CAMPO_AUTOMATICA = "cuotaAutomatica"
    }
}
