package com.spark.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks only whether Android considers the current default network internet-ready. */
class NetworkMonitor(context: Context) {
    private val connectivityManager = checkNotNull(
        context.applicationContext.getSystemService(ConnectivityManager::class.java)
    ) { "Connectivity service is unavailable" }

    private val mutableValidatedInternet = MutableStateFlow<Boolean?>(null)
    val validatedInternet: StateFlow<Boolean?> = mutableValidatedInternet.asStateFlow()

    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null
    @Volatile
    private var defaultNetwork: Network? = null

    fun start() {
        if (callback != null) return

        defaultNetwork = connectivityManager.activeNetwork
        mutableValidatedInternet.value = currentValidatedInternet(defaultNetwork)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (defaultNetwork != network) {
                    // Capabilities arrive next; avoid showing stale offline status during handover.
                    defaultNetwork = network
                    mutableValidatedInternet.value = null
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                defaultNetwork = network
                mutableValidatedInternet.value = isValidatedInternet(
                    hasInternet = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ),
                    isValidated = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                )
            }

            override fun onLost(network: Network) {
                if (defaultNetwork == network) {
                    defaultNetwork = null
                    mutableValidatedInternet.value = false
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        callback = networkCallback
    }

    fun stop() {
        val registeredCallback = callback ?: return
        callback = null
        connectivityManager.unregisterNetworkCallback(registeredCallback)
        defaultNetwork = null
        mutableValidatedInternet.value = null
    }

    private fun currentValidatedInternet(network: Network?): Boolean {
        network ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return isValidatedInternet(
            hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )
    }
}

internal fun isValidatedInternet(hasInternet: Boolean, isValidated: Boolean): Boolean =
    hasInternet && isValidated
