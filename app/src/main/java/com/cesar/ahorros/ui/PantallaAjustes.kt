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
import com.cesar.ahorros.data.ColorTema
import com.cesar.ahorros.data.ColorTexto
import com.cesar.ahorros.data.EstiloGraficas
import com.cesar.ahorros.data.EstiloInterfaz
import com.cesar.ahorros.data.EstiloNumeros
import com.cesar.ahorros.data.FormaDegradado
import com.cesar.ahorros.data.FondosApp

/**
 * La pantalla de **Ajustes**.
 *
 * Está dividida en **secciones** para que no sea una lista interminable:
 *
 *  1. **Apariencia**: los temas de fábrica (un plegable) y el tema
 *     personalizado (otro plegable, con los colores, el fondo y las
 *     animaciones adentro).
 *  2. **Preferencias**: cómo se relacionan los ahorros con las deudas y con
 *     los gastos (un plegable).
 *  3. **Copia de seguridad**: el `.zip` que el usuario guarda donde él quiera
 *     (Drive, WhatsApp, correo...) y que puede volver a meter cuando la
 *     necesite.
 *  4. **Acerca de**: la app, su versión y dónde viven los datos.
 *
 * Dentro de la primera sección todo lo nuevo está en plegables: el usuario
 * ve "Temas" y "Tema personalizado", y solo abre lo que le interesa. Las
 * preferencias también van en un plegable: son dos cosas, y de entrada no
 * interestan a todo el mundo.
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
    onElegirFondo: () -> Unit,
    onQuitarFondo: () -> Unit,
    onVolver: () -> Unit,
    onExportar: () -> Unit,
    onRestaurar: (Uri) -> Unit,
    onCerrar: () -> Unit
) {
    var restaurando by remember { mutableStateOf(false) }
    var confirmandoRestaurar by remember { mutableStateOf<Uri?>(null) }

    // Qué color se está editando (si hay alguno abierto).
    var editor by remember { mutableStateOf<EditorAbierto?>(null) }
    // La explicación que se está mostrando (aparece al apretar y soltar).
    var explicacion by remember { mutableStateOf<Explicacion?>(null) }

    val tema = TemaActual.tema

    Column(
        modifier = Modifier
            .fillMaxSize()
            // El fondo se pinta acá: si la pantalla se abre sola (o en las
            // capturas) no puede quedar el fondo del sistema detrás.
            // Con una foto o un degradado de fondo, deja transparentarse.
            .background(if (AparienciaActual.hayFondo) Color.Transparent else Fondo)
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

        // --- 1.1 Los temas de fábrica -------------------------------
        SeccionPlegable(
            titulo = "Temas",
            subtitulo = "Los ${temas.size} temas de fábrica. Tocá uno y la app se repinta al instante.",
            inicialmenteAbierta = false,
            onExplicacion = {
                explicacion = Explicacion(
                    "Los temas de fábrica",
                    "Cada tema es una paleta completa: fondo, tarjetas, textos y los " +
                        "colores de las gráficas. Los siete están medidos contra WCAG " +
                        "2.2 AA, así que los textos se leen en todos.\n\n" +
                        "El tema que elijas es el punto de partida: si después " +
                        "personalizás los colores, esos cambios se quedan y el resto " +
                        "sigue saliendo del tema."
                )
            }
        ) {
            temas.forEachIndexed { indice, item ->
                if (indice > 0) Spacer(Modifier.height(8.dp))
                FilaTema(
                    tema = item,
                    esActual = item.id == temaActualId,
                    onClick = { onCambiarTema(item.id) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- 1.2 El tema personalizado -------------------------------
        SeccionPlegable(
            titulo = "Tema personalizado",
            subtitulo = "Los colores, el fondo y las animaciones, a tu gusto."
        ) {
            // --- Colores ----------------------------------------------
            SeccionPlegable(
                titulo = "Colores",
                subtitulo = "La paleta de la app y la de los textos."
            ) {
                SubtituloAjuste("Paleta de la app")

                ColorTema.entries.forEach { slot ->
                    val colorActual = tema.colorDe(slot)
                    val propio = TemaActual.paleta.colorTema(slot)
                    Spacer(Modifier.height(6.dp))
                    MuestraColor(
                        color = colorActual,
                        etiqueta = slot.etiqueta,
                        descripcion = slot.descripcion,
                        esPersonalizado = propio != null,
                        // Los colores que SON el fondo no se comparan con el
                        // fondo: el aviso "casi igual al fondo" no diría nada
                        // útil ahí, solo molestaría.
                        avisaSiEsInvisible = slot !in COLORES_DE_FONDO,
                        onClick = {
                            editor = EditorAbierto(
                                titulo = slot.etiqueta,
                                inicial = colorActual,
                                alCambiar = { nuevo ->
                                    TemaActual.paleta = TemaActual.paleta.conTema(slot, nuevo)
                                }
                            )
                        },
                        onRestablecer = if (propio != null) {
                            {
                                TemaActual.paleta = TemaActual.paleta.conTema(slot, null)
                            }
                        } else {
                            null
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))
                SubtituloAjuste("Paleta de los textos")

                ColorTexto.entries.forEach { slot ->
                    val colorActual = tema.colorDe(slot)
                    val propio = TemaActual.paleta.colorTexto(slot)
                    Spacer(Modifier.height(6.dp))
                    MuestraColor(
                        color = colorActual,
                        etiqueta = slot.etiqueta,
                        descripcion = slot.descripcion,
                        esPersonalizado = propio != null,
                        onClick = {
                            editor = EditorAbierto(
                                titulo = slot.etiqueta,
                                inicial = colorActual,
                                alCambiar = { nuevo ->
                                    TemaActual.paleta = TemaActual.paleta.conTexto(slot, nuevo)
                                }
                            )
                        },
                        onRestablecer = if (propio != null) {
                            {
                                TemaActual.paleta = TemaActual.paleta.conTexto(slot, null)
                            }
                        } else {
                            null
                        }
                    )
                }

                if (!TemaActual.paleta.vacia) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            TemaActual.paleta = PaletaPersonalizada.VACIA
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Volver los colores del tema")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- El fondo ----------------------------------------------
            SeccionPlegable(
                titulo = "Fondo",
                subtitulo = "Una foto del teléfono o un degradado que se mueve."
            ) {
                SubtituloAjuste("Imagen del dispositivo")

                VistaPreviaFondo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(RadioBoton))
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Se recomienda una imagen de ${FondosApp.RESOLUCION_RECOMENDADA.first} × " +
                        "${FondosApp.RESOLUCION_RECOMENDADA.second} px. Si la tuya es más " +
                        "grande, la app la reescala para que se vea bien y no pese tanto. " +
                        "La imagen se copia adentro de la app: no se toca la de tu galería.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )

                Spacer(Modifier.height(10.dp))

                if (AparienciaActual.fondo.imagen == null) {
                    Button(
                        onClick = onElegirFondo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadioBoton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcentoCalido,
                            contentColor = Fondo
                        )
                    ) {
                        Text("Elegir una imagen")
                    }
                } else {
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onElegirFondo,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(RadioBoton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AcentoCalido,
                                contentColor = Fondo
                            )
                        ) {
                            Text("Cambiarla")
                        }
                        OutlinedButton(
                            onClick = onQuitarFondo,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Quitar")
                        }
                    }
                }

                if (AparienciaActual.fondo.imagen != null) {
                    Spacer(Modifier.height(14.dp))

                    FilaDeslizador(
                        titulo = "Opacidad",
                        valor = AparienciaActual.fondo.opacidadImagen,
                        textoValor = "${(AparienciaActual.fondo.opacidadImagen * 100).toInt()}%",
                        onCambio = { nuevo ->
                            AparienciaActual.fondo = AparienciaActual.fondo.copy(opacidadImagen = nuevo)
                        }
                    )

                    FilaDeslizador(
                        titulo = "Difuminado",
                        valor = AparienciaActual.fondo.difuminadoImagen,
                        textoValor = "${(AparienciaActual.fondo.difuminadoImagen * 100).toInt()}%",
                        onCambio = { nuevo ->
                            AparienciaActual.fondo = AparienciaActual.fondo.copy(difuminadoImagen = nuevo)
                        }
                    )
                }

                Spacer(Modifier.height(6.dp))
                SubtituloAjuste("Fondo animado")

                FilaInterruptor(
                    titulo = "Degradado animado",
                    descripcion = "Un fondo de colores que se mueve despacio.",
                    valor = AparienciaActual.fondo.degradadoActivo,
                    onCambio = { nuevo ->
                        AparienciaActual.fondo = AparienciaActual.fondo.copy(degradadoActivo = nuevo)
                    }
                )

                if (AparienciaActual.fondo.degradadoActivo) {
                    Spacer(Modifier.height(10.dp))

                    FilaOpciones(
                        opciones = FormaDegradado.entries,
                        seleccionada = AparienciaActual.formaDegradado,
                        etiqueta = { it.etiqueta },
                        onElegir = { nuevo ->
                            AparienciaActual.fondo = AparienciaActual.fondo.copy(formaDegradado = nuevo.id)
                        },
                        onExplicacion = { nuevo ->
                            explicacion = Explicacion(nuevo.etiqueta, nuevo.explicacion)
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    FilaDeslizador(
                        titulo = "Opacidad",
                        valor = AparienciaActual.fondo.opacidadDegradado,
                        textoValor = "${(AparienciaActual.fondo.opacidadDegradado * 100).toInt()}%",
                        onCambio = { nuevo ->
                            AparienciaActual.fondo = AparienciaActual.fondo.copy(opacidadDegradado = nuevo)
                        }
                    )

                    FilaDeslizador(
                        titulo = "Velocidad",
                        valor = normalizarVelocidad(AparienciaActual.fondo.velocidadDegradado),
                        textoValor = "${numeroATexto(AparienciaActual.fondo.velocidadDegradado)}×",
                        onCambio = { nuevo ->
                            AparienciaActual.fondo = AparienciaActual.fondo.copy(
                                velocidadDegradado = redondearVelocidad(nuevo)
                            )
                        },
                        minimo = 0.25f,
                        maximo = 3f
                    )

                    FilaDeslizador(
                        titulo = "Intensidad",
                        valor = AparienciaActual.fondo.intensidadDegradado,
                        textoValor = "${(AparienciaActual.fondo.intensidadDegradado * 100).toInt()}%",
                        onCambio = { nuevo ->
                            AparienciaActual.fondo =
                                AparienciaActual.fondo.copy(intensidadDegradado = nuevo)
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Las animaciones --------------------------------------
            SeccionPlegable(
                titulo = "Animaciones",
                subtitulo = "Cómo se mueven las gráficas, los números y la app."
            ) {
                FilaInterruptor(
                    titulo = "Animaciones",
                    descripcion = "Apagadas, la app se ve quieta: sin conteos, sin movimiento.",
                    valor = AparienciaActual.animaciones.activas,
                    onCambio = { nuevo ->
                        AparienciaActual.animaciones = AparienciaActual.animaciones.copy(activas = nuevo)
                    }
                )

                if (AparienciaActual.animaciones.activas) {
                    Spacer(Modifier.height(12.dp))

                    FilaDeslizador(
                        titulo = "Velocidad",
                        valor = normalizarVelocidad(AparienciaActual.animaciones.velocidad),
                        textoValor = velocidadATexto(AparienciaActual.animaciones.velocidad),
                        onCambio = { nuevo ->
                            AparienciaActual.animaciones = AparienciaActual.animaciones.copy(
                                // Se redondea a dos decimales: el deslizador es
                                // continuo, pero el número guardado y el que
                                // se ve tienen que ser legibles.
                                velocidad = redondearVelocidad(nuevo)
                            )
                        },
                        minimo = 0.25f,
                        maximo = 3f
                    )

                    Spacer(Modifier.height(6.dp))
                    SubtituloAjuste("Las gráficas")

                    FilaOpciones(
                        opciones = EstiloGraficas.entries,
                        seleccionada = EstiloGraficas.porId(AparienciaActual.animaciones.graficas),
                        etiqueta = { it.etiqueta },
                        onElegir = { nuevo ->
                            AparienciaActual.animaciones =
                                AparienciaActual.animaciones.copy(graficas = nuevo.id)
                        },
                        onExplicacion = { nuevo ->
                            explicacion = Explicacion(nuevo.etiqueta, nuevo.explicacion)
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                    SubtituloAjuste("Los números")

                    FilaOpciones(
                        opciones = EstiloNumeros.entries,
                        seleccionada = EstiloNumeros.porId(AparienciaActual.animaciones.numeros),
                        etiqueta = { it.etiqueta },
                        onElegir = { nuevo ->
                            AparienciaActual.animaciones =
                                AparienciaActual.animaciones.copy(numeros = nuevo.id)
                        },
                        onExplicacion = { nuevo ->
                            explicacion = Explicacion(nuevo.etiqueta, nuevo.explicacion)
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                    SubtituloAjuste("La interfaz")

                    FilaOpciones(
                        opciones = EstiloInterfaz.entries,
                        seleccionada = EstiloInterfaz.porId(AparienciaActual.animaciones.interfaz),
                        etiqueta = { it.etiqueta },
                        onElegir = { nuevo ->
                            AparienciaActual.animaciones =
                                AparienciaActual.animaciones.copy(interfaz = nuevo.id)
                        },
                        onExplicacion = { nuevo ->
                            explicacion = Explicacion(nuevo.etiqueta, nuevo.explicacion)
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Apretá y soltá una opción para saber qué hace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ------------------------------------------------------------------
        // 2. Preferencias
        // ------------------------------------------------------------------
        TituloSeccion("Preferencias")

        SeccionPlegable(
            titulo = "Ahorros, deudas y gastos",
            subtitulo = "Cómo se relacionan entre sí tus tres montos.",
            onExplicacion = {
                explicacion = Explicacion(
                    "Ahorros, deudas y gastos",
                    "Estas dos opciones cambian CÓMO se cuentan los números. Vienen " +
                        "apagadas: la app se comporta como siempre hasta que las prendés.\n\n" +
                        "• «Los pagos de las deudas van a los ahorros»: cuando marcás a " +
                        "alguien como pagado de una deuda, el dinero también se suma a tus " +
                        "ahorros (como un depósito más). Si después borrás ese pago, el " +
                        "depósito se borra también.\n\n" +
                        "• «Combinar ahorros con gastos»: en vez de dos montones aparte, los " +
                        "ahorros y los gastos se juntan en uno. Lo que tenés es lo que " +
                        "metiste menos lo que gastaste, así que al gastar, los ahorros bajan."
                )
            }
        ) {
            FilaInterruptor(
                titulo = "Los pagos de las deudas van a los ahorros",
                descripcion = "Cuando marcás a alguien como pagado, el dinero también " +
                    "se suma a tus ahorros.",
                valor = PreferenciasActual.pagosDeDeudasEnAhorros,
                onCambio = { nuevo ->
                    PreferenciasActual.preferencias =
                        PreferenciasActual.preferencias.copy(pagosDeDeudasEnAhorros = nuevo)
                }
            )

            Spacer(Modifier.height(14.dp))

            FilaInterruptor(
                titulo = "Combinar ahorros con gastos",
                descripcion = "Cuando gastás, el dinero baja de tus ahorros: lo que tenés " +
                    "es lo que metiste menos lo que gastaste.",
                valor = PreferenciasActual.combinarAhorrosYGastos,
                onCambio = { nuevo ->
                    PreferenciasActual.preferencias =
                        PreferenciasActual.preferencias.copy(combinarAhorrosYGastos = nuevo)
                }
            )
        }

        Spacer(Modifier.height(18.dp))

        // ------------------------------------------------------------------
        // 3. Copia de seguridad
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
        // 4. Acerca de (aquí viven también los datos de la app)
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

    // El editor de color (sale cuando se toca una muestra).
    editor?.let { abierto ->
        DialogoEditorColor(
            colorInicial = abierto.inicial,
            titulo = abierto.titulo,
            onCambio = abierto.alCambiar,
            onCerrar = { editor = null }
        )
    }

    // La explicación de una opción (sale al apretar y soltar un chip).
    explicacion?.let { texto ->
        DialogoExplicacion(
            titulo = texto.titulo,
            texto = texto.texto,
            onCerrar = { explicacion = null }
        )
    }
}

/** El editor de color que está abierto. */
private data class EditorAbierto(
    val titulo: String,
    val inicial: Color,
    val alCambiar: (Color) -> Unit
)

