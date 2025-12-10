package com.example.storechat

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.storechat.data.api.ApiClient
import com.example.storechat.data.api.MqttInitBizBody
import com.example.storechat.data.api.SignConfig
import com.example.storechat.data.api.SignUtils
import com.example.storechat.data.mqtt.MqttManager
import com.example.storechat.ui.home.HomeFragment
import com.example.storechat.xc.XcServiceManager
import kotlinx.coroutines.launch
import me.jessyan.autosize.internal.CustomAdapt

class MainActivity : AppCompatActivity(), CustomAdapt {  //  实现 CustomAdapt

    private var drawerLayout: DrawerLayout? = null
    private lateinit var mqttManager: MqttManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mqttManager = MqttManager(this)

        // 1. 本地打印一套签名（可选，用于对签和调试）
        val signReqJson = SignUtils.testSign()
        Log.d("SignTest", "requestJson = $signReqJson")

        // 2. 调用“获取 MQTT 连接信息”接口（加签逻辑在 SignInterceptor 里自动完成）
        lifecycleScope.launch {
            try {
                val deviceId = SignConfig.getDeviceId()

                val bizBody = MqttInitBizBody(
                    deviceId = deviceId,
                    deviceName = "智慧终端A1",
                    appId = "X6AM8R3O675RBQEM",
                    version = "1.0",
                    publicIp = "112.45.90.12",
                    cpuUsage = "20%",
                    memoryUsage = "1.2GB/4GB",
                    storageUsage = "32GB/64GB",
                    remark = "测试设备 - 心跳正常"
                )

                val resp = ApiClient.appApi.getMqttInfo(bizBody)
                Log.d("MQTT-API", "code=${resp.code}, msg=${resp.msg}, data=${resp.data}")

                val mqttInfo = resp.data
                if (resp.code == 200 && mqttInfo != null) {
                    // 3. 拿到 MQTT 信息后连接并订阅 topic
                    mqttManager.connectAndSubscribe(mqttInfo) { topic, payload ->
                        Log.d("MQTT-MSG", "topic=$topic payload=$payload")
                        // TODO: 根据设备连接上报的数据更新 UI
                    }
                } else {
                    Log.e("MQTT-API", "获取 MQTT 信息失败 code=${resp.code} msg=${resp.msg}")
                }
            } catch (e: Exception) {
                Log.e("MQTT-API", "请求异常", e)
            }
        }

        // Initialize XC Service Manager
        XcServiceManager.init(this)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            drawerLayout = findViewById(R.id.drawerLayout)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, HomeFragment())
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }

    fun openDrawer() {
        drawerLayout?.openDrawer(GravityCompat.END)
    }

    // 竖屏：按宽度 411 适配；横屏：按高度 731 适配
    override fun isBaseOnWidth(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    override fun getSizeInDp(): Float {
        return if (isBaseOnWidth()) {
            411f   // 竖屏：设计稿宽度
        } else {
            500f   // 横屏：用竖屏的“高度”当基准，保证纵向比例正常
        }
    }
}
