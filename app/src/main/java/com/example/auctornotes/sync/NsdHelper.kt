package com.example.auctornotes.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NsdHelper(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val serviceType = "_auctor._tcp."
    
    private val _discoveredService = MutableStateFlow<NsdServiceInfo?>(null)
    val discoveredService: StateFlow<NsdServiceInfo?> = _discoveredService.asStateFlow()

    fun getLocalIpAddress(): String {
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return Formatter.formatIpAddress(ipAddress)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("NsdHelper", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("NsdHelper", "Service found: ${service.serviceName}")
            if (service.serviceType.contains("auctor")) {
                try {
                    nsdManager.resolveService(service, resolveListener)
                } catch (e: Exception) {
                    Log.e("NsdHelper", "Error calling resolveService", e)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e("NsdHelper", "Service lost: ${service.serviceName}")
            if (_discoveredService.value?.serviceName == service.serviceName) {
                _discoveredService.value = null
            }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("NsdHelper", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NsdHelper", "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NsdHelper", "Discovery stop failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("NsdHelper", "Resolve failed: Error code: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("NsdHelper", "Resolve Succeeded. $serviceInfo")
            _discoveredService.value = serviceInfo
        }
    }

    fun startDiscovery() {
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("AuctorNsdLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        }
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e("NsdHelper", "Error stopping discovery", e)
        }
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        multicastLock = null
    }
}
