package io.github.paulleung93.lobbylens.data.api

import io.github.paulleung93.lobbylens.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for creating and configuring a single instance of Retrofit.
 * This ensures a single, efficient networking client is used throughout the application.
 * It's configured for the official FEC (Federal Election Commission) API.
 */
object RetrofitInstance {

    private const val BASE_URL = "https://api.open.fec.gov/v1/"

    /**
     * An OkHttpClient is configured with an interceptor to automatically add the
     * FEC API key as a query parameter to every single outgoing request.
     * The key is sourced securely from the BuildConfig file.
     */
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("api_key", BuildConfig.FEC_API_KEY)
                .build()
            val request = original.newBuilder().url(url).build()
            chain.proceed(request)
        }
        .build()

    /**
     * Lazily creates the Retrofit instance with the base URL, the custom OkHttpClient,
     * and the Gson converter factory for JSON parsing.
     */
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the FecApiService interface.
     * This is the public entry point for accessing the API service.
     */
    val api: FecApiService by lazy {
        retrofit.create(FecApiService::class.java)
    }

    /**
     * Retrofit instance for Google Cloud Vision API.
     */
    private val cloudVisionRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://vision.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the CloudVisionService interface.
     */
    val cloudVisionApi: io.github.paulleung93.lobbylens.data.network.CloudVisionService by lazy {
        cloudVisionRetrofit.create(io.github.paulleung93.lobbylens.data.network.CloudVisionService::class.java)
    }

    /**
     * Retrofit instance for Vertex AI API.
     */
    private val vertexAiRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://aiplatform.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the VertexAiService interface.
     */
    val vertexAiApi: io.github.paulleung93.lobbylens.data.network.VertexAiService by lazy {
        vertexAiRetrofit.create(io.github.paulleung93.lobbylens.data.network.VertexAiService::class.java)
    }

    /**
     * Retrofit instance for Senate Lobbying Disclosure API.
     */
    private val senateLdaRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://lda.senate.gov/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the SenateLdaApiService interface.
     */
    val senateLdaApi: SenateLdaApiService by lazy {
        senateLdaRetrofit.create(SenateLdaApiService::class.java)
    }
}
