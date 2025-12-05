package com.example.storechat.ui.download

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storechat.databinding.ActivityDownloadQueueBinding
import com.example.storechat.ui.detail.AppDetailActivity
import com.example.storechat.ui.search.SearchActivity

class DownloadQueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadQueueBinding
    private val viewModel: DownloadQueueViewModel by viewModels()

    private lateinit var recentAdapter: DownloadRecentAdapter
    private lateinit var downloadAdapter: DownloadQueueAdapter

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
        // This adapter is for the portrait layout's download list
        downloadAdapter = DownloadQueueAdapter(viewModel)
        binding.adapter = downloadAdapter // Used by data binding in activity_download_queue.xml

        // This adapter is for the recent apps list in both layouts (if the view exists)
        recentAdapter = DownloadRecentAdapter { app ->
            AppDetailActivity.start(this, app)
        }

        binding.recyclerDownloads?.layoutManager = LinearLayoutManager(this@DownloadQueueActivity)

        binding.recyclerRecent?.apply {
            adapter = recentAdapter
            layoutManager = GridLayoutManager(this@DownloadQueueActivity, 4)
        }
    }

    private fun setupClickListeners() {
        // These views are in activity_download_queue.xml and are safe to reference
        binding.ivBack?.setOnClickListener { finish() }
        binding.ivSearch?.setOnClickListener { SearchActivity.start(this) }
        binding.btnBackHome?.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastMessageShown()
            }
        }

        viewModel.recentInstalled.observe(this) { apps ->
            recentAdapter.submitList(apps)
            updateEmptyViewVisibility()
        }

        viewModel.downloadTasks.observe(this) { tasks ->
            downloadAdapter.submitList(tasks)
            updateEmptyViewVisibility()
        }
    }

    private fun updateEmptyViewVisibility() {
        val isDownloadListEmpty = viewModel.downloadTasks.value.isNullOrEmpty()
        val isRecentListEmpty = viewModel.recentInstalled.value.isNullOrEmpty()
        binding.layoutEmpty?.isVisible = isDownloadListEmpty && isRecentListEmpty
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadQueueActivity::class.java)
            context.startActivity(intent)
        }
    }
}
