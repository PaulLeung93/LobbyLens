package io.github.paulleung93.lobbylens.data.network

import io.github.paulleung93.lobbylens.BuildConfig
import io.github.paulleung93.lobbylens.data.api.GeminiApiService
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
    private val fecHttpClient = OkHttpClient.Builder()
        .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
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

    // Defaulting to us-central1 for now. 
    // Ideally this should be dynamic based on BuildConfig.GOOGLE_CLOUD_LOCATION but Retrofit BaseURL is static.
    private const val VERTEX_AI_BASE_URL = "https://us-central1-aiplatform.googleapis.com/"

    private var appContext: android.content.Context? = null
    private var packageName: String? = null
    private var signatureDigest: String? = null

    /**
     * Initializes the RetrofitInstance with the application context.
     * This is required to fetch the package name and signing certificate for API key restrictions.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        packageName = context.packageName
        signatureDigest = io.github.paulleung93.lobbylens.util.SignatureUtils.getSignature(context)
        android.util.Log.d("RetrofitInstance", "Initialized with package: $packageName, signature: $signatureDigest")
    }

    /**
     * Returns debug info about the current header state.
     */
    fun getHeaderInfo(): String = "pkg=$packageName, sig=${signatureDigest?.take(10)}..."

    private fun getCloudClient(): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            
            // Add Android restriction headers if available
            android.util.Log.d("RetrofitInstance", "Interceptor: Package=$packageName, Signature=$signatureDigest")

            packageName?.let {
                builder.addHeader("X-Android-Package", it)
            }
            signatureDigest?.let {
                builder.addHeader("X-Android-Cert", it)
            }

            chain.proceed(builder.build())
        }.build()
    }

    val cloudVisionApi: CloudVisionService by lazy {
        Retrofit.Builder()
            .baseUrl(VISION_BASE_URL)
            .client(getCloudClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudVisionService::class.java)
    }

    val vertexAiApi: VertexAiService by lazy {
        Retrofit.Builder()
            .baseUrl(VERTEX_AI_BASE_URL)
            .client(getCloudClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VertexAiService::class.java)
    }

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val geminiHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)  // 5 minutes
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(geminiHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
