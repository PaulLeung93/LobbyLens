package io.github.paulleung93.lobbylens.data.api

import io.github.paulleung93.lobbylens.data.model.GeminiRequest
import io.github.paulleung93.lobbylens.data.model.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Gemini API
 */
interface GeminiApiService {
    
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
