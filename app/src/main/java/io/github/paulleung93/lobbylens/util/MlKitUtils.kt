package io.github.paulleung93.lobbylens.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A utility object containing suspend functions for ML Kit operations,
 * such as face detection and selfie segmentation, designed for use with coroutines.
 */
object MlKitUtils {

    /**
     * Asynchronously detects faces in a bitmap image using ML Kit.
     *
     * @param bitmap The input image.
     * @return A list of detected faces.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
            val detector = FaceDetection.getClient(options)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (continuation.isActive) {
                        continuation.resume(faces)
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
        }
    }

    /**
     * Asynchronously generates a segmentation mask for a person in an image.
     *
     * @param bitmap The input image.
     * @return A [SegmentationMask] that separates the person from the background.
     */
    suspend fun segmentSelfie(bitmap: Bitmap): SegmentationMask {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)

            segmenter.process(image)
                .addOnSuccessListener { mask ->
                    if (continuation.isActive) {
                        continuation.resume(mask)
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
        }
    }
}
