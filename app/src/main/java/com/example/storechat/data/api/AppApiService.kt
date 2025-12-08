package com.example.storechat.data.api

import com.example.storechat.model.AppInfo
import com.example.storechat.model.VersionInfo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * 后端接口定义占位，方便后续根据文档调整。
 * 目前字段和路径只是示例，可以在接入真实接口时修改。
 */

// b. 下载链接响应模型（占位）
data class DownloadLinkResponse(
    val url: String,          // 有效期下载链接
    val expireTime: Long? = null  // 过期时间（时间戳，可选）
)

// 业务层真正要传的“data”内容（拦截器会把它包起来）
data class AppListRequest(val category: String)

data class AppHistoryRequest(val packageName: String)

data class CheckUpdateRequest(
    val packageName: String,
    val currentVer: String
)

data class DownloadLinkRequest(
    val packageName: String,
    val versionName: String? = null
)



interface AppApiService {

    // a. 应用列表接口
    @POST("api/apps")
    suspend fun getAppList(
        @Body body: AppListRequest
    ): List<AppInfo>

    // c. 应用历史版本列表接口
    @POST("api/app/history")
    suspend fun getAppHistory(
        @Body body: AppHistoryRequest
    ): List<VersionInfo>

    // 检查更新
    @POST("api/app/check_update")
    suspend fun checkUpdate(
        @Body body: CheckUpdateRequest
    ): VersionInfo?

    // b. 下载链接获取接口
    @POST("api/app/download_link")
    suspend fun getDownloadLink(
        @Body body: DownloadLinkRequest
    ): DownloadLinkResponse
}


 /**   // a. 应用列表接口：支持按分类查询
    // 示例：/api/apps?category=1
    @GET("api/apps")
    suspend fun getAppList(
        @Query("category") category: String
    ): List<AppInfo>

    // c. 应用历史版本列表接口
    // 示例：/api/app/history?packageName=com.demo.app
    @GET("api/app/history")
    suspend fun getAppHistory(
        @Query("packageName") packageName: String
    ): List<VersionInfo>

    // 检查更新（你后面如需接后端检查更新，可直接用这个）
    @GET("api/app/check_update")
    suspend fun checkUpdate(
        @Query("packageName") packageName: String,
        @Query("currentVer") version: String
    ): VersionInfo?

    // b. 下载链接获取接口：后台生成有效期下载链接
    // 示例：/api/app/download_link?packageName=xxx&versionName=1.0.2
    @GET("api/app/download_link")
    suspend fun getDownloadLink(
        @Query("packageName") packageName: String,
        @Query("versionName") versionName: String? = null  // null 表示最新版本
    ): DownloadLinkResponse
}





//interface AppApiService {
//
//    // 获取首页应用列表
//    // 假设后端接口为 /api/apps?category=1
//    @GET("api/apps")
//    suspend fun getAppList(@Query("category") category: String): List<AppInfo>
//
//    // 获取指定应用的详情/历史版本
//    @GET("api/app/history")
//    suspend fun getAppHistory(@Query("packageName") packageName: String): List<VersionInfo>
//
//    // 检查更新
//    @GET("api/app/check_update")
//    suspend fun checkUpdate(@Query("packageName") packageName: String, @Query("currentVer") version: String): VersionInfo?
//}
**/