package io.github.paulleung93.lobbylens.ui.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.paulleung93.lobbylens.util.ImageUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavController,
    imageUri: String?,
    viewModel: EditorViewModel = viewModel()
) {
    Log.d("EditorScreen", "EditorScreen: Composing with imageUri=$imageUri")
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

    // Background Gradient (Presidential Theme)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        if (imageUri != null) {
            // --- IMAGE PROCESSING MODE ---
            
            // Effect 1: Load image and Identify
            LaunchedEffect(imageUri) {
                 Log.d("EditorScreen", "LaunchedEffect: Loading and identifying image from URI")
                 processingState = "Loading image..."
                 val decodedUri = URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString())
                 val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                     ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(decodedUri)))
                 } else {
                     MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(decodedUri))
                 }.copy(Bitmap.Config.ARGB_8888, true)
                 originalBitmap = bitmap
                 Log.d("EditorScreen", "LaunchedEffect: Image loaded, size: ${bitmap.width}x${bitmap.height}")
                 
                 // Trigger Identification
                 Log.d("EditorScreen", "LaunchedEffect: Triggering politician identification")
                 viewModel.identifyPolitician(bitmap)
            }

            // Effect 2: Generate Image when organizations are found
            LaunchedEffect(topOrganizations) {
                if (topOrganizations.isNotEmpty() && originalBitmap != null && generatedImage == null) {
                     Log.d("EditorScreen", "LaunchedEffect: Organizations found (${topOrganizations.size}), triggering image generation")
                     viewModel.generateImage(originalBitmap!!)
                }
            }

            // The UI for the image processing flow.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "ANALYZING CANDIDATE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                // Processing Status
                Text(
                    text = processingState,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Processed Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (recognizedCid != null) {
                    CycleSelector(
                        selectedCycle = selectedCycle,
                        onCycleSelected = { newCycle ->
                            Log.d("EditorScreen", "CycleSelector: User selected cycle $newCycle")
                            viewModel.fetchTopOrganizations(recognizedCid!!, newCycle)
                        }
                    )
                }

                if (recognizedCid != null && processingState == "Done!") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save Button
                        Button(
                            onClick = {
                                Log.d("EditorScreen", "Button: Save image clicked")
                                displayBitmap?.let { bmp ->
                                    ImageUtils.saveImageToGallery(context, bmp, "LobbyLens_Image")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                             Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                             Spacer(Modifier.width(8.dp))
                             Text("Save", color = MaterialTheme.colorScheme.secondary)
                        }

                        // Share Button
                        Button(
                            onClick = {
                                Log.d("EditorScreen", "Button: Share image clicked")
                                displayBitmap?.let { bmp ->
                                    val authority = "${context.packageName}.provider"
                                    ImageUtils.shareImage(context, bmp, authority)
                                }
                            },
                             modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Share", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    
                    // View Details Button (Primary)
                    Button(
                        onClick = {
                            Log.d("EditorScreen", "Button: View details clicked for cid=$recognizedCid")
                            navController.navigate("details/$recognizedCid")
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("VIEW FULL RECORD", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

        } else {
            // --- MANUAL SEARCH MODE ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "SEARCH ARCHIVES",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Find financial records by name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Search Input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Candidate Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.searchCandidatesByName(text) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.searchCandidatesByName(text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("SEARCH", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }

                // Results List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(candidates) { candidate ->
                        Card(
                            onClick = { navController.navigate("details/${candidate.candidateId}") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ID: ${candidate.candidateId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Back Button (Floating)
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun CycleSelector(selectedCycle: String, onCycleSelected: (String) -> Unit) {
    val cycles = listOf("2024", "2022", "2020", "2018")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "SELECT ELECTION CYCLE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            cycles.forEach { cycle ->
                val isSelected = cycle == selectedCycle
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f)
                
                Button(
                    onClick = { onCycleSelected(cycle) },
                    colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp),
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f)) else null
                ) {
                    Text(cycle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
