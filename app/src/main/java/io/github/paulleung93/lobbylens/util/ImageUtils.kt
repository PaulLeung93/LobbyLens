package io.github.paulleung93.lobbylens.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.mlkit.vision.segmentation.SegmentationMask
import io.github.paulleung93.lobbylens.data.model.Organization
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
     * @param organizations A list of top contributing organizations.
     * @param logos A map of organization names to their downloaded logo bitmaps.
     * @param faceBounds The bounding box of the detected face, to avoid drawing over it.
     * @return A new bitmap with the data overlays.
     */
    fun composeImage(
        baseBitmap: Bitmap,
        mask: SegmentationMask,
        organizations: List<Organization>,
        logos: Map<String, Bitmap>,
        faceBounds: Rect
    ): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val maxDonation = organizations.maxOfOrNull { it.attributes.total.toDoubleOrNull() ?: 0.0 } ?: 1.0
        val minIconSize = (mutableBitmap.width * 0.1).toInt() // Example: 10% of image width
        val maxIconSize = (mutableBitmap.width * 0.25).toInt() // Example: 25% of image width

        val occupiedAreas = mutableListOf(faceBounds)

        organizations.forEach { organization ->
            // Get the pre-downloaded logo from the map
            val iconBitmap = logos[organization.attributes.orgName] ?: return@forEach

            val donation = organization.attributes.total.toDoubleOrNull() ?: 0.0
            val scale = (donation / maxDonation)
            val size = (minIconSize + (maxIconSize - minIconSize) * sqrt(scale)).toInt().coerceAtLeast(1)

            val scaledIcon = Bitmap.createScaledBitmap(iconBitmap, size, size, true)

            // Find a position for the icon
            val position = findNextAvailablePosition(mask, size, occupiedAreas)
            position?.let {
                canvas.drawBitmap(scaledIcon, it.x.toFloat(), it.y.toFloat(), null)
                occupiedAreas.add(Rect(it.x, it.y, it.x + size, it.y + size))
            }
        }

        return mutableBitmap
    }

    /**
     * Finds an available position to place an icon, avoiding occupied areas.
     * (This is a simplified placement algorithm).
     */
    private fun findNextAvailablePosition(
        mask: SegmentationMask,
        iconSize: Int,
        occupiedRects: List<Rect>
    ): android.graphics.Point? {
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height

        // Search in a grid pattern, starting from the center and moving outwards
        val step = iconSize / 4 // A smaller step for better placement
        for (y in (maskHeight / 2) until (maskHeight - iconSize) step step) {
            for (x in (maskWidth / 4) until (maskWidth - (maskWidth / 4) - iconSize) step step) {

                // Check if the center of the proposed rect is on the person
                val maskConfidence = maskBuffer.getFloat((y + iconSize / 2) * maskWidth + (x + iconSize / 2))
                if (maskConfidence < 0.9) continue // Higher threshold for being confidently on the person

                val newRect = Rect(x, y, x + iconSize, y + iconSize)
                if (occupiedRects.none { it.intersect(newRect) }) {
                    return android.graphics.Point(x, y)
                }
            }
        }
        return null // No position found
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
}
