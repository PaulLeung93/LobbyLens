package io.github.paulleung93.lobbylens.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for camera permission request.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            hasCameraPermission = isGranted
        }
    )

    // Launcher for taking a picture.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success: Boolean ->
            if (success) {
                tempImageUri?.let { uri ->
                    val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                    navController.navigate("editor?imageUri=$encodedUri")
                }
            }
        }
    )

    // Launcher for selecting an image from the gallery.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val encodedUri = URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString())
                navController.navigate("editor?imageUri=$encodedUri")
            }
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "LobbyLens")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text("Select from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (hasCameraPermission) {
                // Create a temporary file to store the captured image.
                val uri = createImageUri(context)
                tempImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                // Request camera permission.
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }) {
            Text("Take Photo")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.navigate("editor") }) {
            Text("Manual Search")
        }
    }
}

/**
 * Creates a content URI for a temporary image file.
 * This uses a FileProvider to ensure secure file sharing.
 */
private fun createImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        context.externalCacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // Authority must match AndroidManifest.xml
        imageFile
    )
}
