import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// Backend for release builds, from `dealio.apiBaseUrl` in gradle.properties and
// overridable with -Pdealio.apiBaseUrl=… . It used to be a literal
// REPLACE_WITH_BACKEND_DOMAIN, so every release build shipped pointing at a host
// that does not exist. Read through a provider so the configuration cache
// invalidates when the value changes.
val releaseApiBaseUrl: String = providers.gradleProperty("dealio.apiBaseUrl")
    .getOrElse("https://d2l7qgxnnc8786.cloudfront.net/api/")

// Upload signing. The key never lives in the repo: copy keystore.properties.example
// to keystore.properties (gitignored), point it at your .jks, and the release
// build signs itself. Without that file the build still runs and simply produces
// an unsigned artifact, so nobody needs a keystore to compile the project.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystorePropertiesFile.exists() &&
    keystoreProperties.getProperty("storeFile").isNullOrBlank().not()

android {
    namespace = "com.dealio.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dealio.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the host machine's localhost from the Android emulator
            buildConfigField("String", "API_BASE_URL", "\"https://d2l7qgxnnc8786.cloudfront.net/api/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // Unsigned rather than broken when no keystore is configured: a
            // developer without the key can still run assembleRelease.
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
            // Left off deliberately. R8 strips the Gson model classes' fields by
            // reflection unless every one is kept, and the app has not been run
            // end-to-end minified yet — turning it on is a change to make and
            // then test, not to slip into a release build. See RELEASE.md.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.biometric)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    // Phone-number OTP. The code is sent and checked by Firebase; the resulting
    // ID token is exchanged for a Dealio session at POST /api/auth/firebase.
    implementation("com.google.firebase:firebase-auth")
}
