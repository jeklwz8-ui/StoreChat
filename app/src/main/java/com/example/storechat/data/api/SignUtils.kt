package com.example.storechat.data.api

import android.util.Log
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 与签名相关的工具方法
 */
object SignUtils {

    /**
     * 毫秒级时间戳（13 位）
     */
    fun generateTimestampMillis(): String = System.currentTimeMillis().toString()

    /**
     * 生成随机 nonce
     */
    fun generateNonce(length: Int = 32): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    // 已经验证过正确的 HMAC-SHA256
    fun hmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)

        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)

        for (b in bytes) {
            val i = b.toInt() and 0xff
            if (i < 0x10) sb.append('0')
            sb.append(i.toString(16))
        }
        return sb.toString()
    }

    /**
     * 动态版本的本地测试：跟拦截器逻辑保持完全一致
     */
    fun testSign(): String {
        val timestamp = generateTimestampMillis()
        val nonce = generateNonce()
        val deviceId = SignConfig.getDeviceId()

        // 注意字段顺序：按你和后端对签通过的顺序来写
        val dataString = "{\"deviceId\":\"$deviceId\"," +
                "\"deviceName\":\"智慧终端A1\"," +
                "\"appId\":\"X6AM8R3O675RBQEM\"," +
                "\"version\":\"1.0\"," +
                "\"publicIp\":\"112.45.90.12\"," +
                "\"cpuUsage\":\"20%\"," +
                "\"memoryUsage\":\"1.2GB/4GB\"," +
                "\"storageUsage\":\"32GB/64GB\"," +
                "\"remark\":\"测试设备 - 心跳正常\"}"

        val signString = "appId=${SignConfig.APP_ID}" +
                "&appSecret=${SignConfig.APP_SECRET}" +
                "&data=$dataString" +
                "&deviceId=$deviceId" +
                "&nonce=$nonce" +
                "&timestamp=$timestamp"

        val sign = hmacSha256Hex(signString, SignConfig.APP_SECRET)

        val requestJson = org.json.JSONObject().apply {
            put("appId", SignConfig.APP_ID)
            put("deviceId", deviceId)
            put("timestamp", timestamp)
            put("nonce", nonce)
            put("data", dataString)
            put("sign", sign)
        }.toString()

        Log.d("MqttSignTest", "dataString  = $dataString")
        Log.d("MqttSignTest", "timestamp   = $timestamp")
        Log.d("MqttSignTest", "nonce       = $nonce")
        Log.d("MqttSignTest", "signString  = $signString")
        Log.d("MqttSignTest", "sign(local) = $sign")
        Log.d("MqttSignTest", "requestJson = $requestJson")

        return requestJson
    }
}
