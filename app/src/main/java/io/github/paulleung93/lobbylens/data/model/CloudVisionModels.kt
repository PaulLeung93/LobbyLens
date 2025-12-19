package io.github.paulleung93.lobbylens.data.model

data class CloudVisionRequest(
    val requests: List<AnnotateImageRequest>
)

data class AnnotateImageRequest(
    val image: ImageContent,
    val features: List<Feature>
)

data class ImageContent(
    val content: String // Base64 encoded image
)

data class Feature(
    val type: String = "WEB_DETECTION",
    val maxResults: Int = 10
)

data class CloudVisionResponse(
    val responses: List<AnnotateImageResponse>?
)

data class AnnotateImageResponse(
    val webDetection: WebDetection?,
    val error: Status?
)

data class WebDetection(
    val webEntities: List<WebEntity>?
)

data class WebEntity(
    val entityId: String?,
    val score: Float?,
    val description: String?
)

data class Status(
    val code: Int,
    val message: String
)
