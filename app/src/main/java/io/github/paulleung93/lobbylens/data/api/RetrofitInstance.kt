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
        .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
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

    private var packageName: String? = null
    private var signatureDigest: String? = null

    /**
     * Initializes the RetrofitInstance with the application context for API key restrictions.
     */
    fun init(context: android.content.Context) {
        packageName = context.packageName
        signatureDigest = io.github.paulleung93.lobbylens.util.SignatureUtils.getSignature(context)
        android.util.Log.d("RetrofitInstance", "Initialized with package: $packageName, signature: $signatureDigest")
    }

    fun getHeaderInfo(): String = "pkg=$packageName, sig=${signatureDigest?.take(10)}..."

    private fun getCloudClient(): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            android.util.Log.d("RetrofitInstance", "Interceptor: Package=$packageName, Signature=$signatureDigest")
            packageName?.let { builder.addHeader("X-Android-Package", it) }
            signatureDigest?.let { builder.addHeader("X-Android-Cert", it) }
            chain.proceed(builder.build())
        }.build()
    }

    /**
     * Retrofit instance for Google Cloud Vision API.
     */
    private val cloudVisionRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://vision.googleapis.com/")
            .client(getCloudClient())
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
     * Creates a client with extended timeout for image generation APIs.
     */
    private fun getLongTimeoutClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                packageName?.let { builder.addHeader("X-Android-Package", it) }
                signatureDigest?.let { builder.addHeader("X-Android-Cert", it) }
                chain.proceed(builder.build())
            }
            .build()
    }

    /**
     * Retrofit instance for Vertex AI API.
     */
    private val vertexAiRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://aiplatform.googleapis.com/")
            .client(getLongTimeoutClient())
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
     * Creates a client for the Senate LDA API that adds the Authorization token header.
     */
    private fun getSenateClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            
            val apiKey = BuildConfig.SENATE_API_KEY
            if (apiKey.isNotEmpty()) {
                builder.addHeader("Authorization", "Token $apiKey")
            }
            
            // The API requires a descriptive User-Agent
            builder.header("User-Agent", "LobbyLens/1.0 (https://github.com/paulleung93/lobbylens)")
            
            chain.proceed(builder.build())
        }.build()
    }

    /**
     * Retrofit instance for Senate Lobbying Disclosure API.
     */
    private val senateLdaRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://lda.senate.gov/api/v1/")
            .client(getSenateClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the SenateLdaApiService interface.
     */
    val senateLdaApi: SenateLdaApiService by lazy {
        senateLdaRetrofit.create(SenateLdaApiService::class.java)
    }

    /**
     * Retrofit instance for Image Generation Cloud Function.
     */
    private val imageGenRetrofit by lazy {
        val baseUrl = io.github.paulleung93.lobbylens.BuildConfig.CLOUD_FUNCTION_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getLongTimeoutClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily creates the implementation of the ImageGenService interface.
     */
    val imageGenApi: io.github.paulleung93.lobbylens.data.network.ImageGenService by lazy {
        imageGenRetrofit.create(io.github.paulleung93.lobbylens.data.network.ImageGenService::class.java)
    }
}
