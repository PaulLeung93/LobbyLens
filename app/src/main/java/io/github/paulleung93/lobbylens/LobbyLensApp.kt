package io.github.paulleung93.lobbylens

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class LobbyLensApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

         // Hilt handles injection.
         // RetrofitInstance.init(this) // Removed