# Manual Steps to Add Dependencies

## Step 1: Add Hilt Plugin

Open `app/build.gradle.kts` and add this line after `id("com.google.gms.google-services")`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android") version "2.51.1"  // ADD THIS LINE
}
```

## Step 2: Add Dependencies

Find this section near the end of the file:

```kotlin
    // ExifInterface for EXIF stripping
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    testImplementation(libs.junit)
```

Add these dependencies BETWEEN those two lines:

```kotlin
    // ExifInterface for EXIF stripping
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // TensorFlow Lite for on-device AI
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // TFLite delegates for hardware acceleration
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    // Note: NNAPI delegate is included in tensorflow-lite core since 2.3.0

    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    testImplementation(libs.junit)
```

## Step 3: Sync Gradle

After making these changes:

```bash
./gradlew --stop
./gradlew build
```

Or in your IDE: Click "Sync Now" when prompted.

## Quick Copy-Paste

**Hilt plugin (add after line 7):**
```
    id("com.google.dagger.hilt.android") version "2.51.1"
```

**Dependencies (add before testImplementation):**
```
    // TensorFlow Lite for on-device AI
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // TFLite delegates for hardware acceleration
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    // Note: NNAPI delegate is included in tensorflow-lite core since 2.3.0

    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

```
