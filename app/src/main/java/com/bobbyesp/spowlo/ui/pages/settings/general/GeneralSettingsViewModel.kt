package com.bobbyesp.spowlo.ui.pages.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.library.data.remote.SpotDLDependencyUpdater
import com.bobbyesp.spowlo.utils.PreferencesUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UpdateUiState(
        val status: UpdateStatus = UpdateStatus.Idle,
        val currentVersion: String = "Loading...",
        val updateAvailable: Boolean = false,
        val updateLog: String = ""
    )

    enum class UpdateStatus {
        Idle, Checking, Updating, Updated, Error
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshVersionDisplay()
        checkForUpdatesBackground()
    }

    private fun refreshVersionDisplay() {
        viewModelScope.launch {
            // Prevent reading version while updating to avoid "0.0.0" flicker
            if (_uiState.value.status == UpdateStatus.Updating) return@launch

            val version = SpotDLDependencyUpdater.getLocalSpotDLVersion(context)
            if (version == "0.0.0") {
                _uiState.update { it.copy(currentVersion = "Not Installed", updateAvailable = true) }
            } else {
                _uiState.update { it.copy(currentVersion = "v$version") }
            }
        }
    }

    private fun checkForUpdatesBackground() {
        viewModelScope.launch {
            val freq = PreferencesUtil.getUpdateFrequency()
            if (freq == PreferencesUtil.FREQ_DISABLED) return@launch

            val lastCheck = PreferencesUtil.getLastUpdateCheck()
            val now = System.currentTimeMillis()
            val daysDiff = TimeUnit.MILLISECONDS.toDays(now - lastCheck)

            val shouldCheck = when (freq) {
                PreferencesUtil.FREQ_DAILY -> daysDiff >= 1
                PreferencesUtil.FREQ_WEEKLY -> daysDiff >= 7
                PreferencesUtil.FREQ_MONTHLY -> daysDiff >= 30
                else -> false
            }

            // Check if it's needed or if it's the first run (lastCheck == 0)
            if (shouldCheck || lastCheck == 0L) {
                _uiState.update { it.copy(status = UpdateStatus.Checking) }
                
                // SpotDLDependencyUpdater.areUpdatesAvailable checks PyPI vs Local
                val hasUpdate = SpotDLDependencyUpdater.areUpdatesAvailable(context)
                
                _uiState.update { 
                    it.copy(
                        updateAvailable = hasUpdate, 
                        status = UpdateStatus.Idle
                    ) 
                }
                PreferencesUtil.setLastUpdateCheck(now)
            }
        }
    }

    fun performUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UpdateStatus.Updating) }
            try {
                // Perform the update (Thread-safe via Mutex in Updater)
                val log = SpotDLDependencyUpdater.updateVolatileDependencies(context)
                
                // Refresh version text
                val newVersion = SpotDLDependencyUpdater.getLocalSpotDLVersion(context)
                
                _uiState.update { 
                    it.copy(
                        status = UpdateStatus.Updated,
                        currentVersion = "v$newVersion (updated)",
                        updateAvailable = false,
                        updateLog = log
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        status = UpdateStatus.Error, 
                        updateLog = e.message ?: "Unknown error" 
                    ) 
                }
            }
        }
    }
    
    fun setFrequency(freq: Int) {
        PreferencesUtil.setUpdateFrequency(freq)
        // Optionally re-check if user changed settings, but not strictly necessary immediately
    }
    
    fun getFrequency(): Int = PreferencesUtil.getUpdateFrequency()
}