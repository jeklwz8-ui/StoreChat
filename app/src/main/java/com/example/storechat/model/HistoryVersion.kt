package com.example.storechat.model

/**
 * 表示一个历史版本的数据类
 * 用于在应用内部表示和传递历史版本信息
 *
 * 设计说明：
 * 这个类是对服务器返回的AppVersionHistoryItem的简化和转换，
 * 只保留了应用内需要的关键信息
 *
 * @param versionName 版本名称，例如 "1.0.2"，用于显示和版本比较
 * @param apkPath 对应的 APK 文件路径或下载链接，用于下载安装
 */
data class HistoryVersion(
    /**
     * 版本名称
     * 用户可见的版本标识，如"1.0.0"、"2.1.3"等
     * 用于在UI中显示版本信息和进行版本比较
     */
    val versionName: String,
    
    /**
     * APK文件路径或下载链接
     * 可以是本地文件路径或网络下载链接
     * 用于下载和安装该历史版本
     */
    val apkPath: String
)