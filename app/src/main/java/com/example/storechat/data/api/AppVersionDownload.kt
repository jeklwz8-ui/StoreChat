package com.example.storechat.data.api

import com.google.gson.annotations.SerializedName

// --- Request ---

data class AppVersionDownloadRequest(
    @SerializedName("appId")
    val appId: String,
    @SerializedName("version")
    val version: String
)

// --- Response ---

data class AppVersionDownloadResponse(
    @SerializedName("msg")
    val msg: String,
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: AppVersionDownloadData?
)

data class AppVersionDownloadData(
    @SerializedName("appId")
    val appId: String,
    @SerializedName("fileUrl")
    val fileUrl: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("versionCode")
    val versionCode: String,
    @SerializedName("versionDesc")
    val versionDesc: String?,
    @SerializedName("status")
    val status: Int,
    @SerializedName("createTime")
    val createTime: String,
    @SerializedName("updateTime")
    val updateTime: String,
    @SerializedName("remark")
    val remark: String?
)
