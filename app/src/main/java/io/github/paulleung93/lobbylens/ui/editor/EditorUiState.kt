package io.github.paulleung93.lobbylens.ui.editor

import io.github.paulleung93.lobbylens.data.model.FecCandidate
import io.github.paulleung93.lobbylens.data.model.FecEmployerContribution

/**
 * Sealed interface representing the various UI states of the Editor screen.
 */
sealed interface EditorUiState {
    /**
     * Initial state before any action is taken.
     */
    data object Initial : EditorUiState

    /**
     * State when loading/decoding an image from URI.
     */
    data object LoadingImage : EditorUiState

    /**
     * State when identifying a politician from the image.
     */
    data object Identifying : EditorUiState

    /**
     * State when generating the visualization overlay.
     */
    data object GeneratingVisualization : EditorUiState

    /**
     * Successful image processing result.
     * Uses URI strings instead of Bitmaps to prevent OOM during configuration changes.
     */
    data class ImageProcessingSuccess(
        val candidate: FecCandidate,
        val originalImageUri: String,
        val generatedImageUri: String?,
        val organizations: List<FecEmployerContribution>
    ) : EditorUiState

    /**
     * Search results from manual candidate search.
     */
    data class SearchResults(
        val candidates: List<FecCandidate>,
        val isLoading: Boolean = false
    ) : EditorUiState

    /**
     * Error state with message.
     */
    data class Error(val message: String) : EditorUiState
}
