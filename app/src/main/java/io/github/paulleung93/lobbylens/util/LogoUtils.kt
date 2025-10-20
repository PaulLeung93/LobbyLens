package io.github.paulleung93.lobbylens.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

object LogoUtils {

    /**
     * Fetches a company logo from a URL.
     *
     * @param context The application context.
     * @param companyName The name of the company.
     * @return The company logo as a Bitmap, or null if it could not be fetched.
     */
    suspend fun fetchLogo(context: Context, companyName: String): Bitmap? {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data("https://logo.clearbit.com/${companyName.replace(" ", "").toLowerCase()}.com")
            .allowHardware(false) // Disable hardware bitmaps for easier processing
            .build()

        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> (result.drawable as BitmapDrawable).bitmap
            else -> null
        }
    }
}
