package io.github.paulleung93.lobbylens

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.paulleung93.lobbylens.data.network.RetrofitInstance

@HiltAndroidApp
class LobbyLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitInstance with application context to allow header injection
        // This will be removed once we complete DI migration
        RetrofitInstance.init(this)
    }
}

