import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("com.google.dagger.hilt.android") version "2.57.2"
  id("com.google.devtools.ksp")
}

dependencies {
    implementation("androidx.activity:activity:1.11.0")
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-compiler:2.57.2")
}

android {
  namespace = "com.example.databinding"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.databinding"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    dataBinding = true
  }

  dataBinding {
    enable = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_11
  }
}

hilt {
  enableAggregatingTask = false
}


