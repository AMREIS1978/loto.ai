plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pt.lotosms.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pt.lotosms.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    val signingFile = System.getenv("LOTO_SIGNING_STORE_FILE")
    val signingPassword = System.getenv("LOTO_SIGNING_STORE_PASSWORD")
    val signingAlias = System.getenv("LOTO_SIGNING_KEY_ALIAS")
    val signingKeyPassword = System.getenv("LOTO_SIGNING_KEY_PASSWORD")

    signingConfigs {
        create("stableRelease") {
            if (!signingFile.isNullOrBlank() && !signingPassword.isNullOrBlank() &&
                !signingAlias.isNullOrBlank() && !signingKeyPassword.isNullOrBlank()) {
                storeFile = file(signingFile)
                storePassword = signingPassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("stableRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.firebase:firebase-messaging:24.0.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
