plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Librería que empaqueta yt-dlp + Python + ffmpeg + QuickJS para Android.
// 0.18.x agrega QuickJS: yt-dlp lo necesita para resolver los desafíos JS de YouTube.
val ytdlpLib = "0.18.1"

// Versión del módulo ffmpeg. Por defecto la misma que la librería (combinación soportada).
// MEDIDO: el ffmpeg de 0.18.1 pesa 34.0 MB por ABI (arm64) frente a 16.3 MB el de 0.17.2.
// Poner "0.17.2" ahorra ~17.7 MB por APK, pero mezcla versiones de módulos y NO está probado:
// si lo activas, verifica MP3/merge a MP4 antes de usarlo.
val ffmpegLib = ytdlpLib

// ABIs a compilar. Por defecto solo celulares (arm64 y arm 32 bits).
// Para probar en el emulador de Android Studio (x86_64):  ./gradlew assembleRelease -Pabis=arm64-v8a,armeabi-v7a,x86_64
val abis = (project.findProperty("abis") as String? ?: "arm64-v8a,armeabi-v7a").split(",").map { it.trim() }

android {
    namespace = "app.ytdlp.descargador"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.ytdlp.descargador"
        minSdk = 29          // Android 10 o superior
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
        buildConfigField("String", "YTDLP_LIB", "\"$ytdlpLib\"")
        resourceConfigurations += listOf("es", "en")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Un APK por arquitectura. Sin APK universal: pesaría ~4 veces más.
    splits {
        abi {
            isEnable = true
            reset()
            include(*abis.toTypedArray())
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Firmado con la llave de depuración para que sea instalable directamente.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // Necesario: yt-dlp/Python/ffmpeg viajan como librerías nativas que se ejecutan.
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("io.github.junkfood02.youtubedl-android:library:$ytdlpLib")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$ffmpegLib")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Solo para pruebas instrumentadas (no entran en el APK).
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
