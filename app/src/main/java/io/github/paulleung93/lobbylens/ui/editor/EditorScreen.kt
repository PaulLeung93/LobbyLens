package io.github.paulleung93.lobbylens.ui.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.paulleung93.lobbylens.util.ImageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun EditorScreen(
    navController: NavController,
    imageUri: String?,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val candidates by remember { viewModel.candidates }
    val isLoading by remember { viewModel.isLoading }
    val topOrganizations by remember { viewModel.topOrganizations }
    val generatedImage by remember { viewModel.generatedImage }
    val selectedCycle by remember { viewModel.selectedCycle }

    // State for the image processing pipeline
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processingState by remember { mutableStateOf("Idle") }
    var recognizedCid by remember { mutableStateOf<String?>(null) }

    // Display generated image if available, otherwise original
    val displayBitmap = generatedImage ?: originalBitmap

    // Sync processing state with ViewModel
    LaunchedEffect(isLoading, viewModel.errorMessage.value) {
        if (isLoading) {
             processingState = if (generatedImage == null && candidates.isEmpty()) "Identifying..." else "Generating Visualization..."
        } else if (viewModel.errorMessage.value != null) {
             processingState = viewModel.errorMessage.value!!
        } else if (generatedImage != null) {
             processingState = "Done!"
        }
    }

        if (imageUri != null) {
        // Effect 1: Load image and Identify
        LaunchedEffect(imageUri) {
             processingState = "Loading image..."
             val decodedUri = URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString())
             val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(decodedUri)))
             } else {
                 MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(decodedUri))
             }.copy(Bitmap.Config.ARGB_8888, true)
             originalBitmap = bitmap
             
             // Trigger Identification
             viewModel.identifyPolitician(bitmap)
        }

        // Effect 2: Generate Image when organizations are found
        LaunchedEffect(topOrganizations) {
            if (topOrganizations.isNotEmpty() && originalBitmap != null && generatedImage == null) {
                 viewModel.generateImage(originalBitmap!!)
            }
        }

        // The UI for the image processing flow.
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = processingState)

            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap!!.asImageBitmap(),
                    contentDescription = "Processed Image",
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.weight(1f))
            }

            if (recognizedCid != null) {
                CycleSelector(
                    selectedCycle = selectedCycle,
                    onCycleSelected = { newCycle ->
                        viewModel.fetchTopOrganizations(recognizedCid!!, newCycle)
                    }
                )
            }

            if (recognizedCid != null && processingState == "Done!") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        displayBitmap?.let { bmp ->
                            ImageUtils.saveImageToGallery(context, bmp, "LobbyLens_Image")
                        }
                    }) { Text("Save") }

                    Button(onClick = {
                        displayBitmap?.let { bmp ->
                            val authority = "${context.packageName}.provider"
                            ImageUtils.shareImage(context, bmp, authority)
                        }
                    }) { Text("Share") }

                    Button(onClick = { navController.navigate("details/$recognizedCid") }) {
                        Text("View Details")
                    }
                }
            }
        }

    } else {
        // Manual search mode (existing implementation)
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(value = text, onValueChange = { text = it }, label = { Text("Enter Politician Name") })
            Button(onClick = { viewModel.searchCandidatesByName(text) }) { Text("Search") }
            if (isLoading) { CircularProgressIndicator() }
            LazyColumn {
                items(candidates) { candidate ->
                    Text(
                        text = candidate.name,
                        // CORRECTED: Use the correct 'candidateId' property
                        modifier = Modifier.clickable { navController.navigate("details/${candidate.candidateId}") }.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CycleSelector(selectedCycle: String, onCycleSelected: (String) -> Unit) {
    val cycles = listOf("2024", "2022", "2020", "2018")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        cycles.forEach { cycle ->
            val isSelected = cycle == selectedCycle
            Button(
                onClick = { onCycleSelected(cycle) },
                colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(cycle)
            }
        }
    }
}
