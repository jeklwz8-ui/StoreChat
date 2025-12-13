package com.example.storechat.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.storechat.data.api.*
import com.example.storechat.model.*
import com.example.storechat.util.AppPackageNameCache
import com.example.storechat.util.AppUtils
import com.example.storechat.xc.XcServiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object AppRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * ✅ 方案B：下载任务按 taskKey 管理
     * taskKey = "packageName@versionId"
     */
    private val downloadJobs = ConcurrentHashMap<String, Job>()


    private val refreshJobs = ConcurrentHashMap<Int, Job>()
    private val lastFetchAt = ConcurrentHashMap<Int, Long>()
    private const val MIN_FETCH_INTERVAL_MS = 1500L

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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _selectedCategory = MutableLiveData(AppCategory.YANNUO)

    val categorizedApps: LiveData<List<AppInfo>> = MediatorLiveData<List<AppInfo>>().apply {
        addSource(allApps) { apps ->
            value = apps.filter { it.category == _selectedCategory.value }
        }
        addSource(_selectedCategory) { category ->
            value = allApps.value?.filter { it.category == category }
        }
    }

    // ----------------------------
    // ✅ taskKey helpers
    // ----------------------------

    private fun taskKey(packageName: String, versionId: Long): String = "$packageName@$versionId"

    private fun taskKey(app: AppInfo): String? {
        val vid = app.versionId ?: return null
        return taskKey(app.packageName, vid)
    }

    // ----------------------------
    // ✅ 状态更新（主列表按 packageName，队列按 taskKey）
    // ----------------------------

    private fun updateAppStatus(
        packageName: String,
        versionId: Long?,
        transform: (AppInfo) -> AppInfo
    ) {
        synchronized(stateLock) {
            val key = versionId?.let { taskKey(packageName, it) }

            // 1) 主列表（一个包只保留一个条目）
            val master = localAllApps.find { it.packageName == packageName }
            val newMaster = master?.let(transform)

            // 2) 队列（同包可多个版本）
            val queueItem = key?.let { k -> localDownloadQueue.find { taskKey(it) == k } }
            val newQueueItem = queueItem?.let(transform)

            var allAppsChanged = false
            var queueChanged = false

            if (newMaster != null) {
                localAllApps = localAllApps.map { if (it.packageName == packageName) newMaster else it }
                allAppsChanged = true
            }

            if (key != null && newQueueItem != null) {
                localDownloadQueue = localDownloadQueue.map { if (taskKey(it) == key) newQueueItem else it }
                queueChanged = true
            }

            if (queueChanged) _downloadQueue.postValue(localDownloadQueue)
            if (allAppsChanged) _allApps.postValue(localAllApps)
        }
    }

    private fun addToDownloadQueue(app: AppInfo) {
        val key = taskKey(app) ?: return
        synchronized(stateLock) {
            if (localDownloadQueue.none { taskKey(it) == key }) {
                localDownloadQueue = localDownloadQueue + app
                _downloadQueue.postValue(localDownloadQueue)
            }
        }
    }

    private fun removeFromDownloadQueue(taskKey: String) {
        synchronized(stateLock) {
            localDownloadQueue = localDownloadQueue.filterNot { taskKey(it) == taskKey }
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

    fun initialize(context: Context) {
        requestAppList(context, AppCategory.YANNUO)
    }

    fun selectCategory(context: Context, category: AppCategory) {
        _selectedCategory.postValue(category)
        requestAppList(context, category)
    }

    private fun requestAppList(context: Context, category: AppCategory) {
        val key = category.id
        val now = System.currentTimeMillis()

        // ✅ 1. 节流：1.5 秒内重复触发直接忽略
        val last = lastFetchAt[key] ?: 0L
        if (now - last < MIN_FETCH_INTERVAL_MS) return
        lastFetchAt[key] = now

        // ✅ 2. 单飞：该分类请求还在跑，就不再发第二次
        if (refreshJobs[key]?.isActive == true) return

        refreshJobs[key] = coroutineScope.launch {
            refreshAppsFromServer(context, category)
        }
    }


    private suspend fun refreshAppsFromServer(context: Context, category: AppCategory?) {
        _isLoading.postValue(true)
        try {
            val response = apiService.getAppList(
                AppListRequestBody(appCategory = category?.id)
            )

            if (response.code == 200 && response.data != null) {
                val distinctList = response.data
                    .groupBy { it.appId }
                    .map { (_, apps) -> apps.maxByOrNull { it.id ?: -1 }!! }

                synchronized(stateLock) {
                    val localAppsMap = localAllApps.associateBy { it.appId }

                    val mergedRemoteList = distinctList.map { serverApp ->
                        val localApp = localAppsMap[serverApp.appId]

                        var realPackageName = AppPackageNameCache.getPackageNameByAppId(serverApp.appId)
                        if (realPackageName == null) {
                            realPackageName = AppPackageNameCache.getPackageNameByName(serverApp.productName)
                            if (realPackageName != null) {
                                AppPackageNameCache.saveMapping(serverApp.appId, serverApp.productName, realPackageName)
                            }
                        }

                        val finalPackageName = realPackageName ?: serverApp.appId
                        val installedVersionCode = AppUtils.getInstalledVersionCode(context, finalPackageName)
                        val isInstalled = installedVersionCode != -1L

                        val serverVersionCode = serverApp.versionCode?.toLongOrNull()

                        val installState = when {
                            !isInstalled -> InstallState.NOT_INSTALLED
                            serverVersionCode != null && serverVersionCode > installedVersionCode -> InstallState.INSTALLED_OLD
                            else -> InstallState.INSTALLED_LATEST
                        }

                        mapToAppInfo(serverApp, localApp).copy(
                            packageName = finalPackageName,
                            installState = installState,
                            isInstalled = isInstalled
                        )
                    }

                    val otherCategoryApps = localAllApps.filter { it.category != category }
                    localAllApps = otherCategoryApps + mergedRemoteList
                    _allApps.postValue(localAllApps)
                }
                _isLoading.postValue(false) // success 才关闭
            } else {
                _allApps.postValue(localAllApps.filter { it.category == category })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _allApps.postValue(localAllApps.filter { it.category == category })
        }
    }

    private fun mapToAppInfo(response: AppInfoResponse, localApp: AppInfo?): AppInfo {
        return AppInfo(
            name = response.productName,
            appId = response.appId,
            versionId = response.id?.toLong(),
            category = AppCategory.from(response.appCategory) ?: AppCategory.YANNUO,
            createTime = response.createTime,
            updateTime = response.updateTime,
            remark = response.remark,
            description = response.versionDesc ?: "",
            size = "N/A",
            downloadCount = 0,
            packageName = response.appId,
            apkPath = "",
            installState = localApp?.installState ?: InstallState.NOT_INSTALLED,
            versionName = response.version ?: "N/A",
            releaseDate = response.updateTime ?: response.createTime,
            downloadStatus = localApp?.downloadStatus ?: DownloadStatus.NONE,
            progress = localApp?.progress ?: 0
        )
    }

    private suspend fun resolveDownloadApkPath(appId: String, versionId: Long): String {
        return try {
            val response = apiService.getDownloadUrl(GetDownloadUrlRequest(appId = appId, id = versionId))
            if (response.code == 200 && response.data != null) {
                response.data.fileUrl
            } else {
                Log.e("DOWNLOAD", "API error getting download link: ${response.msg}")
                ""
            }
        } catch (e: Exception) {
            Log.e("DOWNLOAD", "Failed to resolve download URL for versionId: $versionId", e)
            ""
        }
    }

    /**
     * ✅ 方案B最关键修复点：
     * 不再用 app.downloadStatus 决定"暂停/继续"，而是用 downloadJobs.containsKey(taskKey)
     * 避免低版本详情页拿到的状态和真实 job 状态不一致，导致"一点就暂停"
     */
    fun toggleDownload(app: AppInfo) {
        val versionId = app.versionId
        if (versionId == null) {
            Log.e("DOWNLOAD", "Cannot download ${app.name}, versionId is null.")
            return
        }

        val key = taskKey(app.packageName, versionId)

        // ✅ 是否正在下载：只认 job
        val isDownloading = downloadJobs.containsKey(key)
        
        // ✅ 是否已暂停：检查状态
        val isPaused = app.downloadStatus == DownloadStatus.PAUSED

        if (isDownloading) {
            downloadJobs[key]?.cancel()
            return
        }
        
        // 如果是暂停状态，则恢复下载而不是重新开始
        if (isPaused) {
            // 更新状态为下载中，但保留现有进度
            updateAppStatus(app.packageName, versionId) {
                it.copy(downloadStatus = DownloadStatus.DOWNLOADING)
            }
        } else {
            // 开始新的下载
            addToDownloadQueue(app)

            updateAppStatus(app.packageName, versionId) {
                it.copy(downloadStatus = DownloadStatus.DOWNLOADING, progress = 0)
            }
        }

        val newJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
            try {
                val realApkPath = resolveDownloadApkPath(app.appId, versionId)
                if (realApkPath.isBlank()) {
                    throw IllegalStateException("Download URL is blank for versionId $versionId")
                }

                val installedPackageName = XcServiceManager.downloadAndInstall(
                    appId = app.appId,
                    versionId = versionId,
                    url = realApkPath,
                    onProgress = { percent ->
                        updateAppStatus(app.packageName, versionId) { current ->
                            current.copy(progress = percent)
                        }
                    }
                )

                if (installedPackageName != null) {
                    AppPackageNameCache.saveMapping(app.appId, app.name, installedPackageName)
                } else {
                    throw IllegalStateException("Download or install failed for ${app.name}")
                }

                val masterAppInfo = synchronized(stateLock) {
                    localAllApps.find { it.packageName == app.packageName }
                }
                val latestVersionId = masterAppInfo?.versionId ?: app.versionId

                val newInstallState = if (app.versionId < latestVersionId) {
                    InstallState.INSTALLED_OLD
                } else {
                    InstallState.INSTALLED_LATEST
                }

                updateAppStatus(app.packageName, versionId) {
                    it.copy(downloadStatus = DownloadStatus.NONE, progress = 0, installState = newInstallState)
                }

                addToRecentInstalled(app.copy(installState = newInstallState))

                downloadJobs.remove(key)
                removeFromDownloadQueue(key)

            } catch (e: CancellationException) {
                val isDeletion = cancellationsForDeletion.remove(key)
                if (!isDeletion) {
                    updateAppStatus(app.packageName, versionId) {
                        it.copy(downloadStatus = DownloadStatus.PAUSED)
                    }
                }
                downloadJobs.remove(key)

            } catch (e: Exception) {
                Log.e("DOWNLOAD", "Download/Install failed for ${app.name}", e)
                updateAppStatus(app.packageName, versionId) {
                    it.copy(downloadStatus = DownloadStatus.PAUSED)
                }
                downloadJobs.remove(key)
            }
        }

        downloadJobs[key] = newJob
        newJob.start()
    }

    fun installHistoryVersion(app: AppInfo, historyVersion: HistoryVersion) {
        val historyAppInfo = app.copy(
            versionId = historyVersion.versionId,
            versionName = historyVersion.versionName,
            downloadStatus = DownloadStatus.NONE,
            progress = 0
        )
        toggleDownload(historyAppInfo)
    }

    suspend fun loadHistoryVersions(context: Context, app: AppInfo): List<HistoryVersion> {
        return try {
            val installedVersionCode = AppUtils.getInstalledVersionCode(context, app.packageName)

            val response = apiService.getAppHistory(
                AppVersionHistoryRequest(appId = app.appId)
            )

            if (response.code == 200 && response.data != null) {
                response.data.map { versionItem ->
                    val state = if (versionItem.versionCode.toLongOrNull() == installedVersionCode) {
                        InstallState.INSTALLED_LATEST
                    } else {
                        InstallState.NOT_INSTALLED
                    }

                    HistoryVersion(
                        versionId = versionItem.id,
                        versionName = versionItem.version,
                        apkPath = "",
                        installState = state
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun removeDownload(app: AppInfo) {
        val versionId = app.versionId ?: return
        val key = taskKey(app.packageName, versionId)

        cancellationsForDeletion.add(key)
        downloadJobs[key]?.cancel()

        XcServiceManager.deleteDownloadedFile(app.appId, versionId)

        removeFromDownloadQueue(key)
        updateAppStatus(app.packageName, versionId) {
            it.copy(downloadStatus = DownloadStatus.NONE, progress = 0)
        }
    }

    fun resumeAllPausedDownloads() {
        val pausedApps = synchronized(stateLock) {
            localDownloadQueue.filter { it.downloadStatus == DownloadStatus.PAUSED }
        }
        pausedApps.forEach { toggleDownload(it) }
    }

    fun checkAppUpdate() {
        coroutineScope.launch {
            try {
                val appId = "32DQY9LH260HX43U"
                val currentVersion = _appVersion.value?.removePrefix("V") ?: "1.0.0"

                val latestVersionInfo = apiService.checkUpdate(
                    CheckUpdateRequest(
                        packageName = appId,
                        currentVer = currentVersion
                    )
                )

                if (latestVersionInfo != null && latestVersionInfo.versionName > currentVersion) {
                    _checkUpdateResult.postValue(UpdateStatus.NEW_VERSION(latestVersionInfo.versionName))
                } else {
                    _checkUpdateResult.postValue(UpdateStatus.LATEST)
                }
            } catch (e: Exception) {
                _checkUpdateResult.postValue(UpdateStatus.LATEST)
            }
        }
    }

    fun clearUpdateResult() {
        _checkUpdateResult.postValue(null)
    }
}
