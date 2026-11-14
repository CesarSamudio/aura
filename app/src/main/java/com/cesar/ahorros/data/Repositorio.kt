package com.cesar.ahorros.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistencia en un único archivo JSON dentro del almacenamiento interno.
 * Sin base de datos: para este volumen es instantáneo y no pesa nada en el APK.
 *
 * El constructor principal recibe la CARPETA donde vive el archivo, así el mismo
 * código se puede probar en un test de JVM puro (sin emulador ni Android).
 * En la app se usa el constructor de conveniencia con `Context`.
 */
class Repositorio(private val carpeta: File) {

    /** Constructor de conveniencia para producción: guarda en `context.filesDir`. */
    constructor(context: Context) : this(context.filesDir)

    private val archivo = File(carpeta, NOMBRE_ARCHIVO)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun cargar(): List<Movimiento> = try {
        if (archivo.exists()) json.decodeFromString<List<Movimiento>>(archivo.readText()) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun guardar(lista: List<Movimiento>): Boolean {
        val temporal = File(carpeta, NOMBRE_ARCHIVO + ".tmp")
        return try {
            temporal.writeText(json.encodeToString<List<Movimiento>>(lista))

            // Camino ideal: rename atomico (nunca se ve un archivo a medias).
            if (archivo.exists()) archivo.delete()
            if (temporal.renameTo(archivo)) {
                true
            } else {
                // El rename puede estar bloqueado (permisos, antivirus): se cae a copia.
                temporal.copyTo(archivo, overwrite = true)
                true
            }
        } catch (e: Exception) {
            false
        } finally {
            // Nunca dejar un .tmp huerfano en la carpeta.
            if (temporal.exists()) temporal.delete()
        }
    }

    companion object {
        /** Nombre del archivo de datos dentro de la carpeta de almacenamiento. */
        const val NOMBRE_ARCHIVO = "movimientos.json"
    }
}
