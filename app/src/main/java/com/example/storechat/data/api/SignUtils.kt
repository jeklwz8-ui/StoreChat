package com.example.storechat.data.api

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 签名工具类
 * 提供与签名相关的工具方法，包括时间戳生成、随机数生成和HMAC-SHA256加密
 *
 * HMAC-SHA256 是一种基于哈希的消息认证码（Hash-based Message Authentication Code）算法，
 * 它结合了SHA-256哈希函数和一个密钥，用于验证消息的完整性和真实性
 */
object SignUtils {

    /**
     * 生成毫秒级时间戳（13 位）
     * 用于防止重放攻击（Replay Attack）
     * 服务器会验证时间戳的有效性，通常只接受一定时间范围内的请求
     *
     * @return 当前时间的毫秒表示字符串
     */
    fun generateTimestampMillis(): String = System.currentTimeMillis().toString()

    /**
     * 生成指定长度的随机字符串（nonce）
     * 用于防止请求重复提交，确保每个请求都有唯一的标识
     * nonce（number used once）是一次性随机数
     *
     * @param length 随机字符串长度，默认32位
     * @return 指定长度的随机字符串
     */
    fun generateNonce(length: Int = 32): String {
        // 定义字符集，包含数字和大小写字母，共62个字符
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        // 使用密码学安全的随机数生成器
        val random = SecureRandom()
        // 创建StringBuilder用于高效构建字符串
        val sb = StringBuilder(length)
        // 重复指定次数，每次从字符集中随机选择一个字符
        repeat(length) {
            // 生成0到chars.length-1之间的随机索引
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    /**
     * 使用HMAC-SHA256算法对数据进行签名，并返回十六进制字符串
     * HMAC（Hash-based Message Authentication Code）是一种通过特别计算方式来证实消息完整性和真实性的方法
     * SHA-256是SHA-2家族中的一种哈希函数，输出256位摘要
     *
     * @param data 待签名的数据字符串
     * @param secret 用于签名的密钥
     * @return HMAC-SHA256签名结果的十六进制字符串表示
     */
    fun hmacSha256Hex(data: String, secret: String): String {
        // 获取HmacSHA256算法的Mac实例
        // Mac（Message Authentication Code）是用于生成消息认证码的类
        val mac = Mac.getInstance("HmacSHA256")
        // 将密钥转换为UTF-8字节数组，并创建SecretKeySpec对象
        // SecretKeySpec是密钥的标准化实现
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        // 使用密钥初始化Mac对象
        mac.init(secretKeySpec)

        // 对输入数据执行HMAC运算，得到字节数组结果
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        // 创建StringBuilder用于构建十六进制字符串，预分配空间提升性能
        val sb = StringBuilder(bytes.size * 2)

        // 遍历每个字节，将其转换为两位的十六进制字符串
        for (b in bytes) {
            // 将byte转换为无符号整数（0-255）
            val i = b.toInt() and 0xff
            // 如果数值小于16，则在前面补0，确保始终是两位十六进制数
            if (i < 0x10) sb.append('0')
            // 将数值转换为十六进制字符串并追加到结果中
            sb.append(i.toString(16))
        }
        // 返回最终的十六进制字符串
        return sb.toString()
    }
}