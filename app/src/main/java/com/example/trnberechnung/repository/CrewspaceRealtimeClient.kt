package com.example.trnberechnung.repository

import com.example.trnberechnung.BuildConfig
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.network.ApiRealtimeEnvelope
import com.example.trnberechnung.network.secureHttpsBaseUrl
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

internal class CrewspaceRealtimeClient(
    private val authRepository: AuthRepository,
    private val httpClient: OkHttpClient,
    private val gson: Gson = Gson(),
    private val onEnvelope: suspend (String, ApiRealtimeEnvelope) -> Unit,
    private val onConnected: suspend (String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val desired = AtomicBoolean(false)
    private val generation = AtomicLong(0)
    private val connectionAttempt = AtomicLong(0)
    private val socketLock = Any()
    private val _state = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val state: StateFlow<RealtimeConnectionState> = _state.asStateFlow()

    @Volatile
    private var socket: WebSocket? = null
    private var connectJob: Job? = null
    private var reconnectJob: Job? = null
    private var readyTimeoutJob: Job? = null
    private var reconnectAttempt = 0

    fun start() {
        if (!desired.compareAndSet(false, true)) return
        val ownerId = authRepository.skipperId
        if (ownerId.isBlank()) {
            desired.set(false)
            return
        }
        val session = generation.incrementAndGet()
        reconnectAttempt = 0
        connectJob?.cancel()
        connectJob = scope.launch { connect(session, ownerId) }
    }

    fun stop() {
        desired.set(false)
        generation.incrementAndGet()
        connectJob?.cancel()
        connectJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        readyTimeoutJob?.cancel()
        readyTimeoutJob = null
        connectionAttempt.incrementAndGet()
        synchronized(socketLock) {
            socket?.cancel()
            socket = null
        }
        _state.value = RealtimeConnectionState.DISCONNECTED
    }

    private suspend fun connect(
        session: Long,
        ownerId: String,
    ) {
        if (!isCurrent(session, ownerId)) return
        val attempt = connectionAttempt.incrementAndGet()
        val token =
            runCatching { authRepository.getIdToken() }
                .getOrElse {
                    if (!isCurrentAttempt(session, ownerId, attempt)) return
                    _state.value = RealtimeConnectionState.DISCONNECTED
                    scheduleReconnect(session, ownerId, attempt)
                    return
                }
        if (!isCurrentAttempt(session, ownerId, attempt)) return
        val websocketUrl = websocketUrl() ?: run {
            _state.value = RealtimeConnectionState.DISCONNECTED
            return
        }
        _state.value = RealtimeConnectionState.CONNECTING
        val request =
            Request.Builder()
                .url(websocketUrl)
                .header("Authorization", "Bearer $token")
                .build()
        val terminated = AtomicBoolean(false)
        val createdSocket =
            httpClient.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        if (!isCurrentAttempt(session, ownerId, attempt)) {
                            webSocket.cancel()
                            return
                        }
                        readyTimeoutJob?.cancel()
                        readyTimeoutJob =
                            scope.launch {
                                delay(10_000)
                                if (
                                    isCurrentAttempt(session, ownerId, attempt) &&
                                    _state.value != RealtimeConnectionState.CONNECTED &&
                                    terminated.compareAndSet(false, true)
                                ) {
                                    webSocket.cancel()
                                    handleDisconnect(session, ownerId, attempt)
                                }
                            }
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        if (
                            terminated.get() ||
                            !isCurrentAttempt(session, ownerId, attempt)
                        ) {
                            return
                        }
                        val envelope =
                            runCatching { gson.fromJson(text, ApiRealtimeEnvelope::class.java) }
                                .getOrNull()
                                ?.takeIf { it.version == 1 }
                                ?: return
                        if (envelope.type == "ready") {
                            readyTimeoutJob?.cancel()
                            readyTimeoutJob = null
                            reconnectAttempt = 0
                            _state.value = RealtimeConnectionState.CONNECTED
                            scope.launch {
                                if (isCurrentAttempt(session, ownerId, attempt)) {
                                    onConnected(ownerId)
                                }
                            }
                        } else if (_state.value == RealtimeConnectionState.CONNECTED) {
                            scope.launch {
                                if (isCurrentAttempt(session, ownerId, attempt)) {
                                    onEnvelope(ownerId, envelope)
                                }
                            }
                        }
                    }

                    override fun onClosing(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        if (isCurrentAttempt(session, ownerId, attempt)) {
                            webSocket.close(code, reason)
                        }
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        if (terminated.compareAndSet(false, true)) {
                            handleDisconnect(session, ownerId, attempt)
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        throwable: Throwable,
                        response: Response?,
                    ) {
                        if (terminated.compareAndSet(false, true)) {
                            handleDisconnect(session, ownerId, attempt)
                        }
                    }
                },
            )
        synchronized(socketLock) {
            if (
                isCurrentAttempt(session, ownerId, attempt) &&
                !terminated.get()
            ) {
                socket = createdSocket
            } else {
                createdSocket.cancel()
            }
        }
    }

    private fun handleDisconnect(
        session: Long,
        ownerId: String,
        attempt: Long,
    ) {
        synchronized(socketLock) {
            if (!isCurrentAttempt(session, ownerId, attempt)) return
            socket = null
        }
        readyTimeoutJob?.cancel()
        readyTimeoutJob = null
        _state.value = RealtimeConnectionState.DISCONNECTED
        scheduleReconnect(session, ownerId, attempt)
    }

    private fun scheduleReconnect(
        session: Long,
        ownerId: String,
        attempt: Long,
    ) {
        if (
            !isCurrentAttempt(session, ownerId, attempt) ||
            reconnectJob?.isActive == true
        ) {
            return
        }
        val delayMillis = min(30_000L, 1_000L shl reconnectAttempt.coerceAtMost(5))
        reconnectAttempt++
        reconnectJob =
            scope.launch {
                delay(delayMillis)
                reconnectJob = null
                if (isCurrentAttempt(session, ownerId, attempt)) {
                    connect(session, ownerId)
                }
            }
    }

    private fun isCurrent(
        session: Long,
        ownerId: String,
    ): Boolean =
        desired.get() &&
            generation.get() == session &&
            authRepository.skipperId == ownerId

    private fun isCurrentAttempt(
        session: Long,
        ownerId: String,
        attempt: Long,
    ): Boolean =
        isCurrent(session, ownerId) &&
            connectionAttempt.get() == attempt

    private fun websocketUrl(): String? {
        val base = secureHttpsBaseUrl(BuildConfig.CREWSPACE_BASE_URL).trimEnd('/')
        if (base == "https://example.invalid") return null
        return "wss://${base.removePrefix("https://")}/crewspace/realtime"
    }
}
