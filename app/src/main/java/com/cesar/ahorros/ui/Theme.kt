package com.cesar.ahorros.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Temas de la app.
//
// Antes había UNA paleta fija (el sistema "Awesomic"). Ahora hay 7 temas y el
// usuario elige el que quiere en Ajustes > Apariencia.
//
// Cómo funciona:
//  - `TemaVisual` es la paleta completa de un tema.
//  - `CatalogoTemas` tiene los 7 temas.
//  - `TemaActual` guarda cuál está activo. Es estado de Compose: cuando
//    cambia, todo lo que usa un color se redibuja solo.
//  - Abajo, los tokens (Fondo, Superficie, AcentoCalido...) leen del tema
//    activo, así que el resto de la app no cambia ni una línea.
//
// Todos los contrastes están medidos contra WCAG 2.2 AA y hay un test que los
// vigila para los 7 temas: app/src/test/java/com/cesar/ahorros/ContrasteTest.kt
// ---------------------------------------------------------------------------

/**
 * La paleta completa de un tema.
 *
 * Los nombres son los mismos que usaba la app antes de que existieran los
 * temas, para que cada token siga significando exactamente lo mismo.
 */
data class TemaVisual(
    /** Identificador único: se guarda en el teléfono para recordar el tema. */
    val id: String,
    /** Nombre que se ve en el selector de Ajustes. */
    val nombre: String,
    /** Una línea de 설명 del tema. */
    val descripcion: String,
    /** `true` si el tema tiene fondo claro (texto oscuro encima). */
    val esClaro: Boolean,
    /** El fondo de la app. */
    val fondo: Color,
    /** Tarjetas, barra inferior. */
    val superficie: Color,
    /** KPI, encabezados de día, chips. */
    val superficieAlta: Color,
    /** Hojas y superficies más altas. */
    val superficieMaxima: Color,
    /** Borde de control. */
    val borde: Color,
    /** Separadores decorativos. */
    val hairline: Color,
    /** Texto principal. */
    val textoPrimario: Color,
    /** Texto secundario. */
    val textoSecundario: Color,
    /** Texto terciario. */
    val textoTercero: Color,
    /** Acento de marca: botones, íconos seleccionados. */
    val acentoCalido: Color,
    /** Verde: los ahorros. */
    val acentoMenta: Color,
    /** Azul: serie de las gráficas. */
    val acentoAzul: Color,
    /** Violeta: serie de las gráficas. */
    val acentoVioleta: Color,
    /** Cian de foco: YAPI, adjuntos. */
    val acentoHielo: Color,
    /** Rojo: los gastos y los errores. */
    val colorGasto: Color,
    /** Ámbar: avisos. */
    val colorAviso: Color
) {

    /**
     * Las 6 series de las gráficas, siempre en el mismo orden.
     * El orden importa: los tests lo vigilan.
     */
    val paleta: List<Color>
        get() = listOf(
            acentoCalido, acentoAzul, acentoMenta,
            acentoVioleta, colorAviso, colorGasto
        )

    /**
     * El esquema de Material 3 de este tema.
     *
     * Se fijan TODOS los roles a mano (también los de los temas claros): por
     * eso el mismo constructor sirve para los dos casos y no queda ningún
     * color "por defecto" de Material que se cuele sin querer.
     */
    fun esquema(): ColorScheme {
        val contenedorPrimario = mezclar(acentoCalido, fondo, 0.18f)
        val contenedorSecundario = mezclar(acentoMenta, fondo, 0.18f)
        val contenedorTerciario = mezclar(acentoHielo, fondo, 0.18f)
        val contenedorError = mezclar(colorGasto, fondo, 0.18f)
        val superficieVariante = mezclar(superficie, textoPrimario, 0.07f)
        val superficieBaja = mezclar(superficie, fondo, 0.45f)
        val superficieLaMasBaja = mezclar(superficie, fondo, 0.22f)
        val superficieBrillante = if (esClaro) {
            Color(0xFFFFFFFF)
        } else {
            mezclar(superficie, textoPrimario, 0.10f)
        }
        val superficieOscura = if (esClaro) {
            mezclar(fondo, textoPrimario, 0.10f)
        } else {
            mezclar(fondo, textoPrimario, 0.04f)
        }

        return lightColorScheme(
            primary = acentoCalido,
            onPrimary = fondo,
            primaryContainer = contenedorPrimario,
            onPrimaryContainer = textoPrimario,
            inversePrimary = acentoCalido,

            secondary = acentoMenta,
            onSecondary = fondo,
            secondaryContainer = contenedorSecundario,
            onSecondaryContainer = textoPrimario,

            tertiary = acentoHielo,
            onTertiary = fondo,
            tertiaryContainer = contenedorTerciario,
            onTertiaryContainer = textoPrimario,

            background = fondo,
            onBackground = textoPrimario,

            surface = superficie,
            onSurface = textoPrimario,
            surfaceVariant = superficieVariante,
            onSurfaceVariant = textoTercero,
            surfaceTint = Color.Transparent,

            inverseSurface = textoPrimario,
            inverseOnSurface = fondo,

            error = colorGasto,
            onError = fondo,
            errorContainer = contenedorError,
            onErrorContainer = textoPrimario,

            outline = borde,
            outlineVariant = hairline,

            scrim = Color(0xFF000000),

            surfaceBright = superficieBrillante,
            surfaceDim = superficieOscura,
            surfaceContainerLowest = superficieLaMasBaja,
            surfaceContainerLow = superficieBaja,
            surfaceContainer = superficie,
            surfaceContainerHigh = superficieAlta,
            surfaceContainerHighest = superficieMaxima
        )
    }
}

