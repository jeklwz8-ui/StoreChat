package com.example.storechat.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.storechat.data.api.ApiClient
import com.example.storechat.data.api.AppVersionHistoryRequest
import com.example.storechat.data.api.AppListRequest
import com.example.storechat.data.api.CheckUpdateRequest
import com.example.storechat.data.api.AppVersionDownloadRequest
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

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

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
        coroutineScope.launch {
            refreshAppsFromServer(AppCategory.YANNUO)
        }
    }

    private fun updateAppStatus(packageName: String, transform: (AppInfo) -> AppInfo) {
        synchronized(stateLock) {
            // Use packageName (which is now appId) as the key
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
        _isLoading.postValue(true)
        try {
            val response = apiService.getAppList(
                AppListRequest(appId = null, appCategory = category.id)
            )

            if (response.code == 200) {
                val remoteList = response.data
                synchronized(stateLock) {
                    val localAppsMap = localAllApps.associateBy { it.appId }

                    val mergedRemoteList = remoteList.map { serverApp ->
                        val localApp = localAppsMap[serverApp.appId]
                        val newAppInfo = AppInfo(
                                name = serverApp.productName,
                                appId = serverApp.appId,
                                category = AppCategory.from(serverApp.appCategory) ?: AppCategory.YANNUO,
                                createTime = serverApp.createTime,
                                updateTime = serverApp.updateTime,
                                remark = serverApp.remark,
                                description = serverApp.remark ?: "", // Use remark as description
                                size = "N/A", // Placeholder
                                downloadCount = 0, // Placeholder
                                packageName = serverApp.appId, // Use appId as a temporary substitute for packageName
                                apkPath = "", // Placeholder, critical for install
                                installState = localApp?.installState ?: InstallState.NOT_INSTALLED,
                                versionName = "1.0.0", // Placeholder
                                releaseDate = serverApp.createTime, // Use createTime as releaseDate
                                downloadStatus = localApp?.downloadStatus ?: DownloadStatus.NONE,
                                progress = localApp?.progress ?: 0
                        )
                        newAppInfo
                    }

                    val otherCategoryApps = localAllApps.filter { it.category != category }
                    localAllApps = otherCategoryApps + mergedRemoteList
                    _allApps.postValue(localAllApps)
                }
            }
        } catch (e: Exception) {
            // On network error or other issues, we keep the existing local data to be safe.
            e.printStackTrace()
        } finally {
            _isLoading.postValue(false)
        }
    }

    /**
     * 解析APK下载路径
     * 通过调用服务器API获取指定应用版本的真实下载链接
     * 
     * 实现流程：
     * 1. 构造下载链接查询请求
     * 2. 调用API服务获取下载链接
     * 3. 检查响应结果并提取下载链接
     * 4. 处理异常情况，返回备用路径
     * 
     * 设计考虑：
     * 1. 使用suspend函数支持协程调用
     * 2. 提供fallback机制确保在API调用失败时仍有备用方案
     * 3. 完善的异常处理保证方法稳定性
     * 
     * @param appId 应用ID，用于标识需要下载的应用
     * @param versionName 版本名称，用于指定需要下载的具体版本
     * @param fallbackApkPath 备用APK路径，当API调用失败时使用
     * @return 真实的APK下载链接或备用路径
     */
    private suspend fun resolveDownloadApkPath(
        /**
         * 应用ID
         * 用于标识需要下载APK的应用
         */
        appId: String, 
        
        /**
         * 版本名称
         * 用于指定需要下载的具体版本
         */
        versionName: String, 
        
        /**
         * 备用APK路径
         * 当API调用失败或返回无效数据时使用的备用路径
         */
        fallbackApkPath: String
    ): String {
        // 使用try-catch包围整个过程，确保异常不会向上传播
        return try {
            // 1. 调用API服务获取下载链接
            // 构造请求参数并发送请求到服务器
            val response = apiService.getDownloadLink(
                // 构造下载链接查询请求对象
                AppVersionDownloadRequest(
                    // 设置应用ID参数
                    appId = appId,
                    // 设置版本名称参数
                    version = versionName
                )
            )
            
            // 2. 检查响应结果并提取下载链接
            // 验证响应状态码是否为成功(200)
            // 确保响应数据不为null
            // 确保下载链接不为空白
            if (response.code == 200 && response.data != null && response.data.fileUrl.isNotBlank()) {
                // 如果所有条件都满足，返回服务器提供的真实下载链接
                response.data.fileUrl
            } else {
                // 如果任一条件不满足，使用备用APK路径
                fallbackApkPath
            }
        } catch (e: Exception) {
            // 3. 处理异常情况，返回备用路径
            // 捕获所有可能的异常（网络异常、解析异常等）
            // 在出现异常时，返回备用APK路径确保功能可用性
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
                            appId = app.appId,
                            versionName = app.versionName,
                            fallbackApkPath = app.apkPath
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

    /**
     * 加载指定应用的历史版本列表
     * 从服务器获取应用的历史版本信息，并转换为内部使用的数据格式
     * 
     * 实现流程：
     * 1. 调用API服务查询应用历史版本
     * 2. 检查响应状态码确认请求是否成功
     * 3. 将服务器返回的数据转换为内部使用的HistoryVersion对象
     * 4. 处理异常情况，确保方法不会抛出异常
     * 
     * 数据映射规则：
     * - versionName <- AppVersionHistoryItem.version
     * - apkPath <- AppVersionHistoryItem.versionDesc ?? AppVersionHistoryItem.remark ?? ""
     * 
     * @param app 需要查询历史版本的应用信息
     * @return 历史版本列表，如果查询失败则返回空列表
     */
    suspend fun loadHistoryVersions(app: AppInfo): List<HistoryVersion> {
        return try {
            // 1. 调用API服务查询应用历史版本
            // 构造请求参数，使用应用的appId进行查询
            val response = apiService.getAppHistory(
                AppVersionHistoryRequest(appId = app.appId)
            )
            
            // 2. 检查响应状态码确认请求是否成功
            // 只有状态码为200时才认为请求成功
            if (response.code == 200 && response.data != null) {
                // 3. 将服务器返回的数据转换为内部使用的HistoryVersion对象
                // 遍历服务器返回的版本列表，进行数据映射和转换
                response.data.map { versionItem ->
                    HistoryVersion(
                        // 版本名称直接使用服务器返回的version字段
                        versionName = versionItem.version, // Mapping from the new response field
                        
                        // APK路径优先使用versionDesc，如果没有则使用remark，都不存在则使用空字符串
                        // 这种设计是为了兼容不同情况下服务器返回的数据结构
                        apkPath = versionItem.versionDesc ?: versionItem.remark ?: "" // Use description or remark as a placeholder for path
                    )
                }
            } else {
                // 如果服务器返回错误状态码，则返回空列表
                emptyList()
            }
        } catch (e: Exception) {
            // 4. 处理异常情况，确保方法不会抛出异常
            // 捕获所有异常并打印堆栈跟踪，便于问题排查
            e.printStackTrace()
            // 发生异常时返回空列表，避免影响上层调用
            emptyList()
        }
    }

    /**
     * 安装指定的历史版本
     * 通过解析真实的APK下载路径并调用安装服务进行安装
     * 
     * 实现流程：
     * 1. 在后台协程中执行安装流程
     * 2. 解析真实的APK下载路径
     * 3. 调用XC服务管理器进行APK安装
     * 
     * @param packageName 应用包名
     * @param historyVersion 需要安装的历史版本信息
     */
    fun installHistoryVersion(packageName: String, historyVersion: HistoryVersion) {
        // 在后台协程中执行安装流程，避免阻塞主线程
        coroutineScope.launch {
            // 解析真实的APK下载路径
            // 通过resolveDownloadApkPath方法获取可以直接下载的APK链接
            val realApkPath = resolveDownloadApkPath(
                appId = packageName, // Assuming packageName is used as appId here
                versionName = historyVersion.versionName,
                fallbackApkPath = historyVersion.apkPath
            )
            
            // 调用XC服务管理器进行APK安装
            // 使用解析得到的真实APK路径进行安装
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
            try {
                // NOTE: We are using a hardcoded appId for now. 
                // This should be replaced with the actual app's ID.
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
                _checkUpdateResult.postValue(UpdateStatus.LATEST) // Fail silently
            }
        }
    }

    fun clearUpdateResult() {
        _checkUpdateResult.postValue(null)
    }
}
