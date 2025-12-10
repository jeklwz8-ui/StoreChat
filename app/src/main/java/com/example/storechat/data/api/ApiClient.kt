package com.example.storechat.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit客户端单例
 * 负责创建和配置Retrofit实例，包括HTTP客户端、日志拦截器和签名拦截器
 * 
 * 架构设计：
 * 1. 单例模式：确保全局只有一个ApiClient实例，节省资源
 * 2. 懒加载：各组件按需初始化，提升应用启动速度
 * 3. 职责分离：分别配置OkHttpClient、Retrofit和API服务
 * 
 * 核心组件：
 * 1. BASE_URL: API基础地址
 * 2. loggingInterceptor: HTTP请求日志拦截器
 * 3. okHttpClient: 配置了签名和日志拦截器的OkHttpClient
 * 4. appApi: AppApiService的实例，提供API访问接口
 * 
 * 使用方式：
 * 直接调用ApiClient.appApi即可获取API服务实例
 * 例如：ApiClient.appApi.getAppList(request)
 */
object ApiClient {

    // 换成测试环境域名，注意以 / 结尾
    /**
     * API基础URL地址
     * 所有API端点都将基于此URL构建
     * 
     * 注意事项：
     * 1. 必须以/结尾
     * 2. 应根据环境（开发/测试/生产）进行配置
     * 3. 更换服务器地址时只需修改此处
     */
    private const val BASE_URL = "https://test.yannuozhineng.com/acms/api/"

    /**
     * HTTP日志拦截器（懒加载）
     * 用于记录HTTP请求和响应的详细信息
     * 
     * 功能说明：
     * 1. 开发调试时可以查看请求和响应详情
     * 2. 帮助定位网络请求相关的问题
     * 3. Level.BODY会打印完整的请求和响应内容
     * 
     * 日志级别说明：
     * - NONE: 不记录任何日志
     * - BASIC: 仅记录请求行和响应行
     * - HEADERS: 记录基本信息和请求头/响应头
     * - BODY: 记录所有信息包括请求体和响应体
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            // 调试阶段打印请求/响应日志，方便排查问题
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * OkHttp客户端（懒加载）
     * 配置了签名拦截器和日志拦截器
     * 
     * 拦截器链执行顺序：
     * 1. SignInterceptor: 对请求进行签名处理
     * 2. loggingInterceptor: 记录请求和响应日志
     * 
     * 优势：
     * 1. 自动签名：所有符合条件的请求都会自动签名
     * 2. 透明处理：业务代码无需关心签名细节
     * 3. 统一配置：集中管理网络请求相关配置
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 添加签名拦截器，用于自动签名POST请求
            .addInterceptor(SignInterceptor())
            // 添加日志拦截器，用于调试
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * AppApiService实例（懒加载）
     * 提供对所有API端点的访问
     * 
     * 技术栈：
     * 1. Retrofit: 类型安全的HTTP客户端
     * 2. Gson: JSON序列化/反序列化库
     * 3. OkHttp: 底层HTTP客户端实现
     * 
     * 使用方式：
     * val apiService = ApiClient.appApi
     * val response = apiService.getAppList(request)
     */
    val appApi: AppApiService by lazy {
        Retrofit.Builder()
            // 设置基础URL
            .baseUrl(BASE_URL)
            // 设置OkHttpClient
            .client(okHttpClient)
            // 添加Gson转换器用于JSON序列化/反序列化
            .addConverterFactory(GsonConverterFactory.create())
            // 构建Retrofit实例
            .build()
            // 创建API服务实例
            .create(AppApiService::class.java)
    }
}