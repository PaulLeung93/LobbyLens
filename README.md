# 🔍 LobbyLens

<div align="center">

**Visualizing Political Influence Through AI**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpack-compose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Features](#-features) • [Walkthrough](#-walkthrough) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## 📖 About

**LobbyLens** brings unprecedented transparency to politics by combining the power of AI with public campaign finance data. Simply take a photo of any politician, and LobbyLens will:

1. 🎯 **Identify** them using Google Cloud Vision API
2. 💰 **Reveal** their top campaign contributors from official FEC records
3. 🎨 **Visualize** the influence by overlaying sponsor logos on their image using Gemini 3 Pro Image
4. 📊 **Explore** detailed campaign finance and lobbying disclosure data

LobbyLens transforms complex political finance data into an intuitive, visual experience that anyone can understand.

---

## ✨ Features

### 🤖 AI-Powered Recognition & Visualization
- **Cloud Vision API Integration**: Accurate politician identification from photos
- **Gemini 3 Pro Image**: Generative AI creates visual representations of campaign influence by adding donor logos to politician photos
- **Real-time Processing**: Seamless pipeline from image capture to AI-generated visualization

### 💵 Comprehensive Finance Data
- **FEC Campaign Contributions**: Official Federal Election Commission data
  - Historical contribution trends across multiple election cycles
  - Detailed contributor information with amounts and types
  - Sortable and searchable donor records
- **Lobbyist Disclosures (LD-203)**: Senate Lobbying Disclosure Act reports
  - Gift and contribution reports from registered lobbyists
  - Direct influence tracking beyond traditional campaign donations

### 📊 Interactive Data Exploration
- **Dual View Tabs**: Toggle between FEC contributions and lobbyist disclosures
- **Historical Charts**: Visualize funding trends over time with interactive bar graphs
- **Smart Filtering**: Search contributors by name, sort by amount
- **Direct FEC Links**: Verify data with official source links

### 🎯 Browse & Search
- **Congress Directory**: Browse all current members of Congress
- **Advanced Search**: Find politicians by name, state, or party
- **Detailed Profiles**: Complete campaign finance profiles for each politician

### 💾 Save & Share
- **Gallery Export**: Save AI-generated images to your device
- **Social Sharing**: Share visualizations to raise awareness
- **Toast Confirmations**: User-friendly feedback for all actions

---

## 📱 Walkthrough

> *Coming soon: Screenshots, GIFs, and videos showcasing politician identification, data visualization, and AI-generated images*

<!-- 
Add media here when available:
![Home Screen](screenshots/home.png)
![Editor Screen](screenshots/editor.png)
![Details Screen](screenshots/details.png)
![Generated Image](screenshots/generated.png)
![Demo](screenshots/demo.gif)
-->

---

## 🛠 Tech Stack

### Core Technologies
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
- **Minimum SDK**: Android 13 (API 33)
- **Target SDK**: Android 14 (API 36)

### AI & Cloud Services
- **[Google Cloud Vision API](https://cloud.google.com/vision)**: Politician identification via Web Detection
- **[Gemini 3 Pro Image](https://ai.google.dev/gemini-api)**: Generative image editing and inpainting
- **[Firebase](https://firebase.google.com/)**: Authentication and App Check for secure API access

### Data Sources
- **[FEC API](https://api.open.fec.gov/)**: Federal Election Commission campaign finance data
- **[U.S. Senate LDA API](https://lda.senate.gov/api/)**: Lobbying Disclosure Act reports
- **[Bioguide/ProPublica](https://theunitedstates.io/)**: Congressional member data and images

### Libraries & Tools
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **JSON Parsing**: [Gson](https://github.com/google/gson)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

---

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (Latest stable version recommended)
2. **JDK 11** or higher
3. **API Keys** (see setup below)

### API Setup

#### 1. FEC API Key
Register for a free API key to access campaign finance data:
```bash
# Register at: https://api.data.gov/signup/
# Add to local.properties:
FEC_API_KEY=your_fec_api_key_here
```

#### 2. Google Cloud Setup
Enable Cloud Vision and Gemini APIs in Google Cloud Console:

```bash
# 1. Create a project at: https://console.cloud.google.com/
# 2. Enable APIs:
#    - Cloud Vision API
#    - Gemini API (Vertex AI)
# 3. Create an API key with Android app restrictions
# 4. Add to local.properties:

GOOGLE_API_KEY=your_google_api_key_here
GOOGLE_CLOUD_PROJECT_ID=your_project_id
GOOGLE_CLOUD_LOCATION=us-central1
```

**Important**: Configure API key restrictions in Google Cloud Console:
- Add your app's package name: `io.github.paulleung93.lobbylens`
- Add your SHA-1 and SHA-256 fingerprints (see below)

#### 3. Firebase Setup
Firebase is required for App Check and secure API authentication:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com/)
2. Add an Android app with package name: `io.github.paulleung93.lobbylens`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable **Firebase App Check** in the Firebase Console
5. Register your app's SHA-256 fingerprint

**Get your SHA-256 fingerprint:**
```bash
# For debug keystore:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Look for "SHA256:" in the output
```

#### 4. Senate LDA API (Optional)
The Senate Lobbying API is open but registered keys have higher rate limits:

```bash
# Register at: https://lda.senate.gov/api/v1/accounts/register/
# Add to local.properties:
SENATE_API_KEY=your_senate_api_key_here
```

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/PaulLeung93/LobbyLens.git
   cd LobbyLens
   ```

2. **Create `local.properties` file** in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   FEC_API_KEY=your_fec_api_key
   GOOGLE_API_KEY=your_google_api_key
   GOOGLE_CLOUD_PROJECT_ID=your_project_id
   GOOGLE_CLOUD_LOCATION=us-central1
   SENATE_API_KEY=your_senate_api_key
   ```

3. **Place `google-services.json`** in the `app/` directory

4. **Build and run:**
   ```bash
   # Open in Android Studio and click Run
   # OR use command line:
   ./gradlew installDebug
   ```

---

## 🏗 Architecture

LobbyLens follows clean architecture principles with clear separation of concerns:

```
app/src/main/java/io/github/paulleung93/lobbylens/
├── data/
│   ├── api/                    # Retrofit service interfaces
│   │   ├── FecApiService.kt
│   │   ├── SenateLdaApiService.kt
│   │   ├── CloudVisionService.kt
│   │   └── GeminiApiService.kt
│   ├── model/                  # Data models
│   │   ├── FecModels.kt
│   │   ├── SenateLdaModels.kt
│   │   ├── CloudVisionModels.kt
│   │   └── GeminiModels.kt
│   └── repository/
│       └── PoliticianRepository.kt  # Central data orchestration
├── ui/
│   ├── home/                   # Landing & browse screens
│   ├── editor/                 # AI processing & visualization
│   ├── details/                # Data exploration
│   └── theme/                  # Design system (Colors, Typography)
├── util/                       # Helper utilities
└── MainActivity.kt             # Navigation host
```

### Key Components

- **`PoliticianRepository`**: Orchestrates all data fetching and AI operations
  - Cloud Vision identification
  - FEC data retrieval
  - Senate LDA queries
  - Gemini image generation

- **MVVM Pattern**: Each screen has its own ViewModel managing UI state and business logic

- **Jetpack Compose**: Modern, declarative UI with Material 3 theming

---

## 🎨 Design Philosophy

LobbyLens features a **Premium Dark** theme with a "Presidential" aesthetic:

- **Color Palette**: Navy Blue, Gold, Deep Crimson
- **Typography**: Outfit & Inter fonts for modern, professional appearance
- **Effects**: Glassmorphism, smooth gradients, micro-animations
- **Layout**: Card-based design with clear information hierarchy

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/AmazingFeature`
3. **Commit your changes**: `git commit -m 'Add some AmazingFeature'`
4. **Push to the branch**: `git push origin feature/AmazingFeature`
5. **Open a Pull Request**

### Areas for Contribution
- 📱 UI/UX improvements
- 🔍 Enhanced search and filtering
- 📊 Additional data visualizations
- 🌐 Support for state-level politicians
- 🧪 Test coverage
- 📝 Documentation improvements

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Data Sources**:
  - [Federal Election Commission (FEC)](https://www.fec.gov/) - Campaign finance data
  - [U.S. Senate LDA](https://lda.senate.gov/) - Lobbying disclosure data
  - [The United States Project](https://theunitedstates.io/) - Congressional data

- **AI & Cloud**:
  - [Google Cloud Vision](https://cloud.google.com/vision) - Image recognition
  - [Google Gemini 3 Pro Image](https://ai.google.dev/) - Generative AI image editing

- **Open Source Libraries**:
  - [Jetpack Compose](https://developer.android.com/jetpack/compose)
  - [Retrofit](https://square.github.io/retrofit/)
  - [Coil](https://coil-kt.github.io/coil/)

---



## ⚖️ Disclaimer

This app uses publicly available data from official government sources (FEC, Senate LDA). Data accuracy depends on these sources. LobbyLens is designed for educational and transparency purposes. Always verify information with official sources before making important decisions.

---

<div align="center">

**Made with ❤️ for transparency in democracy**

⭐ Star this repo if you support political transparency!

</div>
