plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.msarobotika.cyberdeck"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.msarobotika.cyberdeck"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-superflyper"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.github.mik3y:usb-serial-for-android:3.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
