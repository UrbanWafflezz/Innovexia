# TensorFlow Lite Dependencies

Add these dependencies to `app/build.gradle.kts` in the `dependencies` block (after ExifInterface):

```kotlin
    // TensorFlow Lite for on-device AI
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // TFLite delegates for hardware acceleration
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    // Note: NNAPI delegate is included in tensorflow-lite core since 2.3.0

    // TODO: Add SentencePiece for tokenization when available
    // implementation("com.github.google:sentencepiece-java:0.1.99")

    // Hilt for dependency injection (if not already present)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
```

## Also Update Plugins

Add Hilt plugin to the top of `app/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    id("com.google.dagger.hilt.android") version "2.51.1"
}
```

## Sync Gradle

After adding dependencies, sync Gradle:
```bash
./gradlew --stop
./gradlew build
```
