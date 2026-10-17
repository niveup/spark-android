package com.spark.app.auth

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.LoginRequest
import com.spark.app.data.remote.SparkApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

enum class AuthStatus { CHECKING, LOGGED_OUT, LOGGING_IN, LOGGED_IN }

data class AuthUiState(
    val status: AuthStatus = AuthStatus.CHECKING,
    val message: String? = null
)

class AuthViewModel(
    private val api: SparkApi,
    private val tokens: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        _state.value = if (tokens.getToken() != null) {
            AuthUiState(AuthStatus.LOGGED_IN)
        } else {
            AuthUiState(AuthStatus.LOGGED_OUT)
        }
    }

    fun login(passcode: String) {
        if (passcode.isBlank()) {
            _state.value = AuthUiState(AuthStatus.LOGGED_OUT, "Type your passcode first.")
            return
        }
        _state.value = AuthUiState(AuthStatus.LOGGING_IN)
        viewModelScope.launch {
            try {
                val res = api.login(LoginRequest(passcode, Build.MODEL.take(64)))
                tokens.saveToken(res.data.token)
                _state.value = AuthUiState(AuthStatus.LOGGED_IN)
            } catch (e: HttpException) {
                _state.value = AuthUiState(
                    AuthStatus.LOGGED_OUT,
                    if (e.code() == 401) "Wrong passcode." else "Server error (${e.code()})."
                )
            } catch (_: IOException) {
                _state.value = AuthUiState(AuthStatus.LOGGED_OUT, "No connection. Check Wi-Fi.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val raw = tokens.getToken()
            tokens.clear()
            _state.value = AuthUiState(AuthStatus.LOGGED_OUT)
            if (raw != null) {
                try {
                    api.logout("Bearer $raw") // best effort: token already cleared locally
                } catch (_: Exception) {
                    // Local logout already done; server revoke retried next login.
                }
            }
        }
    }

    fun authHeader(): String? = tokens.getToken()?.let { "Bearer $it" }
}
