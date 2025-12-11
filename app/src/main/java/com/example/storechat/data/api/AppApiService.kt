package com.example.storechat.data.api

import com.example.storechat.model.VersionInfo
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST


interface AppApiService {

    @POST("iotDeviceData/queryAppList")
    suspend fun getAppList(
        @Body body: AppListRequestBody
    ): ApiWrapper<List<AppInfoResponse>>

    @POST("iotDeviceData/queryAppVersionList")
    suspend fun getAppHistory(
        @Body body: AppVersionHistoryRequest
    ): AppVersionHistoryResponse

    @POST("iotDeviceData/queryAppVersionList")
    suspend fun checkUpdate(
        @Body body: CheckUpdateRequest
    ): VersionInfo?

    @POST("app/download_link")
    suspend fun getDownloadLink(
        @Body body: DownloadLinkRequest
    ): DownloadLinkResponse

    @POST("iotDeviceData/getDeviceMQTTInfo")
    suspend fun getMqttInfo(
        @Body body: MqttInitBizBody
    ): ApiWrapper<MqttInfo>
}

/**
 * 应用列表接口的请求体
 */
data class AppListRequestBody(
    val appId: String? = null,
    val appCategory: Int? = null
)

/**
 * 应用列表接口的响应数据项
 */
data class AppInfoResponse(
    val productName: String,
    val appId: String,
    val appCategory: Int,
    val id: Int?,
    val version: String?,
    val versionCode: String?,
    val versionDesc: String?,
    val status: Int?,
    val createTime: String,
    val updateTime: String?,
    val remark: String?
)

data class DownloadLinkResponse(
    val url: String,
    val expireTime: Long? = null
)

data class CheckUpdateRequest(
    @SerializedName("appId")
    val packageName: String,
    @SerializedName("version")
    val currentVer: String
)

data class DownloadLinkRequest(
    val packageName: String,
    val versionName: String? = null
)

data class MqttInitBizBody(
    val deviceId: String,
    val deviceName: String? = null,
    val appId: String? = null,
    val version: String? = null,
    val publicIp: String? = null,
    val cpuUsage: String? = null,
    val memoryUsage: String? = null,
    val storageUsage: String? = null,
    val remark: String? = null,
)

data class ApiWrapper<T>(
    val msg: String,
    val code: Int,
    val data: T?
)

data class MqttInfo(
    val username: String,
    val password: String,
    val url: String,
    val serverUrl: String,
    val serverPort: String,
    val topic: String,
    val emqxHttpApiUrl: String,
    val emqxHttpApiName: String,
    val emqxHttpApiPassword: String,
    val emqxHttpApiBaseUrl: String
)
