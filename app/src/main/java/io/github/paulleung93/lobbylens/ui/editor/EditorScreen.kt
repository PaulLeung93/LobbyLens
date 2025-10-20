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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.segmentation.SegmentationMask
import io.github.paulleung93.lobbylens.domain.ai.FaceRecognizer
import io.github.paulleung93.lobbylens.util.ImageUtils
import io.github.paulleung93.lobbylens.util.MlKitUtils
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
    val organizationLogos by remember { viewModel.organizationLogos }
    val selectedCycle by remember { viewModel.selectedCycle }

    // State for the image processing pipeline
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var composedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selfieMask by remember { mutableStateOf<SegmentationMask?>(null) }
    var detectedFace by remember { mutableStateOf<Face?>(null) }
    var processingState by remember { mutableStateOf("Idle") }
    var recognizedCid by remember { mutableStateOf<String?>(null) }

    val displayBitmap = composedBitmap ?: originalBitmap

    if (imageUri != null) {
        // Effect 1: One-time recognition pipeline.
        LaunchedEffect(imageUri) {
            try {
                processingState = "Loading image..."
                val decodedUri = URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString())
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(decodedUri)))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(decodedUri))
                }.copy(Bitmap.Config.ARGB_8888, true)
                originalBitmap = bitmap

                processingState = "Analyzing image..."
                val (faces, mask) = coroutineScope {
                    val faceDetectionJob = async { MlKitUtils.detectFaces(bitmap) }
                    val segmentationJob = async { MlKitUtils.segmentSelfie(bitmap) }
                    faceDetectionJob.await() to segmentationJob.await()
                }
                selfieMask = mask

                if (faces.isEmpty() || mask == null) {
                    processingState = "Could not detect a person or face. Please try another photo."
                    return@LaunchedEffect
                }
                val face = faces.first()
                detectedFace = face

                processingState = "Recognizing politician..."
                val croppedFace = Bitmap.createBitmap(bitmap, face.boundingBox.left, face.boundingBox.top, face.boundingBox.width(), face.boundingBox.height())
                val faceRecognizer = FaceRecognizer(context)
                val recognitionResult = faceRecognizer.recognize(croppedFace)

                if (recognitionResult == null) {
                    processingState = "Recognition failed. Please try another photo or search manually."
                    return@LaunchedEffect
                }

                val (politicianCid, _) = recognitionResult
                recognizedCid = politicianCid
                viewModel.fetchTopOrganizations(politicianCid, selectedCycle)

            } catch (e: Exception) {
                e.printStackTrace()
                processingState = "An error occurred: ${e.message}"
            }
        }

        // Effect 2: Image composition pipeline.
        LaunchedEffect(organizationLogos) {
            if (organizationLogos.isNotEmpty() && topOrganizations.isNotEmpty() && originalBitmap != null && selfieMask != null && detectedFace != null) {
                processingState = "Composing final image..."
                composedBitmap = ImageUtils.composeImage(
                    baseBitmap = originalBitmap!!,
                    mask = selfieMask!!,
                    organizations = topOrganizations,
                    logos = organizationLogos,
                    faceBounds = detectedFace!!.boundingBox
                )
                processingState = "Done!"
            } else if (recognizedCid != null) {
                composedBitmap = null
                if (isLoading) {
                    processingState = "Fetching data for $selectedCycle..."
                }
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
