// Top-level build file. AGP 9+ has built-in Kotlin: no kotlin-android plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    // Sentry Gradle plugin: uploads the R8 mapping file + source context on
    // release builds so release stack traces are readable in Sentry.
    alias(libs.plugins.sentry.android.gradle) apply false
}