/**
 * Los colores que SON el fondo de la app.
 *
 * Con estos no se avisa "casi igual al fondo": el aviso compararía el color
 * consigo mismo y siempre saltaría.
 */
private val COLORES_DE_FONDO = setOf(
    ColorTema.FONDO,
    ColorTema.SUPERFICIE,
    ColorTema.SUPERFICIE_ALTA,
    ColorTema.SUPERFICIE_MAXIMA
)

/** La explicación que se está mostrando. */
private data class Explicacion(val titulo: String, val texto: String)

/**
 * La velocidad de las animaciones, en texto.
 *
 * La de 1.0 es la "normal": más alta, más rápido; más baja, más lento.
 */
private fun velocidadATexto(velocidad: Float): String {
    val valor = numeroATexto(velocidad)
    return when {
        velocidad < 0.75f -> "Lenta ($valor×)"
        velocidad < 1.25f -> "Normal ($valor×)"
        velocidad < 2f -> "Rápida ($valor×)"
        else -> "Muy rápida ($valor×)"
    }
}

/**
 * Un número con, como mucho, dos decimales y sin ceros de más.
 *
 * Sin esto el deslizador mostraba "0.5735294×", que es un número de máquina
 * y no algo que un humano pueda leer.
 */
private fun numeroATexto(valor: Float): String {
    val redondeado = Math.round(valor * 100.0) / 100.0
    return if (redondeado == Math.floor(redondeado)) {
        redondeado.toInt().toString()
    } else {
        String.format(java.util.Locale.ROOT, "%.2f", redondeado).trimEnd('0').trimEnd('.')
    }
}

/** La velocidad del degradado, que va de 0.25 a 3. */
private fun normalizarVelocidad(velocidad: Float): Float = velocidad.coerceIn(0.25f, 3f)

/**
 * La velocidad redondeada a dos decimales.
 *
 * El deslizador es continuo, así que sin redondear guardaría cosas como
 * 0.5735294: un número de máquina, que ni se lee ni sirve de nada.
 */
private fun redondearVelocidad(velocidad: Float): Float =
    (Math.round(velocidad * 100.0) / 100.0).toFloat()

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
