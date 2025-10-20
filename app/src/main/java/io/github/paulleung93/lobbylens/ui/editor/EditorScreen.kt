package io.github.paulleung93.lobbylens.ui.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import io.github.paulleung93.lobbylens.domain.ai.FaceRecognizer
import io.github.paulleung93.lobbylens.util.ImageUtils
import io.github.paulleung93.lobbylens.util.MlKitUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    val legislators by remember { viewModel.legislators }
    val isLoading by remember { viewModel.isLoading }

    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processingState by remember { mutableStateOf("Idle") }
    var recognizedCid by remember { mutableStateOf<String?>(null) }

    if (imageUri != null) {
        // The main processing pipeline, running in a LaunchedEffect.
        LaunchedEffect(imageUri, viewModel) {
            try {
                processingState = "Loading image..."
                val decodedUri = URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString())
                val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(decodedUri)))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(decodedUri))
                }.copy(Bitmap.Config.ARGB_8888, true)
                displayBitmap = originalBitmap

                processingState = "Analyzing image..."
                val (faces, mask) = coroutineScope {
                    val faceDetectionJob = async { MlKitUtils.detectFaces(originalBitmap) }
                    val segmentationJob = async { MlKitUtils.segmentSelfie(originalBitmap) }
                    faceDetectionJob.await() to segmentationJob.await()
                }

                if (faces.isEmpty() || mask == null) {
                    processingState = "Could not detect a person or face. Please try another photo."
                    return@LaunchedEffect
                }

                processingState = "Recognizing politician..."
                val face = faces.first()
                val croppedFace = Bitmap.createBitmap(originalBitmap, face.boundingBox.left, face.boundingBox.top, face.boundingBox.width(), face.boundingBox.height())
                val faceRecognizer = FaceRecognizer(context)
                val recognitionResult = faceRecognizer.recognize(croppedFace)

                if (recognitionResult == null) {
                    processingState = "Recognition failed. Please try another photo or search manually."
                    return@LaunchedEffect
                }

                val (cid, _) = recognitionResult
                recognizedCid = cid
                processingState = "Fetching financial data..."
                viewModel.fetchTopOrganizations(cid)

                // Wait for the data to be loaded.
                snapshotFlow { viewModel.topOrganizations.value }
                    .filter { it.isNotEmpty() }.first()

                // TODO: Re-enable visualization once ImageUtils is updated for organizations.
                processingState = "Done! Visualization pending update."
                displayBitmap = originalBitmap // Temporarily display original image.

            } catch (e: Exception) {
                e.printStackTrace()
                processingState = "An error occurred: ${e.message}"
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
            
            if (recognizedCid != null && processingState.startsWith("Done!")) {
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
            Button(onClick = { viewModel.searchLegislators(text) }) { Text("Search") }
            if (isLoading) { CircularProgressIndicator() }
            LazyColumn {
                items(legislators) { legislator ->
                    Text(
                        text = legislator.attributes.firstLast,
                        modifier = Modifier.clickable { navController.navigate("details/${legislator.attributes.cid}") }.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
    }
}
