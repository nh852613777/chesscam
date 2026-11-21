plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nsmod.chesscam"
    compileSdk = 34
    
    androidResources {
        noCompress += listOf("bin")
    }
    
    defaultConfig {
        applicationId = "com.nsmod.chesscam"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    
    // Thư viện chuẩn Android của chess
    implementation("com.github.bhlangonijr:chesslib:1.3.3") 
}
