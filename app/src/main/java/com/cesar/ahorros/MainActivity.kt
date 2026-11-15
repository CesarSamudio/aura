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
import com.cesar.ahorros.ui.App
import com.cesar.ahorros.ui.TemaActual
import com.cesar.ahorros.ui.TemaAhorros

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferencias = Preferencias(this)

        // El tema que eligió el usuario se aplica ANTES de pintar nada: así la
        // app no aparece con el tema viejo y cambia al nuevo delante de él.
        TemaActual.aplicar(preferencias.temaId())

        // El ícono del launcher NO se toca acá a propósito: los ROMs que
        // cierran la app cuando cambia sus componentes (MIUI) lo hacen
        // mientras la app está a la vista. Se aplica cuando el usuario sale
        // de la app (ver `IconosApp` y el `App.kt`).

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
