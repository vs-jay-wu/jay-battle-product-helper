package com.viewsonic.classswift.manager

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber


class NetworkManager(private val connectivityManager: ConnectivityManager) {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _networkAvailabilityState = MutableStateFlow(false)
    // need to send socket event window, please collection networkAvailabilityState
    val networkAvailabilityState: StateFlow<Boolean> = _networkAvailabilityState.asStateFlow()
    private val _delayInformNetworkAvailabilityState = MutableStateFlow(false)
    // need to send api window, please collection networkAvailabilityState
    val delayInformNetworkAvailabilityState: StateFlow<Boolean> = _delayInformNetworkAvailabilityState.asStateFlow()
    private var hasCapabilityNetwork = false
    private var informNetworkCapabilitiesJob: Job? = null

    init {
        registerNetworkCallback()
        hasCapabilityNetwork = isNetworkAvailable()
        coroutineScope.launch {
            _networkAvailabilityState.update { hasCapabilityNetwork }
            _delayInformNetworkAvailabilityState.update { hasCapabilityNetwork }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("[NetworkManager] receive network onAvailable")
            }

            // turn off network onCapabilitiesChanged won't be called so need handle
            override fun onLost(network: Network) {
                Timber.d("[NetworkManager] receive network onLost")
                // if state is has CapabilityNetwork, should inform UI network on lost.
                if (hasCapabilityNetwork) {
                    hasCapabilityNetwork = false
                    AmplitudeEventBuilder(AmplitudeConstant.EventName.DISCONNECT)
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.SYS_ERROR_MESSAGE, "")
                        .appendEventProperty(AmplitudeConstant.EventProperties.Key.SYS_ERROR_CODE, -1)
                        .send()
                    informNetworkCapabilitiesJob = coroutineScope.launch {
                        _networkAvailabilityState.update { hasCapabilityNetwork }
                        delay(THREE_SEC_DELAY)
                        Timber.d("[NetworkManager] onLost to send no network Availability")
                        _delayInformNetworkAvailabilityState.update { hasCapabilityNetwork }
                    }
                }
            }

            // network capabilities change, e.g. from network-only to network-capable
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasCapabilitiesInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Timber.d("[NetworkManager] receive network Capabilities state: $hasCapabilitiesInternet ")
                // avoid received same Capabilities event
                if (hasCapabilityNetwork != hasCapabilitiesInternet) {
                    hasCapabilityNetwork = hasCapabilitiesInternet
                    if (!hasCapabilityNetwork) {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.DISCONNECT)
                            .appendEventProperty(AmplitudeConstant.EventProperties.Key.SYS_ERROR_MESSAGE, "")
                            .appendEventProperty(AmplitudeConstant.EventProperties.Key.SYS_ERROR_CODE, -1)
                            .send()
                        informNetworkCapabilitiesJob = coroutineScope.launch {
                            _networkAvailabilityState.update { hasCapabilityNetwork }
                            delay(THREE_SEC_DELAY)
                            Timber.d("[NetworkManager] send no network Availability $hasCapabilityNetwork ")
                            _delayInformNetworkAvailabilityState.update { hasCapabilityNetwork }
                        }
                    } else {
                        AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT).send()
                        // has network, should cancel inform network no Capabilities or on lost.
                        informNetworkCapabilitiesJob?.cancel()
                        Timber.d("[NetworkManager] send has network Availability $hasCapabilityNetwork ")
                        _networkAvailabilityState.update { hasCapabilityNetwork }
                        _delayInformNetworkAvailabilityState.update { hasCapabilityNetwork }
                    }
                }
            }
        }
        networkCallback?.let { connectivityManager.registerNetworkCallback(networkRequest, it) }
    }

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    }
}