/** Mezcla dos colores: `factor` es cuánto del primero entra (0..1). */
private fun mezclar(primero: Color, segundo: Color, factor: Float): Color = Color(
    red = primero.red * factor + segundo.red * (1f - factor),
    green = primero.green * factor + segundo.green * (1f - factor),
    blue = primero.blue * factor + segundo.blue * (1f - factor),
    alpha = primero.alpha * factor + segundo.alpha * (1f - factor)
)

/**
 * Los 7 temas de la app.
 *
 * Los seis primeros salen de las webs de referencia que eligió Cesar; el
 * primero es el tema con el que nació la app (el sistema "Awesomic"), que se
 * queda como opción por defecto.
 */
object CatalogoTemas {

    /** El tema de siempre: casi-negro con naranja. */
    val CLASICO = TemaVisual(
        id = "clasico",
        nombre = "Clásico",
        descripcion = "El de siempre: negro y naranja",
        esClaro = false,
        fondo = Color(0xFF040308),
        superficie = Color(0xFF14111F),
        superficieAlta = Color(0xFF1C1929),
        superficieMaxima = Color(0xFF252133),
        borde = Color(0xFF71717A),
        hairline = Color(0xFF2C2B46),
        textoPrimario = Color(0xFFF4F4F5),
        textoSecundario = Color(0xFFA1A1AA),
        textoTercero = Color(0xFFD4D4D8),
        acentoCalido = Color(0xFFFF5A00),
        acentoMenta = Color(0xFF91F58E),
        acentoAzul = Color(0xFF3898EC),
        acentoVioleta = Color(0xFF9E7BF0),
        acentoHielo = Color(0xFFB8FEFF),
        colorGasto = Color(0xFFFD542D),
        colorAviso = Color(0xFFFFC24D)
    )

