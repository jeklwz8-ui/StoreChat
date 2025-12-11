package com.example.storechat.model

/**
 * 表示一个历史版本的数据类
 *
 * @param versionId 版本ID, from server
 * @param versionName 版本名称，例如 "1.0.2"
 * @param apkPath 对应的 APK 文件路径 (临时使用 versionDesc)
 */
data class HistoryVersion(
    val versionId: Long,
    val versionName: String,
    val apkPath: String
)
