@file:OptIn(ExperimentalMaterial3Api::class)

package com.cesar.ahorros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.ListaDeGente

/**
 * Una persona que se va a agregar al reparto de la deuda.
 *
 * [cuotaPropia] es el "caso especial": el monto que debe pagar esta persona.
 * Si es `null`, paga la cuota normal (total ÷ personas que lo pagan).
 */
data class PersonaPorAgregar(
    val nombre: String,
    val cuotaPropia: Double? = null
)

/**
 * Pantalla para agregar personas al reparto de una deuda.
 *
 * Es una pantalla completa (no un diálogo) porque hay varias listas que
 * revisar: las que guardó el usuario (la del 11G viene con la app) y la de
 * "otra persona" para quien no está en ninguna de esas listas.
 *
 * La lista de personas **no divide nada**: la cuota la define la deuda (el
 * número de personas que la pagan). La excepción es la cuota automática: si
 * no se dijo cuántos van a pagar, los de la lista son los que dividen. Por
 * eso aquí solo se marca a quién se le cobra, y opcionalmente un monto
 * propio para el caso especial.
 *
 * Las casillas se guardan con la clave "idLista|nombre": el mismo nombre puede
 * estar en dos listas y son dos casillas distintas.
 */
@Composable
fun PantallaAnadirPersonas(
    cuotaBase: Double,
    yaAgregadas: List<String>,
    listas: List<ListaDeGente>,
    /** True si la lista que viene con la app fue borrada (se puede devolver). */
    listaDelColegioBorrada: Boolean,
    /** El nombre de la lista que viene con la app, si esta versión la tiene. */
    nombreListaDelColegio: String?,
    onCrearLista: (String, List<String>) -> Unit,
    onEliminarLista: (String) -> Unit,
    onRestaurarListaDelColegio: () -> Unit,
    onConfirmar: (List<PersonaPorAgregar>) -> Unit,
    onCancelar: () -> Unit
) {
    // Los que ya están en la deuda no se pueden volver a marcar.
    val yaEnDeuda = remember(yaAgregadas) { yaAgregadas.map { it.trim() }.toSet() }

    // Las casillas marcadas: clave "idLista|nombre" (el mismo nombre en dos
    // listas son dos casillas distintas).
    var marcados by remember { mutableStateOf(emptySet<String>()) }
    // Las listas que están plegadas: por defecto todas vienen abiertas.
    var plegadas by remember { mutableStateOf(emptySet<String>()) }

    // Diálogos.
    var dialogoNuevaLista by remember { mutableStateOf(false) }
    var listaABorrar by remember { mutableStateOf<String?>(null) }

    // Personas que no están en ninguna lista.
    var manuales by remember { mutableStateOf(emptyList<PersonaPorAgregar>()) }
    var nombreManual by remember { mutableStateOf("") }
    var montoEspecial by remember { mutableStateOf(false) }
    var montoManualTexto by remember { mutableStateOf(textoMonto(cuotaBase)) }

    val montoManual = montoManualTexto.replace(",", ".").toDoubleOrNull()
    val montoManualValido = montoManual != null && montoManual > 0.0

    // Las que se van a agregar desde las listas, en el orden en que aparecen.
    // Si el mismo nombre está en dos listas, se agrega una sola vez.
    val deListas: List<PersonaPorAgregar> = buildList {
        val yaPuestos = mutableSetOf<String>()
        listas.forEach { lista ->
            lista.personas.forEach { nombre ->
                val limpio = nombre.trim()
                if (limpio.isNotEmpty() &&
                    clave(lista.id, limpio) in marcados &&
                    limpio !in yaPuestos
                ) {
                    yaPuestos += limpio
                    add(PersonaPorAgregar(limpio, null))
                }
            }
        }
    }

    val totalElegidas = deListas.size + manuales.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ------------------------------------------------------------------
        // Barra superior
        // ------------------------------------------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancelar, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextoPrimario,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Añadir personas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // ------------------------------------------------------------------
        // Referencia de la cuota
        // ------------------------------------------------------------------
        Surface(
            color = AcentoCalido.copy(alpha = 0.14f),
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "La cuota normal es ${cuotaBase.dineroSimbolo()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AcentoCalido
                )
                Text(
                    text = "Es lo que paga cada uno por defecto. Si a alguien le pones un " +
                        "monto especial, ese monto se aparta de la cuota normal: según cómo " +
                        "sea la deuda, los demás se reparten lo que sobra o el total se ajusta.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ------------------------------------------------------------------
        // Listas de gente
        // ------------------------------------------------------------------
        Text(
            text = "Listas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Las listas que guardaste: marcá a quien va en esta deuda.",
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario
        )

        Spacer(Modifier.height(10.dp))

        // Botón para crear una lista nueva.
        OutlinedButton(
            onClick = { dialogoNuevaLista = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadioBoton),
            border = BorderStroke(1.dp, Borde)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = AcentoCalido,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Nueva lista", color = AcentoCalido)
        }

        Spacer(Modifier.height(12.dp))

        // Si el usuario borró la lista que viene con la app (la del salón del
        // 11G), sale el botón para devolverla.
        if (listaDelColegioBorrada && nombreListaDelColegio != null) {
            OutlinedButton(
                onClick = onRestaurarListaDelColegio,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadioBoton),
                border = BorderStroke(1.dp, Borde)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = AcentoMenta,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Devolver la lista \"$nombreListaDelColegio\"", color = AcentoMenta)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (listas.isEmpty()) {
            Surface(
                color = Superficie,
                shape = RoundedCornerShape(RadioTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Todavía no hay listas. Creá una y te va a servir para las " +
                        "próximas deudas: no tenés que escribir los nombres a mano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            listas.forEach { lista ->
                TarjetaLista(
                    lista = lista,
                    expandida = lista.id !in plegadas,
                    yaEnDeuda = yaEnDeuda,
                    marcados = marcados,
                    onAlternarExpandida = {
                        plegadas = if (lista.id in plegadas) plegadas - lista.id else plegadas + lista.id
                    },
                    onAlternarPersona = { nombre ->
                        val llave = clave(lista.id, nombre)
                        marcados = if (llave in marcados) marcados - llave else marcados + llave
                    },
                    onMarcarTodos = {
                        val aMarcar = lista.personas
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && it !in yaEnDeuda }
                            .map { clave(lista.id, it) }
                        marcados = marcados + aMarcar
                    },
                    onDesmarcarTodos = {
                        val deEsta = lista.personas
                            .map { clave(lista.id, it.trim()) }
                            .toSet()
                        marcados = marcados - deEsta
                    },
                    onPedirBorrar = { listaABorrar = lista.id }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        // Contador de las marcadas en TODAS las listas.
        Surface(
            color = Superficie,
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$totalElegidas " +
                    (if (totalElegidas == 1) "persona elegida" else "personas elegidas"),
                style = MaterialTheme.typography.labelMedium,
                color = if (totalElegidas == 0) TextoSecundario else AcentoCalido,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ------------------------------------------------------------------
        // Otra persona
        // ------------------------------------------------------------------
        TituloSeccion("Otra persona")

        Surface(
            color = Superficie,
            shape = RoundedCornerShape(RadioTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Para quien no está en ninguna de las listas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nombreManual,
                        onValueChange = { if (it.length <= 40) nombreManual = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ejemplo: María") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val nombre = nombreManual.trim()
                            if (nombre.isNotEmpty() && (!montoEspecial || montoManualValido)) {
                                manuales = manuales + PersonaPorAgregar(
                                    nombre = nombre,
                                    cuotaPropia = if (montoEspecial) montoManual else null
                                )
                                nombreManual = ""
                                montoEspecial = false
                                montoManualTexto = textoMonto(cuotaBase)
                            }
                        },
                        enabled = nombreManual.isNotBlank() && (!montoEspecial || montoManualValido),
                        shape = RoundedCornerShape(RadioBoton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcentoCalido,
                            contentColor = Fondo
                        )
                    ) {
                        Text("Añadir")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monto especial",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Paga otra cosa en vez de los ${cuotaBase.dineroSimbolo()} " +
                                "de la cuota normal.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                    }
                    Switch(checked = montoEspecial, onCheckedChange = { montoEspecial = it })
                }

                if (montoEspecial) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = montoManualTexto,
                        onValueChange = { entrada ->
                            if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                montoManualTexto = entrada
                            }
                        },
                        label = { Text("Monto que debe pagar") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = montoManualTexto.isNotEmpty() && !montoManualValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Los que se agregaron a mano.
        if (manuales.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            manuales.forEach { persona ->
                FilaPersonaManual(
                    persona = persona,
                    onQuitar = {
                        manuales = manuales.filter { it.nombre != persona.nombre }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ------------------------------------------------------------------
        // Resumen y botón de confirmar
        // ------------------------------------------------------------------
        Text(
            text = if (totalElegidas == 0) {
                "No has elegido a nadie todavía."
            } else {
                "Se van a agregar $totalElegidas " +
                    (if (totalElegidas == 1) "persona" else "personas") +
                    " a la deuda. La cuota normal la recalcula la deuda: si definiste " +
                    "cuántos pagan, manda ese número; si no, se calcula con los que agregues."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Button(
            onClick = { onConfirmar(deListas + manuales) },
            enabled = deListas.isNotEmpty() || manuales.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(RadioBoton),
            colors = ButtonDefaults.buttonColors(
                containerColor = AcentoCalido,
                contentColor = Fondo
            )
        ) {
            Text(
                text = if (deListas.isEmpty() && manuales.isEmpty()) {
                    "Añadir personas"
                } else {
                    "Añadir ${deListas.size + manuales.size} " +
                        (if (deListas.size + manuales.size == 1) "persona" else "personas")
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(96.dp))
    }

    // ------------------------------------------------------------------
    // Diálogo: lista nueva
    // ------------------------------------------------------------------
    if (dialogoNuevaLista) {
        DialogoNuevaLista(
            onGuardar = { nombre, personas ->
                onCrearLista(nombre, personas)
                dialogoNuevaLista = false
            },
            onCerrar = { dialogoNuevaLista = false }
        )
    }

    // ------------------------------------------------------------------
    // Diálogo: borrar una lista
    // ------------------------------------------------------------------
    if (listaABorrar != null) {
        val lista = listas.firstOrNull { it.id == listaABorrar }
        DialogoConfirmarBorrar(
            texto = lista?.let {
                val personas = it.personas.size
                val gente = if (personas == 1) "1 persona" else "$personas personas"
                if (it.bloqueada) {
                    "Se borra la lista \"${it.nombre}\" con sus $gente. " +
                        "Podés volver a traerla después con el botón de abajo."
                } else {
                    "Se borra la lista \"${it.nombre}\" con sus $gente."
                }
            } ?: "Se borra la lista.",
            onConfirmar = {
                val id = listaABorrar
                listaABorrar = null
                if (id != null) {
                    onEliminarLista(id)
                    marcados = marcados.filterNot { it.startsWith("$id|") }.toSet()
                }
            },
            onCerrar = { listaABorrar = null },
            titulo = "¿Borrar la lista?"
        )
    }
}

/**
 * Tarjeta de una lista de gente: se pliega y se despliega, y se puede borrar.
 *
 * Se pueden borrar todas, incluso la que viene con la app (la del salón del
 * 11G): esa se anota en los ajustes para que no vuelva a salir sola, y se
 * puede devolver desde la misma pantalla.
 */
@Composable
private fun TarjetaLista(
    lista: ListaDeGente,
    expandida: Boolean,
    yaEnDeuda: Set<String>,
    marcados: Set<String>,
    onAlternarExpandida: () -> Unit,
    onAlternarPersona: (String) -> Unit,
    onMarcarTodos: () -> Unit,
    onDesmarcarTodos: () -> Unit,
    onPedirBorrar: () -> Unit
) {
    // Solo se pueden marcar los que no están ya en la deuda.
    val seleccionables = lista.personas
        .map { it.trim() }
        .filter { it.isNotEmpty() && it !in yaEnDeuda }
    val hayParaMarcar = seleccionables.any { clave(lista.id, it) !in marcados }
    val hayParaDesmarcar = seleccionables.any { clave(lista.id, it) in marcados }

    Surface(
        color = Superficie,
        shape = RoundedCornerShape(RadioTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(RadioTarjeta))
                        .clickable { onAlternarExpandida() }
                ) {
                    Text(
                        text = "${lista.nombre} (${lista.personas.size} " +
                            (if (lista.personas.size == 1) "persona" else "personas") + ")",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (expandida) "Toca la flecha para plegarla" else "Toca para desplegarla",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }

                // La papelera sale en TODAS las listas, incluso en la que viene
                // con la app (la del salón): esa se puede devolver después.
                IconButton(onClick = onPedirBorrar, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar lista",
                        tint = ColorGasto,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (expandida) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expandida) "Plegar" else "Desplegar",
                    tint = TextoSecundario,
                    modifier = Modifier.clickable { onAlternarExpandida() }
                )
            }

            if (expandida) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )

                lista.personas.forEach { persona ->
                    val limpio = persona.trim()
                    FilaPersonaLista(
                        nombre = limpio,
                        yaEsta = limpio in yaEnDeuda,
                        marcado = clave(lista.id, limpio) in marcados,
                        onAlternar = { onAlternarPersona(limpio) }
                    )
                }

                // Marcar / desmarcar todos (solo los que se pueden).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarcarTodos,
                        enabled = hayParaMarcar,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Borde)
                    ) {
                        Text("Marcar todos")
                    }
                    OutlinedButton(
                        onClick = onDesmarcarTodos,
                        enabled = hayParaDesmarcar,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Borde)
                    ) {
                        Text("Desmarcar todos")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )
            }
        }
    }
}

/** Fila de una persona de una lista, con su casilla. */
@Composable
private fun FilaPersonaLista(
    nombre: String,
    yaEsta: Boolean,
    marcado: Boolean,
    onAlternar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !yaEsta) { onAlternar() }
            .padding(start = 8.dp, end = 14.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = marcado,
            onCheckedChange = { onAlternar() },
            enabled = !yaEsta
        )
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyLarge,
            color = if (yaEsta) TextoSecundario else TextoPrimario,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (yaEsta) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ya está",
                style = MaterialTheme.typography.labelSmall,
                color = TextoTercero
            )
        }
    }
}

/** Fila de una persona agregada a mano: su nombre y su monto. */
@Composable
private fun FilaPersonaManual(
    persona: PersonaPorAgregar,
    onQuitar: () -> Unit
) {
    Surface(
        color = Superficie,
        shape = RoundedCornerShape(RadioTarjeta),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (persona.cuotaPropia != null) {
                        "Monto especial: ${persona.cuotaPropia.dineroSimbolo()}"
                    } else {
                        "Paga la cuota normal"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (persona.cuotaPropia != null) AcentoHielo else TextoSecundario
                )
            }
            IconButton(onClick = onQuitar, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Quitar",
                    tint = ColorGasto,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Diálogo para crear una lista nueva de gente. */
@Composable
private fun DialogoNuevaLista(
    onGuardar: (String, List<String>) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }

    val personas = remember(texto) { parsearPersonas(texto) }
    val puedeGuardar = nombre.isNotBlank() && personas.isNotEmpty()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nueva lista") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (it.length <= 40) nombre = it },
                    label = { Text("Nombre de la lista") },
                    placeholder = { Text("Ej: Curso de Historia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Personas (una por línea)") },
                    placeholder = { Text("Ana\nLuis\nMaría") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Un nombre por línea. Los vacíos y los repetidos se ignoran.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = puedeGuardar,
                onClick = { onGuardar(nombre.trim(), personas) }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

/** La clave de una casilla: lista + nombre (el mismo nombre puede estar en dos). */
private fun clave(idLista: String, nombre: String): String = "$idLista|$nombre"

/** El texto de las personas: una por línea, sin vacíos y sin repetidos. */
private fun parsearPersonas(texto: String): List<String> =
    texto.split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

/** "50" o "12.5" (sin ceros de más) para precargar los campos de monto. */
private fun textoMonto(monto: Double): String =
    if (monto % 1.0 == 0.0) monto.toLong().toString() else monto.toString()
