package com.cesar.ahorros

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.cesar.ahorros.data.Preferencias
import com.cesar.ahorros.ui.AparienciaActual
import com.cesar.ahorros.ui.App
import com.cesar.ahorros.ui.TemaActual
import com.cesar.ahorros.ui.TemaAhorros

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferencias = Preferencias(this)

        // Los ajustes de apariencia (los colores que el usuario cambió, el
        // fondo y las animaciones) se cargan ANTES de pintar nada: si no,
        // la app aparecería con el tema viejo y cambiaría delante del usuario.
        AparienciaActual.cargar(preferencias.apariencia())

        // El tema que eligió el usuario se aplica ANTES de pintar nada: así la
        // app no aparece con el tema viejo y cambia al nuevo delante de él.
        TemaActual.aplicar(preferencias.temaId())

        // El fondo de la ventana sigue al tema: en los temas claros no debe
        // verse un destello negro mientras se abre la app.
        window.setBackgroundDrawable(ColorDrawable(TemaActual.tema.fondo.toArgb()))

        setContent {
            TemaAhorros {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    App()
                }
            }
        }
    }
}
