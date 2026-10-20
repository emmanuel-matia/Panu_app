package com.example.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Moniteur global de connectivité réseau pour PANU.
 * Permet à l'ensemble des modules (Supabase, IA, Studio, Feed) d'adapter leur comportement
 * en temps réel face aux variations de connexion (Wi-Fi, 4G, 3G, 2G, Hors-ligne).
 */
class NetworkConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(determineConnectionType())
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch {
                _isOnline.value = true
                _connectionType.value = determineConnectionType()
            }
        }

        override fun onLost(network: Network) {
            scope.launch {
                val hasAny = checkInitialConnectivity()
                _isOnline.value = hasAny
                _connectionType.value = if (hasAny) determineConnectionType() else "OFFLINE"
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            scope.launch {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _isOnline.value = hasInternet
                _connectionType.value = determineConnectionType()
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Repli gracieux si les permissions réseau restreintes
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true // Par défaut optimiste pour ne pas bloquer les caches locaux
        }
    }

    private fun determineConnectionType(): String {
        return try {
            val cm = connectivityManager ?: return "UNKNOWN"
            val active = cm.activeNetwork ?: return "OFFLINE"
            val caps = cm.getNetworkCapabilities(active) ?: return "OFFLINE"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }
}
