import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // Phase 8: uploads the R8 mapping file + source context on release builds
    // so release stack traces are readable in Sentry.
    alias(libs.plugins.sentry.android.gradle)
    alias(libs.plugins.kover)
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


// Sentry DSN — this is NOT a secret. A DSN is a public identifier that only
// allows *sending* events to this one Sentry project (it grants no read
// access to your data). Sentry's own docs put it in the app. The secret is
// the org auth token, which is only used by the build to upload R8 mapping
// files and never enters the APK (see sentry.properties / SENTRY_AUTH_TOKEN).
val sparkSentryDsn =
    "https://f9dea375bbd32c349f695538275262af@o4512291522215936.ingest.de.sentry.io/4512291567763536"

// Sentry org auth token (build-time only — never enters the APK).
// Resolution order: SENTRY_AUTH_TOKEN env var, then `sentry.properties` in the
// repo root (git-ignored). Without it the build still works; only the R8
// mapping upload is skipped.
val sparkSentryAuthToken: String? = System.getenv("SENTRY_AUTH_TOKEN")
    ?: rootProject.file("sentry.properties")
        .takeIf { it.exists() }
        ?.readLines()
        // Accept Sentry's official `auth.token=` key and the short form.
        ?.firstOrNull {
            val line = it.trim()
            line.startsWith("auth.token=") || line.startsWith("authToken=")
        }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Dev: USB tunnel — adb reverse tcp:3000 tcp:3000 makes the
            // phone's 127.0.0.1:3000 hit the PC's API (Wi-Fi is blocked by firewall).
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000\"")
            buildConfigField("String", "SENTRY_DSN", "\"$sparkSentryDsn\"")
        }
        release {
            // Phase 8: production builds are minified and resource-shrunk.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://example.com\"")
            buildConfigField("String", "SENTRY_DSN", "\"$sparkSentryDsn\"")
        }
        // Phase 8 QA: a release-like, R8-minified build signed with the local
        // debug key and pointed at the USB-tunnel API. This is how the release
        // shrinking pipeline is smoke-tested on the phone before Phase 9.
        // Never distribute this variant.
        create("qa") {
            initWith(getByName("release"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000\"")
            buildConfigField("String", "SENTRY_DSN", "\"$sparkSentryDsn\"")
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
    implementation(libs.compose.material.icons.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    // Async image loading with offline memory & disk cache
    implementation(libs.coil.compose)
    // Phase 6 audiobook playback: background + lock-screen controls via a
    // MediaSessionService. Streams only — no audio files are downloaded.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    // Phase 7 offline: Room caches the last feed + saved cards and persists the
    // pending-interaction queue across app restarts.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Phase 8 crash reporting: the BOM keeps every io.sentry:* artifact on
    // one version. Errors + ANRs are captured automatically; no source changes
    // needed for the basic setup.
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Phase 8: on-device Compose UI tests (no emulator; runs on the owner's phone).
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Phase 8 coverage floor. This measures JVM unit tests only; the on-device
// Compose tests are run separately (connectedDebugAndroidTest) and are not
// merged into this number. The floor guards against losing the tested core
// (ranking/SRS/queue/settings) while the UI layer keeps growing.
kover {
    reports {
        verify {
            rule {
                minBound(12)
            }
        }
    }
}

// Room exports its schema as JSON so future migrations start from a known
// history (official Room guidance for schema changes).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

// Sentry Gradle plugin configuration (Phase 8).
// The auth token is needed ONLY to upload the R8 mapping file + source context
// for release builds. It is read from SENTRY_AUTH_TOKEN (env) or sentry.properties
// (git-ignored). Without it the build still succeeds — only the upload is skipped.
sentry {
    org = "nive-kb"
    projectName = "spark-android"
    authToken = sparkSentryAuthToken
    // Only run sentry-cli when a token is configured. Without a token the
    // mapping/source upload is pointless, and sentry-cli's Windows argument
    // handling fails on project paths containing spaces ("D:\new something").
    // With a token, run the mapping upload in CI or from a space-free path.
    includeProguardMapping = sparkSentryAuthToken != null
    autoUploadProguardMapping = sparkSentryAuthToken != null
    includeSourceContext = sparkSentryAuthToken != null
}
