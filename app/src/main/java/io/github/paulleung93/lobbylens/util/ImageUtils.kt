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
    fun composeImage(
        baseBitmap: Bitmap,
        mask: SegmentationMask,
        organizations: List<FecEmployerContribution>, // CORRECTED: Use the new FEC data model
        logos: Map<String, Bitmap>,
        faceBounds: Rect
    ): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // CORRECTED: Use the 'total' property from the new data model
        val maxDonation = organizations.maxOfOrNull { it.total } ?: 1.0
        val minIconSize = (mutableBitmap.width * 0.1).toInt()
        val maxIconSize = (mutableBitmap.width * 0.25).toInt()

        val occupiedAreas = mutableListOf(faceBounds)

        organizations.forEach { organization ->
            // CORRECTED: Use the 'employer' property to get the logo
            val iconBitmap = logos[organization.employer] ?: return@forEach

            // CORRECTED: Use the 'total' property for donation amount
            val donation = organization.total
            val scale = (donation / maxDonation)
            val size = (minIconSize + (maxIconSize - minIconSize) * sqrt(scale)).toInt().coerceAtLeast(1)

            val scaledIcon = Bitmap.createScaledBitmap(iconBitmap, size, size, true)

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
     */
    private fun findNextAvailablePosition(
        mask: SegmentationMask,
        iconSize: Int,
        occupiedRects: List<Rect>
    ): android.graphics.Point? {
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height

        val step = iconSize / 4
        for (y in (maskHeight / 2) until (maskHeight - iconSize) step step) {
            for (x in (maskWidth / 4) until (maskWidth - (maskWidth / 4) - iconSize) step step) {

                val maskConfidence = maskBuffer.getFloat((y + iconSize / 2) * maskWidth + (x + iconSize / 2))
                if (maskConfidence < 0.9) continue

                val newRect = Rect(x, y, x + iconSize, y + iconSize)
                if (occupiedRects.none { it.intersect(newRect) }) {
                    return android.graphics.Point(x, y)
                }
            }
        }
        return null
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
