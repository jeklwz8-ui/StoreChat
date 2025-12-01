package com.example.storechat.model

data class VersionInfo(
    val versionName: String,   // 例如 "1.0.3"
    val versionCode: Int,
    val releaseDate: String,   // "2024-11-01"
    val size: String,          // "25MB"
    val apkPath: String        // 该版本 apk 的路径
)
