package io.github.paulleung93.lobbylens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for Gemini API (REST) image generation/editing
 */

data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiGenerationConfig(
    @SerializedName("responseModalities")
    val responseModalities: List<String>? = null
)

data class GeminiContent(
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("inlineData")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("data")
    val data: String // base64 encoded
)

data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>?,
    @SerializedName("error")
    val error: GeminiError?
)

data class GeminiCandidate(
    @SerializedName("content")
    val content: GeminiContent,
    @SerializedName("safetyRatings")
    val safetyRatings: List<GeminiSafetyRating>? = null,
    @SerializedName("finishReason")
    val finishReason: String? = null
)

data class GeminiSafetyRating(
    @SerializedName("category")
    val category: String,
    @SerializedName("probability")
    val probability: String
)

data class GeminiError(
    @SerializedName("message")
    val message: String,
    @SerializedName("code")
    val code: Int
)
