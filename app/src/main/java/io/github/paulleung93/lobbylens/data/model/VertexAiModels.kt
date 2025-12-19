package io.github.paulleung93.lobbylens.data.model

data class VertexAiPredictionRequest(
    val instances: List<VertexAiInstance>,
    val parameters: VertexAiParameters
)

data class VertexAiInstance(
    val prompt: String,
    val image: VertexAiImage? = null // For editing/inpainting
)

data class VertexAiImage(
    val bytesBase64Encoded: String
)

data class VertexAiParameters(
    val sampleCount: Int = 1,
    val negativePrompt: String? = null,
    val aspectRatio: String? = "1:1"
)

data class VertexAiPredictionResponse(
    val predictions: List<String>? // Base64 encoded images
)