    /** Negro puro con el brillo rojo-naranja en el centro. */
    val OSMO = TemaVisual(
        id = "osmo",
        nombre = "Osmo",
        descripcion = "Negro con un brillo naranja",
        esClaro = false,
        fondo = Color(0xFF000000),
        superficie = Color(0xFF0B0B0B),
        superficieAlta = Color(0xFF141414),
        superficieMaxima = Color(0xFF1E1E1E),
        borde = Color(0xFF6E6E6E),
        hairline = Color(0xFF242424),
        textoPrimario = Color(0xFFFFFFFF),
        textoSecundario = Color(0xFFA8A8A8),
        textoTercero = Color(0xFFDCDCDC),
        acentoCalido = Color(0xFFFF5A00),
        acentoMenta = Color(0xFF8FE8A0),
        acentoAzul = Color(0xFF5AA9FF),
        acentoVioleta = Color(0xFFB79CFF),
        acentoHielo = Color(0xFF9FD8FF),
        colorGasto = Color(0xFFFF4D4D),
        colorAviso = Color(0xFFFFC24D)
    )

    /** Blanco editorial con naranja quemado y violeta. */
    val CALDERA = TemaVisual(
        id = "caldera",
        nombre = "Caldera",
        descripcion = "Claro: naranja y violeta sobre blanco",
        esClaro = true,
        fondo = Color(0xFFFFFFFF),
        superficie = Color(0xFFF7F3EF),
        superficieAlta = Color(0xFFEFE9E2),
        superficieMaxima = Color(0xFFE4DCD2),
        borde = Color(0xFF8A7F72),
        hairline = Color(0xFFE7DED3),
        textoPrimario = Color(0xFF0B0B0B),
        textoSecundario = Color(0xFF6B6157),
        textoTercero = Color(0xFF2B2724),
        acentoCalido = Color(0xFFB53808),
        acentoMenta = Color(0xFF0E6B33),
        acentoAzul = Color(0xFF14509E),
        acentoVioleta = Color(0xFF6B2FBF),
        acentoHielo = Color(0xFF0E6E6E),
        colorGasto = Color(0xFFB3261E),
        colorAviso = Color(0xFF8A5A00)
    )

    /** Azul noche con menta y violeta. */
    val SAFEPAL = TemaVisual(
        id = "safepal",
        nombre = "SafePal",
        descripcion = "Azul noche con menta y violeta",
        esClaro = false,
        fondo = Color(0xFF151138),
        superficie = Color(0xFF1D1745),
        superficieAlta = Color(0xFF262056),
        superficieMaxima = Color(0xFF322A6B),
        borde = Color(0xFF8A84C7),
        hairline = Color(0xFF322A6B),
        textoPrimario = Color(0xFFFFFFFF),
        textoSecundario = Color(0xFFBDB6E8),
        textoTercero = Color(0xFFE8E5F8),
        acentoCalido = Color(0xFFA08FFF),
        acentoMenta = Color(0xFF6EE7B7),
        acentoAzul = Color(0xFF38BDF8),
        acentoVioleta = Color(0xFFC084FC),
        acentoHielo = Color(0xFFB8FEFF),
        colorGasto = Color(0xFFFB7185),
        colorAviso = Color(0xFFFCD34D)
    )

    /** Verde oscuro con la píldora cian y amarilla. */
    val BOOST = TemaVisual(
        id = "boost",
        nombre = "Boost",
        descripcion = "Verde oscuro con cian y amarillo",
        esClaro = false,
        fondo = Color(0xFF00201F),
        superficie = Color(0xFF032B29),
        superficieAlta = Color(0xFF073634),
        superficieMaxima = Color(0xFF0B423F),
        borde = Color(0xFF4E8C89),
        hairline = Color(0xFF0B423F),
        textoPrimario = Color(0xFFFFFFFF),
        textoSecundario = Color(0xFFA8C9C6),
        textoTercero = Color(0xFFDCEDEC),
        acentoCalido = Color(0xFF22D3EE),
        acentoMenta = Color(0xFF4ADE80),
        acentoAzul = Color(0xFF60A5FA),
        acentoVioleta = Color(0xFFC084FC),
        acentoHielo = Color(0xFFA5F3FC),
        colorGasto = Color(0xFFFF6B6B),
        colorAviso = Color(0xFFFDE047)
    )

