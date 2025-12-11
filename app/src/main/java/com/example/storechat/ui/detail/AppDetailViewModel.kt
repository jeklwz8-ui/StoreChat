package com.example.storechat.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.storechat.data.AppRepository
import com.example.storechat.model.AppInfo
import com.example.storechat.model.HistoryVersion
import kotlinx.coroutines.launch

/**
 * 应用详情视图模型
 * 负责管理应用详情页面的数据逻辑
 * 
 * 主要功能：
 * 1. 获取并监听指定应用的信息变化
 * 2. 加载并管理应用的历史版本列表
 * 3. 协调Repository层和UI层之间的数据交互
 */
class AppDetailViewModel : ViewModel() {

    /**
     * 应用包名的LiveData
     * 用于标识当前正在查看详情的应用
     */
    private val _packageName = MutableLiveData<String>()

    /**
     * 应用信息的LiveData
     * 根据包名从AppRepository中获取对应的应用信息
     * 当AppRepository中的应用列表发生变化时，会自动更新
     */
    val appInfo: LiveData<AppInfo> = _packageName.switchMap { packageName ->
        val result = MediatorLiveData<AppInfo>()
        result.addSource(AppRepository.allApps) { apps ->
            apps.find { it.packageName == packageName }?.let { foundApp ->
                if (result.value != foundApp) {
                    result.value = foundApp
                }
            }
        }
        result
    }

    /**
     * 历史版本列表的LiveData
     * 用于在UI中观察和显示应用的历史版本信息
     */
    private val _historyVersions = MutableLiveData<List<HistoryVersion>>()
    
    /**
     * 对外暴露的不可变历史版本列表LiveData
     * UI可以通过观察这个LiveData来获取历史版本列表的更新
     */
    val historyVersions: LiveData<List<HistoryVersion>> = _historyVersions

    /**
     * 加载指定应用的信息
     * 设置需要查看详情的应用包名，触发appInfo的更新
     * 
     * @param packageName 应用包名
     */
    fun loadApp(packageName: String) {
        // 只有当包名发生变化时才更新，避免不必要的刷新
        if (_packageName.value != packageName) {
            _packageName.value = packageName
        }
    }

    /**
     * 加载指定应用的历史版本列表
     * 通过AppRepository从服务器获取历史版本数据并在UI中显示
     * 
     * 实现流程：
     * 1. 在viewModelScope中启动协程执行网络请求
     * 2. 调用AppRepository.loadHistoryVersions获取历史版本数据
     * 3. 将获取到的数据发布到_historyVersions中供UI观察
     * 
     * @param app 需要加载历史版本的应用信息
     */
    fun loadHistoryFor(app: AppInfo) {
        // 在viewModelScope中启动协程执行网络请求
        // viewModelScope会自动管理协程生命周期，避免内存泄漏
        viewModelScope.launch {
            // 调用AppRepository.loadHistoryVersions获取历史版本数据
            // 这是一个挂起函数，会在后台线程执行网络请求
            val history = AppRepository.loadHistoryVersions(app)
            
            // 将获取到的数据发布到_historyVersions中供UI观察
            // 使用postValue而不是value，确保在后台线程也能安全更新LiveData
            _historyVersions.postValue(history)
        }

        // 在真实项目中，这里应该从数据库或网络加载
//        val fakeHistory = listOf(
//            HistoryVersion("1.0.2", "/sdcard/apks/${app.packageName}_102.apk"),
//            HistoryVersion("1.0.1", "/sdcard/apks/${app.packageName}_101.apk"),
//            HistoryVersion("1.0.0", "/sdcard/apks/${app.packageName}_100.apk")
//        )
//        _historyVersions.value = fakeHistory
    }
}