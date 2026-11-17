package com.cesar.ahorros.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * La **imagen de fondo** que el usuario saca de su teléfono.
 *
 * La app no tiene ningún permiso: la imagen entra por el selector de fotos
 * del sistema, que deja leer esa Uri sin pedir nada. De entrada la app se
 * queda con una COPIA dentro de su almacenamiento interno: así el fondo
 * sigue ahí aunque el usuario borre la foto de la galería, y la copia de
 * seguridad puede llevar la imagen con el resto de los datos.
 *
 * El archivo vive en `filesDir/fondos/fondo.jpg`.
 */
object FondosApp {

    /** La carpeta dentro de la app donde vive la imagen del fondo. */
    const val CARPETA = "fondos"

    /** El nombre del archivo de la imagen. */
    const val NOMBRE_ARCHIVO = "fondo.jpg"

    /** La resolución que se le sugiere al usuario (ancho x alto, en px). */
    val RESOLUCION_RECOMENDADA: Pair<Int, Int> = 1080 to 2340

    /**
     * El lado más largo (en px) que puede tener la imagen guardada.
     *
     * 2048 es el techo: una foto de 12 MP (4000x3000) son 48 MB de memoria
     * si se trae entera. Con el tope, lo que se guarda pesa poco y de fondo
     * se ve igual de bien (lo que se ve detrás de la app, tapado por las
     * tarjetas, no necesita más resolución que la pantalla).
     */
    const val LADO_MAXIMO = 2048

    /**
     * La calidad con la que se escribe el JPEG.
     *
     * 85: a simple vista no se nota la diferencia con 95, pero el archivo
     * pesa la mitad (lo que importa: esto vive en el teléfono del usuario).
     */
    private const val CALIDAD_JPEG = 85

    /**
     * Cuántos píxeles se pueden leer como máximo al abrir la foto original.
     *
     * 4 millones de píxeles son 16 MB de memoria: entra cómodo hasta en un
     * teléfono viejo. Una foto de 12 MP (4000x3000) se lee en dos partes
     * (2000x1500) y después se escala al tamaño final.
     */
    private const val MAXIMO_PIXELES = 4_000_000L

    /**
     * Copia la imagen elegida a la carpeta de la app, reescalada.
     * Devuelve true si quedó guardada.
     * NO pide ningún permiso: la Uri viene del selector de fotos del sistema.
     *
     * Se guarda SIEMPRE en JPEG (calidad 85) y no en PNG: el fondo se ve
     * detrás de la app, con opacidad y casi siempre difuminado, así que los
     *otonejos del JPEG no se notan; en cambio, un PNG de una foto de 2048 px
     * pesa varios MB y tarda en escribirse. Si la foto original tiene
     * transparencia (un PNG con fondo transparente), primero se pega sobre un
     * color plano —el JPEG no tiene canal alfa— y por eso el parámetro
     * [colorFondo]: quien llama puede pasarle el color del tema para que las
     * zonas transparentes se vean como el fondo y no como un agujero negro.
     *
     * **Llamalo fuera del hilo principal** (dentro de un
     * `withContext(Dispatchers.IO)`): leer una foto de 12 MP y escribirla
     * tarda, y en el hilo principal la app se congelaría un instante.
     */
    fun guardarFondo(contexto: Context, uri: Uri, colorFondo: Int = Color.BLACK): Boolean {
        val carpeta = File(contexto.filesDir, CARPETA)
        if (!carpeta.exists() && !carpeta.mkdirs()) return false

        val destino = File(carpeta, NOMBRE_ARCHIVO)
        // Se escribe primero en un archivo aparte: si algo falla a mitad de
        // camino, el fondo que ya estaba NO se rompe.
        val temporal = File(carpeta, "$NOMBRE_ARCHIVO.tmp")

        return try {
            if (!escribirImagen(contexto, uri, colorFondo, temporal)) {
                temporal.delete()
                return false
            }
            if (!temporal.exists() || temporal.length() <= 0L) {
                temporal.delete()
                return false
            }

            // Solo ahora se pisa el fondo anterior.
            if (destino.exists() && !destino.delete()) {
                temporal.delete()
                return false
            }
            if (!temporal.renameTo(destino)) {
                temporal.delete()
                return false
            }
            true
        } catch (e: Exception) {
            temporal.delete()
            false
        } catch (e: OutOfMemoryError) {
            // Una foto gigante puede reventar la memoria del teléfono: mejor
            // no poner el fondo que romper la app.
            temporal.delete()
            false
        }
    }

