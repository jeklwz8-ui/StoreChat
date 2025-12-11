package com.example.storechat.data.api

import com.google.gson.annotations.SerializedName

// --- Request ---

/**
 * 应用版本下载链接查询请求数据类
 * 用于向服务器请求指定应用特定版本的下载链接
 * 
 * 设计说明：
 * 这个类封装了获取APK下载链接所需的参数
 * 通过appId和version精确定位需要下载的APK版本
 */
data class AppVersionDownloadRequest(
    /**
     * 应用ID
     * 用于标识需要下载APK的应用
     * 这是服务器识别应用的唯一标识符
     */
    @SerializedName("appId")
    val appId: String,
    
    /**
     * 版本号
     * 用于指定需要下载的具体版本
     * 结合appId可以唯一定位一个APK文件
     */
    @SerializedName("version")
    val version: String
)

// --- Response ---

/**
 * 应用版本下载链接查询响应数据类
 * 服务器返回的下载链接查询结果
 * 
 * 响应结构说明：
 * - msg: 响应消息，描述请求处理结果
 * - code: 状态码，200表示成功，其他值表示失败
 * - data: 下载链接数据，如果查询失败则为null
 */
data class AppVersionDownloadResponse(
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
     * 下载链接数据
     * 如果请求成功且存在下载链接，则包含具体的下载信息
     * 如果请求失败或无下载链接，则为null
     */
    @SerializedName("data")
    val data: AppVersionDownloadData?
)

/**
 * 应用版本下载数据类
 * 包含某个应用特定版本的下载相关信息
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
data class AppVersionDownloadData(
    /**
     * 应用ID
     * 标识该下载链接所属的应用
     */
    @SerializedName("appId")
    val appId: String,
    
    /**
     * APK文件下载URL
     * 用于下载该版本APK文件的完整URL地址
     * 这是客户端真正需要的信息
     */
    @SerializedName("fileUrl")
    val fileUrl: String,
    
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