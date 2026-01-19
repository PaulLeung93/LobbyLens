package io.github.paulleung93.lobbylens.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import io.github.paulleung93.lobbylens.BuildConfig
import io.github.paulleung93.lobbylens.data.api.GeminiApiService
import io.github.paulleung93.lobbylens.data.model.GeminiContent
import io.github.paulleung93.lobbylens.data.model.GeminiGenerationConfig
import io.github.paulleung93.lobbylens.data.model.GeminiInlineData
import io.github.paulleung93.lobbylens.data.model.GeminiPart
import io.github.paulleung93.lobbylens.data.model.GeminiRequest
import io.github.paulleung93.lobbylens.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI image generation operations.
 * Handles Gemini-based image editing to add sponsor logos.
 */
@Singleton
class ImageGenerationRepository @Inject constructor(
    private val geminiApiService: GeminiApiService
) {
    companion object {
        private const val TAG = "ImageGenerationRepo"
    }

    /**
     * Generates a new image using Gemini to edit the original photo,
     * adding sponsor logos to the politician's clothing naturally via AI.
     */
    suspend fun generatePoliticianImage(
        baseBitmap: Bitmap,
        logos: List<String>
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        Log.d(TAG, "generatePoliticianImage: Starting AI image editing with Gemini")
        Log.d(TAG, "generatePoliticianImage: Organizations: ${logos.joinToString(", ")}")
        
        try {
            val companyNames = logos.joinToString(", ")

            // 1. Encode image to base64
            val base64Image = bitmapToBase64(baseBitmap)
            if (base64Image == null) {
                Log.e(TAG, "generatePoliticianImage: Failed to encode base image")
                return@withContext Result.Error(Exception("Failed to encode base image"))
            }

            // 2. Prepare Gemini Request
            val parts = mutableListOf<GeminiPart>()

            // Add image part
            parts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = base64Image
                    )
                )
            )

            // Add editing prompt
            val prompt = """
                Edit the first image (the politician's photo) to add the company sponsor logos/patches for: $companyNames.
                
                Place the logos similarly to how sponsor patches appear on athletic uniforms - on the chest, lapel, or upper arm area.
                Create realistic text-based logo patches or approximate logo designs for these organizations.
                
                Make the logo placement look natural, professional, and realistic.
                Preserve the person's identity exactly - do not change their face, hair, or other features.
                Only add the logos to their clothing.
                
                Return ONLY the edited image. Do not provide any textual explanation.
            """.trimIndent()

            parts.add(GeminiPart(text = prompt))

            // 3. Create request
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = parts)),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("IMAGE")
                )
            )

            // 4. Call Gemini API
            Log.d(TAG, "generatePoliticianImage: Sending request to Gemini API...")
            val response = geminiApiService.generateContent(
                apiKey = BuildConfig.GOOGLE_API_KEY,
                request = request
            )

            if (!response.isSuccessful || response.body() == null) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "generatePoliticianImage: API error: ${response.code()} - $errorBody")
                return@withContext Result.Error(Exception("Gemini API error: ${response.code()}"))
            }

            val geminiResponse = response.body()!!
            Log.d(TAG, "generatePoliticianImage: Response received - candidates count: ${geminiResponse.candidates?.size ?: 0}")

            if (geminiResponse.error != null) {
                Log.e(TAG, "generatePoliticianImage: Gemini error: ${geminiResponse.error.message}")
                return@withContext Result.Error(Exception("Gemini error: ${geminiResponse.error.message}"))
            }

            // 5. Extract edited image from response
            val candidate = geminiResponse.candidates?.firstOrNull()
            if (candidate == null) {
                Log.e(TAG, "generatePoliticianImage: No candidates in response")
                return@withContext Result.Error(Exception("No image generated"))
            }

            var editedImageBase64: String? = null
            val textParts = mutableListOf<String>()

            val responseParts = candidate.content.parts
            if (responseParts == null) {
                Log.e(TAG, "generatePoliticianImage: Response has no parts array")
                return@withContext Result.Error(Exception("Invalid response structure: no parts"))
            }

            for (part in responseParts) {
                if (part.inlineData != null) {
                    editedImageBase64 = part.inlineData.data
                    break
                }
                if (part.text != null) {
                    textParts.add(part.text)
                }
            }

            if (editedImageBase64 == null) {
                Log.e(TAG, "generatePoliticianImage: No image data in response")
                val message = if (textParts.isNotEmpty()) {
                    "Gemini Message: ${textParts.joinToString("\n")}"
                } else {
                    "No edited image returned. FinishReason: ${candidate.finishReason ?: "Unknown"}"
                }
                return@withContext Result.Error(Exception(message))
            }

            // 6. Decode base64 to Bitmap
            val editedBitmap = base64ToBitmap(editedImageBase64)
            if (editedBitmap == null) {
                Log.e(TAG, "generatePoliticianImage: Failed to decode image")
                return@withContext Result.Error(Exception("Failed to decode edited image"))
            }

            Log.i(TAG, "generatePoliticianImage: Successfully created AI-edited image")
            Result.Success(editedBitmap)

        } catch (e: Exception) {
            Log.e(TAG, "generatePoliticianImage: Exception occurred", e)
            Result.Error(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "bitmapToBase64: Failed to encode", e)
            null
        }
    }

    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "base64ToBitmap: Failed to decode", e)
            null
        }
    }
}
