package com.edu.pe

import android.app.Application
import pe.edu.upeu.di.initKoinAndroid

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
