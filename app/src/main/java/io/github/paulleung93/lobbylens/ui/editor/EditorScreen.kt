package io.github.paulleung93.lobbylens.ui.editor

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.paulleung93.lobbylens.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavController,
    imageUri: String?,
    viewModel: EditorViewModel = hiltViewModel()
) {
    Log.d("EditorScreen", "EditorScreen: Composing with imageUri=$imageUri")
    val context = LocalContext.current
    
    // Collect state with lifecycle awareness
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Background Gradient (Presidential Theme)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    // Trigger image processing when imageUri is present
    LaunchedEffect(imageUri) {
        if (imageUri != null && uiState is EditorUiState.Initial) {
            viewModel.processImage(imageUri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        if (imageUri != null) {
            // --- IMAGE PROCESSING MODE ---
            ImageProcessingContent(
                uiState = uiState,
                navController = navController,
                context = context
            )
        } else {
            // --- MANUAL SEARCH MODE ---
            SearchContent(
                uiState = uiState,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.searchCandidatesByName(searchQuery) },
                onBrowseMembers = { viewModel.loadCongressMembers() },
                navController = navController
            )
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
private fun ImageProcessingContent(
    uiState: EditorUiState,
    navController: NavController,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiState) {
            is EditorUiState.Initial,
            is EditorUiState.LoadingImage -> {
                HeaderSection(title = "LOADING IMAGE", subtitle = null)
                StatusText("Loading image...")
                LoadingImagePlaceholder()
            }
            
            is EditorUiState.Identifying -> {
                HeaderSection(title = "ANALYZING CANDIDATE", subtitle = null)
                StatusText("Identifying...")
                LoadingImagePlaceholder()
            }
            
            is EditorUiState.GeneratingVisualization -> {
                HeaderSection(title = "ANALYZING CANDIDATE", subtitle = null)
                StatusText("Generating Visualization...")
                LoadingImagePlaceholder()
            }
            
            is EditorUiState.ImageProcessingSuccess -> {
                // Determine which URI to display
                val displayUri = uiState.generatedImageUri ?: uiState.originalImageUri
                
                // Load bitmap from URI
                val displayBitmap = rememberBitmapFromUri(displayUri)
                
                HeaderSection(
                    title = uiState.candidate.name.uppercase(),
                    subtitle = "${uiState.candidate.party ?: "Unspecified"} • ${uiState.candidate.state ?: "Unknown State"}"
                )
                StatusText("Done!")
                
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
                
                if (displayBitmap != null) {
                    ActionButtons(
                        displayBitmap = displayBitmap,
                        candidateId = uiState.candidate.candidateId,
                        context = context,
                        navController = navController
                    )
                }
            }
            
            is EditorUiState.Error -> {
                HeaderSection(title = "ERROR", subtitle = null)
                StatusText(uiState.message)
                LoadingImagePlaceholder()
            }
            
            else -> {
                // Handle SearchResults state in image mode (shouldn't happen normally)
                LoadingImagePlaceholder()
            }
        }
    }
}

@Composable
private fun SearchContent(
    uiState: EditorUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBrowseMembers: () -> Unit,
    navController: NavController
) {
    val isLoading = uiState is EditorUiState.SearchResults && uiState.isLoading
    val candidates = when (uiState) {
        is EditorUiState.SearchResults -> uiState.candidates
        else -> emptyList()
    }
    val errorMessage = if (uiState is EditorUiState.Error) uiState.message else null

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
            value = searchQuery,
            onValueChange = onSearchQueryChange,
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
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Browse Button
        Button(
            onClick = onBrowseMembers,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("BROWSE CURRENT MEMBERS", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearch,
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
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                                text = "${candidate.party ?: "N/A"} • ${candidate.state ?: "N/A"} • ID: ${candidate.candidateId}",
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

@Composable
private fun HeaderSection(title: String, subtitle: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun ColumnScope.LoadingImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun ActionButtons(
    displayBitmap: android.graphics.Bitmap,
    candidateId: String,
    context: android.content.Context,
    navController: NavController
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Save Button
        Button(
            onClick = {
                ImageUtils.saveImageToGallery(context, displayBitmap, "LobbyLens_Image")
                android.widget.Toast.makeText(context, "Image Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
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
                val authority = "${context.packageName}.provider"
                ImageUtils.shareImage(context, displayBitmap, authority)
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
        onClick = { navController.navigate("details/$candidateId") },
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

/**
 * Composable helper to load a Bitmap from a URI string asynchronously.
 */
@Composable
private fun rememberBitmapFromUri(uri: String?): android.graphics.Bitmap? {
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(uri) {
        if (uri != null) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(context.contentResolver, android.net.Uri.parse(uri))
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, android.net.Uri.parse(uri))
                    }.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                } catch (e: Exception) {
                    Log.e("EditorScreen", "Failed to load bitmap from URI: $uri", e)
                    null
                }
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}
