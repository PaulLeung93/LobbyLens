package io.github.paulleung93.lobbylens.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Request body for the image generation Cloud Function.
 */
data class ImageGenRequest(
    val prompt: String
)

/**
 * Response from the image generation Cloud Function.
 */
data class ImageGenResponse(
    val image: String?, // Base64 encoded image
    val error: String?
)

/**
 * Retrofit interface for the image generation Cloud Function.
 */
interface ImageGenService {
    @POST("generateImage")
    suspend fun generateImage(@Body request: ImageGenRequest): Response<ImageGenResponse>
}
