package com.cesar.ahorros.data

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * La copia de seguridad, **local y sin nube**.
 *
 * La app no tiene ningún permiso y no habla con nadie: todo vive en el
 * teléfono. Para respaldar, se crea un `.zip` con los datos y las imágenes y
 * el usuario decide dónde guardarlo (Drive, WhatsApp, correo, una USB...).
 *
 * Restaurar es la operación inversa: se elige un `.zip` y se descomprime
 * encima de los datos.
 */
object CopiaLocal {

    /** Prefijo de los archivos de copia, para reconocerlos. */
    const val PREFIJO = "ahorros-copia-"

    /**
     * Arma un `.zip` con todo: los dos JSON y las carpetas de imágenes
     * (comprobantes y pruebas). Se guarda en la caché (se borra sola).
     */
    fun crearZip(contexto: android.content.Context): File? {
        val datos = contexto.filesDir
        val destino = File(contexto.cacheDir, PREFIJO + nombreCopia() + ".zip")
        return try {
            ZipOutputStream(FileOutputStream(destino)).use { zip ->
                // "ajustes.json" viaja con los datos: el tema vuelve con la copia.
                listOf("movimientos.json", "deudas.json", Preferencias.NOMBRE_ARCHIVO)
                    .forEach { nombre ->
                        val archivo = File(datos, nombre)
                        if (archivo.exists()) {
                            zip.putNextEntry(ZipEntry(nombre))
                            FileInputStream(archivo).use { entrada -> entrada.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                listOf("comprobantes", "pruebas").forEach { carpeta ->
                    val origen = File(datos, carpeta)
                    if (!origen.exists()) return@forEach
                    origen.listFiles()?.forEach { imagen ->
                        if (imagen.isFile) {
                            zip.putNextEntry(ZipEntry("$carpeta/${imagen.name}"))
                            FileInputStream(imagen).use { entrada -> entrada.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            destino
        } catch (e: Exception) {
            null
        }
    }

    /** Descomprime una copia encima de los datos del teléfono. */
    fun restaurar(contexto: android.content.Context, zip: File): Boolean {
        val datos = contexto.filesDir
        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { entrada ->
                var entradaActual: ZipEntry? = entrada.nextEntry
                while (entradaActual != null) {
                    val nombre = entradaActual.name
                    // Nunca se sale de la carpeta de la app.
                    if (nombre.contains("..")) {
                        entradaActual = entrada.nextEntry
                        continue
                    }
                    val destino = File(datos, nombre)
                    if (entradaActual.isDirectory) {
                        destino.mkdirs()
                    } else {
                        destino.parentFile?.mkdirs()
                        FileOutputStream(destino).use { salida -> entrada.copyTo(salida) }
                    }
                    entradaActual = entrada.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Nombre del archivo: `ahorros-copia-2026-11-14-1330.zip`. */
    private fun nombreCopia(): String {
        val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
        return Instant.now().atZone(ZoneId.systemDefault()).format(formato)
    }
}
