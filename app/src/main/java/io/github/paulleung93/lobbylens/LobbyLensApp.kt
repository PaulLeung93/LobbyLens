package io.github.paulleung93.lobbylens

import android.app.Application
import io.github.paulleung93.lobbylens.data.api.RetrofitInstance

class LobbyLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitInstance with application context to allow header injection
        RetrofitInstance.init(this)
    }
}
