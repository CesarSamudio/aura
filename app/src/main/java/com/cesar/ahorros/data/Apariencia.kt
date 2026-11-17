package com.cesar.ahorros.data

import kotlinx.serialization.Serializable

/**
 * Los ajustes de **apariencia**: lo que el usuario puede cambiar a mano para
 * que la app se vea como él quiere.
 *
 * Son tres cosas independientes:
 *  1. **La paleta**: los colores del tema y los colores de los textos.
 *  2. **El fondo**: una imagen del teléfono y/o un degradado animado.
 *  3. **Las animaciones**: cómo se mueven las gráficas, los números y la
 *     interfaz en general.
 *
 * Este archivo tiene lo "decible": los catálogos (con su nombre y su
 * explicación) y el formato en que se guarda todo en `ajustes.json`.
 * Lo que se ve (los colores ya convertidos a `Color`, el estado de Compose)
 * vive en `ui/Apariencia.kt`.
 *
 * Los colores se guardan como texto hexadecimal (`"#RRGGBB"`) para que el
 * archivo de ajustes se pueda leer y editar a mano: es texto plano, como
 * todo lo demás de la app.
 */

// ---------------------------------------------------------------------------
// La paleta
// ---------------------------------------------------------------------------

/**
 * Los colores del **tema** que el usuario puede cambiar.
 *
 * Cada uno tiene su lugar en la app: el acento de marca son los botones, el
 * verde son los ahorros, el rojo son los gastos. Cambiarlos no rompe nada:
 * los textos se vuelven a calcular para que siempre se lean.
 */
