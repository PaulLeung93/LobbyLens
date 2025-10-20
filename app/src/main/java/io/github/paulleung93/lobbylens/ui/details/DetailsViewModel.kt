package io.github.paulleung93.lobbylens.ui.details

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Industry
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 * It is responsible for fetching top industry data for a specific politician.
 */
class DetailsViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val industries = mutableStateOf<List<Industry>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Fetches the top contributing industries for a given politician's campaign ID (cid).
     * @param cid The campaign ID of the politician.
     */
    fun fetchTopIndustries(cid: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val response = repository.getTopIndustries(cid)
                industries.value = response.response.industries.industry
            } catch (e: Exception) {
                errorMessage.value = "Failed to fetch data: ${e.message}"
            }
            isLoading.value = false
        }
    }
}