
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.paulleung93.lobbylens"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.paulleung93.lobbylens"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "io.github.paulleung93.lobbylens.HiltTestRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // Add the buildConfigField for the FEC API key
        buildConfigField(
            "String",
            "FEC_API_KEY",
            "\"${localProperties.getProperty("FEC_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_API_KEY",
            "\"${localProperties.getProperty("GOOGLE_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_CLOUD_PROJECT_ID",
            "\"${localProperties.getProperty("GOOGLE_CLOUD_PROJECT_ID") ?: ""}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_CLOUD_LOCATION",
            "\"${localProperties.getProperty("GOOGLE_CLOUD_LOCATION") ?: "us-central1"}\""
        )
        buildConfigField(
            "String",
            "CLOUD_FUNCTION_URL",
            "\"${localProperties.getProperty("CLOUD_FUNCTION_URL") ?: ""}\""
        )
        buildConfigField(
            "String",
            "SENATE_API_KEY",
            "\"${localProperties.getProperty("SENATE_API_KEY") ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE*"
            merges += "META-INF/NOTICE*"
            merges += "META-INF/DEPENDENCIES"
            pickFirsts += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck)
    implementation(libs.androidx.compose.foundation.layout)
    debugImplementation(libs.firebase.appcheck.debug)
    
    // Google GenAI SDK
    implementation(libs.google.genai)
    
    // --- Unit Testing Libraries ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    
    // --- Android/Integration Testing Libraries ---
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // --- Hilt Testing ---
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.55")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.55")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Lifecycle runtime compose for collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
}
