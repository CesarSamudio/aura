package com.cesar.ahorros.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.cesar.ahorros.R

/**
 * Los **iconos de la app**: los 4 logos con los que se puede abrir Aura en la
 * pantalla del teléfono, y el mecanismo para cambiar el que ve el launcher.
 *
 * Cómo funciona el cambio:
 * - El manifiesto declara 4 "alias" de la actividad principal ([AliasIcono1]
 *   a [AliasIcono4]), cada uno con su propio icono. De entrada solo el
 *   primero está habilitado: por eso el launcher muestra UN ícono y no cuatro.
 * - Para cambiarlo, la app habilita el alias del logo elegido y deshabilita
 *   los demás. Es lo único que el sistema permite: una app no puede cambiar
 *   su propio ícono, pero sí puede habilitar y deshabilitar sus componentes.
 *
 * Por esto, en el launcher puede tardar un par de segundos en verse el
 * cambio: hay launchers que cachean los iconos y los refrescan cuando les
 * parece.
 */

/** Un logo de la app. */
data class IconoApp(
    val id: String,
    val nombre: String,
    val descripcion: String,
    /**
     * La vista previa para la lista de Ajustes.
     *
     * Es un PNG aparte (`ic_previa_v*`) y no el ícono del launcher: Compose no
     * sabe pintar un "adaptive-icon" (es un XML que solo entiende el
     * launcher), así que la vista previa usa el diseño completo en PNG.
     */
    @DrawableRes val recurso: Int
)

/** Los 4 logos, en el orden en que salen en Ajustes. */
object CatalogoIconos {

    /** El que viene puesto de fábrica. */
    const val POR_DEFECTO = "v1"

    val lista: List<IconoApp> = listOf(
        IconoApp(
            id = "v1",
            nombre = "Aureola",
            descripcion = "Una moneda con su aureola",
            recurso = R.mipmap.ic_previa_v1
        ),
        IconoApp(
            id = "v2",
            nombre = "Billete",
            descripcion = "Un billete con el signo del peso",
            recurso = R.mipmap.ic_previa_v2
        ),
        IconoApp(
            id = "v3",
            nombre = "Cascada",
            descripcion = "Monedas cayendo en diagonal",
            recurso = R.mipmap.ic_previa_v3
        ),
        IconoApp(
            id = "v4",
            nombre = "Destello",
            descripcion = "Una moneda con su destello",
            recurso = R.mipmap.ic_previa_v4
        )
    )

    /** El logo pedido; si no se reconoce, el de fábrica. */
    fun porId(id: String?): IconoApp =
        lista.firstOrNull { it.id == id } ?: lista.first()
}

/** El manejo del ícono del launcher. */
object IconosApp {

    /** Cómo se llaman los alias en el manifiesto: `AliasIcono1` … `AliasIcono4`. */
    private const val PREFIJO_ALIAS = "AliasIcono"

    /** Cuántos logos hay. */
    private const val TOTAL = 4

    /**
     * Deja el logo elegido como el único habilitado.
     *
     * Se habilita el nuevo **antes** de deshabilitar los otros: si se hiciera
     * al revés hay un instante en que la app no tiene ícono, y algunos
     * launchers la esconden de la pantalla de inicio.
     */
    fun aplicar(context: Context, id: String) {
        val pm = context.packageManager
        val paquete = context.packageName
        val elegido = numeroDe(id)

        // 1. El que se quiere, habilitado.
        val nuevo = ComponentName(paquete, aliasDe(elegido))
        if (!estaHabilitado(pm, nuevo, porDefecto = elegido == 1)) {
            pm.setComponentEnabledSetting(
                nuevo,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // 2. Los otros, deshabilitados.
        for (numero in 1..TOTAL) {
            if (numero == elegido) continue
            val otro = ComponentName(paquete, aliasDe(numero))
            if (estaHabilitado(pm, otro, porDefecto = numero == 1)) {
                pm.setComponentEnabledSetting(
                    otro,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    /**
     * Deja el ícono como estaba en el preference, sin tocar nada si ya está
     * bien. Se llama al abrir la app: después de una actualización o de una
     * reinstalación, el alias habilitado puede no ser el que el usuario había
     * elegido.
     */
    fun asegurar(context: Context, id: String?) {
        val elegido = numeroDe(id ?: CatalogoIconos.POR_DEFECTO)
        val pm = context.packageManager
        val paquete = context.packageName

        for (numero in 1..TOTAL) {
            val comp = ComponentName(paquete, aliasDe(numero))
            val habilitado = estaHabilitado(pm, comp, porDefecto = numero == 1)
            if (habilitado != (numero == elegido)) {
                aplicar(context, if (numero == 1) "v1" else "v$numero")
                return
            }
        }
    }

    /**
     * Si el componente está habilitado.
     *
     * `DEFAULT` quiere decir "mande el manifiesto": en el manifiesto el
     * primero viene habilitado y los otros no.
     */
    private fun estaHabilitado(
        pm: PackageManager,
        componente: ComponentName,
        porDefecto: Boolean
    ): Boolean = when (pm.getComponentEnabledSetting(componente)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
        else -> porDefecto
    }

    /** `v1` → 1, `v3` → 3. Lo que no se reconoce es el primero. */
    private fun numeroDe(id: String): Int =
        id.removePrefix("v").toIntOrNull()?.takeIf { it in 1..TOTAL } ?: 1

    /** El nombre del alias en el manifiesto. */
    private fun aliasDe(numero: Int): String = "$PREFIJO_ALIAS$numero"
}
