plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.b1gbr0ther"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.b1gbr0ther"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.alphacephei:vosk-android:0.3.47")
    // Room Database dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Task to download the Vosk model automatically
tasks.register("downloadVoskModel") {
    val zipUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    val output = File(buildDir, "model.zip")
    val assetsDir = File(projectDir, "src/main/assets/model")

    doLast {
        println("Downloading Vosk model...")
        ant.invokeMethod("get", mapOf("src" to zipUrl, "dest" to output, "verbose" to true))
        println("Unzipping model...")
        copy {
            from(zipTree(output))
            into(assetsDir)
        }
        println("Vosk model downloaded and unzipped to src/main/assets/model")
    }
}