    /** Verde bosque con el verde lima. */
    val MODE = TemaVisual(
        id = "mode",
        nombre = "Mode",
        descripcion = "Verde bosque con lima",
        esClaro = false,
        fondo = Color(0xFF0D2B20),
        superficie = Color(0xFF123A2B),
        superficieAlta = Color(0xFF174A35),
        superficieMaxima = Color(0xFF1E5A40),
        borde = Color(0xFF6BA585),
        hairline = Color(0xFF1E5A40),
        textoPrimario = Color(0xFFFFFFFF),
        textoSecundario = Color(0xFFB5CFC2),
        textoTercero = Color(0xFFE4F0E9),
        acentoCalido = Color(0xFFC6F24E),
        acentoMenta = Color(0xFF6EE7A8),
        acentoAzul = Color(0xFF7DD3FC),
        acentoVioleta = Color(0xFFD8B4FE),
        acentoHielo = Color(0xFFCCFBF1),
        colorGasto = Color(0xFFFCA5A5),
        colorAviso = Color(0xFFFDE68A)
    )

    /** Blanco de cuaderno con tinta negra y trazos a mano. */
    val TRUNK = TemaVisual(
        id = "trunk",
        nombre = "Trunk",
        descripcion = "Claro: papel y tinta negra",
        esClaro = true,
        fondo = Color(0xFFFFFFFF),
        superficie = Color(0xFFF4F4F2),
        superficieAlta = Color(0xFFEAEAE6),
        superficieMaxima = Color(0xFFDEDEDA),
        borde = Color(0xFF767676),
        hairline = Color(0xFFE2E2DD),
        textoPrimario = Color(0xFF0A0A0A),
        textoSecundario = Color(0xFF5F5F5F),
        textoTercero = Color(0xFF262626),
        acentoCalido = Color(0xFF111111),
        acentoMenta = Color(0xFF0E6B33),
        acentoAzul = Color(0xFF14509E),
        acentoVioleta = Color(0xFF6B2FBF),
        acentoHielo = Color(0xFF0E6E6E),
        colorGasto = Color(0xFFB3261E),
        colorAviso = Color(0xFF8A5A00)
    )

    /** Todos los temas, en el orden en que se ven en Ajustes. */
    val todos: List<TemaVisual> = listOf(CLASICO, OSMO, CALDERA, SAFEPAL, BOOST, MODE, TRUNK)

    /** El tema con el que arranca la app. */
    val porDefecto: TemaVisual = CLASICO

    /** Busca un tema por su id; si no existe, devuelve el de por defecto. */
    fun porId(id: String?): TemaVisual =
        todos.firstOrNull { it.id == id } ?: porDefecto
}

/**
 * El tema que está activo ahora mismo.
 *
 * Es estado de Compose (`mutableStateOf`): cuando cambia, todo lo que lee un
 * token de color se redibuja solo, sin tocar ni una línea de las pantallas.
 *
 * Son dos cosas juntas:
 *  - el **tema base** (uno de los 7 del catálogo), y
 *  - la **paleta del usuario** (los colores que cambió a mano).
 *
 * El tema que se ve es el base con la paleta encima, y se recalcula solo
 * cuando cambia cualquiera de las dos.
 */
object TemaActual {

    private val base = mutableStateOf(CatalogoTemas.porDefecto)

    /** El tema del catálogo que está elegido (sin los colores del usuario). */
    var temaBase: TemaVisual
        get() = base.value
        set(valor) {
            base.value = valor
        }

    /**
     * Los colores que el usuario cambió a mano.
     *
     * Vive en [AparienciaActual] (que es donde se guardan los demás ajustes
     * de apariencia) pero se expone acá porque el tema es quien los usa.
     */
    var paleta: PaletaPersonalizada
        get() = AparienciaActual.paleta
        set(valor) {
            AparienciaActual.paleta = valor
        }

    /**
     * El tema que se ve: el base con la paleta del usuario encima.
     *
     * `derivedStateOf` lo recalcula SOLO cuando cambia el tema base o la
     * paleta: por eso cambiar un color repinta la app entera sin tocar las
     * pantallas, y leer el tema no cuesta nada.
     */
    val tema: TemaVisual by derivedStateOf {
        base.value.conPaleta(AparienciaActual.paleta)
    }

