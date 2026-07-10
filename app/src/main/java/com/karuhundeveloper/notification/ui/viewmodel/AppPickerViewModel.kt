package com.karuhundeveloper.notification.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfoItem(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val installedApps: StateFlow<List<AppInfoItem>> = _installedApps

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                Log.d("AppPicker", "Loading apps...")
                val packages = try {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                } catch (e: Exception) {
                    Log.e("AppPicker", "Error getting packages", e)
                    emptyList()
                }
                
                Log.d("AppPicker", "Found ${packages.size} total packages")
                
                packages
                    .map { app ->
                        AppInfoItem(
                            name = app.loadLabel(pm).toString(),
                            packageName = app.packageName,
                            icon = null
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
            Log.d("AppPicker", "Loaded ${apps.size} apps after mapping")
            _installedApps.value = apps
            _isLoading.value = false
        }
    }
}
