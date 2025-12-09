package com.example.storechat.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.json.JSONObject
import java.util.UUID

class SignInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 只对 POST + JSON 做加签，其他直接放行（比如下载文件等）
        val body = originalRequest.body
        val contentType = body?.contentType()?.toString() ?: ""
        if (originalRequest.method != "POST" ||
            !contentType.startsWith("application/json")
        ) {
            return chain.proceed(originalRequest)
        }

        // 1. 读取原始业务 JSON
        val bizJsonString = bodyToString(body)

        // 2. 规范化 data
        val canonicalData = SignUtils.canonicalJson(bizJsonString)

        // 3. 生成签名所需参数
        val timestamp = SignUtils.generateTimestampSeconds()
        val nonce = SignUtils.generateNonce()
        val deviceId = SignConfig.getDeviceId()

        // 4. 按文档顺序拼接 signString
        val signString = "appId=${SignConfig.APP_ID}" +
                "&appSecret=${SignConfig.APP_SECRET}" +
                "&data=$canonicalData" +
                "&deviceId=$deviceId" +
                "&nonce=$nonce" +
                "&timestamp=$timestamp"

        // 5. HMAC-SHA256 生成 sign
        val sign = SignUtils.hmacSha256Hex(signString, SignConfig.APP_SECRET)

        // 6. 构造最终请求体 JSON
        val finalJson = JSONObject().apply {
            put("appId", SignConfig.APP_ID)
            put("deviceId", deviceId)
            put("timestamp", timestamp)
            put("nonce", nonce)
            put("data", canonicalData) // 注意：这里是字符串
            put("sign", sign)
        }

        val newBody: RequestBody = finalJson
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // 7. 生成 traceId，写入头
        val traceId = UUID.randomUUID().toString()

        val newRequest: Request = originalRequest.newBuilder()
            .header("Device-Traced-Id", traceId)
            .method(originalRequest.method, newBody)
            .build()

        return chain.proceed(newRequest)
    }

    private fun bodyToString(body: RequestBody?): String {
        if (body == null) return "{}"
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            "{}"
        }
    }
}
