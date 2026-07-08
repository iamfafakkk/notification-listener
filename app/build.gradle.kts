plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.karuhundeveloper.notification"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.karuhundeveloper.notification"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Tells Room's KSP processor where to write the schema JSON files.
        // These files should be committed to source control to enable migration testing.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // ── Room ─────────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)           // Coroutines / Flow extensions
    ksp(libs.room.compiler)                 // Annotation processor (replaces kapt)
    androidTestImplementation(libs.room.testing)

    // ── JSON serialization (used by TypeConverters) ───────────────────────────
    implementation(libs.gson)

    // ── Test ─────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}