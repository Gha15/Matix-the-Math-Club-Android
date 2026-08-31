// Top-level build file for Matix the Math Club (Android).
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

// `./gradlew clean` from the root.
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
