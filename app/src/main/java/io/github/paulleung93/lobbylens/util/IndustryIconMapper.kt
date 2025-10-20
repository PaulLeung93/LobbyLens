package io.github.paulleung93.lobbylens.util

import androidx.annotation.DrawableRes
import io.github.paulleung93.lobbylens.R

/**
 * A utility object to map OpenSecrets industry codes to drawable resources for icons.
 */
object IndustryIconMapper {

    // A default icon to use when no specific industry icon is found.
    @DrawableRes
    private val defaultIcon = R.drawable.ic_launcher_foreground // TODO: Replace with a real default icon

    /**
     * Returns a drawable resource ID for a given OpenSecrets industry code.
     *
     * You should add your own icons to the `res/drawable` folder and map them here.
     * The industry codes can be found on the OpenSecrets website.
     *
     * @param industryCode The 2-character industry code from the OpenSecrets API.
     * @return A drawable resource ID for the corresponding industry icon.
     */
    @DrawableRes
    fun getIconForIndustry(industryCode: String): Int {
        return when (industryCode.first()) {
            'A' -> R.drawable.ic_launcher_background // Placeholder for Agribusiness
            'B' -> R.drawable.ic_launcher_background // Placeholder for Construction
            'C' -> R.drawable.ic_launcher_background // Placeholder for Communications/Electronics
            'D' -> R.drawable.ic_launcher_background // Placeholder for Defense
            'E' -> R.drawable.ic_launcher_background // Placeholder for Energy/Natural Resources
            'F' -> R.drawable.ic_launcher_background // Placeholder for Finance/Insurance/Real Estate
            'H' -> R.drawable.ic_launcher_background // Placeholder for Health
            'K' -> R.drawable.ic_launcher_background // Placeholder for Lawyers & Lobbyists
            'M' -> R.drawable.ic_launcher_background // Placeholder for Transportation
            'N' -> R.drawable.ic_launcher_background // Placeholder for Misc. Business
            'Q' -> R.drawable.ic_launcher_background // Placeholder for Ideological/Single-Issue
            'W' -> R.drawable.ic_launcher_background // Placeholder for Labor
            else -> defaultIcon
        }
    }
}
