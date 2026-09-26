plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

fun secret(name: String): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse("")
        .get()

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.vibeos.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vibeos.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "FIREBASE_API_KEY", quoted(secret("VIBE_FIREBASE_API_KEY")))
        buildConfigField("String", "FIREBASE_APP_ID", quoted(secret("VIBE_FIREBASE_APP_ID")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", quoted(secret("VIBE_FIREBASE_PROJECT_ID")))
        buildConfigField("String", "FIREBASE_SENDER_ID", quoted(secret("VIBE_FIREBASE_SENDER_ID")))
        buildConfigField("String", "REVENUECAT_API_KEY", quoted(secret("VIBE_REVENUECAT_API_KEY")))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.work:work-runtime-ktx:2.12.0")

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")

    implementation("com.revenuecat.purchases:purchases:10.18.1")
}
