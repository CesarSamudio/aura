package com.cesar.ahorros.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            try {
                json.decodeFromString<List<Deuda>>(texto)
            } catch (e: Exception) {
                // Deudas creadas con la versión anterior del modelo: no tenían
                // "personasQuePagan" (la cuota se dividía entre los de la lista).
                // Se reparan poniendo ese número igual a la cantidad de la lista,
                // para que no se pierdan.
                json.decodeFromString<List<Deuda>>(reparar(texto))
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Le pone "personasQuePagan" a las deudas viejas que no lo tenían. */
    private fun reparar(texto: String): String {
        val elemento = Json.parseToJsonElement(texto) as? JsonArray ?: return texto
        val reparado = elemento.map { deuda ->
            val objeto = deuda.jsonObject
            if (objeto.containsKey(CAMPO_PAGADORES)) {
                objeto
            } else {
                val cantidad = (objeto["personas"] as? JsonArray)?.size ?: 0
                JsonObject(objeto + (CAMPO_PAGADORES to JsonPrimitive(cantidad)))
            }
        }
        return JsonArray(reparado).toString()
    }

    fun guardar(lista: List<Deuda>): Boolean {
        val temporal = File(carpeta, NOMBRE_ARCHIVO + ".tmp")
        return try {
            temporal.writeText(json.encodeToString<List<Deuda>>(lista))
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
    }
}
