package com.example.storechat.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.storechat.model.AppInfo
import com.example.storechat.model.DownloadStatus
import com.example.storechat.model.InstallState
import com.example.storechat.ui.home.AppCategory
import com.example.storechat.ui.search.UpdateStatus
import com.example.storechat.xc.XcServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadJobs = mutableMapOf<String, Job>()

    private val _appVersion = MutableLiveData("V1.0.0")
    val appVersion: LiveData<String> = _appVersion
    private val _checkUpdateResult = MutableLiveData<UpdateStatus?>()
    val checkUpdateResult: LiveData<UpdateStatus?> = _checkUpdateResult

    fun checkAppUpdate() {
        coroutineScope.launch {
            delay(800L)
            val current = _appVersion.value?.removePrefix("V") ?: "1.0.0"
            val latestFromServer = "1.0.1"
            val status = if (latestFromServer == current) UpdateStatus.LATEST else UpdateStatus.NEW_VERSION
            _checkUpdateResult.postValue(status)
        }
    }

    fun clearUpdateResult() {
        _checkUpdateResult.postValue(null)
    }

    private val categorizedData: Map<AppCategory, List<AppInfo>>
    private val _allApps = MutableLiveData<List<AppInfo>>()
    val allApps: LiveData<List<AppInfo>> = _allApps
    private val _categorizedApps = MutableLiveData<List<AppInfo>>()
    val categorizedApps: LiveData<List<AppInfo>> = _categorizedApps

    init {
        categorizedData = mapOf(
            AppCategory.YANNUO to listOf(
                AppInfo(
                    name = "应用名称A", description = "应用简介说明...", size = "83MB",
                    downloadCount = 1002, packageName = "com.demo.appa",
                    apkPath = "/sdcard/apks/app_a.apk", installState = InstallState.INSTALLED_LATEST,
                    versionName = "1.0.3", releaseDate = "2025-11-12"
                ),
                AppInfo(
                    name = "应用名称B", description = "应用简介说明...", size = "83MB",
                    downloadCount = 1003, packageName = "com.demo.appb",
                    apkPath = "/sdcard/apks/app_b.apk", installState = InstallState.INSTALLED_OLD,
                    versionName = "1.0.1", releaseDate = "2025-10-20"
                )
            ),
            AppCategory.ICBC to listOf(
                AppInfo(
                    name = "工行掌上银行", description = "工行官方移动银行客户端", size = "80MB",
                    downloadCount = 500000, packageName = "com.icbc.mobilebank",
                    apkPath = "/sdcard/apks/icbc_mobilebank.apk", installState = InstallState.NOT_INSTALLED,
                    versionName = "8.2.0.1.1", releaseDate = "2025-11-05"
                )
            ),
            AppCategory.CCB to listOf(
                AppInfo(
                    name = "建行生活", description = "吃喝玩乐建行优惠", size = "70MB",
                    downloadCount = 300000, packageName = "com.ccb.life",
                    apkPath = "/sdcard/apks/ccb_life.apk", installState = InstallState.NOT_INSTALLED,
                    versionName = "3.2.1", releaseDate = "2025-11-08"
                )
            )
        )
        _allApps.postValue(categorizedData.values.flatten())
        _categorizedApps.postValue(categorizedData[AppCategory.YANNUO] ?: emptyList())
    }

    fun selectCategory(category: AppCategory) {
        _categorizedApps.postValue(categorizedData[category] ?: emptyList())
    }

    private fun updateAppStatus(packageName: String, transform: (AppInfo) -> AppInfo) {
        val currentAll = _allApps.value ?: return
        val newAll = currentAll.map { if (it.packageName == packageName) transform(it) else it }
        _allApps.postValue(newAll)

        val currentCategorized = _categorizedApps.value ?: return
        val newCategorized = currentCategorized.map { if (it.packageName == packageName) transform(it) else it }
        _categorizedApps.postValue(newCategorized)
    }

    fun toggleDownload(app: AppInfo) {
        when (app.downloadStatus) {
            DownloadStatus.DOWNLOADING -> {
                downloadJobs[app.packageName]?.cancel()
                downloadJobs.remove(app.packageName)
                updateAppStatus(app.packageName) { it.copy(downloadStatus = DownloadStatus.PAUSED) }
            }
            DownloadStatus.NONE, DownloadStatus.PAUSED -> {
                val newJob = coroutineScope.launch {
                    updateAppStatus(app.packageName) { it.copy(downloadStatus = DownloadStatus.DOWNLOADING) }

                    for (p in app.progress..100) {
                        delay(80L)
                        updateAppStatus(app.packageName) { it.copy(progress = p) }
                    }

                    updateAppStatus(app.packageName) { it.copy(downloadStatus = DownloadStatus.VERIFYING) }
                    delay(500)

                    updateAppStatus(app.packageName) { it.copy(downloadStatus = DownloadStatus.INSTALLING) }
                    XcServiceManager.installApk(app.apkPath, app.packageName, true)
                    delay(1500)

                    updateAppStatus(app.packageName) {
                        it.copy(
                            downloadStatus = DownloadStatus.NONE,
                            progress = 0,
                            installState = InstallState.INSTALLED_LATEST
                        )
                    }
                    downloadJobs.remove(app.packageName)
                }
                downloadJobs[app.packageName] = newJob
            }
            DownloadStatus.VERIFYING, DownloadStatus.INSTALLING -> {}
        }
    }
}
