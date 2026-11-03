package com.veylora.vpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServerRepository(app)

    private val _servers = MutableStateFlow(repo.load())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    private val _selectedId = MutableStateFlow(repo.selectedId() ?: _servers.value.firstOrNull()?.id)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _seconds = MutableStateFlow(0L)
    val seconds: StateFlow<Long> = _seconds.asStateFlow()

    val status = VpnManager.status
    private var timer: Job? = null

    init {
        viewModelScope.launch {
            status.collect { st ->
                timer?.cancel()
                _seconds.value = 0
                if (st == VpnStatus.CONNECTED) {
                    timer = launch {
                        while (true) {
                            delay(1000)
                            _seconds.value += 1
                        }
                    }
                }
            }
        }
    }

    fun selected(): ServerConfig? = _servers.value.firstOrNull { it.id == _selectedId.value }

    fun select(id: String) {
        _selectedId.value = id
        repo.setSelected(id)
    }

    fun connect() {
        val server = selected()
        if (server == null) {
            _message.value = "No server available. Add one from the Servers tab."
            return
        }
        viewModelScope.launch {
            try {
                VpnManager.connect(getApplication(), server)
            } catch (t: Throwable) {
                _message.value = "Could not connect: " + (t.message ?: "invalid server configuration")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                VpnManager.disconnect(getApplication())
            } catch (t: Throwable) {
                _message.value = "Could not disconnect: " + (t.message ?: "unknown error")
            }
        }
    }

    fun importConfig(name: String, text: String) {
        try {
            VpnManager.parse(text)
            repo.addImported(name, text)
            _servers.value = repo.load()
            _message.value = "Server \"$name\" imported"
        } catch (t: Throwable) {
            _message.value = "Invalid WireGuard config: " + (t.message ?: "")
        }
    }

    fun remove(id: String) {
        repo.removeImported(id)
        _servers.value = repo.load()
        if (_selectedId.value == id) _selectedId.value = _servers.value.firstOrNull()?.id
    }

    fun showMessage(msg: String) { _message.value = msg }
    fun clearMessage() { _message.value = null }
}
