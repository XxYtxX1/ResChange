package com.reschange.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val manager = ResolutionManager(app)

    data class UiState(
        val currentW: Int = 0,
        val currentH: Int = 0,
        val currentDpi: Int = 0,
        val isApplied: Boolean = false,
        val shellMode: ResolutionManager.ShellMode = ResolutionManager.ShellMode.NONE,
        val isLoading: Boolean = false,
        val message: String? = null
    )

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> = _state

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true)
            val (w, h, dpi, mode) = withContext(Dispatchers.IO) {
                val size = manager.currentSize()
                val dpi = manager.currentDpi()
                val mode = manager.shellMode
                val (w, h) = size ?: Pair(0, 0)
                listOf(w, h, dpi, mode)
            }
            @Suppress("UNCHECKED_CAST")
            _state.value = UiState(
                currentW   = w as Int,
                currentH   = h as Int,
                currentDpi = dpi as Int,
                isApplied  = manager.isApplied(),
                shellMode  = mode as ResolutionManager.ShellMode,
                isLoading  = false
            )
        }
    }

    fun applyPreset(preset: DevicePreset) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
            val ok = withContext(Dispatchers.IO) { manager.apply(preset) }
            _state.value = _state.value?.copy(
                isLoading = false,
                isApplied = ok,
                message = if (ok) "Applied ${preset.name}" else "Failed — check Shizuku/root"
            )
            if (ok) refresh()
        }
    }

    fun reset() {
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
            val ok = withContext(Dispatchers.IO) { manager.reset() }
            _state.value = _state.value?.copy(
                isLoading = false,
                isApplied = !ok,
                message = if (ok) "Reset to original" else "Reset failed"
            )
            if (ok) refresh()
        }
    }

    fun clearMessage() {
        _state.value = _state.value?.copy(message = null)
    }
}

// Destructuring helper
private operator fun List<Any>.component4(): Any = this[3]
