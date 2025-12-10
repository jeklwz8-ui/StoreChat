package com.example.storechat.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.storechat.data.api.ApiClient
import com.example.storechat.data.api.AppHistoryRequest
import com.example.storechat.data.api.AppListRequest
import com.example.storechat.data.api.DownloadLinkRequest
import com.example.storechat.model.AppCategory
import com.example.storechat.model.AppInfo
import com.example.storechat.model.DownloadStatus
import com.example.storechat.model.HistoryVersion
import com.example.storechat.model.InstallState
import com.example.storechat.model.UpdateStatus
import com.example.storechat.xc.XcServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object AppRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val cancellationsForDeletion = ConcurrentHashMap.newKeySet<String>()

    private val apiService = ApiClient.appApi

    private val stateLock = Any()

    private var localAllApps: List<AppInfo> = emptyList()
    private var localDownloadQueue: List<AppInfo> = emptyList()
    private var localRecentApps: List<AppInfo> = emptyList()

    private val _allApps = MutableLiveData<List<AppInfo>>()
    val allApps: LiveData<List<AppInfo>> = _allApps

    private val _downloadQueue = MutableLiveData<List<AppInfo>>()
    val downloadQueue: LiveData<List<AppInfo>> = _downloadQueue

    private val _recentInstalledApps = MutableLiveData<List<AppInfo>>()
    val recentInstalledApps: LiveData<List<AppInfo>> = _recentInstalledApps

    private val _appVersion = MutableLiveData("V1.0.0")
    val appVersion: LiveData<String> = _appVersion

    private val _checkUpdateResult = MutableLiveData<UpdateStatus?>()
    val checkUpdateResult: LiveData<UpdateStatus?> = _checkUpdateResult

    private val _selectedCategory = MutableLiveData(AppCategory.YANNUO)

    val categorizedApps: LiveData<List<AppInfo>> = MediatorLiveData<List<AppInfo>>().apply {
        addSource(allApps) { apps ->
            value = apps.filter { it.category == _selectedCategory.value }
        }
        addSource(_selectedCategory) { category ->
            value = allApps.value?.filter { it.category == category }
        }
    }

    init {
        val initialApps = listOf(
            AppInfo(
                name = "应用名称A", description = "应用简介说明...", size = "83MB",
                downloadCount = 1002, packageName = "com.demo.appa",
                apkPath = "/sdcard/apks/app_a.apk", installState = InstallState.INSTALLED_LATEST,
                versionName = "1.0.3", releaseDate = "2025-11-12", category = AppCategory.YANNUO
            ),
            AppInfo(
                name = "应用名称C", description = "应用简介说明...", size = "83MB",
                downloadCount = 1002, packageName = "com.demo.appc",
                apkPath = "/sdcard/apks/app_a_103.apk",
                installState = InstallState.INSTALLED_OLD,
                versionName = "1.0.4",
                releaseDate = "2025-11-12",
                category = AppCategory.YANNUO
            ),
            AppInfo(
                name = "应用名称B", description = "应用简介说明...", size = "83MB",
                downloadCount = 1003, packageName = "com.demo.appb",
                apkPath = "/sdcard/apks/app_b.apk", installState = InstallState.INSTALLED_LATEST,
                versionName = "1.0.1", releaseDate = "2025-10-20", category = AppCategory.YANNUO
            ),
            AppInfo(
                name = "工行掌上银行", description = "工行官方移动银行客户端", size = "80MB",
                downloadCount = 500000, packageName = "com.icbc.mobilebank",
                apkPath = "/sdcard/apks/icbc_mobilebank.apk", installState = InstallState.NOT_INSTALLED,
                versionName = "8.2.0.1.1", releaseDate = "2025-11-05", category = AppCategory.ICBC
            ),
            AppInfo(
                name = "建行生活", description = "吃喝玩乐建行优惠", size = "70MB",
                downloadCount = 300000, packageName = "com.ccb.life",
                apkPath = "/sdcard/apks/ccb_life.apk", installState = InstallState.NOT_INSTALLED,
                versionName = "3.2.1", releaseDate = "2025-11-08", category = AppCategory.CCB
            )
        )

        synchronized(stateLock) {
            localAllApps = initialApps
            _allApps.postValue(localAllApps)
        }

        coroutineScope.launch {
            refreshAppsFromServer(AppCategory.YANNUO)
        }
    }

    private fun updateAppStatus(packageName: String, transform: (AppInfo) -> AppInfo) {
        synchronized(stateLock) {
            val app = localAllApps.find { it.packageName == packageName } ?: return
            val newApp = transform(app)

            localAllApps = localAllApps.map { if (it.packageName == packageName) newApp else it }

            if (localDownloadQueue.any { it.packageName == packageName }) {
                localDownloadQueue = localDownloadQueue.map { if (it.packageName == packageName) newApp else it }
                _downloadQueue.postValue(localDownloadQueue)
            }

            _allApps.postValue(localAllApps)
        }
    }

    private fun addToDownloadQueue(app: AppInfo) {
        synchronized(stateLock) {
            if (localDownloadQueue.none { it.packageName == app.packageName }) {
                localDownloadQueue = localDownloadQueue + app
                _downloadQueue.postValue(localDownloadQueue)
            }
        }
    }

    private fun removeFromDownloadQueue(packageName: String) {
        synchronized(stateLock) {
            localDownloadQueue = localDownloadQueue.filterNot { it.packageName == packageName }
            _downloadQueue.postValue(localDownloadQueue)
        }
    }

    private fun addToRecentInstalled(app: AppInfo) {
        synchronized(stateLock) {
            if (localRecentApps.none { it.packageName == app.packageName }) {
                localRecentApps = listOf(app) + localRecentApps
                _recentInstalledApps.postValue(localRecentApps)
            }
        }
    }

    fun selectCategory(category: AppCategory) {
        _selectedCategory.postValue(category)
        coroutineScope.launch {
            refreshAppsFromServer(category)
        }
    }

    private suspend fun refreshAppsFromServer(category: AppCategory) {
        try {
            val categoryParam = when (category) {
                AppCategory.YANNUO -> "1"
                AppCategory.ICBC -> "2"
                AppCategory.CCB -> "3"
            }
            val remoteList = apiService.getAppList(
                AppListRequest(category = categoryParam)
            )

            synchronized(stateLock) {
                // Create a map of existing apps for quick lookup.
                val localAppsMap = localAllApps.associateBy { it.packageName }

                // Merge the server list with the local state.
                val mergedRemoteList = remoteList.map { serverApp ->
                    val localApp = localAppsMap[serverApp.packageName]
                    if (localApp != null) {
                        // App exists locally. Trust the server for metadata (name, icon, etc.),
                        // but preserve the local download/install state.
                        serverApp.copy(
                            installState = localApp.installState,
                            downloadStatus = localApp.downloadStatus,
                            progress = localApp.progress
                        )
                    } else {
                        // This is a new app from the server that we haven't seen before.
                        serverApp
                    }
                }

                // Get all apps from other categories that we are not touching.
                val otherCategoryApps = localAllApps.filter { it.category != category }

                // The new source of truth is the combination of untouched apps and the newly merged list.
                localAllApps = otherCategoryApps + mergedRemoteList
                _allApps.postValue(localAllApps)
            }
        } catch (e: Exception) {
            // On network error or other issues, we keep the existing local data to be safe.
        }
    }

    private suspend fun resolveDownloadApkPath(
        packageName: String,
        fallbackApkPath: String,
        versionName: String?
    ): String {
        return try {
            val resp = apiService.getDownloadLink(
                DownloadLinkRequest(
                    packageName = packageName,
                    versionName = versionName
                )
            )
            if (resp.url.isBlank()) fallbackApkPath else resp.url
        } catch (e: Exception) {
            fallbackApkPath
        }
    }

    fun toggleDownload(app: AppInfo) {
        when (app.downloadStatus) {
            DownloadStatus.DOWNLOADING -> {
                downloadJobs[app.packageName]?.cancel()
            }

            DownloadStatus.NONE, DownloadStatus.PAUSED -> {
                if (downloadJobs.containsKey(app.packageName)) {
                    return
                }

                addToDownloadQueue(app)

                val newJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        val realApkPath = resolveDownloadApkPath(
                            packageName = app.packageName,
                            fallbackApkPath = app.apkPath,
                            versionName = app.versionName
                        )

                        updateAppStatus(app.packageName) {
                            it.copy(downloadStatus = DownloadStatus.DOWNLOADING)
                        }

                        for (p in app.progress..100) {
                            delay(80L)
                            updateAppStatus(app.packageName) { it.copy(progress = p) }
                        }

                        updateAppStatus(app.packageName) {
                            it.copy(downloadStatus = DownloadStatus.VERIFYING)
                        }
                        delay(500)

                        updateAppStatus(app.packageName) {
                            it.copy(downloadStatus = DownloadStatus.INSTALLING)
                        }
                        XcServiceManager.installApk(realApkPath, app.packageName, true)
                        delay(1500)

                        var installedApp: AppInfo? = null
                        updateAppStatus(app.packageName) {
                            installedApp = it.copy(
                                downloadStatus = DownloadStatus.NONE,
                                progress = 0,
                                installState = InstallState.INSTALLED_LATEST
                            )
                            installedApp!!
                        }

                        downloadJobs.remove(app.packageName)
                        removeFromDownloadQueue(app.packageName)
                        installedApp?.let { addToRecentInstalled(it) }

                    } catch (e: Exception) {
                        val packageName = app.packageName
                        val wasDeletion = cancellationsForDeletion.remove(packageName)

                        if (e is kotlinx.coroutines.CancellationException) {
                            if (wasDeletion) {
                                updateAppStatus(packageName) {
                                    it.copy(downloadStatus = DownloadStatus.NONE, progress = 0)
                                }
                            } else {
                                updateAppStatus(packageName) {
                                    it.copy(downloadStatus = DownloadStatus.PAUSED)
                                }
                            }
                            downloadJobs.remove(packageName)
                            throw e
                        } else {
                            updateAppStatus(packageName) {
                                it.copy(downloadStatus = DownloadStatus.PAUSED)
                            }
                            downloadJobs.remove(packageName)
                        }
                    }
                }

                downloadJobs[app.packageName] = newJob
                newJob.start()
            }

            DownloadStatus.VERIFYING, DownloadStatus.INSTALLING -> {
                // Ignore clicks
            }
        }
    }

    fun cancelDownload(app: AppInfo) {
        val packageName = app.packageName
        val job = downloadJobs[packageName]

        removeFromDownloadQueue(packageName)

        if (job != null) {
            cancellationsForDeletion.add(packageName)
            job.cancel()
        } else {
            updateAppStatus(packageName) {
                it.copy(downloadStatus = DownloadStatus.NONE, progress = 0)
            }
            cancellationsForDeletion.remove(packageName)
        }
    }

    fun removeDownload(app: AppInfo) {
        removeFromDownloadQueue(app.packageName)
    }

    suspend fun loadHistoryVersions(app: AppInfo): List<HistoryVersion> {
        return try {
            val versions = apiService.getAppHistory(
                AppHistoryRequest(packageName = app.packageName)
            )

            if (versions.isNotEmpty()) {
                versions.map {
                    HistoryVersion(
                        versionName = it.versionName,
                        apkPath = it.apkPath
                    )
                }
            } else {
                buildFakeHistory(app)
            }
        } catch (e: Exception) {
            buildFakeHistory(app)
        }
    }

    private fun buildFakeHistory(app: AppInfo): List<HistoryVersion> {
        return listOf(
            HistoryVersion("1.0.2", "/sdcard/apks/${app.packageName}_102.apk"),
            HistoryVersion("1.0.1", "/sdcard/apks/${app.packageName}_101.apk"),
            HistoryVersion("1.0.0", "/sdcard/apks/${app.packageName}_100.apk")
        )
    }

    fun installHistoryVersion(packageName: String, historyVersion: HistoryVersion) {
        coroutineScope.launch {
            val realApkPath = resolveDownloadApkPath(
                packageName = packageName,
                fallbackApkPath = historyVersion.apkPath,
                versionName = historyVersion.versionName
            )
            XcServiceManager.installApk(realApkPath, packageName, true)
        }
    }

    fun resumeAllPausedDownloads() {
        val pausedApps = synchronized(stateLock) {
            localDownloadQueue.filter { it.downloadStatus == DownloadStatus.PAUSED }
        }
        pausedApps.forEach {
            toggleDownload(it)
        }
    }

    fun checkAppUpdate() {
        coroutineScope.launch {
            delay(800L)
            val current = _appVersion.value?.removePrefix("V") ?: "1.0.0"
            val latestFromServer = "1.0.1"
            val status = if (latestFromServer == current) UpdateStatus.LATEST else UpdateStatus.NEW_VERSION(latestFromServer)
            _checkUpdateResult.postValue(status)
        }
    }

    fun clearUpdateResult() {
        _checkUpdateResult.postValue(null)
    }
}
