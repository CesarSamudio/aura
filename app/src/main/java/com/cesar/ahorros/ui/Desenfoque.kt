package com.cesar.ahorros.ui

import android.graphics.Bitmap
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// El desenfoque gaussiano de verdad.
//
// Antes el fondo se difuminaba "achicando la foto y estirándola": eso no es
// un desenfoque, es una foto chiquita con los bordes Quadrados (se veía "a
// píxeles"). Esto es el kernel gaussiano de verdad: el mismo que usan las
// cámaras y el Photoshop, que deja los degradados limpios.
//
// Dos cosas lo hacen posible en Android 7 (API 24), donde no existen ni
// `Modifier.blur` ni `RenderEffect` (esos son de Android 12):
//
//  1. El kernel gaussiano es SEPARABLE: el desenfoque en dos direcciones es
//     lo mismo que un solo desenfoque en dos ejes. Eso baja el costo de
//     O(n·r²) a O(n·r) y hace que se pueda hacer a mano, sin librerías.
//  2. Se desenfoca una imagen CHICA (unos 256 px) y después se estira: el
//     resultado se ve igual de suave y cuesta 64 veces menos.
//
// Todo el trabajo pesado se hace con `IntArray` (números pelados), así que
// la parte que importa se puede probar en el PC, sin emulador.
// ---------------------------------------------------------------------------

/**
 * La suma de los pesos del kernel: 65536 (2^16).
 *
 * Son pesos ENTEROS y no decimales a propósito: así la suma da exacta y un
 * píxel blanco (255) sigue siendo blanco después del desenfoque. Con pesos
 * decimales el redondeo lo oscurece un poquito y el fondo se ve sucio.
 */
private const val ESCALA_PESOS = 1 shl 16

/**
 * Los pesos del kernel gaussiano de una dimensión, ya normalizados.
 *
 * `sigma = radio / 2` hace que el kernel (de `2*radio+1` elementos) cubra
 * hasta ±2 desvíos estándar, que es donde está prácticamente toda la masa de
 * la campana.
 */
fun pesosGaussiano(radio: Int): IntArray {
    if (radio <= 0) return intArrayOf(ESCALA_PESOS)

    val sigma = radio / 2.0
    val pesos = DoubleArray(2 * radio + 1)
    var suma = 0.0
    for (i in pesos.indices) {
        val distancia = (i - radio).toDouble()
        val peso = exp(-(distancia * distancia) / (2.0 * sigma * sigma))
        pesos[i] = peso
        suma += peso
    }

    val enteros = IntArray(pesos.size)
    var acumulado = 0
    for (i in pesos.indices) {
        val valor = (pesos[i] / suma * ESCALA_PESOS + 0.5).toInt()
        enteros[i] = valor
        acumulado += valor
    }
    // El redondeo deja el total un pelín desviado: se corrige en el peso del
    // centro para que la suma sea EXACTAMENTE [ESCALA_PESOS].
    enteros[radio] += ESCALA_PESOS - acumulado
    return enteros
}

/**
 * El núcleo del desenfoque: trabaja sobre los píxeles sueltos, sin Android.
 *
 * Recibe los colores en el orden ARGB (como los da `Bitmap.getPixels`) y
 * devuelve un arreglo nuevo ya desenfocado. Con `radio <= 0` devuelve el
 * mismo arreglo, sin tocarlo.
 *
 * Son dos pasadas: una horizontal y otra vertical. Como el kernel es
 * separable, el resultado es el mismo que si se hiciera de una sola vez en
 * dos dimensiones, pero el costo es la mitad (y la mitad otra vez).
 */
fun desenfoqueGaussiano(pixeles: IntArray, ancho: Int, alto: Int, radio: Int): IntArray {
    if (radio <= 0 || ancho <= 0 || alto <= 0) return pixeles
    if (pixeles.size < ancho * alto) return pixeles

    val pesos = pesosGaussiano(radio)
    val horizontal = pasarHorizontal(pixeles, ancho, alto, pesos)
    return pasarVertical(horizontal, ancho, alto, pesos)
}

/**
 * Desenfoca una imagen con el kernel gaussiano de verdad.
 *
 * Con `radio <= 0` devuelve la MISMA imagen, sin tocar: el desenfoque de 0
 * tiene que verse nítido, no "un poco borroso".
 */
fun desenfoqueGaussiano(bitmap: Bitmap, radio: Int): Bitmap {
    if (radio <= 0) return bitmap
    val ancho = bitmap.width
    val alto = bitmap.height
    if (ancho <= 0 || alto <= 0) return bitmap

    return try {
        val pixeles = IntArray(ancho * alto)
        bitmap.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        val suave = desenfoqueGaussiano(pixeles, ancho, alto, radio)
        val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        salida.setPixels(suave, 0, ancho, 0, 0, ancho, alto)
        salida
    } catch (e: Exception) {
        // Si algo falla, se dibuja la foto como estaba: mejor una foto
        // nítida que un fondo roto.
        bitmap
    } catch (e: OutOfMemoryError) {
        bitmap
    }
}

