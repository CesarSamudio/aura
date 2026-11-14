package com.cesar.ahorros.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Muestra el comprobante (la imagen que se adjuntó a un pago con YAPI).
 *
 * La imagen se decodifica a mano con `BitmapFactory` (no hay librerías de
 * imágenes en el proyecto) y se reduce antes de pintar para no gastar
 * memoria con fotos grandes del teléfono.
 */
@Composable
fun DialogoComprobante(archivo: File, onCerrar: () -> Unit) {

    val bitmap = remember(archivo) { leerImagen(archivo) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Comprobante") },
        text = {
            Column {
                if (bitmap == null) {
                    Text(
                        text = "No se pudo abrir la imagen del comprobante.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .clip(RoundedCornerShape(RadioBoton))
                            .background(SuperficieMaxima),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Comprobante",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = archivo.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

/**
 * Lee la imagen del disco ya reducida: se calcula primero el tamaño que
 * tiene y se pide a `BitmapFactory` un mapa de bits más pequeño.
 *
 * El HEIC (el formato por defecto de muchas fotos del teléfono) no lo
 * entiende `BitmapFactory`: para eso está `ImageDecoder`, que existe desde
 * la API 28.
 */
private fun leerImagen(archivo: File): Bitmap? {
    if (!archivo.exists()) return null

    if (archivo.extension.equals("heic", ignoreCase = true) ||
        archivo.extension.equals("heif", ignoreCase = true)
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(archivo)) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    return try {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, opciones)
        var muestreo = 1
        val ancho = opciones.outWidth
        val alto = opciones.outHeight
        while (ancho / muestreo > 1200 || alto / muestreo > 1200) {
            muestreo *= 2
        }
        val finales = BitmapFactory.Options().apply { inSampleSize = muestreo }
        BitmapFactory.decodeFile(archivo.absolutePath, finales)
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
