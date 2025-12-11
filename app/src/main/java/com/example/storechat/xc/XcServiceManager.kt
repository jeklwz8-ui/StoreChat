package com.example.storechat.xc

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.proembed.service.MyService
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * XcServiceManager 保留原来的静默安装能力，并新增一个
 * suspend fun downloadAndInstall(...) 方法，用于：
 *   - 下载远程 APK 到 app 的 external files dir
 *   - 下载过程中回调进度 percent (0..100)
 *   - 下载完成后调用 MyService.silentInstallApk(localPath,...)
 *
 * 这样 AppRepository 只需要把后端返回的 fileUrl 交给这个方法即可。
 */
object XcServiceManager {

    @Volatile
    private var service: MyService? = null
    private lateinit var appContext: Context

    // 用于下载/安装的协程作用域（内部使用）
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        appContext = context.applicationContext
        coroutineScope.launch {
            if (service == null) {
                service = MyService(appContext)
            }
        }
    }

    /**
     * 异步静默安装 APK（保持原样，兼容现有调用）
     */
    fun installApk(
        apkPath: String,
        packageName: String,
        openAfter: Boolean
    ) {
        coroutineScope.launch {
            val s = service
            if (s == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "服务正在初始化，请稍后重试", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            s.silentInstallApk(apkPath, packageName, openAfter)
        }
    }

    /**
     * 异步静默卸载 APK（保持原样）
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
            s.silentUnInstallApk(packageName)
        }
    }

    /**
     * 新增：下载远程 APK，并在完成后调用静默安装。
     *
     * @param apkUrl 远程文件 URL（由后端返回的 fileUrl）
     * @param packageName 用于通知 / 安装时的包名
     * @param openAfter 安装完成后是否打开
     * @param progressCallback 回调下载进度 0..100（content-length 未返回时，会尽量发送 -1/100）
     *
     * 返回：true 表示下载 + 安装流程已触发（并且没有显式报错），false 表示失败。
     *
     * 注意：该方法会在 IO dispatcher 执行，支持协程取消（会抛出 CancellationException）。
     */
    suspend fun downloadAndInstall(
        apkUrl: String,
        packageName: String,
        openAfter: Boolean,
        progressCallback: ((Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val req = Request.Builder().url(apkUrl).get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "下载失败: HTTP ${resp.code}", Toast.LENGTH_SHORT).show()
                }
                resp.close()
                return@withContext false
            }

            val body = resp.body ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "下载失败：响应体为空", Toast.LENGTH_SHORT).show()
                }
                resp.close()
                return@withContext false
            }

            val contentLength = body.contentLength() // 可能为 -1
            val input: InputStream = body.byteStream()

            // 目标文件：/Android/data/<package>/files/Download/<pkg>_timestamp.apk
            val downloadsDir: File? = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = downloadsDir ?: appContext.filesDir
            if (!targetDir.exists()) targetDir.mkdirs()
            val targetFile = File(targetDir, "${packageName}_${System.currentTimeMillis()}.apk")

            BufferedOutputStream(FileOutputStream(targetFile)).use { out ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                var totalRead = 0L
                while (true) {
                    // 支持协程取消
                    if (!coroutineContext.isActive) {
                        throw CancellationException("下载已取消")
                    }
                    read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    totalRead += read
                    if (contentLength > 0) {
                        val percent = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        try {
                            progressCallback?.invoke(percent)
                        } catch (_: Exception) { /* 忽略回调异常 */ }
                    } else {
                        // contentLength 未知时，尝试发送 0/100 或根据已下载大小通知
                        try {
                            val approx = if (totalRead == 0L) 0 else 50
                            progressCallback?.invoke(approx)
                        } catch (_: Exception) {}
                    }
                }
                out.flush()
            }
            resp.close()

            // 下载完成，触发静默安装（MyService）
            val s = service
            if (s == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "安装服务未就绪，请稍后", Toast.LENGTH_SHORT).show()
                }
                // 仍然返回 false，调用方可以选择重试
                return@withContext false
            }

            // 切到 IO 线程执行安装（silentInstallApk 本身可能是阻塞）
            withContext(Dispatchers.IO) {
                s.silentInstallApk(targetFile.absolutePath, packageName, openAfter)
            }

            // 成功
            return@withContext true
        } catch (ce: CancellationException) {
            // 下载被取消：删除未完成文件（如果存在）
            // 注意：CancellationException 代表用户主动取消
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, "下载或安装发生异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }
}
