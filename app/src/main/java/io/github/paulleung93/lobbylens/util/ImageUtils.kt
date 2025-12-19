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
}
