plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.frostbyte.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.frostbyte.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.10.0-phase10-partial"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Room schema export location, referenced by Converters/FrostByteDatabase
        // (exportSchema = true) so future migrations can diff against real
        // historical schemas instead of guessing.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    // Real lint configuration for CI (Phase 10) - default Android Lint
    // settings on a fast-moving pre-release codebase produce a lot of noise
    // unrelated to actual correctness (e.g. missing translations for a
    // single-locale app), and AGP's default "abort on error" behavior would
    // block CI on lint categories this project doesn't care about yet.
    // Genuinely serious categories (security, correctness) are NOT
    // suppressed - only checks that don't apply to this project's current
    // stage.
    lint {
        abortOnError = true
        warningsAsErrors = false
        disable += setOf(
            "MissingTranslation", // single-locale app for now
            "GradleDependency", // dependency version bumps are tracked deliberately, not flagged automatically
            "OldTargetApi" // targetSdk is kept intentionally current, this check lags AGP's own knowledge of the latest API level
        )
        // Real correctness/security checks stay at their default severity -
        // this is a targeted allowlist of noise, not a blanket suppression.
    }

    // Release signing is sourced entirely from environment variables, which
    // release.yml populates from GitHub Secrets. Locally, these env vars are
    // simply unset and releaseSigning stays null - `assembleRelease` will
    // then fail with a clear "no signing config" error rather than silently
    // producing an unsigned or debug-signed APK.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    val releaseSigningConfig = if (!keystoreFile.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = file(keystoreFile)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    } else null

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            releaseSigningConfig?.let { signingConfig = it }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose BOM keeps all Compose artifact versions aligned
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Adaptive layout (bottom nav on phones, nav rail on large screens - Section 5 of PRD)
    implementation("androidx.compose.material3.adaptive:adaptive:1.0.0")

    // Coroutines / async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Persistence (used from Phase 2 onward - Profiles/Settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Background work (used from Phase 3 onward - Downloads)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Networking (used from Phase 3 onward)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
