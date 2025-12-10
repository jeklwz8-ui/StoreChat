package com.example.storechat.data.api

/**
 * 签名配置对象
 * 包含应用认证信息和设备标识配置
 * 
 * API签名机制的核心组成部分：
 * 1. APP_ID: 应用标识，用于服务端识别客户端身份
 * 2. APP_SECRET: 应用密钥，用于签名计算的核心密钥
 * 3. deviceId: 设备标识，用于服务端识别具体设备
 * 
 * 安全注意事项：
 * - APP_SECRET属于敏感信息，不应硬编码在生产环境中
 * - deviceId应当使用真实的设备标识而非固定值
 */
object SignConfig {

    // 用你提供的正式测试账号
    /**
     * 应用ID（Application Identifier）
     * 用于API身份验证的唯一标识符
     * 
     * 作用：
     * 1. 标识客户端身份，服务端据此判断是否允许访问
     * 2. 服务端根据APP_ID查找对应的APP_SECRET进行签名验证
     * 3. 可用于统计分析和访问控制
     */
    const val APP_ID = "32DQY9LH260HX43U"
    
    /**
     * 应用密钥（Application Secret）
     * 用于API请求签名的密钥
     * 
     * 作用：
     * 1. 作为HMAC算法的密钥，只有客户端和服务端知道
     * 2. 确保请求的真实性和完整性
     * 3. 防止未授权的第三方伪造请求
     * 
     * 安全性要求：
     * 1. 绝对不能泄露，否则签名机制失效
     * 2. 应定期更换以提高安全性
     * 3. 在生产环境中应使用更安全的方式存储和传递
     */
    const val APP_SECRET = "Ask1shvYpchfi0LH4O5Hj2RfBK7VsRDOKVKri5BY8JCx60qWDC"

    /**
     * 获取设备唯一标识
     * 
     * 设备标识的作用：
     * 1. 标识具体设备，可用于统计、限制设备数量等
     * 2. 服务端可以根据设备ID进行访问频率控制
     * 3. 有助于问题排查和日志追踪
     * 
     * 当前实现：
     * 现在先写死一个，方便你和后台查数据库、
     * 后面真上生产再改成 AndroidID / SN / UUID 等。
     * 
     * 推荐的生产环境实现方式：
     * 1. Android ID: Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
     * 2. Serial Number: Build.SERIAL (需要权限)
     * 3. 自定义UUID并持久化存储
     * 
     * @return 设备唯一标识字符串
     */
    fun getDeviceId(): String {
        // 返回固定的设备ID，实际项目中应改为动态获取
        return "DEV202501180001"
    }
}