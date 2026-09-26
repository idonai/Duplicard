plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "nl.duplicard.wallet"
 compileSdk = 35
 defaultConfig {
  applicationId = "nl.duplicard.wallet"
  minSdk = 26
  targetSdk = 35
  versionCode = 4
  versionName = "1.1.2"
 }
 buildTypes {
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.camera:camera-camera2:1.4.1")
 implementation("androidx.camera:camera-lifecycle:1.4.1")
 implementation("androidx.camera:camera-view:1.4.1")
 implementation("com.google.mlkit:barcode-scanning:17.3.0")
 implementation("com.google.zxing:core:3.5.3")
 debugImplementation("androidx.compose.ui:ui-tooling")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.json:json:20240303")
}
