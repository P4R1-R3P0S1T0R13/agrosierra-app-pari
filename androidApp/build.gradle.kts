plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "pe.upeu.agrosierra"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.upeu.agrosierra.pari"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-unidad2"
        // 10.0.2.2 es el localhost de la PC visto desde el emulador.
        // Para un dispositivo fisico, reemplazar por la IP de la PC en la red local.
        buildConfigField("String", "URL_BASE", "\"http://10.0.2.2:8080/api/v1/\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "REGISTRO_DETALLADO", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "REGISTRO_DETALLADO", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.koin.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.kotlinx.coroutines.android)
}
