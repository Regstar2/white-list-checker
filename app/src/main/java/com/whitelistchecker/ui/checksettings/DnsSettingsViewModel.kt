package com.whitelistchecker.ui.checksettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitelistchecker.data.dns.DnsServersRepository
import com.whitelistchecker.domain.model.EditableDnsServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DnsSettingsUiState(
    val servers: List<EditableDnsServer> = emptyList(),
)

class DnsSettingsViewModel(
    private val repository: DnsServersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsSettingsUiState())
    val uiState: StateFlow<DnsSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeServers().collect { servers ->
                _uiState.update { it.copy(servers = servers) }
            }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setServerEnabled(id, enabled)
        }
    }

    fun add(server: EditableDnsServer) {
        viewModelScope.launch {
            repository.addServer(server)
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            repository.removeServer(id)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