    /**
     * Lee la foto de la Uri, la reescala y la escribe (en JPEG) en el archivo
     * temporal. Devuelve false si la foto no se pudo leer o escribir.
     */
    private fun escribirImagen(
        contexto: Context,
        uri: Uri,
        colorFondo: Int,
        temporal: File
    ): Boolean {
        val leida = leerImagen(contexto, uri) ?: return false
        val original = leida.bitmap
        try {
            val (largoFinal, altoFinal) = escalaParaFondo(leida.ancho, leida.alto)
            val escalado = escalar(original, largoFinal, altoFinal)
            val plano = aplanar(escalado, colorFondo, puedeSerTransparente(contexto, uri))
            try {
                FileOutputStream(temporal).use { salida ->
                    plano.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
                }
            } finally {
                // Se sueltan los mapas de bits: ya están escritos en el
                // archivo. Cada uno se suelta UNA vez (a veces son el mismo).
                if (plano !== escalado) plano.recycle()
                if (escalado !== original) escalado.recycle()
            }
        } finally {
            original.recycle()
        }
        return true
    }

    /** Borra la imagen del fondo (si está). */
    fun borrarFondo(contexto: Context) {
        val carpeta = File(contexto.filesDir, CARPETA)
        try {
            val archivo = File(carpeta, NOMBRE_ARCHIVO)
            if (archivo.exists()) archivo.delete()
        } catch (e: Exception) {
            // Si no se puede borrar, no vale la pena romper la app por eso.
        }
        try {
            // También se va el archivo temporal que quedó a medias.
            val temporal = File(carpeta, "$NOMBRE_ARCHIVO.tmp")
            if (temporal.exists()) temporal.delete()
        } catch (e: Exception) {
        }
    }

    /** El archivo del fondo, o null si no hay. */
    fun archivoFondo(contexto: Context): File? {
        val archivo = File(File(contexto.filesDir, CARPETA), NOMBRE_ARCHIVO)
        return if (archivo.exists() && archivo.isFile) archivo else null
    }

    /**
     * A qué tamaño hay que escalar una imagen de `largo` x `alto` para que
     * se vea bien de fondo sin pesar demasiado. Función PURA (testeable).
     * Nunca agranda: si la imagen ya es chica, la deja como está.
     *
     * El lado más largo no puede pasar de [LADO_MAXIMO] (2048 px). Si la
     * imagen ya entra, se devuelve tal cual; si no, se escala de forma
     * proporcional (los dos lados bajan en la misma proporción, así que la
     * foto no se deforma).
     */
    fun escalaParaFondo(largo: Int, alto: Int): Pair<Int, Int> {
        if (largo <= 0 || alto <= 0) return 0 to 0
        val ladoMayor = max(largo, alto)
        if (ladoMayor <= LADO_MAXIMO) return largo to alto
        val factor = LADO_MAXIMO.toDouble() / ladoMayor
        return (largo * factor).roundToInt() to (alto * factor).roundToInt()
    }

    /**
     * Cuál es el factor de muestreo (inSampleSize) para leer esa imagen sin
     * pasarse de memoria. Función PURA.
     *
     * Es la cuenta al revés: en vez de agrandar, achica. El presupuesto es
     * [MAXIMO_PIXELES] (4 millones de píxeles = 16 MB): se va dividiendo la
     * imagen en 2, en 4, en 8… hasta que lo que se lee entra en ese
     * presupuesto. Una foto de 12 MP (4000x3000) se lee en 2 (2000x1500) y
     * una de 1080x2340 se lee entera (son 2,5 MP: entra de sobra).
     */
    fun factorDeMuestreo(largo: Int, alto: Int): Int {
        if (largo <= 0 || alto <= 0) return 1
        var factor = 1
        while (largo.toLong() * alto.toLong() / (factor.toLong() * factor.toLong()) > MAXIMO_PIXELES) {
            factor *= 2
        }
        return factor
    }

    // ------------------------------------------------------------------
    // Lo de adentro (nada de esto se usa desde afuera)
    // ------------------------------------------------------------------

