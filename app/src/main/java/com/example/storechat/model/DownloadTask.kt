package com.example.storechat.model

// 将所有下载状态统一在此处定义
enum class DownloadStatus {
    NONE,          // 未下载
    DOWNLOADING,   // 下载中
    PAUSED,        // 已暂停
    VERIFYING,     // 验证中
    INSTALLING     // 安装中
}

data class DownloadTask(
    val id: Long,               // 新增ID，用于列表更新
    val app: AppInfo,
    val speed: String,          // 当前速度，如 "1.8MB/s"
    val downloadedSize: String, // 已下大小，如 "1.8MB"
    val totalSize: String,      // 总大小，如 "83.00MB"
    val progress: Int,          // 进度百分比
    val status: DownloadStatus
) {

    val statusButtonText: String
        get() = when (status) {
            DownloadStatus.NONE -> "安装"
            DownloadStatus.DOWNLOADING -> "暂停"
            DownloadStatus.PAUSED -> "继续"
            DownloadStatus.VERIFYING -> "验证中"
            DownloadStatus.INSTALLING -> "安装中"
        }

    // 右侧胶囊里显示的文字
    val rightText: String
        get() = when (status) {
            DownloadStatus.NONE -> ""
            DownloadStatus.DOWNLOADING -> progressText
            DownloadStatus.PAUSED -> "继续"
            DownloadStatus.VERIFYING -> "验证中"
            DownloadStatus.INSTALLING -> "安装中"
        }

    val statusButtonEnabled: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED || status == DownloadStatus.NONE

    val progressText: String
        get() = "$progress%"
}
