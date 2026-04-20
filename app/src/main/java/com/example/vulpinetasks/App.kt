package com.example.vulpinetasks
import android.app.Application
import com.example.vulpinetasks.room.AppGraph

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        AppGraph.init(this)
    }
}