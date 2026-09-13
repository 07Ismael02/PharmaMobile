package com.edu.pe

import android.app.Application
import pe.edu.upeu.di.initKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
