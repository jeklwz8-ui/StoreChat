package com.example.storechat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.storechat.ui.home.HomeFragment
import com.example.storechat.xc.XcServiceManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 XC 服务管理器
        XcServiceManager.init(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, HomeFragment())
                .commit()
        }

    }
}
