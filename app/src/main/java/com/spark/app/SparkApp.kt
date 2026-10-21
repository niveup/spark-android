package com.spark.app

import android.app.Application
import io.sentry.android.core.SentryAndroid

/**
 * Phase 8: Sentry crash reporting.
 *
 * The SDK is initialised here (the Application class) so crashes are captured
 * even if they happen before the first Activity is drawn.
 *
 * Privacy: `sendDefaultPii = false` means no IP address, no user data, no
 * device identifiers are attached. SPARK is a single-user app — there is
 * nothing personal to leak anyway, so we keep the strict default.
 */
class SparkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = BuildConfig.BUILD_TYPE
            options.release =
                "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            // 20% of transactions: enough to see startup/network trends without
            // burning the free-tier quota (5,000 errors + 10,000 perf units/mo).
            options.tracesSampleRate = 0.2
            // ANR watchdog: catches "app frozen for 5s" — the class of bug that
            // is invisible in logs but obvious to the user.
            options.isAnrEnabled = true
            options.isAnrReportInDebug = false
            // No personal data attached (IP, user, device ids).
            // (SDK 8.x exposes this as a Java-style setter, not a Kotlin property.)
            options.setSendDefaultPii(false)
            // Verbose SDK logging only in debug builds.
            options.isDebug = BuildConfig.DEBUG
            // SPARK ships no native code — skip the NDK layer (smaller APK,
            // one less moving part).
            options.isEnableNdk = false
        }
    }
}
