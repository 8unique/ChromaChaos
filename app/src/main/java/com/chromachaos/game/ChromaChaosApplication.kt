package com.chromachaos.game

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.chromachaos.game.data.local.GameDatabase

@HiltAndroidApp
class ChromaChaosApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Ensure database is created (this will trigger the onCreate callback)
        GameDatabase.getDatabase(this)
    }
}