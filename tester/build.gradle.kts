import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rdevzph.redirectguard.tester"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rdevzph.redirectguard.tester"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            var keystorePath = System.getenv("KEYSTORE_FILE")
            var keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            var keyAlias = System.getenv("KEY_ALIAS") ?: "redirectguard"
            var keyPassword = System.getenv("KEY_PASSWORD") ?: keystorePassword

            val envFile = rootProject.file(".env")
            if (envFile.exists()) {
                val envProps = Properties()
                FileInputStream(envFile).use { envProps.load(it) }
                keystorePath = keystorePath ?: envProps.getProperty("KEYSTORE_FILE")
                keystorePassword = keystorePassword ?: envProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: envProps.getProperty("KEY_ALIAS") ?: "redirectguard"
                keyPassword = keyPassword ?: envProps.getProperty("KEY_PASSWORD") ?: keystorePassword
            }

            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { releaseSigning ->
                if (releaseSigning.storeFile != null) {
                    signingConfig = releaseSigning
                } else {
                    signingConfig = signingConfigs.getByName("debug")
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
