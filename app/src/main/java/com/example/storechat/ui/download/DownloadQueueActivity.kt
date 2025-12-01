package com.example.storechat.ui.download

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storechat.databinding.ActivityDownloadQueueBinding
import com.example.storechat.model.AppInfo
import com.example.storechat.ui.detail.AppDetailActivity
import com.example.storechat.ui.search.SearchActivity

class DownloadQueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadQueueBinding
    private val viewModel: DownloadQueueViewModel by viewModels()

    private lateinit var recentAdapter: DownloadRecentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recentAdapter = DownloadRecentAdapter { app ->
            AppDetailActivity.start(this, app)
        }
        binding.recyclerRecent.apply {
            adapter = recentAdapter
            layoutManager = LinearLayoutManager(
                this@DownloadQueueActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.ivSearch.setOnClickListener { SearchActivity.start(this) }
        binding.btnBackHome.setOnClickListener { finish() }

        // 所有点击事件都直接委托给 ViewModel
        binding.tvStatus.setOnClickListener { viewModel.onStatusClick() }
        binding.tvAllResume.setOnClickListener { viewModel.resumeAllPausedTasks() }
        binding.tvAllPause.setOnClickListener { viewModel.pauseAllDownloadingTasks() }
    }

    private fun observeViewModel() {
        // activeTask 的 UI 更新已完全交由 DataBinding 处理，不再需要手动观察

        // 观察最近安装列表的变化
        viewModel.recentInstalled.observe(this) { list ->
            recentAdapter.submitList(list)
        }
        // 观察 Toast 事件
        viewModel.toastMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                // 消费事件，防止重复显示
                viewModel.onToastMessageShown()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadQueueActivity::class.java)
            context.startActivity(intent)
        }
    }
}
