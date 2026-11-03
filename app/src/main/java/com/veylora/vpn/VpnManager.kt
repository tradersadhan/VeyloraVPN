package com.veylora.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED }

object VpnManager {
    private var backend: GoBackend? = null
    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val tunnel = object : Tunnel {
        override fun getName(): String = "veylora"
        override fun onStateChange(newState: Tunnel.State) {
            _status.value = if (newState == Tunnel.State.UP) VpnStatus.CONNECTED else VpnStatus.DISCONNECTED
        }
    }

    private fun backend(ctx: Context): GoBackend =
        backend ?: GoBackend(ctx.applicationContext).also { backend = it }

    fun parse(configText: String): Config =
        Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))

    suspend fun connect(ctx: Context, server: ServerConfig) = withContext(Dispatchers.IO) {
        _status.value = VpnStatus.CONNECTING
        try {
            val cfg = parse(server.config)
            backend(ctx).setState(tunnel, Tunnel.State.UP, cfg)
            _status.value = VpnStatus.CONNECTED
        } catch (t: Throwable) {
            _status.value = VpnStatus.DISCONNECTED
            throw t
        }
    }

    suspend fun disconnect(ctx: Context) = withContext(Dispatchers.IO) {
        try {
            backend(ctx).setState(tunnel, Tunnel.State.DOWN, null)
        } finally {
            _status.value = VpnStatus.DISCONNECTED
        }
    }
}
