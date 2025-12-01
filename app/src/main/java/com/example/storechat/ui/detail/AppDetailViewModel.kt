package com.example.storechat.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storechat.data.AppRepository
import com.example.storechat.model.AppInfo
import com.example.storechat.model.HistoryVersion

class AppDetailViewModel : ViewModel() {

    // 将 appInfo 修改为 MediatorLiveData，以便观察 Repository 的变化
    private val _appInfo = MediatorLiveData<AppInfo>()
    val appInfo: LiveData<AppInfo> = _appInfo

    // 历史版本列表
    private val _historyVersions = MutableLiveData<List<HistoryVersion>>()
    val historyVersions: LiveData<List<HistoryVersion>> = _historyVersions

    /**
     * Activity 传入 packageName 后，开始观察 Repository 中对应的 AppInfo
     */
    fun loadApp(packageName: String) {
        _appInfo.addSource(AppRepository.allApps) { apps ->
            val foundApp = apps.find { it.packageName == packageName }
            if (foundApp != null && _appInfo.value != foundApp) {
                _appInfo.value = foundApp
            }
        }
    }

    /**
     * 加载指定应用的历史版本（目前使用假数据）
     */
    fun loadHistoryFor(app: AppInfo) {
        // 在真实项目中，这里应该从数据库或网络加载
        val fakeHistory = listOf(
            HistoryVersion("1.0.2", "/sdcard/apks/${app.packageName}_102.apk"),
            HistoryVersion("1.0.1", "/sdcard/apks/${app.packageName}_101.apk"),
            HistoryVersion("1.0.0", "/sdcard/apks/${app.packageName}_100.apk")
        )
        _historyVersions.value = fakeHistory
    }
}
