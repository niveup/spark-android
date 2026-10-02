// Top-level build file. AGP 9+ has built-in Kotlin: no kotlin-android plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
