package io.github.paulleung93.lobbylens.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.mlkit.vision.segmentation.SegmentationMask
import io.github.paulleung93.lobbylens.data.model.Industry
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
     * Creates a composite image by overlaying industry icons onto a base image.
     *
     * @param context The context for resolving drawable resources.
     * @param baseBitmap The original image of the politician.
     * @param mask The selfie segmentation mask to identify the person.
     * @param industries A list of top contributing industries.
     * @param faceBounds The bounding box of the detected face, to avoid drawing over it.
     * @return A new bitmap with the data overlays.
     */
    fun composeImage(
        context: Context,
        baseBitmap: Bitmap,
        mask: SegmentationMask,
        industries: List<Industry>,
        faceBounds: Rect
    ): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val maxDonation = industries.maxOfOrNull { it.attributes.total.toDoubleOrNull() ?: 0.0 } ?: 1.0
        val minIconSize = (mutableBitmap.width * 0.1).toInt() // Example: 10% of image width
        val maxIconSize = (mutableBitmap.width * 0.25).toInt() // Example: 25% of image width

        val occupiedAreas = mutableListOf(faceBounds)

        industries.forEach { industry ->
            val iconResId = IndustryIconMapper.getIconForIndustry(industry.attributes.industryCode)
            val iconBitmap = BitmapFactory.decodeResource(context.resources, iconResId)

            val donation = industry.attributes.total.toDoubleOrNull() ?: 0.0
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
        val step = iconSize / 2
        for (y in (maskHeight / 2) until (maskHeight - iconSize) step step) {
            for (x in (maskWidth / 3) until (maskWidth - iconSize) step step) {

                // Check if the center of the proposed rect is on the person
                val maskConfidence = maskBuffer.getFloat((y + iconSize / 2) * maskWidth + (x + iconSize / 2))
                if (maskConfidence < 0.8) continue // Threshold for being on the person

                val newRect = Rect(x, y, x + iconSize, y + iconSize)
                if (occupiedRects.none { it.intersect(newRect) }) {
                    return android.graphics.Point(x, y)
                }
            }
        }
        return null // No position found
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        // ... (existing save code) ...
    }

    fun shareImage(context: Context, bitmap: Bitmap, authority: String) {
        // ... (existing share code) ...
    }
}
