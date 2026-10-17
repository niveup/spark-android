import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Dev helper: the API is reached through a USB tunnel ("adb reverse"), so the
// phone can call http://127.0.0.1:3000 even though Wi-Fi firewall blocks PC.
// adb drops that tunnel whenever its server restarts, so we re-create it on
// every build. Task state is a plain string -> safe in any Gradle mode.
val sparkLocalProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val sparkAdb = sparkLocalProps.getProperty("sdk.dir")?.let { sdk ->
    val name = if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb"
    file("$sdk/platform-tools/$name")
}

// Task class (not an ad-hoc closure): plain-string state, no Gradle-mode
// surprises. A doLast closure form of this failed with the configuration
// cache on ("cannot serialize Gradle script object references").
abstract class SparkAdbReverseTask : DefaultTask() {
    @get:Input
    abstract val adbPath: Property<String>

    @TaskAction
    fun run() {
        val path = adbPath.get()
        if (path.isEmpty()) return // no local.properties -> e.g. CI
        try {
            val proc = ProcessBuilder(path, "reverse", "tcp:3000", "tcp:3000")
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            logger.lifecycle(
                if (output.isBlank()) "SPARK dev: USB tunnel active (adb reverse tcp:3000)"
                else "SPARK dev: adb reverse: $output"
            )
        } catch (e: Exception) {
            logger.lifecycle("SPARK dev: adb reverse skipped (${e.message})")
        }
    }
}

val adbReverseDev = tasks.register("adbReverseDev", SparkAdbReverseTask::class) {
    adbPath.set(sparkAdb?.absolutePath ?: "")
}

// preBuild does not exist at plain script time (AGP creates/tasks register in
// afterEvaluate), so attach the dependency there. With configuration cache
// disabled this takes effect on every build (verified locally).
afterEvaluate {
    tasks.named("preBuild") { dependsOn(adbReverseDev) }
}

android {
    namespace = "com.spark.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spark.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-phase0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Dev: USB tunnel — adb reverse tcp:3000 tcp:3000 makes the
            // phone's 127.0.0.1:3000 hit the PC's API (Wi-Fi is blocked by firewall).
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://example.com\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.security.crypto)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
