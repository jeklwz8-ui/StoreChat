package com.example.storechat.model

import java.io.Serializable

enum class InstallState {
    NOT_INSTALLED,     // 未安装 -> 安装
    INSTALLED_OLD,     // 已安装有更新 -> 升级
    INSTALLED_LATEST   // 已最新 -> 打开
}

// DownloadStatus 现在是单一数据源，定义在 DownloadTask.kt 中

data class AppInfo(
    val name: String,
    val description: String?,
    val size: String,
    val downloadCount: Int,
    val packageName: String,
    val apkPath: String,
    val installState: InstallState,
    val versionName: String,      // 新增：版本名
    val releaseDate: String,      // 新增：发布日期
    val downloadStatus: DownloadStatus = DownloadStatus.NONE,
    val progress: Int = 0
) : Serializable {

    val buttonText: String
        get() = when (downloadStatus) {
            DownloadStatus.DOWNLOADING -> "暂停"
            DownloadStatus.PAUSED -> "继续"
            DownloadStatus.VERIFYING -> "验证中"
            DownloadStatus.INSTALLING -> "安装中"
            DownloadStatus.NONE -> when (installState) {
                InstallState.NOT_INSTALLED -> "安装"
                InstallState.INSTALLED_OLD -> "升级"
                InstallState.INSTALLED_LATEST -> "打开"
            }
        }

    val buttonEnabled: Boolean
        get() = when (downloadStatus) {
            DownloadStatus.VERIFYING, DownloadStatus.INSTALLING -> false
            DownloadStatus.NONE -> installState != InstallState.INSTALLED_LATEST
            else -> true // DOWNLOADING, PAUSED
        }

    val showProgress: Boolean
        get() = when(downloadStatus) {
            DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.VERIFYING, DownloadStatus.INSTALLING -> true
            DownloadStatus.NONE -> false
        }

    val showButton: Boolean
        get() = !showProgress

    val progressText: String
        get() = when (downloadStatus) {
            DownloadStatus.DOWNLOADING -> "$progress%"
            DownloadStatus.PAUSED -> "已暂停"
            DownloadStatus.VERIFYING -> "验证中..."
            DownloadStatus.INSTALLING -> "安装中..."
            DownloadStatus.NONE -> ""
        }
}
