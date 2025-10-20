Project Roadmap: LobbyLens
Vision: To create a data-driven Android application that provides transparency in politics by visualizing campaign finance data directly onto images of politicians, using on-device AI for recognition and processing.
Core Features
1.	True Facial Recognition: Utilizes an on-device TensorFlow Lite model (FaceNet) to identify politicians directly from their faces, without relying on text in the image.
2.	Manual Politician Search: A robust fallback option allowing users to search for any US politician by name, ensuring full accessibility.
3.	Industry Icon Visualization: Instead of individual company logos, the app visualizes the top contributing industries (e.g., Health, Finance, Defense) using a clean, symbolic icon set. The size of the icon corresponds to the total donation amount from that sector.
4.	Historical Campaign Data: Users can select different election cycles to see how a politician's funding sources have changed over time.
5.	Detailed Industry Breakdown: A dedicated details screen provides a comprehensive view of the top contributing industries, their total donation amounts, and a historical bar chart showing funding trends across multiple cycles.
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
○	Renders the final image with industry icons placed on the politician's clothing.
○	Features a dropdown menu to select the election cycle.
○	Provides "Save," "Share," and "Details" actions.
3.	Details Screen:
○	Displays the politician's name.
○	Features a Bar Chart visualizing total donations across multiple election cycles.
○	Shows a detailed list of the top contributing industries for the selected cycle, with their corresponding icons and total donation amounts.
Technical Stack
●	Language: Kotlin
●	UI Framework: Jetpack Compose
●	Architecture: MVVM (Model-View-ViewModel)
●	On-Device AI:
○	ML Kit: For initial face detection and selfie segmentation.
○	TensorFlow Lite: For running the FaceNet model for true facial recognition.
●	Networking: Retrofit2 for handling API calls.
●	API: OpenSecrets.org for all campaign finance data.
●	Development Workflow: A separate Python script (generate_embeddings.py) is used offline to create the embeddings.json database.
Final Project Architecture (MVVM)
This structure separates concerns, making the app scalable, testable, and maintainable.
●	MainActivity.kt: The single activity entry point; its only job is to host the Jetpack Compose navigation.
●	data/: The data layer, responsible for all data sources.
○	model/Models.kt: Contains all Kotlin data classes (e.g., Industry, Legislator) used for parsing API responses.
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
○	details/DetailsScreen.kt: The Composable for the screen showing charts and industry breakdowns.
○	components/: A directory for small, reusable Composables used across multiple screens (e.g., BarChart.kt, ActionButton.kt).
○	theme/: Auto-generated files for app styling and colors.
●	util/: A package for utility and helper functions.
○	ImageUtils.kt: Contains helper functions for bitmap manipulation, saving, and sharing.
○	MlKitUtils.kt: Helper functions for ML Kit operations like face detection and segmentation.
Development Plan
This plan reflects the logical progression from a simple idea to a fully architected application.
Phase 1: Core Functionality & Data Pipeline (Prototyping)
●	Objective: Build a functional MVP in a single file to quickly test the core concept.
●	Steps:
1.	Set up the base project and implement UI for all screens in MainActivity.kt.
2.	Integrate the OpenSecrets API to fetch data for a hardcoded politician ID.
3.	Implement the first version of the visualization and the details screen.
Phase 2: On-Device AI & Feature Expansion (Still in a single file)
●	Objective: Make the app intelligent and add key user-facing features.
●	Steps:
1.	Create the embeddings.json database offline.
2.	Integrate the TensorFlow Lite FaceRecognizer logic.
3.	Replace the hardcoded ID with the full facial recognition pipeline.
4.	Implement Save/Share, manual search, and cycle selection.
5.	Upgrade visualization logic from logos to industry icons and add the historical data chart.
Phase 3: Professional Refactor (Final Architecture)
●	Objective: Refactor the entire project from a single file into a clean, professional MVVM architecture.
●	Steps:
1.	Create the new directory structure (data, domain, ui, util).
2.	Migrate all data models, networking code, and the repository to the data layer.
3.	Move the FaceRecognizer to the domain layer.
4.	Separate each screen's Composables into its own file in the ui layer.
5.	Create EditorViewModel to manage the state and logic for the EditorScreen.
6.	Simplify MainActivity.kt to only handle navigation setup.
