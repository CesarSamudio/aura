package com.cesar.ahorros.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * La pantalla de **Ajustes**.
 *
 * Tres secciones, en este orden:
 *  1. **Apariencia**: el selector de temas. Cada tema se ve con una
 *     mini-vista previa de cómo queda (fondo, texto y botón con su acento)
 *     y se aplica al instante, sin reiniciar la app.
 *  2. **Copia de seguridad**: el `.zip` que el usuario guarda donde él quiera
 *     (Drive, WhatsApp, correo...) y que puede volver a meter cuando la
 *     necesite.
 *  3. **Acerca de**: la app, su versión y dónde viven los datos.
 *
 * No hay nube ni cuentas: la app no tiene ningún permiso y no habla con
 * nadie.
 */
@Composable
fun PantallaAjustes(
    versionApp: String,
    temas: List<TemaVisual>,
    temaActualId: String,
    onCambiarTema: (String) -> Unit,
    onVolver: () -> Unit,
    onExportar: () -> Unit,
    onRestaurar: (Uri) -> Unit,
    onCerrar: () -> Unit
) {
    var restaurando by remember { mutableStateOf(false) }
    var confirmandoRestaurar by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // El fondo se pinta acá: si la pantalla se abre sola (o en las
            // capturas) no puede quedar el fondo del sistema detrás.
            .background(Fondo)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ------------------------------------------------------------------
        // Barra superior
        // ------------------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextoPrimario,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextoPrimario,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCerrar) {
                Text(
                    text = "Cerrar",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextoSecundario
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ------------------------------------------------------------------
        // 1. Apariencia
        // ------------------------------------------------------------------
        TituloSeccion("Apariencia")

        Text(
            text = "Tocá un tema y la app se repinta al instante.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )

        Spacer(Modifier.height(12.dp))

        temas.forEachIndexed { indice, tema ->
            if (indice > 0) Spacer(Modifier.height(8.dp))
            FilaTema(
                tema = tema,
                esActual = tema.id == temaActualId,
                onClick = { onCambiarTema(tema.id) }
            )
        }

        Spacer(Modifier.height(18.dp))

        // ------------------------------------------------------------------
        // 2. Copia de seguridad
        // ------------------------------------------------------------------
        TituloSeccion("Copia de seguridad")

        Surface(
            color = Superficie,
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tus datos viven dentro de la app, en este teléfono. La app no " +
                        "tiene ningún permiso y no manda nada a ningún lado: la copia la " +
                        "guardás vos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoTercero
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onExportar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(RadioBoton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcentoCalido,
                        contentColor = Fondo
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Guardar una copia",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Crea un archivo .zip con todos tus movimientos, deudas, " +
                        "comprobantes y pruebas. Después lo guardás en Drive, lo mandás " +
                        "por WhatsApp o por correo: donde vos quieras.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { restaurando = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Restaurar una copia")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Elegís un archivo .zip que hayas guardado antes y se reemplaza " +
                        "lo que hay ahora en el teléfono.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }

        // ------------------------------------------------------------------
        // 3. Acerca de (aquí viven también los datos de la app)
        // ------------------------------------------------------------------
        Spacer(Modifier.height(18.dp))
        TituloSeccion("Acerca de")

        Surface(
            color = Superficie,
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Aura",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = versionApp,
                        style = EstiloDinero,
                        color = TextoSecundario
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tus ahorros, gastos y deudas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Los datos y las imágenes de los comprobantes viven dentro de " +
                        "la app, en este teléfono. Nada sale de acá: la app no tiene " +
                        "internet ni ningún permiso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoTercero
                )
                Spacer(Modifier.height(12.dp))
                FilaDato(
                    etiqueta = "Dónde viven los datos",
                    valor = "Dentro de la app",
                    colorValor = AcentoHielo
                )
                Spacer(Modifier.height(8.dp))
                FilaDato(
                    etiqueta = "Permisos de la app",
                    valor = "Ninguno",
                    colorValor = AcentoMenta
                )
            }
        }

        Spacer(Modifier.height(96.dp))
    }

    // La restauración necesita un archivo: se avisa antes de reemplazar todo.
    confirmandoRestaurar?.let { archivo ->
        DialogoConfirmarBorrar(
            titulo = "¿Restaurar esta copia?",
            texto = "Se van a reemplazar TODOS los datos que tenés ahora en el " +
                "teléfono por los de la copia. Lo que tengas ahora y no esté en esa " +
                "copia se pierde.",
            onConfirmar = {
                onRestaurar(archivo)
                confirmandoRestaurar = null
            },
            onCerrar = { confirmandoRestaurar = null }
        )
    }
}

/**
 * Una fila del selector de temas: la mini-vista previa, el nombre, la
 * descripción y el check del tema que está puesto.
 */
@Composable
private fun FilaTema(
    tema: TemaVisual,
    esActual: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(RadioBoton)
    Surface(
        color = if (esActual) SuperficieAlta else Superficie,
        shape = forma,
        border = BorderStroke(
            width = if (esActual) 1.5.dp else 1.dp,
            color = if (esActual) AcentoCalido else Borde
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VistaPreviaTema(tema = tema, modifier = Modifier.size(72.dp, 52.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tema.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrimario
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tema.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            if (esActual) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Tema activo",
                    tint = AcentoCalido,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * La mini-vista previa de un tema: un rectángulo con el fondo del tema, una
 * barrita de texto y un botón con su color de acento.
 *
 * Se dibuja con `Canvas` (cero imágenes, cero librerías): son cuatro
 * rectángulos con los colores del tema.
 */
@Composable
private fun VistaPreviaTema(tema: TemaVisual, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radio = 10.dp.toPx()
        val margen = 6.dp.toPx()

        // El fondo del tema.
        drawRoundRect(
            color = tema.fondo,
            cornerRadius = CornerRadius(radio)
        )

        // Una tarjeta del tema, adentro.
        drawRoundRect(
            color = tema.superficie,
            topLeft = Offset(margen, margen),
            size = Size(size.width - margen * 2, size.height - margen * 2),
            cornerRadius = CornerRadius(radio * 0.7f)
        )

        // Una barrita de texto (la línea "grande").
        drawRoundRect(
            color = tema.textoPrimario,
            topLeft = Offset(margen + 7.dp.toPx(), margen + 7.dp.toPx()),
            size = Size(size.width * 0.42f, 5.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // Otra más finita (la línea "chica").
        drawRoundRect(
            color = tema.textoSecundario,
            topLeft = Offset(margen + 7.dp.toPx(), margen + 16.dp.toPx()),
            size = Size(size.width * 0.30f, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // El botón/chip con el color de acento del tema.
        drawRoundRect(
            color = tema.acentoCalido,
            topLeft = Offset(margen + 7.dp.toPx(), size.height - margen - 15.dp.toPx()),
            size = Size(size.width * 0.36f, 9.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

/** Una fila de "etiqueta: valor". */
@Composable
private fun FilaDato(etiqueta: String, valor: String, colorValor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall,
            color = colorValor
        )
    }
}
