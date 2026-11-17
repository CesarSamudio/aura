package com.cesar.ahorros.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.cesar.ahorros.R
import com.cesar.ahorros.data.AppViewModel
import com.cesar.ahorros.data.CopiaLocal
import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.FondosApp
import com.cesar.ahorros.data.FuenteAhorro
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.Movimiento
import com.cesar.ahorros.data.Pago
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.data.Preferencias
import com.cesar.ahorros.data.Tipo
import com.cesar.ahorros.dominio.CalculosDeudas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * La activity que está atrás de un contexto.
 *
 * El contexto de Compose puede ser la activity o un wrapper de ella: esto
 * desenrolla el wrapper para poder llegar a la activity (que es la que sabe
 * cerrarse cuando el usuario sale de la app).
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private enum class Pestana(val etiqueta: String, val icono: Int, val tipo: Tipo?) {    AHORROS("Ahorros", R.drawable.ic_savings, Tipo.AHORRO),
    GASTOS("Gastos", R.drawable.ic_gastos, Tipo.GASTO),
    DEUDAS("Deudas", R.drawable.ic_deudas, null),
    BALANCE("Balance", R.drawable.ic_balance, null)
}

private data class DialogoAbierto(val tipo: Tipo, val movimiento: Movimiento?)

/** Qué diálogo de deuda está abierto: uno nuevo o uno en edición. */
private data class DialogoDeudaAbierto(val deuda: Deuda?, val individual: Boolean = false)

/** A quién se le está registrando un pago. */
private data class DialogoPagoAbierto(val persona: PersonaDeuda)

/** A quién se le está editando el nombre o el monto propio. */
private data class DialogoPersonaAbierto(val persona: PersonaDeuda?)

/** A quién se le están adjuntando pruebas (comprobantes). */
private data class DialogoPruebasAbierto(val personaId: String, val nombre: String)

/** En qué parte de la pestaña Deudas está el usuario. */
private enum class VistaDeudas { LISTA, DETALLE, ANADIR_PERSONAS }

