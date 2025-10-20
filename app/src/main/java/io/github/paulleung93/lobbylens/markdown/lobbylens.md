Project Roadmap: LobbyLens
Vision: To create a data-driven Android application that provides transparency in politics by visualizing campaign finance data directly onto images of politicians, using on-device AI for recognition and processing.

Core Features
1.	True Facial Recognition: Utilizes an on-device TensorFlow Lite model (FaceNet) to identify politicians directly from their faces, without relying on text in the image.
2.	Manual Politician Search: A robust fallback option allowing users to search for any US politician by name, ensuring full accessibility.
3.	Company Logo Visualization: Instead of generic icons, the app fetches and visualizes the logos of the top contributing organizations. The size of the logo corresponds to the total donation amount from that company.
4.	Historical Campaign Data: Users can select different election cycles to see how a politician's funding sources have changed over time.
5.	Detailed Organization Breakdown: A dedicated details screen provides a comprehensive view of the top contributing organizations, their total donation amounts, and a historical bar chart showing funding trends across multiple cycles.
6.	Save & Share: Users can save the final generated image to their device or share it directly to social media and messaging apps.

UI/UX Design
The app will have a clean, modern, and data-focused interface with three main screens:
1.	Home Screen:
○	App title and tagline.
○	Primary user actions: "Take Photo" and "Select from Gallery."
○	A prominent search bar for the manual politician lookup feature.
2.	Editor Screen:
○	Displays the user's selected image.
○	Overlays the processing status (e.g., "Recognizing Face," "Fetching Data...").
○	Presents a confirmation dialog after facial recognition is successful.
○	Renders the final image with company logos placed on the politician's clothing.
○	Features a selector to switch between election cycles.
○	Provides "Save," "Share," and "Details" actions.
3.	Details Screen:
○	Displays the politician's name.
○	Features a Bar Chart visualizing total donations across multiple election cycles.
○	Shows a detailed list of the top contributing organizations for the selected cycle, with their total donation amounts.

Technical Stack
●	Language: Kotlin
●	UI Framework: Jetpack Compose
●	Architecture: MVVM (Model-View-ViewModel)
●	On-Device AI:
○	ML Kit: For initial face detection and selfie segmentation.
○	TensorFlow Lite: For running the FaceNet model for true facial recognition.
●	Networking: Retrofit2 for handling API calls.
●	Image Loading: Coil for fetching, caching, and displaying company logos.
●	API: OpenSecrets.org for all campaign finance data.
●	Logo API: A third-party logo-finding service is used to automatically find company logos.

Final Project Architecture (MVVM)
This structure separates concerns, making the app scalable, testable, and maintainable.
●	MainActivity.kt: The single activity entry point; its only job is to host the Jetpack Compose navigation.
●	data/: The data layer, responsible for all data sources.
○	model/Models.kt: Contains all Kotlin data classes (e.g., Organization, Legislator) used for parsing API responses.
○	network/OpenSecretsApiService.kt: The Retrofit interface defining the API endpoints.
○	network/RetrofitInstance.kt: The singleton object that provides a configured Retrofit instance.
○	repository/PoliticianRepository.kt: The single source of truth for data; it abstracts away the network calls from the ViewModels.
●	domain/: The domain layer for core business logic.
○	ai/FaceRecognizer.kt: A specialized class that encapsulates all TensorFlow Lite logic for loading the model and performing facial recognition.
●	ui/: The UI layer, containing all Composables and state holders.
○	navigation/AppNavigation.kt: Defines the NavHost and all navigation routes for the app.
○	home/HomeScreen.kt: The Composable for the main landing screen.
○	editor/EditorScreen.kt: The Composable for the main editor/visualization screen.
○	editor/EditorViewModel.kt: The ViewModel for EditorScreen; it holds the UI state and orchestrates all AI and data operations for that screen.
	details/DetailsScreen.kt: The Composable for the screen showing charts and organization breakdowns.
○	components/: A directory for small, reusable Composables used across multiple screens (e.g., BarChart.kt).
○	theme/: Auto-generated files for app styling and colors.
●	util/: A package for utility and helper functions.
○	ImageUtils.kt: Contains helper functions for bitmap manipulation, saving, and sharing.
○	MlKitUtils.kt: Helper functions for ML Kit operations like face detection and segmentation.
○	LogoUtils.kt: A helper utility for fetching company logos.
○	Result.kt: A generic wrapper for handling operations that can succeed or fail.

