@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.cesar.ahorros.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cesar.ahorros.data.Deuda
import com.cesar.ahorros.data.MetodoPago
import com.cesar.ahorros.data.ModoDeuda
import com.cesar.ahorros.data.PersonaDeuda
import com.cesar.ahorros.data.RepositorioDeudas
import com.cesar.ahorros.dominio.CalculosDeudas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Alta o edición de una deuda.
 *
 * Hay dos formas de armarla, y el diálogo deja elegir una:
 * - [ModoDeuda.META] ("Sé el total"): César sabe cuánto cuesta la cosa. Se
 *   pone el costo total y, si se sabe, cuántas personas lo van a pagar, y la
 *   app muestra en vivo cuánto paga cada uno.
 * - [ModoDeuda.LIBRE] ("Cuánto paga cada uno"): César no sabe el total, pero
 *   sí cuánto cobra a cada quien. Se pone el monto y el total sale de
 *   multiplicar.
 *
 * El campo de cuántas personas pagan es **opcional**: si queda vacío, la
 * deuda queda en **cuota automática** y la cuota se calcula con la gente que
 * se vaya añadiendo a la lista. Sirve para cuando no se sabe (o no se ha
 * contado) cuántos van a pagar.
 *
 * En modo META hay un campo extra opcional: la **cuota que vas a cobrar**.
 * Es para cuando César no quiere que el costo se divida (si divide $42,80
 * entre 23 y sale $1,86, pero él cobra $1,90). Si el campo queda vacío, la
 * cuota sale de dividir como siempre: no cambia nada.
 *
 * La lista de personas NO divide nada: es a quién se le cobra, no
 * quién divide. Salvo en la cuota automática, donde la lista es justamente
 * la que dice cuántos pagan. Si alguien tiene un monto propio ("caso
 * especial"), eso se ajusta persona por persona desde el detalle de la deuda.
 */
@Composable
fun DialogoDeuda(
    inicial: Deuda?,
    individual: Boolean = false,
    onGuardar: (
        nombre: String,
        modo: ModoDeuda,
        total: Double,
        montoPorPersona: Double,
        personasQuePagan: Int?,
        cuotaPersonalizada: Double?
    ) -> Unit,
    onCerrar: () -> Unit
) {
    var modo by remember { mutableStateOf(inicial?.modo ?: ModoDeuda.META) }
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var totalTexto by remember {
        mutableStateOf(
            inicial?.let {
                if (it.modo == ModoDeuda.META) {
                    montoEnTexto(it.total)
                } else {
                    // Si después se cambia a META, el total ya calculado sirve.
                    montoEnTexto(CalculosDeudas.totalDe(it))
                }
            } ?: ""
        )
    }
    var montoTexto by remember {
        mutableStateOf(
            inicial?.let {
                if (it.modo == ModoDeuda.LIBRE) {
                    montoEnTexto(it.montoPorPersona)
                } else {
                    // Si después se cambia a LIBRE, la cuota actual sirve.
                    montoEnTexto(CalculosDeudas.montoBaseDe(it))
                }
            } ?: ""
        )
    }
    var personasTexto by remember {
        // Si la deuda está en cuota automática, el campo arranca vacío: así se
        // ve que el número es opcional. En una deuda individual no hay campo:
        // le debe a una sola persona.
        mutableStateOf(
            if (individual) {
                "1"
            } else {
                inicial?.personasQuePagan?.coerceAtLeast(1)?.toString() ?: ""
            }
        )
    }
    // La cuota a mano (solo en modo META): vacía = se divide el costo como
    // siempre. Si la deuda ya la tenía, el campo arranca con ese valor.
    var cuotaTexto by remember {
        mutableStateOf(inicial?.cuotaPersonalizada?.let { montoEnTexto(it) } ?: "")
    }

    val total = totalTexto.replace(",", ".").toDoubleOrNull()
    val totalValido = total != null && total > 0.0
    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val montoValido = monto != null && monto > 0.0
    val personas = if (individual) 1 else personasTexto.trim().toIntOrNull()
    // Vacío es VÁLIDO: la cuota se calcula con la gente de la lista (cuota
    // automática). Si tiene algo, tiene que ser un número mayor que cero.
    val personasValido = individual || personas == null || personas >= 1
    // Sin número = cuota automática: manda la lista de personas. En una
    // deuda individual nunca hay cuota automática: es una sola persona.
    val automatico = !individual && personas == null
    // Vacía es válido (se divide el costo); si tiene algo, tiene que ser un
    // número mayor que cero.
    val cuota = cuotaTexto.replace(",", ".").toDoubleOrNull()
    val cuotaValido = cuotaTexto.isEmpty() || (cuota != null && cuota > 0.0)

    val valido = nombre.isNotBlank() && personasValido && when (modo) {
        ModoDeuda.META -> totalValido && cuotaValido
        ModoDeuda.LIBRE -> montoValido
    }

    // Una "deuda de prueba" con lo que se está escribiendo. Sirve para ver la
    // cuota REAL: los montos especiales que ya tenga redistribuyen, así que
    // el total puede no salir con una división simple.
    val simulada = (inicial ?: Deuda(id = "vista", nombre = "", ts = 0L)).copy(
        modo = modo,
        total = if (modo == ModoDeuda.META) (total ?: 0.0) else 0.0,
        montoPorPersona = if (modo == ModoDeuda.LIBRE) (monto ?: 0.0) else 0.0,
        personasQuePagan = personas,
        // Con el campo vacío, la cuota sale de la lista.
        cuotaAutomatica = automatico,
        // Con cuota a mano, esa manda: los montos especiales no reparten.
        cuotaPersonalizada = if (modo == ModoDeuda.META) cuota else null
    )

    // Cuánta gente hay en la lista: en cuota automática, esa es la que
    // divide el total.
    val genteEnLista = simulada.personas.size
    val hayGente = genteEnLista > 0

    // La vista en vivo: lo que se cobra o lo que se paga, según el modo.
    // En cuota automática sin nadie en la lista todavía no se puede decir un
    // número: se dice que se calcula al añadir gente.
    val cuotaPrevia: Double? = when {
        modo == ModoDeuda.META && cuota != null -> CalculosDeudas.redondear(cuota)
        modo == ModoDeuda.META && totalValido ->
            if (automatico && !hayGente) null else CalculosDeudas.montoBaseDe(simulada)
        else -> null
    }
    val totalPrevio: Double? = when {
        modo == ModoDeuda.LIBRE && montoValido ->
            if (automatico && !hayGente) null else CalculosDeudas.totalDe(simulada)
        // Con cuota a mano, el total a cobrar sale de multiplicar.
        modo == ModoDeuda.META && cuota != null ->
            if (automatico && !hayGente) null else CalculosDeudas.totalDe(simulada)
        else -> null
    }
    // Cuando no hay número que mostrar, se dice por qué: en cuota automática
    // sin gente todavía, la cuota se calcula al añadirla; si falta el monto,
    // un simple guion.
    val sinNumero = if (automatico && !hayGente) "se calcula al añadir gente" else "—"

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                text = when {
                    individual && inicial == null -> "Nueva deuda individual"
                    individual -> "Editar deuda individual"
                    inicial == null -> "Nueva deuda"
                    else -> "Editar deuda"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ------------------------------------------------------------------
                // Modo de la deuda
                // ------------------------------------------------------------------
                Text(
                    text = "Cómo se armaron los números",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModoDeuda.entries.forEach { opcion ->
                        FilterChip(
                            selected = modo == opcion,
                            onClick = { modo = opcion },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (modo == ModoDeuda.META) {
                        "Sabes cuánto cuesta la cosa y se divide entre los que la pagan."
                    } else {
                        "Sabes cuánto cobra a cada uno y el total sale de sumar."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (it.length <= 40) nombre = it },
                    label = { Text(if (individual) "Nombre de la persona" else "Nombre de la deuda") },
                    placeholder = { Text(if (individual) "Ejemplo: Pedro" else "Ejemplo: banner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                // ------------------------------------------------------------------
                // El monto depende del modo
                // ------------------------------------------------------------------
                if (modo == ModoDeuda.META) {
                    OutlinedTextField(
                        value = totalTexto,
                        onValueChange = { entrada ->
                            // Solo dígitos, punto o coma.
                            if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                totalTexto = entrada
                            }
                        },
                        label = { Text("Costo total") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = totalTexto.isNotEmpty() && !totalValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    // La cuota a mano: es para cuando Cesar no quiere que el
                    // costo se divida. Vacía = se divide como siempre.
                    OutlinedTextField(
                        value = cuotaTexto,
                        onValueChange = { entrada ->
                            if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                cuotaTexto = entrada
                            }
                        },
                        label = { Text("Cuota que vas a cobrar") },
                        placeholder = { Text("Ejemplo: 1.90") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = cuotaTexto.isNotEmpty() && !cuotaValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Opcional. Si la pones, cobra esa cantidad a cada uno y el " +
                            "total a cobrar sale de multiplicarla por la gente. Si la dejas " +
                            "vacía, se divide el costo total.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                } else {
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { entrada ->
                            if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                montoTexto = entrada
                            }
                        },
                        label = { Text("Monto para cada uno") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = montoTexto.isNotEmpty() && !montoValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))
                // En una deuda individual no hay lista de personas: le debe
                // a una sola, así que el campo no tiene sentido.
                if (individual) {
                    Surface(
                        color = AcentoCalido.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(RadioChip),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Es una deuda individual: solo le debe a esta persona, " +
                                "así que no hay lista de gente ni cuota que repartir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AcentoCalido,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = personasTexto,
                        onValueChange = { entrada ->
                            if (entrada.isEmpty() || entrada.all { it.isDigit() }) {
                                personasTexto = entrada
                            }
                        },
                        label = { Text("¿Cuántas personas lo van a pagar?") },
                        placeholder = { Text("Ejemplo: 4") },
                        singleLine = true,
                        isError = personasTexto.isNotEmpty() && !personasValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Opcional: si no sabés cuántos van a pagar, dejalo vacío " +
                            "y la cuota se calcula con la gente que añadas.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }

                Spacer(Modifier.height(12.dp))
                // Con el número a mano, el número es el que divide: la cuota
                // sale de ahí y no de la lista de personas. Sin número, en
                // cambio, divide la lista.
                Surface(
                    color = AcentoCalido.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(RadioTarjeta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = when {
                                individual && modo == ModoDeuda.META -> "Esta persona paga: " +
                                    (cuotaPrevia?.dineroSimbolo() ?: sinNumero)
                                individual -> "Total a cobrar: " +
                                    (totalPrevio?.dineroSimbolo() ?: sinNumero)
                                modo == ModoDeuda.META -> "Cada uno paga: " +
                                    (cuotaPrevia?.dineroSimbolo() ?: sinNumero)
                                else -> "Total a cobrar: " +
                                    (totalPrevio?.dineroSimbolo() ?: sinNumero)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = AcentoCalido
                        )
                        Text(
                            text = when {
                                // Deuda individual: no hay lista, así que no
                                // hay nada que repartir ni que explicar.
                                individual && modo == ModoDeuda.META && !totalValido ->
                                    "Pon el costo total para ver cuánto le toca."
                                individual && modo == ModoDeuda.META ->
                                    "Le toca pagar ${total?.dineroSimbolo() ?: "—"}."
                                individual && modo == ModoDeuda.LIBRE && !montoValido ->
                                    "Pon el monto para ver el total."
                                individual ->
                                    "Total a cobrar: ${totalPrevio?.dineroSimbolo() ?: "—"}."
                                // Cuota automática sin nadie en la lista: no hay
                                // número que dar todavía.
                                automatico && !hayGente ->
                                    "Dejaste vacío cuántas personas pagan: la cuota se " +
                                        "calcula con la gente que añadas."
                                // Cuota automática con gente: la lista divide.
                                automatico -> when (modo) {
                                    ModoDeuda.META ->
                                        "La cuota sale de los $genteEnLista que están " +
                                            "en la lista: cada persona que añadas la baja."
                                    ModoDeuda.LIBRE ->
                                        "El total sale de los $genteEnLista que están " +
                                            "en la lista: cada persona que añadas suma su monto."
                                }
                                modo == ModoDeuda.META && cuota != null ->
                                    // Con cuota a mano el total a cobrar sale
                                    // de multiplicar, no de dividir.
                                    "Total a cobrar: " + (totalPrevio?.dineroSimbolo() ?: "—")
                                modo == ModoDeuda.META && !totalValido ->
                                    "Pon el costo total para ver cuánto paga cada uno."
                                modo == ModoDeuda.META ->
                                    "El costo total se divide entre $personas " +
                                        (if (personas == 1) "persona" else "personas") + "."
                                modo == ModoDeuda.LIBRE && !montoValido ->
                                    "Pon el monto para ver el total."
                                else -> "$personas " +
                                    (if (personas == 1) "persona paga" else "personas pagan") +
                                    " ${monto?.dineroSimbolo() ?: "—"} cada uno."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )

                        // Con cuota a mano, el costo real no se borra: se
                        // muestra aparte para que César lo vea.
                        if (modo == ModoDeuda.META && cuota != null && totalValido) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Costo real: ${total?.dineroSimbolo() ?: "—"} " +
                                    "(queda guardado aparte).",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextoSecundario
                            )
                        }

                        // Si ya hay montos especiales, se avisa de que cambian
                        // el monto de los demás (o el total, en modo libre).
                        val nota = when {
                            modo == ModoDeuda.META && !totalValido -> null
                            modo == ModoDeuda.LIBRE && !montoValido -> null
                            else -> textoRedistribucion(simulada)
                        }
                        if (nota != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = nota,
                                style = MaterialTheme.typography.labelSmall,
                                color = AcentoHielo
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (individual) {
                    Text(
                        text = if (inicial == null) {
                            "La persona queda agregada sola: después solo hay que marcar " +
                                "cuánto pagó."
                        } else {
                            "Esta deuda es de una sola persona: no se le puede añadir más gente."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                } else if (inicial == null) {
                    Text(
                        text = when {
                            automatico -> "Después de crear la deuda agregas a las " +
                                "personas que van a pagar. Como no pusiste cuántos son, " +
                                (if (modo == ModoDeuda.META) {
                                    "la cuota se calcula con la gente que añadas."
                                } else {
                                    "el total se calcula con la gente que añadas."
                                })
                            modo == ModoDeuda.META && cuota != null ->
                                "Después de crear la deuda agregas a las personas que van " +
                                    "a pagar. La lista NO divide: cada una paga la cuota que pusiste."
                            else ->
                                "Después de crear la deuda agregas a las personas que van " +
                                    "a pagar. La lista NO divide: la cuota la define el número de " +
                                    "personas que la pagan."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                } else {
                    Text(
                        text = when {
                            automatico -> "En la lista hay ${inicial.personas.size} " +
                                (if (inicial.personas.size == 1) "persona" else "personas") +
                                ". Como no pusiste cuántos pagan, " +
                                (if (modo == ModoDeuda.META) {
                                    "la cuota se calcula con ellos: cada persona que agregues " +
                                        "la baja."
                                } else {
                                    "el total se calcula con ellos: cada persona que agregues " +
                                        "suma su monto."
                                })
                            modo == ModoDeuda.META && cuota != null ->
                                // Con cuota a mano no hay nada que repartir: el
                                // monto especial cambia el total, no la cuota.
                                "En la lista hay ${inicial.personas.size} " +
                                    (if (inicial.personas.size == 1) "persona" else "personas") +
                                    ". La cuota no cambia si agregas o quitas gente: es la que " +
                                    "pusiste. Si alguien tiene un monto especial, no cambia lo " +
                                    "que pagan los demás: cambia el total."
                            modo == ModoDeuda.META ->
                                "En la lista hay ${inicial.personas.size} " +
                                    (if (inicial.personas.size == 1) "persona" else "personas") +
                                    ". La cuota no cambia si agregas o quitas gente: la define " +
                                    "el número de personas que la pagan. Si alguien tiene un " +
                                    "monto especial, los demás se reparten lo que queda."
                            else ->
                                "En la lista hay ${inicial.personas.size} " +
                                    (if (inicial.personas.size == 1) "persona" else "personas") +
                                    ". El monto de cada uno no cambia si agregas o quitas gente: " +
                                    "lo define el número de personas que la pagan."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    onGuardar(
                        nombre.trim(),
                        modo,
                        if (modo == ModoDeuda.META) total!! else 0.0,
                        if (modo == ModoDeuda.LIBRE) monto!! else 0.0,
                        // Sin número: la cuota la calcula la gente de la lista.
                        personas,
                        // La cuota a mano solo existe en modo META: en modo
                        // libre el monto por persona ya es la cuota fija.
                        if (modo == ModoDeuda.META) cuota else null
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

/**
 * Alta o edición de una persona del reparto.
 *
 * - Editar (`persona != null`): se cambia el nombre y el **monto que debe
 *   pagar**. Con "Usar la cuota normal" el monto vuelve a ser el normal
 *   (queda en null).
 * - Alta (`persona == null`): nombre y, si hace falta, un monto especial.
 */
@Composable
fun DialogoPersona(
    persona: PersonaDeuda?,
    cuotaBase: Double,
    onGuardar: (nombre: String, cuotaPropia: Double?) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(persona?.nombre ?: "") }
    var montoPropio by remember { mutableStateOf(persona?.cuotaPropia != null) }
    var montoTexto by remember {
        mutableStateOf(montoEnTexto(persona?.cuotaPropia ?: cuotaBase))
    }

    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val montoValido = monto != null && monto > 0.0
    val valido = nombre.isNotBlank() && (!montoPropio || montoValido)

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (persona == null) "Añadir persona" else "Editar persona") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (it.length <= 40) nombre = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ejemplo: María") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                if (persona == null) {
                    // Alta: el monto especial es opcional.
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
                        Switch(checked = montoPropio, onCheckedChange = { montoPropio = it })
                    }
                    if (montoPropio) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = montoTexto,
                            onValueChange = { entrada ->
                                if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                    montoTexto = entrada
                                }
                            },
                            label = { Text("Monto que debe pagar") },
                            prefix = { Text("$") },
                            singleLine = true,
                            isError = montoTexto.isNotEmpty() && !montoValido,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Edición: el monto que debe pagar esta persona.
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { entrada ->
                            if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                                montoTexto = entrada
                                // Tocar el monto lo vuelve un caso especial.
                                montoPropio = true
                            }
                        },
                        label = { Text("Monto que debe pagar") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = montoPropio && montoTexto.isNotEmpty() && !montoValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    if (montoPropio) {
                        TextButton(
                            onClick = {
                                montoPropio = false
                                montoTexto = montoEnTexto(cuotaBase)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Usar la cuota normal (${cuotaBase.dineroSimbolo()})",
                                color = AcentoCalido
                            )
                        }
                    } else {
                        Text(
                            text = "Esta persona paga la cuota normal (${cuotaBase.dineroSimbolo()}).",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Lo que la persona ya pagó no se toca: solo cambia lo que le " +
                        "falta por pagar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    onGuardar(nombre.trim(), if (montoPropio) monto else null)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

/**
 * Registro de un pago de una persona.
 *
 * Todos los montos van redondeados a 2 decimales: nadie necesita ver
 * 8.333333333333334.
 *
 * El botón de adjuntar imagen SOLO aparece con YAPI: es la única forma de
 * "mandar" el comprobante. La imagen se devuelve como Uri porque el permiso de
 * la galería es temporal: el Lead la copia al almacenamiento interno de la app
 * (ViewModel.guardarComprobante) y guarda solo el nombre del archivo.
 */
@Composable
fun DialogoPago(
    persona: PersonaDeuda,
    cuota: Double,
    pendiente: Double,
    onGuardar: (monto: Double, metodo: MetodoPago, nota: String, comprobanteUri: Uri?) -> Unit,
    onCerrar: () -> Unit
) {
    val cuotaMostrada = CalculosDeudas.redondear(cuota)
    val pendienteMostrado = CalculosDeudas.redondear(pendiente)

    var montoTexto by remember {
        mutableStateOf(if (pendienteMostrado > 0.0) montoEnTexto(pendienteMostrado) else "")
    }
    var metodo by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var nota by remember { mutableStateOf("") }
    var imagen by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imagen = uri
    }

    val monto = montoTexto.replace(",", ".").toDoubleOrNull()
    val montoValido = monto != null && monto > 0.0
    val miniatura = rememberMiniatura(imagen)

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Pago de ${persona.nombre}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Cuota y pendiente de esta persona.
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CUOTA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                        Text(
                            text = cuotaMostrada.dineroSimbolo(),
                            style = EstiloDinero,
                            color = TextoPrimario
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PENDIENTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                        Text(
                            text = pendienteMostrado.dineroSimbolo(),
                            style = EstiloDinero,
                            color = if (pendienteMostrado > 0.0) ColorGasto else AcentoMenta
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { entrada ->
                        if (entrada.isEmpty() || entrada.all { it.isDigit() || it == '.' || it == ',' }) {
                            montoTexto = entrada
                        }
                    },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    singleLine = true,
                    isError = montoTexto.isNotEmpty() && !montoValido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Método de pago",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetodoPago.entries.forEach { opcion ->
                        FilterChip(
                            selected = metodo == opcion,
                            onClick = {
                                metodo = opcion
                                // El comprobante solo existe con YAPI.
                                if (opcion != MetodoPago.YAPI) imagen = null
                            },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nota,
                    onValueChange = { if (it.length <= 80) nota = it },
                    label = { Text("Nota (opcional)") },
                    placeholder = { Text("Ejemplo: me lo pasó por YAPI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // El comprobante solo se puede adjuntar con YAPI.
                if (metodo == MetodoPago.YAPI) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Comprobante",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoSecundario
                    )
                    Spacer(Modifier.height(8.dp))

                    val uri = imagen
                    if (uri == null) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adjuntar imagen")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (miniatura != null) {
                                Image(
                                    bitmap = miniatura,
                                    contentDescription = "Comprobante elegido",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SuperficieAlta),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Imagen\nelegida",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextoSecundario
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uri.lastPathSegment ?: "Imagen elegida",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoSecundario,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(onClick = { imagen = null }) {
                                    Text("Quitar imagen", color = ColorGasto)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "YAPI es la única forma de mandar el comprobante. La imagen se " +
                                "guarda dentro de la app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = montoValido,
                onClick = {
                    onGuardar(
                        monto!!,
                        metodo,
                        nota.trim(),
                        if (metodo == MetodoPago.YAPI) imagen else null
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}

/**
 * Pruebas (comprobantes) de una persona.
 *
 * El método de pago y la nota son información **adicional** de la persona:
 * quedan guardados aunque la persona no haya pagado nada todavía. Por eso el
 * método se puede cambiar aquí mismo, sin registrar un pago.
 *
 * El botón de adjuntar SOLO aparece con YAPI: es la única forma de "mandar"
 * el comprobante. Con Efectivo se explica por qué no se puede.
 *
 * La imagen se devuelve como Uri porque el permiso de la galería es temporal:
 * el Lead la copia al almacenamiento interno de la app (ViewModel.agregarPrueba)
 * y guarda solo el nombre del archivo.
 */
@Composable
fun DialogoPruebas(
    persona: PersonaDeuda?,
    onAdjuntar: (Uri) -> Unit,
    onQuitar: (String) -> Unit,
    onMetodo: (MetodoPago?) -> Unit,
    onVer: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onAdjuntar(it) }
    }

    // El método se guarda al instante, pero aquí también se recuerda para que
    // los chips respondan sin esperar a que vuelva el dato guardado.
    var metodo by remember(persona?.id) { mutableStateOf(persona?.metodoPago) }
    val esYapi = metodo == MetodoPago.YAPI
    val pruebas = persona?.pruebas.orEmpty()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(text = if (persona != null) "Pruebas de ${persona.nombre}" else "Pruebas")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (persona == null) {
                    Text(
                        text = "No se encontró a esta persona: puede que se haya quitado " +
                            "del reparto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                } else {
                    // ------------------------------------------------------------------
                    // Método de pago de la persona
                    // ------------------------------------------------------------------
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoSecundario
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetodoPago.entries.forEach { opcion ->
                            FilterChip(
                                selected = metodo == opcion,
                                onClick = {
                                    metodo = opcion
                                    onMetodo(opcion)
                                },
                                label = { Text(opcion.etiqueta) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Es información adicional de esta persona: queda guardado " +
                            "aunque todavía no haya pagado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )

                    Spacer(Modifier.height(16.dp))

                    // ------------------------------------------------------------------
                    // Las pruebas que ya tiene
                    // ------------------------------------------------------------------
                    Text(
                        text = "Pruebas adjuntas",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoSecundario
                    )
                    Spacer(Modifier.height(8.dp))

                    if (pruebas.isEmpty()) {
                        Text(
                            text = "Esta persona todavía no tiene pruebas adjuntas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundario
                        )
                    } else {
                        Text(
                            text = "Toca una miniatura para verla en grande.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoTercero
                        )
                        Spacer(Modifier.height(8.dp))
                        pruebas.forEach { nombre ->
                            FilaPrueba(
                                nombreArchivo = nombre,
                                onVer = { onVer(nombre) },
                                onQuitar = { onQuitar(nombre) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ------------------------------------------------------------------
                    // Adjuntar
                    // ------------------------------------------------------------------
                    if (esYapi) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adjuntar prueba")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Se acepta cualquier imagen (PNG, JPG, WEBP, HEIC). " +
                                "La copia queda guardada dentro de la app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextoSecundario
                        )
                    } else {
                        Text(
                            text = "El comprobante solo se puede adjuntar con YAPI: es la " +
                                "única forma de mandar el archivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorAviso
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

/**
 * Una prueba adjunta: la miniatura, el nombre del archivo y el 🗑 para
 * borrarla. Tocar la miniatura la abre en grande.
 */
@Composable
private fun FilaPrueba(
    nombreArchivo: String,
    onVer: () -> Unit,
    onQuitar: () -> Unit
) {
    val miniatura = rememberMiniaturaPrueba(nombreArchivo)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (miniatura != null) {
                Image(
                    bitmap = miniatura,
                    contentDescription = "Abrir $nombreArchivo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(RadioBoton))
                        .clickable { onVer() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(RadioBoton))
                        .background(SuperficieAlta)
                        .clickable { onVer() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin vista\nprevia",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombreArchivo,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Comprobante adjunto",
                style = MaterialTheme.typography.labelSmall,
                color = TextoTercero
            )
        }
        IconButton(onClick = onQuitar, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Quitar prueba",
                tint = ColorGasto,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Miniatura de la imagen elegida. Si el Uri no se puede decodificar, se
 * devuelve null y el diálogo muestra el texto "Imagen elegida" sin romperse.
 */
@Composable
private fun rememberMiniatura(uri: Uri?): ImageBitmap? {
    val contexto = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, uri) {
        value = null
        if (uri != null) {
            value = withContext(Dispatchers.IO) {
                try {
                    contexto.contentResolver.openInputStream(uri)?.use { entrada ->
                        BitmapFactory.decodeStream(entrada)?.asImageBitmap()
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }.value
}

/** "50" o "12.5" (sin ceros de más) para precargar los campos de monto. */
private fun montoEnTexto(monto: Double): String =
    if (monto % 1.0 == 0.0) monto.toLong().toString() else monto.toString()

/**
 * Miniatura de una prueba ya guardada dentro de la app.
 *
 * El archivo vive en la carpeta interna "pruebas" (ver
 * [RepositorioDeudas.CARPETA_PRUEBAS]): se lee a mano con [BitmapFactory] y se
 * reduce antes de pintar para no gastar memoria con fotos grandes del
 * teléfono. Si el archivo no está (o no se puede leer) se devuelve null y el
 * diálogo no se rompe.
 */
@Composable
private fun rememberMiniaturaPrueba(nombreArchivo: String): ImageBitmap? {
    val contexto = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, nombreArchivo) {
        value = null
        val archivo = File(File(contexto.filesDir, RepositorioDeudas.CARPETA_PRUEBAS), nombreArchivo)
        value = withContext(Dispatchers.IO) {
            try {
                leerMiniatura(archivo)
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
    }.value
}

/**
 * Lee la imagen del disco ya reducida: se calcula primero el tamaño que
 * tiene y se pide a [BitmapFactory] un mapa de bits más pequeño.
 *
 * El HEIC no lo entiende [BitmapFactory], pero desde Android 9 sí lo lee
 * [ImageDecoder]: por eso se prueba de segunda.
 */
private fun leerMiniatura(archivo: File): ImageBitmap? {
    if (!archivo.exists()) return null

    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.absolutePath, opciones)
    var muestreo = 1
    val ancho = opciones.outWidth
    val alto = opciones.outHeight
    while (ancho / muestreo > 320 || alto / muestreo > 320) {
        muestreo *= 2
    }
    val bitmap = BitmapFactory.decodeFile(
        archivo.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = muestreo }
    )
    if (bitmap != null) return bitmap.asImageBitmap()

    return leerConImageDecoder(archivo)
}

/** Segundo intento, para los archivos HEIC (Android 9 o más). */
private fun leerConImageDecoder(archivo: File): ImageBitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return try {
        val fuente = ImageDecoder.createSource(archivo)
        ImageDecoder.decodeBitmap(fuente) { decodificador, info, _ ->
            val ancho = info.size.width.coerceAtLeast(1)
            val alto = info.size.height.coerceAtLeast(1)
            val escala = minOf(1f, 320f / ancho, 320f / alto)
            decodificador.setTargetSize(
                (ancho * escala).toInt().coerceAtLeast(1),
                (alto * escala).toInt().coerceAtLeast(1)
            )
        }.asImageBitmap()
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
