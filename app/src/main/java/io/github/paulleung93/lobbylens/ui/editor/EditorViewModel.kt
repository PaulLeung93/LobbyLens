package io.github.paulleung93.lobbylens.ui.editor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.paulleung93.lobbylens.data.model.Legislator
import io.github.paulleung93.lobbylens.data.repository.PoliticianRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Editor screen.
 * It is responsible for fetching legislator data and managing the UI state.
 */
class EditorViewModel : ViewModel() {

    private val repository = PoliticianRepository()

    val legislators = mutableStateOf<List<Legislator>>(emptyList())
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
}