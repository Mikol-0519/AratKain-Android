package com.aratkain

import android.app.Application
import com.aratkain.core.model.UserData

// ── Custom Application class ──────────────────────────────────
// Holds app-wide state accessible from any screen
class AratKainApp : Application() {

    // In-memory cache of the logged-in user
    var currentUser: UserData? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AratKainApp
            private set
    }
}