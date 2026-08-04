import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.skofqq.domainmanager"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.skofqq.domainmanager"
        minSdk = 29
        targetSdk = 37
        versionCode = 8
        versionName = "1.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProps["RELEASE_STORE_FILE"] ?: "release.jks")
            storePassword = localProps["RELEASE_STORE_PASSWORD"] as String?
            keyAlias = localProps["RELEASE_KEY_ALIAS"] as String?
            keyPassword = localProps["RELEASE_KEY_PASSWORD"] as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Seed-based Material3 color scheme (brand accent when dynamic color is off)
    implementation(libs.materialkolor)
    // Avatar loading in About
    implementation(libs.coil.compose)

    // Biometric/PIN app lock
    implementation(libs.androidx.biometric)

    // Network & storage
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)

    // Home-screen widget refresh (svc_list poll on the widget's schedule)
    implementation(libs.androidx.work.runtime)
    // QR generation (setup QR in Authorization settings)
    implementation(libs.zxing.core)
    // System QR scanner via Play services — no in-app camera permission
    implementation(libs.play.services.code.scanner)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