// ---------------------------------------------------------------------------
// Las dos pasadas
// ---------------------------------------------------------------------------

/** Primera pasada: de izquierda a derecha (cada fila se mezcla con sus vecinas). */
private fun pasarHorizontal(
    origen: IntArray,
    ancho: Int,
    alto: Int,
    pesos: IntArray
): IntArray {
    val radio = pesos.size / 2
    val salida = IntArray(origen.size)

    for (y in 0 until alto) {
        val fila = y * ancho
        for (x in 0 until ancho) {
            var rojo = 0L
            var verde = 0L
            var azul = 0L
            var alfa = 0L
            for (k in -radio..radio) {
                // En el borde se repite el píxel de al lado (no se inventa un
                // negro de fuera): si no, los bordes de la imagen se
                // oscurecerían.
                val px = x + k
                val indice = fila + px.coerceIn(0, ancho - 1)
                val color = origen[indice]
                val peso = pesos[k + radio]
                rojo += ((color shr 16) and 0xFF) * peso
                verde += ((color shr 8) and 0xFF) * peso
                azul += (color and 0xFF) * peso
                alfa += ((color ushr 24) and 0xFF) * peso
            }
            salida[fila + x] = empaquetar(rojo, verde, azul, alfa)
        }
    }
    return salida
}

/** Segunda pasada: de arriba abajo (cada columna se mezcla con sus vecinas). */
private fun pasarVertical(
    origen: IntArray,
    ancho: Int,
    alto: Int,
    pesos: IntArray
): IntArray {
    val radio = pesos.size / 2
    val salida = IntArray(origen.size)

    for (x in 0 until ancho) {
        for (y in 0 until alto) {
            var rojo = 0L
            var verde = 0L
            var azul = 0L
            var alfa = 0L
            for (k in -radio..radio) {
                val py = y + k
                val indice = py.coerceIn(0, alto - 1) * ancho + x
                val color = origen[indice]
                val peso = pesos[k + radio]
                rojo += ((color shr 16) and 0xFF) * peso
                verde += ((color shr 8) and 0xFF) * peso
                azul += (color and 0xFF) * peso
                alfa += ((color ushr 24) and 0xFF) * peso
            }
            salida[y * ancho + x] = empaquetar(rojo, verde, azul, alfa)
        }
    }
    return salida
}

/** Convierte los cuatro acumulados (ya pesados) en un color ARGB. */
private fun empaquetar(rojo: Long, verde: Long, azul: Long, alfa: Long): Int {
    return (canal(alfa) shl 24) or (canal(rojo) shl 16) or (canal(verde) shl 8) or canal(azul)
}

/** Un canal acumulado / sumaDePesos, redondeado y acotado entre 0 y 255. */
private fun canal(valor: Long): Int =
    ((valor + ESCALA_PESOS / 2) / ESCALA_PESOS).toInt().coerceIn(0, 255)

/**
 * A qué tamaño hay que reducir una imagen para desenfocarla.
 *
 * Se deja el lado más largo en [ladoMaximo]: desenfocar una imagen de 256 px
 * cuesta 64 veces menos que una de 2048, y el resultado se ve igual de
 * suave (después se estira).
 *
 * La reducción se hace de a mitades (cada paso usa el filtrado del
 * anterior) porque bajar de golpe deja "puntitos" de aliasing.
 */
internal fun reducirParaDesenfoque(bitmap: Bitmap, ladoMaximo: Int): Bitmap {
    val ladoMayor = max(bitmap.width, bitmap.height)
    if (ladoMayor <= ladoMaximo || ladoMaximo <= 0) return bitmap

    val escala = ladoMaximo.toFloat() / ladoMayor
    val anchoObjetivo = (bitmap.width * escala).roundToInt().coerceAtLeast(1)
    val altoObjetivo = (bitmap.height * escala).roundToInt().coerceAtLeast(1)

    var actual = bitmap
    // De a mitades, mientras no se pase del tamaño pedido.
    while (actual.width / 2 >= anchoObjetivo && actual.height / 2 >= altoObjetivo) {
        val nuevo = Bitmap.createScaledBitmap(actual, actual.width / 2, actual.height / 2, true)
        if (nuevo === actual) break
        if (actual !== bitmap) actual.recycle()
        actual = nuevo
    }
    // El último paso, al tamaño exacto.
    if (actual.width != anchoObjetivo || actual.height != altoObjetivo) {
        val nuevo = Bitmap.createScaledBitmap(actual, anchoObjetivo, altoObjetivo, true)
        if (nuevo !== actual) {
            if (actual !== bitmap) actual.recycle()
            actual = nuevo
        }
    }
    return actual
}
