package io.github.paulleung93.lobbylens.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt

/**
 * A utility object containing helper functions for image manipulation,
 * such as saving, sharing, and composition.
 */
object ImageUtils {

    /**
     * Creates a composite image by overlaying company logos onto a base image.
     *
     * @param baseBitmap The original image of the politician.
     * @param mask The selfie segmentation mask to identify the person.
     * @param organizations A list of top contributing organizations (by employer) from the FEC API.
     * @param logos A map of organization names to their downloaded logo bitmaps.
     * @param faceBounds The bounding box of the detected face, to avoid drawing over it.
     * @return A new bitmap with the data overlays.
     */
    /**
     * Converts a Bitmap to a Base64 encoded string (JPEG format).
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(imageCollection, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Failed to save bitmap.")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                return it
            } catch (e: IOException) {
                resolver.delete(it, null, null)
                e.printStackTrace()
            }
        }
        return null
    }

    fun shareImage(context: Context, bitmap: Bitmap, authority: String) {
        val file = File(context.cacheDir, "images/shared_image.jpg")
        file.parentFile?.mkdirs()
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStream)
        fileOutputStream.close()

        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Image"))
    }

    /**
     * Overlays a list of logo bitmaps onto the image.
     * Use Gemini-guided placement if available.
     *
     * @param baseBitmap The original image.
     * @param logos The list of logo bitmaps to overlay.
     * @param placement Optional placement coordinates from Gemini.
     * @return A new Bitmap with the logos overlaid.
     */
    fun overlayLogosOnImage(
        baseBitmap: Bitmap, 
        logos: List<Bitmap>,
        placement: io.github.paulleung93.lobbylens.data.model.LogoPlacement? = null
    ): Bitmap {
        if (logos.isEmpty()) return baseBitmap

        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = android.graphics.Paint()

        // Default: Bottom placement
        var startY = mutableBitmap.height - (mutableBitmap.height * 0.15).toInt() - 40
        var targetSize = (mutableBitmap.height * 0.15).toInt()
        var customX = -1
        
        // Gemini Placement
        if (placement != null) {
            // Gemini returns 0-1000 scale
            val centerX = (placement.x / 1000f) * mutableBitmap.width
            val centerY = (placement.y / 1000f) * mutableBitmap.height
            var scale = (placement.scale_percent / 100f)
            
            // Safety clamp
            if (scale < 0.05f) scale = 0.15f
            if (scale > 0.5f) scale = 0.5f
            
            targetSize = (mutableBitmap.width * scale).toInt()
            startY = centerY.toInt() - (targetSize / 2)
            customX = centerX.toInt()
        }
        
        // Configuration for logo placement
        val padding = targetSize / 4
        val totalRowWidth = (logos.size * targetSize) + ((logos.size - 1) * padding)
        
        // Center horizontally based on customX if provided, or image center
        var currentX = if (customX != -1) {
            customX - (totalRowWidth / 2)
        } else {
            (mutableBitmap.width - totalRowWidth) / 2
        }

        // Ensure we don't go off screen
        if (startY + targetSize > mutableBitmap.height) startY = mutableBitmap.height - targetSize
        if (startY < 0) startY = 0

        for (logo in logos) {
            val scaledLogo = Bitmap.createScaledBitmap(logo, targetSize, targetSize, true)
            canvas.drawBitmap(scaledLogo, currentX.toFloat(), startY.toFloat(), paint)
            currentX += targetSize + padding
        }

        return mutableBitmap
    }
}
