package io.github.paulleung93.lobbylens.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for providing a configured Retrofit instance.
 * This ensures that a single, efficient networking client is used throughout the app.
 */
object RetrofitInstance {

    private const val BASE_URL = "https://www.opensecrets.org/api/"

    /**
     * Lazily creates and configures a Retrofit instance.
     * The lazy delegate ensures that the instance is created only once, when it's first needed.
     */
    val api: OpenSecretsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenSecretsApiService::class.java)
    }
}
