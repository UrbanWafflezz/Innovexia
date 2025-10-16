#!/bin/bash

# Backup the original file
cp app/build.gradle.kts app/build.gradle.kts.backup

# Add Hilt plugin after line 7 (after google-services)
sed -i '7 a\    id("com.google.dagger.hilt.android") version "2.51.1"' app/build.gradle.kts

# Add dependencies before testImplementation
sed -i '/testImplementation(libs.junit)/i\
    \/\/ TensorFlow Lite for on-device AI\
    implementation("org.tensorflow:tensorflow-lite:2.14.0")\
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")\
    \/\/ TFLite delegates for hardware acceleration\
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")\
    \/\/ Note: NNAPI delegate is included in tensorflow-lite core since 2.3.0\
\
    \/\/ Hilt for dependency injection\
    implementation("com.google.dagger:hilt-android:2.51.1")\
    ksp("com.google.dagger:hilt-compiler:2.51.1")\
\
' app/build.gradle.kts

echo "Dependencies added successfully!"
echo "Original file backed up to app/build.gradle.kts.backup"
