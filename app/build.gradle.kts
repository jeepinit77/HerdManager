import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Version properties file
val versionPropsFile = project.file("version.properties") // Relative to app module dir
val versionProps = Properties()

// Function to load version properties
fun loadVersionProperties() {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use {
            versionProps.load(it)
        }
    } else {
        // Should not happen if we ensure file is created, but good fallback
        versionProps["APP_VERSION_MAJOR"] = "1"
        versionProps["APP_VERSION_MINOR"] = "0"
        versionProps["APP_VERSION_PATCH"] = "0" // Will be incremented to 1
        versionProps["APP_BUILD_NUMBER"] = "0" // Will be incremented to 1
    }
}

// Removed saving of version properties to prevent auto-increment on each build

// Load current versions
loadVersionProperties()

val appVersionMajor = versionProps.getProperty("APP_VERSION_MAJOR", "1").toInt()
val appVersionMinor = versionProps.getProperty("APP_VERSION_MINOR", "0").toInt()
val appVersionPatch = versionProps.getProperty("APP_VERSION_PATCH", "0").toInt()
val appBuildNumber = versionProps.getProperty("APP_BUILD_NUMBER", "0").toInt()

val calculatedVersionName = "${appVersionMajor}.${appVersionMinor}.${appVersionPatch}"
val calculatedVersionCode = appBuildNumber

// Do not write back to version.properties; keep values static until manually changed

android {
    namespace = "com.jumblemint.cows"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jumblemint.cows"
        minSdk = 30
        targetSdk = 34
        versionCode = calculatedVersionCode
        versionName = calculatedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")
    
    // Tips persistence
    implementation(libs.androidx.datastore.preferences)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.androidx.compose.bom))

    //fix automirrored errors?
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

}