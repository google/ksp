plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
}
dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    ksp("androidx.room:room-compiler:2.4.2")
    implementation("androidx.room:room-runtime:2.4.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
android {
    defaultConfig {
        minSdkVersion(24)
    }
    compileSdk = 34
    buildFeatures { 
        viewBinding = true
    }
}
