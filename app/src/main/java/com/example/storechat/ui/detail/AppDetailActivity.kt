package com.example.storechat.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.storechat.R
import com.example.storechat.data.AppRepository
import com.example.storechat.databinding.ActivityAppDetailBinding
import com.example.storechat.model.AppInfo
import com.example.storechat.ui.download.DownloadQueueActivity
import com.example.storechat.ui.search.SearchActivity
import com.google.android.material.tabs.TabLayoutMediator

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private val viewModel: AppDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_app_detail)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (packageName == null) {
            finish()
            return
        }

        viewModel.loadApp(packageName)

        setupViews()
        setupViewPagerAndTabs()
    }

    private fun setupViews() {
        // Toolbar
        binding.ivBack.setOnClickListener { finish() }
        binding.ivSearch.setOnClickListener { SearchActivity.start(this) }
        binding.ivDownload.setOnClickListener { DownloadQueueActivity.start(this) }

        // 为“安装”按钮和“进度条”布局都设置相同的点击事件
        val clickListener: (view: android.view.View) -> Unit = {
            viewModel.appInfo.value?.let { currentAppInfo ->
                AppRepository.toggleDownload(currentAppInfo)
            }
        }

        binding.btnInstall.setOnClickListener(clickListener)
        binding.layoutProgress.setOnClickListener(clickListener)
    }

    private fun setupViewPagerAndTabs() {
        binding.viewPager.adapter = AppDetailPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "最新版本"
                1 -> "历史版本"
                else -> ""
            }
        }.attach()

        //让Tab根据文字内容自适应宽度
//        binding.tabLayout.tabGravity = com.google.android.material.tabs.TabLayout.GRAVITY_FILL
        binding.tabLayout.tabMode = com.google.android.material.tabs.TabLayout.MODE_AUTO


    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun start(context: Context, app: AppInfo) {
            val intent = Intent(context, AppDetailActivity::class.java)
            intent.putExtra(EXTRA_PACKAGE_NAME, app.packageName)
            context.startActivity(intent)
        }
    }
}