    /** Una imagen ya leída: el mapa de bits y de qué tamaño era. */
    private data class ImagenLeida(val bitmap: Bitmap, val ancho: Int, val alto: Int)

    /**
     * Lee la imagen de la Uri.
     *
     * Primero se mide el tamaño sin cargarla (con `inJustDecodeBounds`) y
     * después se lee ya muestreada, para no traer 48 MB a la memoria.
     *
     * El HEIC (el formato por defecto de muchas fotos del teléfono) no lo
     * entiende `BitmapFactory`: desde Android 9 se lee con `ImageDecoder`,
     * que además sabe hacer la cuenta del tamaño final y así no multiplica
     * la memoria.
     */
    private fun leerImagen(contexto: Context, uri: Uri): ImagenLeida? {
        // 1. ¿De qué tamaño es la foto?
        val medidas = medir(contexto, uri)
        if (medidas != null) {
            val (largo, alto) = medidas
            val muestreo = factorDeMuestreo(largo, alto)
            val bitmap = decodificar(contexto, uri, muestreo) ?: return null
            return ImagenLeida(bitmap, largo, alto)
        }

        // 2. Formato que `BitmapFactory` no entiende (HEIC): se prueba con
        //    `ImageDecoder`, que existe desde Android 9.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val fuente = ImageDecoder.createSource(contexto.contentResolver, uri)
            var original = 0
            var altoOriginal = 0
            val bitmap = ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                original = info.size.width
                altoOriginal = info.size.height
                val (ancho, alto) = escalaParaFondo(original, altoOriginal)
                // Se le pide el tamaño final directo: `ImageDecoder` escala
                // mientras lee y así no hay mapa de bits gigante en memoria.
                decoder.setTargetSize(ancho, alto)
                decoder.isMutableRequired = false
            }
            if (original <= 0 || altoOriginal <= 0) null
            else ImagenLeida(bitmap, original, altoOriginal)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** El tamaño de la foto, sin cargarla. `null` si no se puede leer. */
    private fun medir(contexto: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val entrada = contexto.contentResolver.openInputStream(uri)
            if (entrada == null) return null
            entrada.use { BitmapFactory.decodeStream(it, null, opciones) }
            if (opciones.outWidth > 0 && opciones.outHeight > 0) {
                opciones.outWidth to opciones.outHeight
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Lee la foto de la Uri con el factor de muestreo indicado. */
    private fun decodificar(contexto: Context, uri: Uri, muestreo: Int): Bitmap? {
        return try {
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
            val entrada = contexto.contentResolver.openInputStream(uri) ?: return null
            entrada.use { BitmapFactory.decodeStream(it, null, opciones) }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Escala el mapa de bits al tamaño pedido (nunca lo agranda). */
    private fun escalar(bitmap: Bitmap, ancho: Int, alto: Int): Bitmap {
        if (ancho <= 0 || alto <= 0) return bitmap
        if (bitmap.width == ancho && bitmap.height == alto) return bitmap
        return try {
            Bitmap.createScaledBitmap(bitmap, ancho, alto, true)
        } catch (e: Exception) {
            bitmap
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    /**
     * ¿La foto puede tener zonas transparentes?
     *
     * El JPEG no tiene canal alfa, así que una foto con transparencia tiene
     * que pegarse antes sobre un color plano (el del tema), si no esas
     * zonas se guardarían como un rectángulo negro.
     */
    private fun puedeSerTransparente(contexto: Context, uri: Uri): Boolean {
        val tipo = try {
            contexto.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        // PNG y WEBP pueden tener alfa; el JPEG no. Si no se sabe el tipo,
        // se pega igual: el resultado es el mismo y es más seguro.
        return tipo == null || tipo == "image/png" || tipo == "image/webp"
    }

    /** Pega la imagen sobre un color plano (para poder guardarla en JPEG). */
    private fun aplanar(bitmap: Bitmap, colorFondo: Int, puedeSerTransparente: Boolean): Bitmap {
        if (!puedeSerTransparente) return bitmap
        return try {
            val plano = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val lienzo = Canvas(plano)
            lienzo.drawColor(colorFondo)
            lienzo.drawBitmap(bitmap, 0f, 0f, null)
            plano
        } catch (e: Exception) {
            bitmap
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }
}
