package com.example.auctornotes.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

data class DiscoveredHost(val ip: String, val port: Int)

/**
 * Listens for UDP broadcast discovery packets sent by the Auctor desktop app.
 * The desktop broadcasts JSON like { "service": "auctor-sync", "port": 48xxx, "ip": "..." }
 * every 2 seconds on UDP port 48099.
 */
class UdpDiscoveryReceiver(private val scope: CoroutineScope) {

    private val DISCOVERY_PORT = 48099
    private var socket: DatagramSocket? = null
    private var job: Job? = null

    private val _discovered = MutableStateFlow<DiscoveredHost?>(null)
    val discovered: StateFlow<DiscoveredHost?> = _discovered.asStateFlow()

    fun start() {
        job?.cancel()
        _discovered.value = null
        job = scope.launch(Dispatchers.IO) {
            try {
                val sock = DatagramSocket(DISCOVERY_PORT)
                sock.broadcast = true
                sock.soTimeout = 3000  // wake up every 3 s to check isActive
                socket = sock
                val buf = ByteArray(512)
                val packet = DatagramPacket(buf, buf.size)
                Log.d("UdpDiscovery", "Listening for Auctor broadcasts on UDP $DISCOVERY_PORT")
                while (isActive) {
                    try {
                        sock.receive(packet)
                        val json = String(packet.data, 0, packet.length)
                        Log.d("UdpDiscovery", "Received packet: $json")
                        val obj = JSONObject(json)
                        if (obj.optString("service") == "auctor-sync") {
                            val ip = obj.getString("ip")
                            val port = obj.getInt("port")
                            Log.d("UdpDiscovery", "Discovered Auctor at $ip:$port")
                            _discovered.value = DiscoveredHost(ip, port)
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // normal — just loop again to check isActive
                    } catch (e: Exception) {
                        Log.w("UdpDiscovery", "Packet parse error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("UdpDiscovery", "UDP discovery error: ${e.message}")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                socket = null
                Log.d("UdpDiscovery", "UDP discovery stopped")
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        _discovered.value = null
    }
}