    /**
     * Una clave para los `remember`.
     *
     * Si solo se usara el id del tema, cambiar un color no volvería a
     * dibujar lo que depende de los colores (por ejemplo, la paleta de la
     * dona de las gráficas). Con esta clave, cualquier cambio repinta.
     */
    val clave: String
        get() = "${base.value.id}#${AparienciaActual.paleta.huella}"

    /**
     * Cambia al tema con ese id.
     * Devuelve `true` si el tema era distinto al que ya estaba.
     */
    fun aplicar(id: String?): Boolean {
        val nuevo = CatalogoTemas.porId(id)
        if (nuevo.id == base.value.id) return false
        base.value = nuevo
        return true
    }
}

// ---------------------------------------------------------------------------
// Los tokens de color.
//
// Son los mismos nombres que usaba la app de antes, pero ahora leen del tema
// activo. Quien los use (dibujar un color, escribir un texto) los usa igual
// que siempre.
// ---------------------------------------------------------------------------

/** Fondo de la app. */
val Fondo: Color get() = TemaActual.tema.fondo

/** Tarjetas y barra inferior. */
val Superficie: Color get() = TemaActual.tema.superficie

/** KPI, encabezados de día, chips. */
val SuperficieAlta: Color get() = TemaActual.tema.superficieAlta

/** Hojas y superficies más altas. */
val SuperficieMaxima: Color get() = TemaActual.tema.superficieMaxima

/** Borde de control. */
val Borde: Color get() = TemaActual.tema.borde

/** Separadores decorativos. */
val Hairline: Color get() = TemaActual.tema.hairline

/** Texto principal. */
val TextoPrimario: Color get() = TemaActual.tema.textoPrimario

/** Texto secundario. */
val TextoSecundario: Color get() = TemaActual.tema.textoSecundario

/** Texto terciario. */
val TextoTercero: Color get() = TemaActual.tema.textoTercero

/** Acento de marca: botones, íconos seleccionados. */
val AcentoCalido: Color get() = TemaActual.tema.acentoCalido

/** Verde: los ahorros. */
val AcentoMenta: Color get() = TemaActual.tema.acentoMenta

/** Azul: serie de las gráficas. */
val AcentoAzul: Color get() = TemaActual.tema.acentoAzul

/** Violeta: serie de las gráficas. */
val AcentoVioleta: Color get() = TemaActual.tema.acentoVioleta

/** Cian de foco: YAPI, adjuntos. */
val AcentoHielo: Color get() = TemaActual.tema.acentoHielo

/** Rojo: los gastos y los errores. */
val ColorGasto: Color get() = TemaActual.tema.colorGasto

/** Ámbar: avisos. */
val ColorAviso: Color get() = TemaActual.tema.colorAviso

/** Paleta categórica para las gráficas: 6 series, siempre en este orden. */
val PaletaCategorica: List<Color> get() = TemaActual.tema.paleta

// Radios (canónicos de Awesomic)
val RadioTarjeta = 20.dp
val RadioBoton = 16.dp
val RadioChip = 999.dp

/** Cifras de dinero: siempre con cifras tabulares para que el decimal no baile. */
val EstiloDinero = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum"
)

private val TipografiaApp = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
)

private val FormasApp = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(RadioBoton),
    large = RoundedCornerShape(RadioTarjeta),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Envuelve la app en el tema activo.
 *
 * Lee el tema dentro de la composición: cuando el usuario cambia de tema en
 * Ajustes, esta función se vuelve a ejecutar y toda la app se repinta.
 */
@Composable
fun TemaAhorros(contenido: @Composable () -> Unit) {
    val tema = TemaActual.tema
    MaterialTheme(
        colorScheme = tema.esquema(),
        typography = TipografiaApp,
        shapes = FormasApp,
        content = contenido
    )
}
