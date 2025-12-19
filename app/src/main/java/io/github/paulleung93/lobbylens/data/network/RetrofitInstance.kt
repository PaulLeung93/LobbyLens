package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object for providing a configured Retrofit instance for the FEC API.
 * This ensures that a single, efficient networking client is used throughout the app.
 */
object RetrofitInstance {

    // The base URL for the official FEC API.
    private const val BASE_URL = "https://api.open.fec.gov/v1/"

    /**
     * Creates an OkHttpClient that adds the FEC API key as a query parameter to every request.
     * This is the standard way to handle API key authentication with Retrofit.
     */
    private val fecHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val original = chain.request()
        val originalHttpUrl = original.url

        val url = originalHttpUrl.newBuilder()
            .addQueryParameter("api_key", BuildConfig.FEC_API_KEY)
            .build()

        val requestBuilder = original.newBuilder().url(url)
        val request = requestBuilder.build()
        chain.proceed(request)
    }.build()

    /**
     * Lazily creates and configures a Retrofit instance for the new FEC API service.
     * The lazy delegate ensures that the instance is created only once, when it's first needed.
     */
    /**
     * Lazily creates and configures a Retrofit instance for the new FEC API service.
     */
    val api: FecApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(fecHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FecApiService::class.java)
    }

    private const val VISION_BASE_URL = "https://vision.googleapis.com/"

    val cloudVisionApi: CloudVisionService by lazy {
        Retrofit.Builder()
            .baseUrl(VISION_BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudVisionService::class.java)
    }

    // Defaulting to us-central1 for now. 
    // Ideally this should be dynamic based on BuildConfig.GOOGLE_CLOUD_LOCATION but Retrofit BaseURL is static.
    private const val VERTEX_AI_BASE_URL = "https://us-central1-aiplatform.googleapis.com/"

    val vertexAiApi: VertexAiService by lazy {
        Retrofit.Builder()
            .baseUrl(VERTEX_AI_BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VertexAiService::class.java)
    }
}
