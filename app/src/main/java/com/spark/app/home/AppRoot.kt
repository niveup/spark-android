package com.spark.app.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spark.app.auth.AuthStatus
import com.spark.app.auth.AuthViewModel
import com.spark.app.auth.LoginScreen
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.SparkApi
import com.spark.app.ui.OfflineBanner
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
}

// Login gate (Navigation Compose) -> tabbed main.
// Feed is served by the API since Phase 5a (FeedViewModel in the Reels tab).
@Composable
fun AppRoot(
    api: SparkApi,
    tokens: TokenStore,
    onCloseApp: () -> Unit,
    startOnReels: Boolean = false,
    onStartOnReelsHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vm: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(api, tokens) as T
        }
    })
    val auth by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var online by remember { mutableStateOf<Boolean?>(null) }

    suspend fun ping(): Boolean = try {
        api.health()
        true
    } catch (_: Exception) {
        false
    }

    LaunchedEffect(auth.status) {
        if (auth.status == AuthStatus.LOGGED_IN) {
            online = ping()
            nav.navigate(Routes.MAIN) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        } else {
            online = null
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (auth.status == AuthStatus.LOGGED_IN && online == false) {
            OfflineBanner(onRetry = { scope.launch { online = ping() } })
        }
        NavHost(navController = nav, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) {
                LoginScreen(state = auth, onLogin = vm::login)
            }
            composable(Routes.MAIN) {
                AppNav(
                    api = api,
                    tokens = tokens,
                    startOnReels = startOnReels,
                    onStartOnReelsHandled = onStartOnReelsHandled,
                    onCloseApp = onCloseApp,
                    onLogout = { vm.logout() }
                )
            }
        }
    }
}
