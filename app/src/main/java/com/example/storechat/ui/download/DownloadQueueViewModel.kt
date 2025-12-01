// 包声明，指定当前文件所属的包
package com.example.storechat.ui.download

// 导入必要的Android架构组件和项目相关类
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.storechat.data.AppRepository
import com.example.storechat.model.AppInfo
import com.example.storechat.model.DownloadStatus
import com.example.storechat.model.DownloadTask
import com.example.storechat.model.InstallState

/**
 * 下载队列界面的ViewModel类
 * 负责管理下载队列页面的数据逻辑
 */
class DownloadQueueViewModel : ViewModel() {

    // 活动任务的MediatorLiveData，用于观察和组合多个数据源
    private val _activeTask = MediatorLiveData<DownloadTask?>()
    // 公开的活动任务LiveData，供UI层观察
    val activeTask: LiveData<DownloadTask?> = _activeTask

    // 最近安装应用列表的MediatorLiveData
    private val _recentInstalled = MediatorLiveData<List<AppInfo>>()
    // 公开的最近安装应用列表LiveData，供UI层观察
    val recentInstalled: LiveData<List<AppInfo>> = _recentInstalled

    // 用于向Activity发送一次性Toast消息事件的MutableLiveData
    private val _toastMessage = MutableLiveData<String?>()
    // 公开的Toast消息LiveData，供UI层观察
    val toastMessage: LiveData<String?> = _toastMessage

    /**
     * 初始化代码块
     * 设置数据源观察和数据转换逻辑
     */
    init {
        // 获取所有应用数据源
        val allAppsSource = AppRepository.allApps

        // 为_activeTask添加数据源观察者，从中找出正在下载的应用
        _activeTask.addSource(allAppsSource) { apps ->
            // 查找第一个处于非NONE下载状态的应用
            val downloadingApp = apps.find { it.downloadStatus != DownloadStatus.NONE }
            // 如果找到了正在下载的应用
            if (downloadingApp != null) {
                // 创建DownloadTask对象并赋值给_activeTask
                _activeTask.value = DownloadTask(
                    app = downloadingApp,
                    speed = "1.3 MB/s",  // 模拟下载速度
                    downloadedSize = String.format("%.1fMB", downloadingApp.progress * 0.83),  // 计算已下载大小
                    totalSize = "83.0MB",  // 总大小
                    progress = downloadingApp.progress,  // 下载进度
                    status = downloadingApp.downloadStatus  // 下载状态
                )
            } else {
                // 如果没有找到正在下载的应用，将_activeTask设为null
                _activeTask.value = null
            }
        }

        // 为_recentInstalled添加数据源观察者，筛选出已安装最新版的应用
        _recentInstalled.addSource(allAppsSource) { apps ->
            // 过滤出安装状态为INSTALLED_LATEST的应用并赋值
            _recentInstalled.value = apps.filter { it.installState == InstallState.INSTALLED_LATEST }
        }
    }

    // --- 业务逻辑方法 --- //

    /**
     * 切换单个任务的下载状态
     * 当用户点击暂停/继续按钮时调用
     */
    fun onStatusClick() {
        // 获取当前活动任务对应的应用信息
        activeTask.value?.app?.let {
            // 调用Repository中的方法切换该应用的下载状态
            AppRepository.toggleDownload(it)
        }
    }

    /**
     * 恢复所有已暂停的任务
     * 当用户点击"全部开始"按钮时调用
     */
    fun resumeAllPausedTasks() {
        // 从Repository中获取所有应用，并筛选出下载状态为PAUSED的应用
        val pausedApps = AppRepository.allApps.value?.filter {
            it.downloadStatus == DownloadStatus.PAUSED
        } ?: emptyList()

        // 如果没有已暂停的任务
        if (pausedApps.isEmpty()) {
            // 发送提示消息
            _toastMessage.value = "没有已暂停的任务"
        } else {
            // 遍历所有已暂停的应用，逐一恢复下载
            pausedApps.forEach { AppRepository.toggleDownload(it) }
        }
    }

    /**
     * 暂停所有正在下载的任务
     * 当用户点击"全部暂停"按钮时调用
     */
    fun pauseAllDownloadingTasks() {
        // 从Repository中获取所有应用，并筛选出下载状态为DOWNLOADING的应用
        val downloadingApps = AppRepository.allApps.value?.filter {
            it.downloadStatus == DownloadStatus.DOWNLOADING
        } ?: emptyList()

        // 如果没有正在下载的任务
        if (downloadingApps.isEmpty()) {
            // 发送提示消息
            _toastMessage.value = "没有正在下载的任务"
        } else {
            // 遍历所有正在下载的应用，逐一暂停下载
            downloadingApps.forEach { AppRepository.toggleDownload(it) }
        }
    }

    /**
     * 当Toast消息被显示后，调用此方法来"消费"这个事件，防止重复显示
     */
    fun onToastMessageShown() {
        // 将toast消息置为null，表示事件已被处理
        _toastMessage.value = null
    }
}