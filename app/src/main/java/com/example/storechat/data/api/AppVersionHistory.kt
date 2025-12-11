package com.example.storechat.data.api

import com.google.gson.annotations.SerializedName

// --- Request ---

/**
 * 应用历史版本查询请求数据类
 * 用于向服务器请求指定应用的历史版本信息
 * 
 * 请求参数说明：
 * - appId: 应用唯一标识符，必填
 * - version: 版本号，可选，用于查询特定版本的信息
 */
data class AppVersionHistoryRequest(
    /**
     * 应用ID
     * 用于标识需要查询历史版本的应用
     */
    @SerializedName("appId")
    val appId: String,
    
    /**
     * 版本号（可选）
     * 如果指定，则只查询该特定版本的信息
     * 如果为null，则查询该应用的所有历史版本
     */
    @SerializedName("version")
    val version: String? = null // Optional, for querying a specific version
)

// --- Response ---

/**
 * 应用历史版本查询响应数据类
 * 服务器返回的应用历史版本列表响应
 * 
 * 响应结构说明：
 * - msg: 响应消息，描述请求处理结果
 * - code: 状态码，200表示成功，其他值表示失败
 * - data: 历史版本数据列表，如果查询失败则为null
 */
data class AppVersionHistoryResponse(
    /**
     * 响应消息
     * 描述请求处理的结果信息，如"success"或错误描述
     */
    @SerializedName("msg")
    val msg: String,
    
    /**
     * 状态码
     * 用于标识请求处理的结果状态
     * 200: 成功
     * 其他值: 失败（具体含义由服务器定义）
     */
    @SerializedName("code")
    val code: Int,
    
    /**
     * 历史版本数据列表
     * 如果请求成功且存在历史版本，则包含版本信息列表
     * 如果请求失败或无历史版本，则为null
     */
    @SerializedName("data")
    val data: List<AppVersionHistoryItem>?
)

/**
 * 单个历史版本信息数据类
 * 包含某个应用特定版本的详细信息
 * 
 * 数据字段说明：
 * - appId: 应用ID
 * - fileUrl: APK文件下载地址
 * - version: 版本名称
 * - versionCode: 版本代码
 * - versionDesc: 版本描述
 * - status: 版本状态
 * - createTime: 创建时间
 * - updateTime: 更新时间
 * - remark: 备注信息
 */
data class AppVersionHistoryItem(
    /**
     * 应用ID
     * 标识该版本所属的应用
     */
    @SerializedName("appId")
    val appId: String,
    
    /**
     * APK文件下载URL
     * 用于下载该版本APK文件的完整URL地址
     * 可能为null，表示暂无下载地址
     */
    @SerializedName("fileUrl")
    val fileUrl: String?,
    
    /**
     * 版本名称
     * 用户可见的版本标识，如"1.0.0"、"2.1.3"等
     */
    @SerializedName("version")
    val version: String,
    
    /**
     * 版本代码
     * Android系统使用的整数版本号，用于版本比较
     */
    @SerializedName("versionCode")
    val versionCode: String,
    
    /**
     * 版本描述
     * 关于该版本功能变更、修复bug等的详细描述
     * 可能为null，表示无描述信息
     */
    @SerializedName("versionDesc")
    val versionDesc: String?,
    
    /**
     * 版本状态
     * 标识该版本的当前状态
     * 例如：0-正常，1-废弃，2-测试中等（具体含义由服务器定义）
     */
    @SerializedName("status")
    val status: Int,
    
    /**
     * 创建时间
     * 该版本记录的创建时间，ISO格式时间字符串
     */
    @SerializedName("createTime")
    val createTime: String,
    
    /**
     * 更新时间
     * 该版本记录的最后更新时间，ISO格式时间字符串
     */
    @SerializedName("updateTime")
    val updateTime: String,
    
    /**
     * 备注信息
     * 关于该版本的额外备注信息
     * 可能为null，表示无备注
     */
    @SerializedName("remark")
    val remark: String?
)