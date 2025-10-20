package io.github.paulleung93.lobbylens.ui.editor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Industry
import io.github.paulleung93.lobbylens.data.model.Legislator
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Editor screen.
 * It is responsible for fetching legislator and industry data, and managing the UI state.
 */
class EditorViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val legislators = mutableStateOf<List<Legislator>>(emptyList())
    val topIndustries = mutableStateOf<List<Industry>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Searches for legislators by name.
     * @param name The name of the politician to search for.
     */
    fun searchLegislators(name: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val response = repository.getLegislators(name)
                legislators.value = response.response.legislator
            } catch (e: Exception) {
                errorMessage.value = "Failed to fetch data: ${e.message}"
            }
            isLoading.value = false
        }
    }

    /**
     * Fetches the top contributing industries for a given politician and cycle.
     * @param cid The politician's unique campaign ID.
     * @param cycle The election cycle to query (e.g., "2022").
     */
    fun fetchTopIndustries(cid: String, cycle: String = "2022") { // Hardcoded cycle for now
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val response = repository.getTopIndustries(cid, cycle)
                topIndustries.value = response.response.industries.industryList
            } catch (e: Exception) {
                errorMessage.value = "Failed to fetch industry data: ${e.message}"
            }
            isLoading.value = false
        }
    }
}
