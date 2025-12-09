package com.example.storechat.data.api

object SignConfig {
    // 跟后台约好的 appId / appSecret
    const val APP_ID = "你的appId"
    const val APP_SECRET = "你的appSecret"

    // 简单写法：先写死一个设备号，后面你可以自己换成 ANDROID_ID / SN 等
    fun getDeviceId(): String {
        return "demo-device-no" // TODO: 换成真实设备号
    }
}
