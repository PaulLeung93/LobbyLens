package io.github.paulleung93.lobbylens.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

object SignatureUtils {
    /**
     * Gets the SHA1 signature of the app's signing certificate.
     * This is required for API key restrictions on Android.
     */
    fun getSignature(context: Context): String? {
        try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val md = MessageDigest.getInstance("SHA1")
            val digest = md.digest(cert)

            return bytesToHex(digest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices) {
            sb.append(String.format(Locale.US, "%02X", bytes[i]))
            if (i < bytes.size - 1) {
                sb.append(":")
            }
        }
        return sb.toString()
    }
}
