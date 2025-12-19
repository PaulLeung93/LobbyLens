# Project Roadmap: LobbyLens

**Vision:** To create a data-driven Android application that provides transparency in politics by visualizing campaign finance data directly onto images of politicians, leveraging powerful Cloud AI for recognition and generative visualization.

## Core Features
1.  **Cloud-Powered Recognition:** Utilizes **Google Cloud Vision API** to accurately identify public figures from images, replacing the need for on-device models.
2.  **Generative Visualization:** Uses **Vertex AI (Imagen 3)** to seamlessly generate high-quality images of politicians wearing "donor badges" (company logos) on their clothing.
3.  **Manual Politician Search:** A robust fallback option allowing users to search for any US politician by name.
4.  **Historical Campaign Data:** Users can select different election cycles to see how a politician's funding sources have changed over time.
5.  **Detailed Organization Breakdown:** A dedicated details screen provides a comprehensive view of the top contributing organizations, their total donation amounts, and a historical bar chart showing funding trends.
6.  **Premium Dark UI:** A sleek, modern "Premium Dark" aesthetic with Navy Blue and Gold accents, enhancing readability and visual appeal.
7.  **Save & Share:** Users can save the generative AI masterpieces to their gallery or share them directly to social media.

## UI/UX Design
The app features a cohesive **Premium Dark** theme to convey authority and trust.

1.  **Home Screen:**
    *   Large "Scan Politician" and "Upload Photo" buttons with Gold/Navy styling.
    *   Glassmorphism-style search area.
    *   Modern typography and clean layout.
2.  **Editor Screen:**
    *   Handles the Cloud AI pipeline: Upload -> Identify (Cloud Vision) -> Fetch Data (FEC) -> Generate (Vertex AI).
    *   Displays status messages ("Identifying...", "Generating...") to keep the user informed.
    *   Shows the final generative image.
    *   Allows cycle selection for historical data comparison.
3.  **Details Screen:**
    *   Interactive Bar Charts for historical data.
    *   Card-based list layout for top donors.
    *   Clean financial formatting.

## Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Cloud AI:**
    *   **Google Cloud Vision API:** For politician detection and identification (Web Detection).
    *   **Vertex AI (Imagen 3):** For generative image creation (inpainting/editing).
*   **Networking:** Retrofit2 for all API interactions.
*   **API:**
    *   **FEC (Federal Election Commission) API:** For official campaign finance data.
*   **Image Loading:** Coil.

## Project Architecture (MVVM)
*   `MainActivity.kt`: Hosts the Jetpack Compose navigation.
*   `data/`:
    *   `model/`: Data classes for FEC, Cloud Vision, and Vertex AI responses.
    *   `network/`: Retrofit services (`FecApiService`, `CloudVisionService`, `VertexAiService`) and `RetrofitInstance`.
    *   `repository/PoliticianRepository.kt`: Single source of truth. Orchestrates Cloud Vision identification -> FEC data fetching -> Vertex AI generation.
*   `ui/`:
    *   `home/`: Redesigned landing screen.
    *   `editor/`: Main screen for AI processing and visualization.
    *   `details/`: Data visualization screen.
    *   `theme/`: Contains `Color.kt` (Premium Palette), `Type.kt`, and `Theme.kt`.
*   `util/`: Helpers for `ImageUtils` (Base64 encoding, saving/sharing).

### Project Setup & Prerequisites

**1. Obtain a U.S. Government FEC API Key:**
*   Register at [https://api.data.gov/signup/](https://api.data.gov/signup/).
*   Add to `local.properties`: `FEC_API_KEY="YOUR_API_KEY"`

**2. Google Cloud Setup:**
*   Create a Google Cloud Project.
*   Enable **Cloud Vision API** and **Vertex AI API**.
*   Create an **API Key** in Credentials.
*   Add the following to `local.properties`:
    ```properties
    GOOGLE_API_KEY="YOUR_GOOGLE_CLOUD_API_KEY"
    GOOGLE_CLOUD_PROJECT_ID="YOUR_PROJECT_ID"
    GOOGLE_CLOUD_LOCATION="us-central1"
    ```

**3. Android SDK:**
*   Ensure `sdk.dir` or `ANDROID_HOME` is set in your environment or `local.properties` to build the app.