@Composable
fun App() {
    val vm: AppViewModel = viewModel()
    val movimientos by vm.movimientos.collectAsStateWithLifecycle()
    val deudas by vm.deudas.collectAsStateWithLifecycle()
    val listas by vm.listas.collectAsStateWithLifecycle()
    val listaDelColegioBorrada by vm.listaDelColegioBorrada.collectAsStateWithLifecycle()

    var pestana by rememberSaveable { mutableStateOf(Pestana.AHORROS) }
    var dialogo by remember { mutableStateOf<DialogoAbierto?>(null) }
    var aBorrar by remember { mutableStateOf<Movimiento?>(null) }
    // Set<String> no es guardable por Bundle, así que este estado no sobrevive
    // a la rotación. No importa: los días plegados vuelven a abrirse.
    var diasColapsados by remember { mutableStateOf(setOf<String>()) }

    // Estado de la pestaña Deudas.
    var deudaAbierta by rememberSaveable { mutableStateOf<String?>(null) }
    var vistaDeudas by rememberSaveable { mutableStateOf(VistaDeudas.LISTA) }
    var dialogoDeuda by remember { mutableStateOf<DialogoDeudaAbierto?>(null) }
    var dialogoPersona by remember { mutableStateOf<DialogoPersonaAbierto?>(null) }
    var dialogoPruebas by remember { mutableStateOf<DialogoPruebasAbierto?>(null) }
    var pruebaAVer by remember { mutableStateOf<String?>(null) }
    var dialogoPago by remember { mutableStateOf<DialogoPagoAbierto?>(null) }
    var aBorrarDeuda by remember { mutableStateOf<Deuda?>(null) }
    var aBorrarPersona by remember { mutableStateOf<PersonaDeuda?>(null) }
    var aBorrarPago by remember { mutableStateOf<Pago?>(null) }
    var comprobanteAVer by remember { mutableStateOf<String?>(null) }

    // ------------------------------------------------------------------
    // La pantalla de ajustes (engranaje arriba a la derecha de Balance).
    // ------------------------------------------------------------------
    var mostrarAjustes by rememberSaveable { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val avisoGuardado by vm.errorGuardado.collectAsStateWithLifecycle()
    val estadoSnackbar = remember { SnackbarHostState() }

    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    // La activity, para poder cerrar la app cuando el usuario sale con el
    // botón "atrás".
    val activity = contexto.findActivity()

    // Los ajustes que la app recuerda entre sesiones (el tema y la
    // apariencia: colores, fondo y animaciones).
    val preferencias = remember(contexto) { Preferencias(contexto) }

    /** "1.7 (8)" — la versión de la app, para la pantalla de ajustes. */
    val versionApp = remember {
        try {
            val info = contexto.packageManager.getPackageInfo(contexto.packageName, 0)
            // OJO: `info.longVersionCode` NO existe antes de Android 9 (API 28):
            // en Android 7 lanzaba un NoSuchMethodError y la app moría al abrir.
            // PackageInfoCompat hace la diferencia por versión.
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        } catch (e: Throwable) {
            ""
        }
    }

    // ------------------------------------------------------------------
    // El fondo: la foto del teléfono y el degradado animado
    // ------------------------------------------------------------------
    // El selector de fotos del sistema: no pide ningún permiso (la app sigue
    // teniendo cero permisos) y devuelve una Uri que se puede leer.
    val selectorDeFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { elegida ->
            alcance.launch {
                val guardada = withContext(Dispatchers.IO) {
                    FondosApp.guardarFondo(contexto, elegida)
                }
                if (guardada) {
                    AparienciaActual.fondo = AparienciaActual.fondo.copy(
                        imagen = FondosApp.NOMBRE_ARCHIVO
                    )
                    aviso = "Listo. Tu imagen quedó de fondo."
                } else {
                    aviso = "No se pudo usar esa imagen."
                }
            }
        }
    }

    // Los ajustes de apariencia se guardan solos en cuanto cambian: así el
    // usuario no tiene que pensar en guardar nada.
    LaunchedEffect(
        AparienciaActual.paleta.huella,
        AparienciaActual.animaciones,
        AparienciaActual.fondo
    ) {
        preferencias.guardarApariencia(
            AparienciaActual.paleta.aAjustes().copy(
                animaciones = AparienciaActual.animaciones,
                fondo = AparienciaActual.fondo
            )
        )
    }

    // Las preferencias también se guardan solas: son un interruptor, no hay
    // nada que el usuario pueda olvidar.
    LaunchedEffect(PreferenciasActual.preferencias) {
        preferencias.guardarPreferencias(PreferenciasActual.preferencias)
    }

    // ------------------------------------------------------------------
    // El botón "atrás" del dispositivo
    // ------------------------------------------------------------------
    // En las pantallas de adentro (Ajustes, el detalle de una deuda, agregar
    // personas) vuelve a la pantalla de al lado; en las 4 pantallas
    // principales (Ahorros, Gastos, Deudas, Balance) sale de la app, y hace
    // falta apretarlo dos veces para no cerrarla sin querer.
    var confirmarSalida by remember { mutableStateOf(false) }

    BackHandler {
        when {
            mostrarAjustes -> mostrarAjustes = false
            vistaDeudas == VistaDeudas.ANADIR_PERSONAS -> vistaDeudas = VistaDeudas.DETALLE
            deudaAbierta != null -> {
                vistaDeudas = VistaDeudas.LISTA
                deudaAbierta = null
            }
            confirmarSalida -> activity?.finish()
            else -> {
                confirmarSalida = true
                aviso = "Presioná de nuevo para salir."
                alcance.launch {
                    delay(2000)
                    confirmarSalida = false
                }
            }
        }
    }

    // Para restaurar una copia: el usuario elige el archivo .zip.
    val launcherRestaurar = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { elegido ->
            alcance.launch {
                val ok = withContext(Dispatchers.IO) {
                    val copia = File(contexto.cacheDir, "restauracion.zip")
                    val copiado = try {
                        val entrada = contexto.contentResolver.openInputStream(elegido)
                        if (entrada != null) {
                            entrada.use { flujo ->
                                copia.outputStream().use { salida -> flujo.copyTo(salida) }
                            }
                            true
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                    val restaurado = copiado && CopiaLocal.restaurar(contexto, copia)
                    copia.delete()
                    restaurado
                }
                aviso = if (ok) {
                    "Copia restaurada. Cerrá y abrí la app para ver los datos."
                } else {
                    "No se pudo restaurar esa copia."
                }
            }
        }
    }

    LaunchedEffect(aviso) {
        aviso?.let { mensaje ->
            estadoSnackbar.showSnackbar(mensaje)
            aviso = null
        }
    }

    LaunchedEffect(avisoGuardado) {
        avisoGuardado?.let { mensaje ->
            estadoSnackbar.showSnackbar(mensaje)
            vm.limpiarError()
        }
    }

    val totalAhorros = remember(movimientos) {
        movimientos.filter { it.tipo == Tipo.AHORRO }.sumOf { it.monto }
    }
    val totalGastos = remember(movimientos) {
        movimientos.filter { it.tipo == Tipo.GASTO }.sumOf { it.monto }
    }
    val balance = totalAhorros - totalGastos

    // La deuda que se está viendo en detalle (null si no hay ninguna abierta).
    val deudaActual = remember(deudas, deudaAbierta) {
        deudas.firstOrNull { it.id == deudaAbierta }
    }

    Scaffold(
        containerColor = if (AparienciaActual.hayFondo) Color.Transparent else Fondo,
        snackbarHost = { SnackbarHost(estadoSnackbar) },
        bottomBar = {
            if (!mostrarAjustes) {
                NavigationBar(containerColor = Superficie, tonalElevation = 0.dp) {
                    Pestana.entries.forEach { opcion ->
                        NavigationBarItem(
                            selected = pestana == opcion,
                            onClick = {
                                pestana = opcion
                                // Al cambiar de pestaña se cierra el detalle de la deuda.
                                deudaAbierta = null
                                vistaDeudas = VistaDeudas.LISTA
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(opcion.icono),
                                    contentDescription = opcion.etiqueta,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(opcion.etiqueta, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Fondo,
                                selectedTextColor = AcentoCalido,
                                indicatorColor = AcentoCalido,
                                unselectedIconColor = TextoSecundario,
                                unselectedTextColor = TextoSecundario
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val tipo = pestana.tipo
            val enDetalle = pestana == Pestana.DEUDAS && deudaActual != null
            val addingPeople = vistaDeudas == VistaDeudas.ANADIR_PERSONAS
            if (tipo != null && !mostrarAjustes) {
                FloatingActionButton(
                    onClick = { dialogo = DialogoAbierto(tipo, null) },
                    containerColor = AcentoCalido,
                    contentColor = Fondo,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar movimiento")
                }
            } else if (pestana == Pestana.DEUDAS && !enDetalle && !addingPeople && !mostrarAjustes) {
                FloatingActionButton(
                    onClick = { dialogoDeuda = DialogoDeudaAbierto(null) },
                    containerColor = AcentoCalido,
                    contentColor = Fondo,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar deuda")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // El fondo (la foto del teléfono o el degradado animado) va POR
            // DEBAJO de todo. Cuando no hay nada puesto, no se dibuja nada
            // extra: el Scaffold sigue siendo el que pinta el fondo del tema.
            FondoApp(Modifier.fillMaxSize())

            if (mostrarAjustes) {
                PantallaAjustes(
                    versionApp = versionApp,
                    temas = CatalogoTemas.todos,
                    temaActualId = TemaActual.tema.id,
                    onCambiarTema = { id ->
                        // Se aplica al instante y se guarda para la próxima vez
                        // que se abra la app.
                        if (TemaActual.aplicar(id)) {
                            preferencias.guardarTema(id)
                        }
                    },
                    onElegirFondo = {
                        selectorDeFotos.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onQuitarFondo = {
                        FondosApp.borrarFondo(contexto)
                        AparienciaActual.fondo = AparienciaActual.fondo.copy(imagen = null)
                    },
                    onVolver = { mostrarAjustes = false },
                    onExportar = {
                        // Se arma el .zip y se abre el menú de compartir: el
                        // usuario lo guarda en Drive, WhatsApp, correo...
                        val zip = CopiaLocal.crearZip(contexto)
                        if (zip == null) {
                            aviso = "No se pudo preparar la copia."
                        } else {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    contexto,
                                    "${contexto.packageName}.fileprovider",
                                    zip
                                )
                                val enviar = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                contexto.startActivity(
                                    Intent.createChooser(enviar, "Guardar o compartir la copia")
                                )
                            } catch (e: Exception) {
                                aviso = "No se pudo compartir la copia."
                            }
                        }
                    },
                    onRestaurar = { uri -> launcherRestaurar.launch(arrayOf("application/zip", "*/*")) },
                    onCerrar = { mostrarAjustes = false }
                )
                return@Box
            }

            when (pestana) {
                Pestana.AHORROS, Pestana.GASTOS -> PantallaMovimiento(
                    tipo = pestana.tipo!!,
                    movimientos = movimientos,
                    balance = balance,
                    colapsados = diasColapsados,
                    onAlternarDia = { clave ->
                        diasColapsados = if (clave in diasColapsados) {
                            diasColapsados - clave
                        } else {
                            diasColapsados + clave
                        }
                    },
                    onEditar = { movimiento -> dialogo = DialogoAbierto(movimiento.tipo, movimiento) },
                    onEliminar = { movimiento -> aBorrar = movimiento }
                )

                Pestana.DEUDAS -> {
                    val deuda = deudaActual
                    when {
                        // Pantalla para agregar personas (con la lista del salón).
                        vistaDeudas == VistaDeudas.ANADIR_PERSONAS && deuda != null -> {
                            PantallaAnadirPersonas(
                                cuotaBase = CalculosDeudas.montoBaseDe(deuda),
                                yaAgregadas = deuda.personas.map { it.nombre },
                                listas = listas,
                                listaDelColegioBorrada = listaDelColegioBorrada,
                                nombreListaDelColegio = vm.nombreListaDelColegio,
                                onCrearLista = { nombre, personas ->
                                    vm.crearLista(nombre, personas)
                                },
                                onEliminarLista = { id -> vm.eliminarLista(id) },
                                onRestaurarListaDelColegio = {
                                    vm.restaurarListaDelColegio()
                                },
                                onConfirmar = { nuevas ->
                                    nuevas.forEach { nueva ->
                                        vm.agregarPersona(
                                            deuda.id,
                                            PersonaDeuda(vm.nuevoId(), nueva.nombre, nueva.cuotaPropia)
                                        )
                                    }
                                    vistaDeudas = VistaDeudas.DETALLE
                                },
                                onCancelar = { vistaDeudas = VistaDeudas.DETALLE }
                            )
                        }

                        deuda == null -> PantallaDeudas(
                            deudas = deudas,
                            onAbrir = { id ->
                                deudaAbierta = id
                                vistaDeudas = VistaDeudas.DETALLE
                            },
                            onEditar = { objetivo -> dialogoDeuda = DialogoDeudaAbierto(objetivo) },
                            onEliminar = { objetivo -> aBorrarDeuda = objetivo },
                            onNuevaIndividual = {
                                dialogoDeuda = DialogoDeudaAbierto(null, individual = true)
                            }
                        )

                        else -> PantallaDeudaDetalle(
                            deuda = deuda,
                            onVolver = {
                                vistaDeudas = VistaDeudas.LISTA
                                deudaAbierta = null
                            },
                            onAgregarPersona = { vistaDeudas = VistaDeudas.ANADIR_PERSONAS },
                            onEditarPersona = { persona -> dialogoPersona = DialogoPersonaAbierto(persona) },
                            onEliminarPersona = { persona -> aBorrarPersona = persona },
                            onRegistrarPago = { persona -> dialogoPago = DialogoPagoAbierto(persona) },
                            onEliminarPago = { pago -> aBorrarPago = pago },
                            onAlternarSaldada = { vm.alternarSaldada(deuda.id) },
                            onEditar = { dialogoDeuda = DialogoDeudaAbierto(deuda) },
                            onEliminar = { aBorrarDeuda = deuda },
                            onVerComprobante = { nombre -> comprobanteAVer = nombre },
                            onAdjuntarPrueba = { persona ->
                                dialogoPruebas = DialogoPruebasAbierto(persona.id, persona.nombre)
                            }
                        )
                    }
                }

                Pestana.BALANCE -> PantallaBalance(
                    movimientos = movimientos,
                    onAjustes = { mostrarAjustes = true }
                )
            }
        }
    }

    dialogo?.let { abierto ->
        DialogoMovimiento(
            tipo = abierto.tipo,
            inicial = abierto.movimiento,
            onGuardar = { movimiento ->
                if (abierto.movimiento == null) {
                    vm.agregar(movimiento.copy(id = vm.nuevoId()))
                } else {
                    vm.actualizar(movimiento)
                }
                dialogo = null
            },
            onCerrar = { dialogo = null }
        )
    }

    aBorrar?.let { movimiento ->
        DialogoConfirmarBorrar(
            texto = if (movimiento.tipo == Tipo.GASTO) {
                "Se eliminará el gasto de ${movimiento.monto.dinero()}. Tu balance volverá a subir."
            } else {
                "Se eliminará el ahorro de ${movimiento.monto.dinero()}. Tu balance se recalculará."
            },
            onConfirmar = {
                vm.eliminar(movimiento.id)
                aBorrar = null
            },
            onCerrar = { aBorrar = null }
        )
    }

    // ------------------------------------------------------------------
    // Deudas
    // ------------------------------------------------------------------

    dialogoDeuda?.let { abierto ->
        // Una deuda individual le debe a UNA persona: el nombre que se
        // escribió es el de la persona (y también el de la deuda).
        val esIndividual = abierto.individual || (abierto.deuda?.individual == true)
        DialogoDeuda(
            inicial = abierto.deuda,
            individual = esIndividual,
            onGuardar = { nombre, modo, total, montoPorPersona, personasQuePagan, cuotaPersonalizada ->
                val ahora = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val existente = abierto.deuda
                // La cuota a mano solo tiene sentido en modo META: en modo
                // libre el monto por persona ya es la cuota fija.
                val cuota = if (modo == ModoDeuda.META) cuotaPersonalizada else null
                if (existente == null) {
                    vm.agregarDeuda(
                        Deuda(
                            id = vm.nuevoId(),
                            nombre = nombre,
                            modo = modo,
                            // En modo libre el total se calcula: no se guarda.
                            total = if (modo == ModoDeuda.META) total else 0.0,
                            montoPorPersona = if (modo == ModoDeuda.LIBRE) montoPorPersona else 0.0,
                            // En una deuda individual solo paga uno: no hay
                            // lista que repartir.
                            personasQuePagan = if (esIndividual) 1 else personasQuePagan,
                            // La persona queda en la lista sola: es a quien se
                            // le cobra (y a quien se le marca lo que pagó).
                            personas = if (esIndividual) {
                                listOf(PersonaDeuda(vm.nuevoId(), nombre))
                            } else {
                                emptyList()
                            },
                            pagos = emptyList(),
                            saldada = false,
                            cuotaPersonalizada = cuota,
                            individual = esIndividual,
                            ts = ahora
                        )
                    )
                } else {
                    // Editar la meta o la cantidad de pagadores cambia la cuota
                    // de todos: eso es lo pedido.
                    vm.actualizarDeuda(
                        existente.copy(
                            nombre = nombre,
                            modo = modo,
                            total = if (modo == ModoDeuda.META) total else 0.0,
                            montoPorPersona = if (modo == ModoDeuda.LIBRE) montoPorPersona else 0.0,
                            personasQuePagan = if (esIndividual) 1 else personasQuePagan,
                            cuotaPersonalizada = cuota,
                            individual = esIndividual,
                            // En una deuda individual la persona de la lista
                            // se llama como la deuda: si se cambia el nombre,
                            // cambia con ella.
                            personas = if (esIndividual) {
                                existente.personas.map { persona -> persona.copy(nombre = nombre) }
                            } else {
                                existente.personas
                            }
                        )
                    )
                }
                dialogoDeuda = null
            },
            onCerrar = { dialogoDeuda = null }
        )
    }

    dialogoPersona?.let { abierto ->
        DialogoPersona(
            persona = abierto.persona,
            cuotaBase = deudaActual?.let { CalculosDeudas.montoBaseDe(it) } ?: 0.0,
            onGuardar = { nombre, cuotaPropia ->
                val deuda = deudaActual
                if (deuda != null) {
                    val persona = abierto.persona
                    if (persona == null) {
                        vm.agregarPersona(deuda.id, PersonaDeuda(vm.nuevoId(), nombre, cuotaPropia))
                    } else {
                        // Editar el nombre o el monto propio no toca lo pagado.
                        vm.actualizarPersona(
                            deuda.id,
                            persona.copy(nombre = nombre, cuotaPropia = cuotaPropia)
                        )
                    }
                }
                dialogoPersona = null
            },
            onCerrar = { dialogoPersona = null }
        )
    }

    dialogoPruebas?.let { abierto ->
        val deuda = deudaActual
        val persona = deuda?.personas?.firstOrNull { it.id == abierto.personaId }
        DialogoPruebas(
            persona = persona,
            onAdjuntar = { uri ->
                if (deuda != null) {
                    vm.agregarPrueba(deuda.id, abierto.personaId, uri)
                }
            },
            onQuitar = { nombre ->
                if (deuda != null) {
                    vm.quitarPrueba(deuda.id, abierto.personaId, nombre)
                }
            },
            onMetodo = { metodo ->
                if (deuda != null) {
                    vm.actualizarMetodoPersona(deuda.id, abierto.personaId, metodo)
                }
            },
            onVer = { nombre ->
                // Se cierra el diálogo de pruebas y se abre la imagen en grande.
                pruebaAVer = nombre
                dialogoPruebas = null
            },
            onCerrar = { dialogoPruebas = null }
        )
    }

    dialogoPago?.let { abierto ->
        val deuda = deudaActual
        val cuota = deuda?.let { CalculosDeudas.cuotaDe(it, abierto.persona) } ?: 0.0
        val pendiente = deuda?.let { CalculosDeudas.pendienteDe(it, abierto.persona.id) } ?: 0.0
        DialogoPago(
            persona = abierto.persona,
            cuota = cuota,
            pendiente = pendiente,
            onGuardar = { monto, metodo, nota, imagen ->
                if (deuda != null) {
                    // El comprobante solo se guarda con YAPI: es la única forma de mandarlo.
                    val comprobante = if (metodo == MetodoPago.YAPI && imagen != null) {
                        vm.guardarComprobante(imagen, "comprobante_${vm.nuevoId()}")
                    } else {
                        null
                    }
                    val ahora = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // Si la preferencia "los pagos de las deudas van a los
                    // ahorros" está prendida, lo cobrado entra a los ahorros
                    // como un depósito más. El id del movimiento se anota en
                    // el pago: si el pago se borra, el depósito se borra con
                    // él (si no, el dinero se quedaría en los ahorros para
                    // siempre).
                    val movimientoId = if (PreferenciasActual.pagosDeDeudasEnAhorros && monto > 0.0) {
                        val id = vm.nuevoId()
                        vm.agregar(
                            Movimiento(
                                id = id,
                                tipo = Tipo.AHORRO,
                                monto = monto,
                                // YAPI es digital; el efectivo, físico.
                                categoria = if (metodo == MetodoPago.YAPI) {
                                    FuenteAhorro.DIGITAL.etiqueta
                                } else {
                                    FuenteAhorro.FISICO.etiqueta
                                },
                                nota = "Cobro de la deuda \"${deuda.nombre}\"",
                                ts = ahora
                            )
                        )
                        id
                    } else {
                        null
                    }

                    vm.agregarPago(
                        deuda.id,
                        Pago(
                            id = vm.nuevoId(),
                            personaId = abierto.persona.id,
                            monto = monto,
                            metodo = metodo,
                            nota = nota,
                            comprobante = comprobante,
                            movimientoId = movimientoId,
                            ts = ahora
                        )
                    )
                }
                dialogoPago = null
            },
            onCerrar = { dialogoPago = null }
        )
    }

    aBorrarDeuda?.let { deuda ->
        DialogoConfirmarBorrar(
            texto = "Se eliminará la deuda \"${deuda.nombre}\" y todos sus pagos. Esto no se puede deshacer.",
            onConfirmar = {
                // Los depósitos que crearon los pagos de esta deuda se van
                // con ella: si no, el dinero se quedaría en los ahorros de
                // una deuda que ya no existe.
                deuda.pagos.forEach { pago -> pago.movimientoId?.let { vm.eliminar(it) } }
                vm.eliminarDeuda(deuda.id)
                if (deudaAbierta == deuda.id) deudaAbierta = null
                aBorrarDeuda = null
            },
            onCerrar = { aBorrarDeuda = null }
        )
    }

    aBorrarPersona?.let { persona ->
        DialogoConfirmarBorrar(
            texto = "Se quitará a ${persona.nombre} del reparto. La cuota de los demás se recalcula, pero lo que ya pagaron no se toca.",
            onConfirmar = {
                deudaActual?.let { vm.eliminarPersona(it.id, persona.id) }
                aBorrarPersona = null
            },
            onCerrar = { aBorrarPersona = null }
        )
    }

    aBorrarPago?.let { pago ->
        DialogoConfirmarBorrar(
            texto = if (pago.movimientoId != null) {
                "Se eliminará el pago de ${pago.monto.dinero()} y el depósito que entró a " +
                    "los ahorros. La deuda se recalcula."
            } else {
                "Se eliminará el pago de ${pago.monto.dinero()}. La deuda se recalcula."
            },
            onConfirmar = {
                deudaActual?.let { vm.eliminarPago(it.id, pago.id) }
                // El depósito que creó este pago se va con él: el dinero
                // vuelve a estar como estaba antes de cobrar.
                pago.movimientoId?.let { vm.eliminar(it) }
                aBorrarPago = null
            },
            onCerrar = { aBorrarPago = null }
        )
    }

    comprobanteAVer?.let { nombre ->
        DialogoComprobante(
            archivo = vm.rutaComprobante(nombre),
            onCerrar = { comprobanteAVer = null }
        )
    }

    pruebaAVer?.let { nombre ->
        DialogoComprobante(
            archivo = vm.rutaPrueba(nombre),
            onCerrar = { pruebaAVer = null }
        )
    }
}
