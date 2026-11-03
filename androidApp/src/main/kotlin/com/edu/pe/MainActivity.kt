package com.edu.pe

import pe.edu.upeu.App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.core.context.GlobalContext
import pe.edu.upeu.data.permiso.GestorPermisosAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        GlobalContext.get().get<GestorPermisosAndroid>().puente = PuentePermisosActivity(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        GlobalContext.get().get<GestorPermisosAndroid>().puente = null
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
