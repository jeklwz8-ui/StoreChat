package com.example.storechat.model

/**
 * 表示一个历史版本的数据类
 *
 * @param versionName 版本名称，例如 "1.0.2"
 * @param apkPath 对应的 APK 文件路径
 */
data class HistoryVersion(
    val versionName: String,
    val apkPath: String
)
