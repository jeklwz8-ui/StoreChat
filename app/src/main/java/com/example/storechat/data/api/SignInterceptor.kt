package com.example.storechat.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/**
 * OkHttp请求签名拦截器
 * 自动对POST JSON请求进行封装和加签处理
 * 
 * 工作原理：
 * 1. 拦截所有通过OkHttp发出的请求
 * 2. 筛选出需要签名的POST JSON请求
 * 3. 提取原始请求数据并生成签名所需参数
 * 4. 按照约定格式组装签名字符串并计算签名
 * 5. 构造新的包含签名信息的请求体
 * 6. 发送签名后的请求
 * 
 * 签名参数组成：
 * - appId: 应用标识
 * - appSecret: 应用密钥（仅用于签名计算，不出现在最终请求中）
 * - data: 原始请求数据
 * - deviceId: 设备标识
 * - timestamp: 时间戳（毫秒级）
 * - nonce: 随机字符串
 * 
 * 签名安全机制：
 * 1. 防重放攻击：通过timestamp限制请求有效期
 * 2. 防重复提交：通过nonce确保每个请求唯一
 * 3. 数据完整性：通过签名确保数据未被篡改
 * 4. 身份认证：通过appId和appSecret确认客户端身份
 */
class SignInterceptor : Interceptor {

    /**
     * 拦截并处理网络请求
     * 
     * @param chain 请求处理链
     * @return 服务器响应
     * @throws IOException 网络IO异常
     */
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // 获取原始请求对象
        var request = chain.request()

        // 只处理POST + JSON请求
        // GET请求参数在URL中，签名方式不同；非JSON请求通常不需要签名
        val body = request.body
        if (request.method != "POST" || body == null || body.contentType()?.subtype != "json") {
            // 不符合条件的请求直接放行，不进行签名处理
            return chain.proceed(request)
        }

        // 1. 读取原始请求体内容
        // 创建Buffer用于临时存储请求体数据
        val buffer = Buffer()
        // 将原始请求体写入Buffer中
        body.writeTo(buffer)
        // 从Buffer中读取UTF-8编码的字符串形式的请求数据
        val dataString = buffer.readUtf8()

        // 2. 准备签名所需参数
        // 生成毫秒级时间戳，用于防止重放攻击
        val timestamp = SignUtils.generateTimestampMillis()
        // 生成随机nonce，用于防止请求重复提交
        val nonce = SignUtils.generateNonce()
        // 获取设备标识，用于服务端识别具体设备
        val deviceId = SignConfig.getDeviceId()

        // 3. 严格按照服务器要求的固定顺序拼接参数，并将appSecret包含在内
        // 注意：拼接顺序必须与服务端完全一致，否则验签会失败
        // 签名字符串格式为标准的查询参数格式（key=value&key=value）
        val signString = "appId=${SignConfig.APP_ID}" +
                "&appSecret=${SignConfig.APP_SECRET}" +
                "&data=$dataString" +
                "&deviceId=$deviceId" +
                "&nonce=$nonce" +
                "&timestamp=$timestamp"

        // 4. 使用appSecret作为密钥进行HmacSHA256加密生成签名
        // 这是整个签名机制的核心步骤，确保请求的真实性和完整性
        val sign = SignUtils.hmacSha256Hex(signString, SignConfig.APP_SECRET)

        // 5. 构建包含签名的新请求体JSON
        // 创建JSONObject对象，用于封装签名后的新请求体
        val newJsonBody = JSONObject().apply {
            // 添加应用ID，服务端用来识别客户端身份
            put("appId", SignConfig.APP_ID)
            // 添加设备ID，服务端用来识别具体设备
            put("deviceId", deviceId)
            // 添加时间戳，服务端用来防止重放攻击
            put("timestamp", timestamp)
            // 添加随机nonce，服务端用来防止请求重复
            put("nonce", nonce)
            // 添加原始数据，服务端需要处理的实际业务数据
            put("data", dataString)
            // 添加签名，服务端用来验证请求合法性
            put("sign", sign)
        }

        // 6. 创建新的RequestBody
        // 将JSON对象转换为字符串，并创建新的请求体
        // 设置Content-Type为application/json;charset=utf-8
        val newRequestBody = newJsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // 7. 用新的请求体和Header构建最终请求
        // 基于原始请求构建新的请求对象
        request = request.newBuilder()
            // 添加设备追踪ID头部，可用于服务端日志追踪和问题排查
            .header("Device-Traced-Id", UUID.randomUUID().toString())
            // 替换为新的签名后的请求体
            .post(newRequestBody)
            .build()

        // 继续执行请求链，发送签名后的请求
        return chain.proceed(request)
    }
}