enum class ColorTema(
    val id: String,
    val etiqueta: String,
    val descripcion: String
) {
    FONDO("FONDO", "Fondo", "El fondo de la app, detrás de todo"),
    SUPERFICIE("SUPERFICIE", "Tarjetas", "Las tarjetas y la barra de abajo"),
    SUPERFICIE_ALTA("SUPERFICIE_ALTA", "Tarjetas altas", "Los KPI y los encabezados de día"),
    SUPERFICIE_MAXIMA("SUPERFICIE_MAXIMA", "Hojas", "Las superficies más altas (diálogos)"),
    BORDE("BORDE", "Bordes", "El borde de los controles"),
    ACENTO_MARCA("ACENTO_MARCA", "Acento de marca", "Los botones y lo que la app marca"),
    ACENTO_MENTA("ACENTO_MENTA", "Acento verde", "Los ahorros"),
    ACENTO_AZUL("ACENTO_AZUL", "Acento azul", "Una de las series de las gráficas"),
    ACENTO_VIOLETA("ACENTO_VIOLETA", "Acento violeta", "Una de las series de las gráficas"),
    ACENTO_HIELO("ACENTO_HIELO", "Acento cian", "YAPI y los adjuntos"),
    COLOR_GASTO("COLOR_GASTO", "Rojo de gastos", "Los gastos y los errores"),
    COLOR_AVISO("COLOR_AVISO", "Ámbar de avisos", "Los avisos y lo que está pendiente");

    companion object {
        /** Busca un color por su id; `null` si no se reconoce. */
        fun porId(id: String?): ColorTema? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Los colores de los **textos**: los que el usuario puede cambiar por
 * separado de los del tema, porque hay gente que prefiere el texto de otra
 * forma sin tocar los colores de las tarjetas.
 */
enum class ColorTexto(
    val id: String,
    val etiqueta: String,
    val descripcion: String
) {
    TEXTO_PRIMARIO("TEXTO_PRIMARIO", "Texto principal", "Las cifras grandes y los títulos"),
    TEXTO_SECUNDARIO("TEXTO_SECUNDARIO", "Texto secundario", "Las explicaciones y los subtítulos"),
    TEXTO_TERCERO("TEXTO_TERCERO", "Texto terciario", "El texto de apoyo, el más pequeño");

    companion object {
        /** Busca un color por su id; `null` si no se reconoce. */
        fun porId(id: String?): ColorTexto? = entries.firstOrNull { it.id == id }
    }
}

// ---------------------------------------------------------------------------
// Las animaciones
// ---------------------------------------------------------------------------

/**
 * Cómo se animan **las gráficas**.
 *
 * Cada estilo tiene su explicación: esa explicación es la que sale cuando el
 * usuario deja el dedo apretado sobre la opción en Ajustes.
 */
enum class EstiloGraficas(
    val id: String,
    val etiqueta: String,
    val explicacion: String
) {
    CRECER(
        "CRECER",
        "Crecer",
        "Las barras crecen desde la base hacia arriba y la dona se abre desde " +
            "arriba, como si el dato saliera del cero. Es la más clara: el " +
            "lector entiende de una dónde está el valor más alto."
    ),
    DIBUJAR(
        "DIBUJAR",
        "Dibujar",
        "La línea del balance se dibuja de izquierda a derecha y el área de " +
            "debajo se llena a la vez. Se lee como si la gráfica se escribiera " +
            "sola, de izquierda a derecha."
    ),
    SUAVE(
        "SUAVE",
        "Suave",
        "Todo aparece de golpe, pero con un desvanecido corto: sin que las " +
            "barras se mueven. Para quien quiere el dato ya ahí, sin teatro."
    ),
    NINGUNA(
        "NINGUNA",
        "Sin animación",
        "Las gráficas aparecen tal cual, listas desde el primer fotograma."
    );

    companion object {
        /** Busca un estilo por su id; si no se reconoce, el de por defecto. */
        fun porId(id: String?): EstiloGraficas =
            entries.firstOrNull { it.id == id } ?: CRECER
    }
}

/** Cómo se animan **los números** (las cifras de dinero que aparecen). */
enum class EstiloNumeros(
    val id: String,
    val etiqueta: String,
    val explicacion: String
) {
    CONTAR(
        "CONTAR",
        "Contar",
        "Las cifras suben desde cero hasta el valor real, como un contador. " +
            "Se ve de una vez cuánto money se movió; el número grande del " +
            "Balance también cuenta."
    ),
    FUNDIDO(
        "FUNDIDO",
        "Fundido",
        "El número aparece con un cambio suave de opacidad, en su posición " +
            "final. Sin movimientos raros cuando el valor es negativo."
    ),
    NINGUNA(
        "NINGUNA",
        "Sin animación",
        "Las cifras aparecen fijas, sin contar ni fundirse."
    );

    companion object {
        /** Busca un estilo por su id; si no se reconoce, el de por defecto. */
        fun porId(id: String?): EstiloNumeros =
            entries.firstOrNull { it.id == id } ?: CONTAR
    }
}

/** Cómo se animan **la interfaz** (las tarjetas, los paneles, las pantallas). */
enum class EstiloInterfaz(
    val id: String,
    val etiqueta: String,
    val explicacion: String
) {
    FUNDIDO(
        "FUNDIDO",
        "Fundido",
        "Lo que aparece se vuelve visible despacio, sin moverse. Es el " +
            "movimiento más discreto: nada se desliza de la nada."
    ),
    DESLIZAR(
        "DESLIZAR",
        "Deslizar",
        "Las tarjetas entran desde abajo, como si se acomodaran solas en la " +
            "pantalla. Se nota qué es nuevo y qué ya estaba."
    ),
    ESCALAR(
        "ESCALAR",
        "Asentar",
        "Las tarjetas aparecen un poco más grandes y se asientan con un " +
            "rebote suave, como si tuvieran peso. Da sensación de materialized."
    ),
    NINGUNA(
        "NINGUNA",
        "Sin animación",
        "La interfaz aparece de golpe, sin transiciones."
    );

    companion object {
        /** Busca un estilo por su id; si no se reconoce, el de por defecto. */
        fun porId(id: String?): EstiloInterfaz =
            entries.firstOrNull { it.id == id } ?: FUNDIDO
    }
}

/** Los ajustes de las animaciones, tal como se guardan. */
@Serializable
data class AjustesAnimacion(
    /** El interruptor maestro: si está apagado, no se anima nada. */
    val activas: Boolean = true,
    /** Multiplicador de velocidad: 0.5 (lenta) … 2.0 (rápida). */
    val velocidad: Float = 1.0f,
    val graficas: String = EstiloGraficas.CRECER.id,
    val numeros: String = EstiloNumeros.CONTAR.id,
    val interfaz: String = EstiloInterfaz.FUNDIDO.id
)

// ---------------------------------------------------------------------------
// El fondo
// ---------------------------------------------------------------------------

/**
 * Las formas del degradado animado del fondo.
 *
 * Cada una tiene su explicación (sale al apretar y soltar sobre la opción).
 */
enum class FormaDegradado(
    val id: String,
    val etiqueta: String,
    val explicacion: String
) {
    AUREOLA(
        "AUREOLA",
        "Aureola",
        "Un halo de luz que respira en el centro de la pantalla, con los " +
            "colores del tema. Es el más tranquilo: se siente como un resplandor."
    ),
    AURORA(
        "AURORA",
        "Aurora",
        "Varias manchas de color que se deslizan despacio y se mezclan entre " +
            "sí, como las auroras del norte. La más viva de todas."
    ),
    BARRIDO(
        "BARRIDO",
        "Barrido",
        "Una banda de luz que recorre la pantalla de lado a lado, como un " +
            "foco que pasa. Da sensación de movimiento continuo."
    ),
    CONICO(
        "CONICO",
        "Cónico",
        "Un abanico de color que gira despacio alrededor de un punto. Se nota " +
            "el giro, pero no marean."
    ),
    MALLA(
        "MALLA",
        "Malla",
        "Cuatro focos de luz en las esquinas que se cruzan y tejen un fondo " +
            "que cambia de forma poco a poco."
    );

    companion object {
        /** Busca una forma por su id; si no se reconoce, la de por defecto. */
        fun porId(id: String?): FormaDegradado =
            entries.firstOrNull { it.id == id } ?: AUREOLA
    }
}

/** Los ajustes del fondo, tal como se guardan. */
@Serializable
data class AjustesFondo(
    /**
     * El nombre del archivo de la imagen de fondo dentro de la carpeta
     * `fondos/` de la app. `null` si el usuario no puso ninguna.
     */
    val imagen: String? = null,
    /** Qué tan visible se ve la imagen (0 = nada, 1 = opaca). */
    val opacidadImagen: Float = 0.35f,
    /** Qué tan difuminada se ve la imagen (0 = nítida, 1 = borrosa). */
    val difuminadoImagen: Float = 0.0f,
    /** Si el degradado animado está prendido. */
    val degradadoActivo: Boolean = false,
    val formaDegradado: String = FormaDegradado.AUREOLA.id,
    /** Qué tan fuerte se ve el degradado. */
    val opacidadDegradado: Float = 0.55f,
    /** Qué tan rápido se mueve. */
    val velocidadDegradado: Float = 1.0f,
    /** Qué tan saturado es el color del degradado. */
    val intensidadDegradado: Float = 0.6f
)

// ---------------------------------------------------------------------------
// Todo junto
// ---------------------------------------------------------------------------

/**
 * Los ajustes de apariencia completos, tal como viajan en `ajustes.json`
 * (y por lo tanto en la copia de seguridad).
 */
@Serializable
data class AjustesApariencia(
    /** Los colores del tema que el usuario cambió: id → `#RRGGBB`. */
    val coloresTema: Map<String, String> = emptyMap(),
    /** Los colores de los textos que el usuario cambió: id → `#RRGGBB`. */
    val coloresTexto: Map<String, String> = emptyMap(),
    val animaciones: AjustesAnimacion = AjustesAnimacion(),
    val fondo: AjustesFondo = AjustesFondo()
)
