package com.spark.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.spark.app.sync.TokenReader

// Raw token lives ONLY here (Keystore-backed). Never logged, never in backups
// (allowBackup=false in the manifest). TokenReader is Phase 7's test seam
// (single-method interface so sync logic runs on the JVM without a Context).
class TokenStore(context: Context) : TokenReader {
    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "spark_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }
}
