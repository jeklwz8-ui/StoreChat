package com.example.storechat.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.storechat.databinding.ActivitySearchBinding
import com.example.storechat.model.AppInfo
import com.example.storechat.ui.detail.AppDetailActivity
import com.example.storechat.ui.home.AppListAdapter

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()

    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupViews()
        observeViewModel()

        val initialQuery = intent.getStringExtra(EXTRA_QUERY)
        if (!initialQuery.isNullOrEmpty()) {
            binding.etQuery.setText(initialQuery)
            viewModel.search(initialQuery)
        }
    }

    private fun setupViews() {
        adapter = AppListAdapter(
            onItemClick = { app -> openDetail(app) },
            onActionClick = { app -> viewModel.handleAppAction(app) }
        )
        binding.recyclerSearchResult.adapter = adapter

        // 为搜索图标添加点击事件 (移除不必要的空安全调用)
        binding.ivSearch.setOnClickListener { performSearch() }

        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.ivBack.setOnClickListener { finish() }

        binding.tvVersion?.setOnClickListener { viewModel.checkAppUpdate() }
    }

    private fun performSearch() {
        viewModel.search(binding.etQuery.text.toString())
    }

    private fun observeViewModel() {
        viewModel.result.observe(this) { list ->
            adapter.submitList(list)
            // 可以添加一个空状态的显示逻辑
            binding.tvVersion?.isVisible = list.isEmpty()
        }

        viewModel.checkUpdateResult.observe(this) { status ->
            when (status) {
                UpdateStatus.LATEST -> Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                UpdateStatus.NEW_VERSION -> showUpdateDialog()
                null -> {}
            }
            viewModel.clearUpdateResult()
        }

        viewModel.navigationEvent.observe(this) { packageName ->
            if (packageName != null) {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "无法打开应用", Toast.LENGTH_SHORT).show()
                }
                viewModel.onNavigationComplete()
            }
        }
    }

    private fun showUpdateDialog() {
        val currentVer = viewModel.appVersion.value ?: "V1.0.0"
        val latestVer = "V1.0.1"

        AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage("当前版本：$currentVer\n最新版本：$latestVer")
            .setNegativeButton("稍后") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("去更新") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "这里以后接入应用商店自更新逻辑", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun openDetail(app: AppInfo) {
        AppDetailActivity.start(this, app)
    }

    companion object {
        private const val EXTRA_QUERY = "extra_query"

        fun start(context: Context, query: String? = null) {
            val intent = Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_QUERY, query)
            }
            context.startActivity(intent)
        }
    }
}
