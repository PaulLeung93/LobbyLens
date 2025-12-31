# Project Roadmap: LobbyLens

**Vision:** To provide unprecedented transparency in politics by visualizing campaign finance and lobbying data directly onto images of politicians. LobbyLens leverages powerful Cloud AI for recognition and generative visualization to create an intuitive, data-driven experience.

## Core Features
1.  **Cloud-Powered Recognition:** Utilizes **Google Cloud Vision API** (Web Detection) to accurately identify public figures from images, replacing the need for unreliable on-device models.
2.  **Generative Visualization:** Uses **Gemini 3 Pro Image** to naturally edit politician photos, adding "donor badges" (company logos) to their clothing based on their top contributors.
3.  **Campaign Finance Data:** Pulls official campaign contribution data from the **FEC API**, including historical trends across different election cycles.
4.  **Lobbyist Disclosures (LD-203):** Integrates the **U.S. Senate Lobbying Disclosure API** to show "Lobbyist Gift & Contribution Reports," providing a deeper look at influence beyond standard campaign donations.
5.  **Interactive Data Exploration:** 
    *   **Sorting & Filtering:** Sort contributions by amount or search for specific donors by name.
    *   **Historical Trends:** Interactive bar charts visualize funding sources over time.
    *   **Tabbed Interface:** Seamlessly toggle between FEC campaign data and Senate lobbyist reports.
6.  **Premium Aesthetic:** A sleek, modern "Presidential" theme with Navy Blue, Gold, and Deep Crimson accents, utilizing glassmorphism and Material 3 components.
7.  **Save & Share:** Users can save the AI-generated "enhanced" images to their gallery or share them to social media.

## UI/UX Design
The app features a cohesive **Premium Dark** theme designed to convey authority and transparency.

1.  **Home Screen:**
    *   A bold, presidential-themed landing page with "Scan Politician" and "Browse Congress" entry points.
    *   Dynamic background elements and modern typography (Outfit/Inter).
2.  **Editor Screen:**
    *   Orchestrates the AI pipeline: Identify -> Fetch -> Generate.
    *   Real-time status tracking for identification and generative editing.
    *   Interactive controls for cycle selection and organization filtering.
3.  **Details Screen:**
    *   **Tabbed Layout:** "FEC Contributions" vs "Lobbyist Disclosures."
    *   **Data Visualization:** High-performance bar charts for historical cycles.
    *   **Searchable Lists:** Filterable donor records with formatted currency and contribution types.

## Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM (Model-View-ViewModel) with Repository pattern.
*   **Cloud AI / Services:**
    *   **Google Cloud Vision API:** For politician identification.
    *   **Gemini 3 Pro Image:** For AI image editing (generative inpainting).
*   **APIs:**
    *   **FEC (Federal Election Commission):** Official campaign finance records.
    *   **U.S. Senate LDA:** Official Lobbying Disclosure Act (LD-203) reports.
*   **Networking:** Retrofit2 + OkHttp (with custom interceptors for API keys and User-Agents).
*   **Utilities:** Coil (Image loading), Gson (JSON parsing).

## Project Architecture
*   `MainActivity.kt`: Hosts Jetpack Compose navigation and theme initialization.
*   `data/`:
    *   `model/`: Unified models for FEC, Senate LDA, Cloud Vision, and Gemini.
    *   `api/`: Retrofit service definitions (`FecApiService`, `SenateLdaApiService`, `GeminiApiService`, `CloudVisionService`).
    *   `repository/PoliticianRepository.kt`: The central orchestrator for all data and AI operations.
*   `ui/`:
    *   `home/`: Landing screen and congress browsing.
    *   `editor/`: Processing and AI visualization.
    *   `details/`: Comprehensive data visualization and search.
    *   `theme/`: Design system tokens (Colors, Typography, Shapes).

---

## Project Setup & Prerequisites

**1. FEC API Key:**
*   Register at [api.data.gov](https://api.data.gov/signup/).
*   Add to `local.properties`: `FEC_API_KEY=YOUR_KEY`

**2. Google Cloud API Key:**
*   Enable **Cloud Vision API** and **Gemini API** in your Google Cloud Console.
*   Add to `local.properties`: 
    ```properties
    GOOGLE_API_KEY=YOUR_KEY
    GOOGLE_CLOUD_PROJECT_ID=YOUR_PROJECT_ID
    GOOGLE_CLOUD_LOCATION=us-central1
    ```

**3. Firebase Setup:**
*   Create a project on the [Firebase Console](https://console.firebase.google.com/).
*   Add an Android App with package name `io.github.paulleung93.lobbylens`.
*   Download `google-services.json` and place it in the `app/` directory.
*   **App Check:** Enable **Firebase App Check** to secure your API calls.
    *   For debug builds, the app is configured to use the `DebugAppCheckProviderFactory`.
    *   Register your **SHA-256 fingerprint** in the Firebase settings to allow local debugging.

**4. Senate LDA API (Optional):**
*   The Senate API is open, but registered keys have higher rate limits (120 req/min).
*   Register at [lda.senate.gov](https://lda.senate.gov/api/v1/accounts/register/).
*   Add to `local.properties`: `SENATE_API_KEY=YOUR_TOKEN`