### Project Setup & Prerequisites

Before the application can be fully functional, a few manual setup steps are required. The codebase is complete, but it relies on external assets and keys that you must provide.

**1. Obtain an OpenSecrets API Key:**
*   **Action:** Register for a free API key at [OpenSecrets.org](https://www.opensecrets.org/api/admin/user_register.php).
*   **Integration:** Open the file `app/src/main/java/io/github/paulleung93/lobbylens/data/network/OpenSecretsApiService.kt` and replace the placeholder `"YOUR_API_KEY_HERE"` with your actual key.

**2. Download the TensorFlow Lite Model:**
*   **Action:** Download a pre-trained `FaceNet.tflite` model. You can find compatible versions by searching online for "FaceNet tflite model download". You do **not** need to train your own model.
*   **Integration:** Place the downloaded `FaceNet.tflite` file directly into the `app/src/main/assets/` directory in your project. Create this folder if it doesn't exist.

**3. Generate the Embeddings Database:**
*   **Action:** This is the most crucial step for facial recognition. You need to create the `embeddings.json` file using the provided offline Python script (`generate_embeddings.py`). This script processes a folder of politician images (ideally named by their OpenSecrets CID, e.g., `N00007360.jpg`) and uses the FaceNet model to create a unique facial "fingerprint" for each one.
*   **Integration:** Once generated, place the `embeddings.json` file into the same `app/src/main/assets/` directory.

Development Plan
This plan reflects the logical progression from a simple idea to a fully architected application.
Phase 1: Core Functionality & Data Pipeline
●	Objective: Establish the core MVVM architecture, data layer, and navigation.
●	Steps:
1.	Set up the base project with a professional MVVM structure from the start.
2.	Implement the Retrofit data layer to fetch politician and industry data from the OpenSecrets API.
3.	Create ViewModels to manage UI state and interact with the data repository.
4. Set up the basic navigation graph for the Home, Editor, and Details screens.

Phase 2: On-Device AI & Feature Expansion
●	Objective: Make the app intelligent and add key user-facing features.
●	Steps:
1.	Create the embeddings.json database offline.
2.	Integrate ML Kit for face detection and selfie segmentation.
3.	Integrate the TensorFlow Lite FaceRecognizer to identify politicians from an image.
4.  Implement the full AI pipeline on the EditorScreen, from image selection to recognition.
5.	Implement the image composition logic to overlay company logos on the processed image.
6.	Implement the "Save" and "Share" features.

Phase 3: Polish and Advanced Features
●	Objective: Add advanced features, polish the user experience, and make the app production-ready.
●	Steps:
1.	**DONE: Implement Full Camera Integration:** Enable the "Take Photo" button to launch the device's camera for live image capture and analysis.
2.	**DONE: Add Historical Data & Dynamic Cycle Selection:** Implement a selector on the `EditorScreen` to allow users to select different election cycles and see how funding changes over time.
3.	**DONE: Build the Advanced Details Screen:** Enhance the `DetailsScreen` to display a bar chart visualizing total donations across multiple election cycles.
4.  **DONE: Implement Robust Error Handling:** Add user-friendly error messages for common issues (e.g., no internet, no face detected, no match found) using a `Result` wrapper.
5.  **DONE: Add a Caching Layer:** Optimize the `PoliticianRepository` to cache API results, improving performance and reducing API usage.

Phase 4: Production Readiness
●	Objective: Ensure the app is stable, polished, and ready for a public release.
●	Steps:
1.	**Write Unit Tests:** Create unit tests for the ViewModels (`EditorViewModel`, `DetailsViewModel`) to verify business logic and prevent regressions.
2.	**Write Instrumentation Tests:** Create basic UI tests to ensure key user flows are working correctly.
3.	**Final UI/UX Polish:** Conduct a full review of the application to identify and fix any inconsistencies in design, layout, or user experience.
4.	**Prepare for Publishing:** Create the necessary release assets, including high-resolution app icons, feature graphics, and store screenshots. Write the app's official store listing description.
