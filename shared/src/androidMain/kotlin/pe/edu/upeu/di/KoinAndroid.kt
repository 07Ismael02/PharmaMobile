package pe.edu.upeu.di

import android.content.Context
import org.koin.dsl.module

fun initKoinAndroid(contexto: Context) {
    initKoin(listOf(module { single<Context> { contexto.applicationContext } }))
}
