package com.cesar.ahorros.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.cesar.ahorros.dominio.GestorMovimientos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = Repositorio(application)
    private val gestor = GestorMovimientos(repositorio.cargar())

    private val _movimientos = MutableStateFlow(gestor.movimientos)
    val movimientos: StateFlow<List<Movimiento>> = _movimientos.asStateFlow()

    // ------------------------------------------------------------------
    // Deudas (pestaña aparte: no son ingresos ni gastos)
    // ------------------------------------------------------------------

    private val repositorioDeudas = RepositorioDeudas(application)
    private val _deudas = MutableStateFlow(repositorioDeudas.cargar())
    val deudas: StateFlow<List<Deuda>> = _deudas.asStateFlow()

    /** Se enciende si no se pudo escribir el archivo: la UI lo avisa con un Snackbar. */
    private val _errorGuardado = MutableStateFlow<String?>(null)
    val errorGuardado: StateFlow<String?> = _errorGuardado.asStateFlow()

    fun agregar(movimiento: Movimiento) = escribir { gestor.agregar(movimiento) }

    fun actualizar(movimiento: Movimiento) = escribir { gestor.actualizar(movimiento) }

    fun eliminar(id: String) = escribir { gestor.eliminar(id) }

    fun nuevoId(): String = UUID.randomUUID().toString()

    fun limpiarError() {
        _errorGuardado.value = null
    }

    // ------------------------------------------------------------------
    // Deudas
    // ------------------------------------------------------------------

    fun agregarDeuda(deuda: Deuda) = escribirDeudas {
        _deudas.value = (_deudas.value + deuda).sortedByDescending { it.ts }
    }

    fun actualizarDeuda(deuda: Deuda) = escribirDeudas {
        _deudas.value = _deudas.value
            .map { if (it.id == deuda.id) deuda else it }
            .sortedByDescending { it.ts }
    }

    /** Borra la deuda y los comprobantes que se guardaron con sus pagos. */
    fun eliminarDeuda(id: String) = escribirDeudas {
        val deuda = _deudas.value.firstOrNull { it.id == id }
        deuda?.pagos?.forEach { borrarArchivoComprobante(it.comprobante) }
        _deudas.value = _deudas.value.filterNot { it.id == id }
    }

    /** Añade una persona al reparto. La cuota NO cambia: la define la deuda. */
    fun agregarPersona(deudaId: String, persona: PersonaDeuda) = escribirDeudas {
        transformarDeuda(deudaId) { deuda ->
            deuda.copy(personas = deuda.personas + persona)
        }
    }

    /**
     * Edita una persona: cambia su nombre o su monto propio (el "caso especial").
     * Editar el nombre NO toca lo que ya pagó.
     */
    fun actualizarPersona(deudaId: String, persona: PersonaDeuda) = escribirDeudas {
        transformarDeuda(deudaId) { deuda ->
            deuda.copy(personas = deuda.personas.map { if (it.id == persona.id) persona else it })
        }
    }

    /** Quita a una persona. Lo que ya pagó no se toca; la cuota se recalcula. */
    fun eliminarPersona(deudaId: String, personaId: String) = escribirDeudas {
        transformarDeuda(deudaId) { deuda ->
            deuda.copy(personas = deuda.personas.filterNot { it.id == personaId })
        }
    }

    fun agregarPago(deudaId: String, pago: Pago) = escribirDeudas {
        transformarDeuda(deudaId) { deuda ->
            deuda.copy(pagos = deuda.pagos + pago)
        }
    }

    /** Borra un pago y el comprobante que tenía guardado. */
    fun eliminarPago(deudaId: String, pagoId: String) = escribirDeudas {
        val deuda = _deudas.value.firstOrNull { it.id == deudaId }
        val pago = deuda?.pagos?.firstOrNull { it.id == pagoId }
        borrarArchivoComprobante(pago?.comprobante)
        transformarDeuda(deudaId) { actual ->
            actual.copy(pagos = actual.pagos.filterNot { it.id == pagoId })
        }
    }

    /** Marca o desmarca la deuda como saldada (a mano, nunca sola). */
    fun alternarSaldada(deudaId: String) = escribirDeudas {
        transformarDeuda(deudaId) { deuda -> deuda.copy(saldada = !deuda.saldada) }
    }

    /**
     * Copia la imagen elegida por el usuario dentro del almacenamiento interno
     * y devuelve el nombre del archivo. Se guarda una copia para no depender
     * del permiso temporal de la galería.
     */
    fun guardarComprobante(uri: Uri, nombreArchivo: String): String? =
        guardarArchivo(uri, RepositorioDeudas.CARPETA_COMPROBANTES, nombreArchivo)

    /** Ruta absoluta de un comprobante guardado (para mostrarlo). */
    fun rutaComprobante(nombreArchivo: String): File =
        File(File(getApplication<Application>().filesDir, RepositorioDeudas.CARPETA_COMPROBANTES), nombreArchivo)

    // ------------------------------------------------------------------
    // Pruebas (comprobantes adjuntos de una persona)
    // ------------------------------------------------------------------

    /**
     * Guarda el método de pago de una persona (información adicional).
     */
    fun actualizarMetodoPersona(deudaId: String, personaId: String, metodo: MetodoPago?) =
        escribirDeudas {
            transformarDeuda(deudaId) { deuda ->
                deuda.copy(
                    personas = deuda.personas.map {
                        if (it.id == personaId) it.copy(metodoPago = metodo) else it
                    }
                )
            }
        }

    /**
     * Copia la imagen elegida por el usuario y la adjunta a la persona.
     * La copia queda dentro de la app: no depende del permiso de la galería.
     */
    fun agregarPrueba(deudaId: String, personaId: String, uri: Uri): Boolean {
        val nombre = guardarArchivo(uri, RepositorioDeudas.CARPETA_PRUEBAS, "prueba_${nuevoId()}") ?: return false
        escribirDeudas {
            transformarDeuda(deudaId) { deuda ->
                deuda.copy(
                    personas = deuda.personas.map {
                        if (it.id == personaId) it.copy(pruebas = it.pruebas + nombre) else it
                    }
                )
            }
        }
        return true
    }

    /** Quita una prueba de una persona y borra el archivo. */
    fun quitarPrueba(deudaId: String, personaId: String, nombreArchivo: String) {
        borrarArchivo(RepositorioDeudas.CARPETA_PRUEBAS, nombreArchivo)
        escribirDeudas {
            transformarDeuda(deudaId) { deuda ->
                deuda.copy(
                    personas = deuda.personas.map {
                        if (it.id == personaId) {
                            it.copy(pruebas = it.pruebas.filterNot { prueba -> prueba == nombreArchivo })
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    /** Ruta absoluta de una prueba guardada (para mostrarla). */
    fun rutaPrueba(nombreArchivo: String): File =
        File(File(getApplication<Application>().filesDir, RepositorioDeudas.CARPETA_PRUEBAS), nombreArchivo)

    /** Copia un archivo elegido por el usuario a una carpeta interna de la app. */
    private fun guardarArchivo(uri: Uri, carpetaNombre: String, nombreBase: String): String? {
        return try {
            val contexto = getApplication<Application>()
            val carpeta = File(contexto.filesDir, carpetaNombre)
            if (!carpeta.exists()) carpeta.mkdirs()
            val tipo = contexto.contentResolver.getType(uri)
            val extension = when (tipo) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/heic" -> "heic"
                else -> "jpg"
            }
            val destino = File(carpeta, "$nombreBase.$extension")
            val entrada = contexto.contentResolver.openInputStream(uri)
            if (entrada == null) {
                null
            } else {
                entrada.use { flujo ->
                    destino.outputStream().use { salida -> flujo.copyTo(salida) }
                }
                destino.name
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun borrarArchivo(carpetaNombre: String, nombreArchivo: String) {
        if (nombreArchivo.isBlank()) return
        try {
            val archivo = File(
                File(getApplication<Application>().filesDir, carpetaNombre),
                nombreArchivo
            )
            if (archivo.exists()) archivo.delete()
        } catch (e: Exception) {
            // Si no se puede borrar, no vale la pena romper la app por eso.
        }
    }

    private fun borrarArchivoComprobante(nombreArchivo: String?) {
        if (nombreArchivo.isNullOrBlank()) return
        borrarArchivo(RepositorioDeudas.CARPETA_COMPROBANTES, nombreArchivo)
    }

    private inline fun transformarDeuda(deudaId: String, transformacion: (Deuda) -> Deuda) {
        _deudas.value = _deudas.value
            .map { if (it.id == deudaId) transformacion(it) else it }
            .sortedByDescending { it.ts }
    }

    // ------------------------------------------------------------------
    // Escritura en disco
    // ------------------------------------------------------------------

    private fun escribir(operacion: () -> Unit) {
        operacion()
        _movimientos.value = gestor.movimientos
        if (!repositorio.guardar(gestor.movimientos)) {
            _errorGuardado.value =
                "No se pudo guardar en el dispositivo. Los datos siguen en memoria: no cierres la app todavía."
        }
    }

    private inline fun escribirDeudas(operacion: () -> Unit) {
        operacion()
        if (!repositorioDeudas.guardar(_deudas.value)) {
            _errorGuardado.value =
                "No se pudo guardar la deuda en el dispositivo. Los datos siguen en memoria: no cierres la app todavía."
        }
    }

    // ------------------------------------------------------------------
    // Listas de gente (las que se guardan para las próximas deudas)
    // ------------------------------------------------------------------

    private val repositorioListas = RepositorioListas(application)
    private val preferencias = Preferencias(application)

    /**
     * Las listas que hay: primero la que viene con la app (la del 11G, solo en
     * esa versión) y después las que creó el usuario.
     */
    private val _listas = MutableStateFlow(cargarListas())
    val listas: StateFlow<List<ListaDeGente>> = _listas.asStateFlow()

    /**
     * Si la lista que viene con la app está borrada: si lo está, la pantalla
     * ofrece devolverla.
     */
    private val _listaDelColegioBorrada = MutableStateFlow(listaDelColegioEstaBorrada())
    val listaDelColegioBorrada: StateFlow<Boolean> = _listaDelColegioBorrada.asStateFlow()

    private fun cargarListas(): List<ListaDeGente> {
        val guardadas = repositorioListas.cargar()
        val delColegio = ListaSalon.lista
        return when {
            delColegio == null -> guardadas
            listaDelColegioEstaBorrada() -> guardadas
            else -> listOf(delColegio) + guardadas
        }
    }

    /** Si el usuario borró la lista que viene con la app. */
    private fun listaDelColegioEstaBorrada(): Boolean {
        val delColegio = ListaSalon.lista ?: return false
        return delColegio.id in preferencias.listasBorradas()
    }

    /** El nombre de la lista que viene con la app, si esta versión la tiene. */
    val nombreListaDelColegio: String? get() = ListaSalon.lista?.nombre

    /**
     * Crea una lista nueva con la gente que se le pase.
     * Devuelve `true` si se creó.
     */
    fun crearLista(nombre: String, personas: List<String>): Boolean {
        val nombreLimpio = nombre.trim()
        val gente = personas.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (nombreLimpio.isEmpty() || gente.isEmpty()) return false

        val nueva = ListaDeGente(
            id = UUID.randomUUID().toString(),
            nombre = nombreLimpio,
            personas = gente
        )
        // La lista que viene con la app no se guarda en el archivo: no se toca.
        val bloqueadas = _listas.value.filter { it.bloqueada }
        val propias = _listas.value.filter { !it.bloqueada }
        _listas.value = bloqueadas + propias + nueva
        if (!repositorioListas.guardar(propias + nueva)) {
            _errorGuardado.value =
                "No se pudo guardar la lista en el dispositivo. La lista sigue en memoria: no cierres la app todavía."
            return false
        }
        return true
    }

    /**
     * Borra una lista.
     *
     * Las que creó el usuario se borran del archivo. La que **viene con la
     * app** (la del salón del 11G) también se puede borrar: queda anotada en
     * los ajustes para que no vuelva a salir cada vez que se abre la app, y
     * se puede devolver desde la misma pantalla.
     */
    fun eliminarLista(id: String): Boolean {
        val lista = _listas.value.firstOrNull { it.id == id } ?: return false

        if (lista.bloqueada) {
            _listas.value = _listas.value.filter { it.id != id }
            _listaDelColegioBorrada.value = true
            preferencias.guardarListaBorrada(id)
            return true
        }

        val propias = _listas.value.filter { !it.bloqueada && it.id != id }
        _listas.value = _listas.value.filter { it.bloqueada || it.id != id }
        if (!repositorioListas.guardar(propias)) {
            _errorGuardado.value =
                "No se pudo borrar la lista del dispositivo. La lista sigue en memoria: no cierres la app todavía."
            return false
        }
        return true
    }

    /**
     * Devuelve la lista que viene con la app (la del salón del 11G), que el
     * usuario había borrado.
     */
    fun restaurarListaDelColegio(): Boolean {
        val delColegio = ListaSalon.lista ?: return false
        if (_listas.value.any { it.id == delColegio.id }) return false

        _listas.value = listOf(delColegio) + _listas.value
        _listaDelColegioBorrada.value = false
        preferencias.restaurarLista(delColegio.id)
        return true
    }
}
