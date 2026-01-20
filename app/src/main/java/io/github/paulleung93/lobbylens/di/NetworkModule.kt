package io.github.paulleung93.lobbylens.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.paulleung93.lobbylens.BuildConfig
import io.github.paulleung93.lobbylens.data.api.FecApiService
import io.github.paulleung93.lobbylens.data.api.GeminiApiService
import io.github.paulleung93.lobbylens.data.network.CloudVisionService
import io.github.paulleung93.lobbylens.data.api.SenateLdaApiService
import io.github.paulleung93.lobbylens.util.SignatureUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module that provides network-related dependencies.
 * Centralizes all Retrofit configuration and API service creation.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val FEC_BASE_URL = "https://api.open.fec.gov/v1/"
    private const val VISION_BASE_URL = "https://vision.googleapis.com/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val SENATE_LDA_BASE_URL = "https://lda.senate.gov/api/v1/"

    /**
     * Provides an OkHttpClient configured for the FEC API.
     * Adds the API key as a query parameter to every request.
     */
    @Provides
    @Singleton
    @Named("FecClient")
    fun provideFecHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.FEC_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
            .build()
    }

    /**
     * Provides an OkHttpClient configured for Google Cloud APIs.
     * Adds Android package and certificate headers for API key restrictions.
     */
    @Provides
    @Singleton
    @Named("CloudClient")
    fun provideCloudHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val packageName = context.packageName
        val signatureDigest = SignatureUtils.getSignature(context)
        
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                builder.addHeader("X-Android-Package", packageName)
                signatureDigest?.let { builder.addHeader("X-Android-Cert", it) }
                chain.proceed(builder.build())
            }
            .build()
    }

    /**
     * Provides an OkHttpClient configured for Gemini API.
     * Has longer read timeout for AI image generation.
     * Only adds logging in debug builds.
     */
    @Provides
    @Singleton
    @Named("GeminiClient")
    fun provideGeminiHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 2 minutes for AI generation
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    /**
     * Provides an OkHttpClient configured for Senate LDA API.
     * Increased timeouts for potentially slow/large responses.
     */
    @Provides
    @Singleton
    @Named("SenateClient")
    fun provideSenateHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideFecApiService(@Named("FecClient") client: OkHttpClient): FecApiService {
        return Retrofit.Builder()
            .baseUrl(FEC_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FecApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCloudVisionService(@Named("CloudClient") client: OkHttpClient): CloudVisionService {
        return Retrofit.Builder()
            .baseUrl(VISION_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudVisionService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("GeminiClient") client: OkHttpClient): GeminiApiService {
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSenateLdaApiService(@Named("SenateClient") client: OkHttpClient): SenateLdaApiService {
        return Retrofit.Builder()
            .baseUrl(SENATE_LDA_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SenateLdaApiService::class.java)
    }
}
