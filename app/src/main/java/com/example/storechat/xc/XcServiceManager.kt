package com.example.storechat.xc

import android.content.Context
import android.widget.Toast
import com.proembed.service.MyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object XcServiceManager {

    @Volatile
    private var service: MyService? = null
    private lateinit var appContext: Context

    // 创建一个协程作用域，用于在后台执行耗时操作
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        appContext = context.applicationContext
        // 在后台线程启动服务初始化，避免阻塞主线程
        coroutineScope.launch {
            if (service == null) {
                // MyService() 的构造函数可能是耗时操作
                service = MyService(appContext)
            }
        }
    }

    /**
     * 异步静默安装 APK。
     * 此操作在后台线程执行。
     */
    fun installApk(
        apkPath: String,
        packageName: String,
        openAfter: Boolean
    ) {
        coroutineScope.launch {
            val s = service
            if (s == null) {
                // 切换到主线程显示 Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "服务正在初始化，请稍后重试", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            // silentInstallApk 本身也可能是耗时操作
            s.silentInstallApk(apkPath, packageName, openAfter)
        }
    }

    /**
     * 异步静默卸载 APK。
     * 此操作在后台线程执行。
     */
    fun uninstallApk(packageName: String) {
        coroutineScope.launch {
            val s = service
            if (s == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "服务正在初始化，请稍后重试", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            // silentUnInstallApk 本身也可能是耗时操作
            s.silentUnInstallApk(packageName)
        }
    }
}
