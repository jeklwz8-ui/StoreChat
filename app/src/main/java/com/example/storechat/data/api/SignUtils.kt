package com.example.storechat.data.api

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignUtils {

    private const val HMAC_SHA256 = "HmacSHA256"

    /** 秒级时间戳 */
    fun generateTimestampSeconds(): Long = System.currentTimeMillis() / 1000

    /** 随机 nonce */
    fun generateNonce(length: Int = 16): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder()
        repeat(length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    /**
     * 规范化 JSON：key 按字典序排序，嵌套对象递归处理
     * 返回字符串用于参与签名 + 填到 data 字段
     */
    fun canonicalJson(rawJson: String): String {
        if (rawJson.isBlank()) return "{}"
        val json = JSONObject(rawJson)
        return canonicalJson(json)
    }

    private fun canonicalJson(json: JSONObject): String {
        val keys = json.keys().asSequence().toList().sorted()
        val result = JSONObject()
        for (key in keys) {
            val value = json.get(key)
            when (value) {
                is JSONObject -> result.put(key, JSONObject(canonicalJson(value)))
                is JSONArray -> result.put(key, canonicalJsonArray(value))
                else -> result.put(key, value)
            }
        }
        return result.toString()
    }

    private fun canonicalJsonArray(array: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until array.length()) {
            val v = array.get(i)
            when (v) {
                is JSONObject -> result.put(JSONObject(canonicalJson(v)))
                is JSONArray -> result.put(canonicalJsonArray(v))
                else -> result.put(v)
            }
        }
        return result
    }

    /** HMAC-SHA256 -> Hex */
    fun hmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256)
        mac.init(key)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun testSign(): String {
        val appId = "testAppId"
        val appSecret = "testSecret123"
        val deviceId = "device123"
        val timestamp = 1700000000L
        val nonce = "abc123xyz"

        // 原始业务 JSON 字符串
        val bizJsonString = """{"category":"1"}"""

        // 如果你写了 canonicalJson，就用它；没有就直接用上面这个字符串
        val canonicalData = canonicalJson(bizJsonString)

        val signString = "appId=$appId" +
                "&appSecret=$appSecret" +
                "&data=$canonicalData" +
                "&deviceId=$deviceId" +
                "&nonce=$nonce" +
                "&timestamp=$timestamp"

        val sign = hmacSha256Hex(signString, appSecret)

        android.util.Log.d("SignTest", "signString = $signString")
        android.util.Log.d("SignTest", "sign       = $sign")

        return sign
    }



}
