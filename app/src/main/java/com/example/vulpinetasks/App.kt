package com.example.vulpinetasks

import android.app.Application
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.utils.SettingsManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализируем базу данных
        AppGraph.init(this)
    }
}