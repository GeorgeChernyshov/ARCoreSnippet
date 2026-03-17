import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val properties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        load(propertiesFile.inputStream())
    }
}

android {
    namespace = "com.example.arcoresnippet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcoresnippet"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = properties.getProperty("ARCORE_API_KEY") ?: ""
        manifestPlaceholders["ARCORE_API_KEY"] = apiKey
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    hilt {
        enableAggregatingTask = false
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(compose.uiTooling)
    implementation(compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    //ARCore
    implementation(libs.arCore)
    implementation(libs.arSceneView)

    //hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    //maps
    implementation(libs.maps.compose)

    //play services
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
}