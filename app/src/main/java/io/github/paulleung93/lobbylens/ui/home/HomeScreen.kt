package io.github.paulleung93.lobbylens.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {

    // Launcher for selecting an image from the gallery.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // URL-encode the image URI to safely pass it as a navigation argument.
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

        // Button to launch the gallery for image selection.
        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text("Select from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // A placeholder for the camera functionality.
        Button(onClick = { /* TODO: Implement camera functionality */ }, enabled = false) {
            Text("Take Photo")
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Button to navigate to the manual search screen.
        Button(onClick = { navController.navigate("editor") }) {
            Text("Manual Search")
        }
    }
}
