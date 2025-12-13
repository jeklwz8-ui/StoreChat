package com.example.storechat.ui.detail

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.storechat.data.AppRepository
import com.example.storechat.model.AppInfo
import com.example.storechat.model.HistoryVersion
import kotlinx.coroutines.launch

class AppDetailViewModel : ViewModel() {

    private val _appInfo = MediatorLiveData<AppInfo>()
    val appInfo: LiveData<AppInfo> = _appInfo

    private var appInfoSource: LiveData<AppInfo?>? = null

    private val _historyVersions = MutableLiveData<List<HistoryVersion>>()
    val historyVersions: LiveData<List<HistoryVersion>> = _historyVersions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val historyVersionCache = mutableMapOf<String, List<HistoryVersion>>()

    // ✅ 历史版本展示覆盖（仅覆盖版本号/描述等，状态仍来自 repo）
    private var historyOverride: AppInfo? = null

    fun loadApp(packageName: String) {
        appInfoSource?.let { _appInfo.removeSource(it) }

        val newSource = AppRepository.allApps.map { apps ->
            apps.find { it.packageName == packageName }
        }

        _appInfo.addSource(newSource) { appFromRepo ->
            appFromRepo?.let { repoApp ->
                val override = historyOverride
                _appInfo.value = if (override != null) {
                    repoApp.copy(
                        versionId = override.versionId,
                        versionName = override.versionName,
                        description = override.description
                    )
                } else {
                    repoApp
                }
            }
        }

        appInfoSource = newSource
    }

    fun setAppInfo(app: AppInfo) {
        historyOverride = null
        loadApp(app.packageName)
    }

    fun setHistoryAppInfo(app: AppInfo) {
        historyOverride = app
        loadApp(app.packageName)
    }

    fun loadHistoryFor(context: Context, app: AppInfo) {
        if (historyVersionCache.containsKey(app.appId)) {
            _historyVersions.value = historyVersionCache[app.appId]
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            val history = AppRepository.loadHistoryVersions(context, app)
            _historyVersions.postValue(history)
            _isLoading.postValue(false)
            historyVersionCache[app.appId] = history
        }
    }

    fun clearHistoryCache() {
        historyVersionCache.clear()
    }
}
