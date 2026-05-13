package com.example.auctornotes.sync

import android.util.Log
import com.example.auctornotes.sync.model.SyncMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketManager {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val messageAdapter = moshi.adapter(SyncMessage::class.java)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SyncMessage>(extraBufferCapacity = 8)
    val incomingMessages: SharedFlow<SyncMessage> = _incomingMessages.asSharedFlow()

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketManager", "Connected to $url")
                _isConnected.value = true
                _connectionError.value = null
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketManager", "Received message: $text")
                try {
                    val message = messageAdapter.fromJson(text)
                    if (message != null) {
                        _incomingMessages.tryEmit(message)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketManager", "Error parsing message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                Log.d("WebSocketManager", "Closing: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                val msg = t.message ?: "Unknown error"
                _connectionError.value = msg
                Log.e("WebSocketManager", "Error: $msg")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                Log.d("WebSocketManager", "Closed: $code / $reason")
            }
        })
    }

    fun sendMessage(message: SyncMessage) {
        val json = messageAdapter.toJson(message)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        _isConnected.value = false
    }
}
