package com.example.storechat.ui.download

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
        recentAdapter = DownloadRecentAdapter { app ->
            AppDetailActivity.start(this, app)
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.recyclerRecent?.apply {
                adapter = recentAdapter
                layoutManager = LinearLayoutManager(this@DownloadQueueActivity, LinearLayoutManager.VERTICAL, false)
            }
        } else {
            // For Portrait mode
            downloadAdapter = DownloadQueueAdapter(viewModel)
            binding.adapter = downloadAdapter

            binding.recyclerDownloads?.layoutManager = LinearLayoutManager(this@DownloadQueueActivity)

            binding.recyclerRecent?.apply {
                adapter = recentAdapter
                layoutManager = GridLayoutManager(this@DownloadQueueActivity, 4)
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivBack?.setOnClickListener { finish() }
        binding.ivSearch?.setOnClickListener { SearchActivity.start(this) }
        binding.btnBackHome?.setOnClickListener { finish() }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.tvStatus?.setOnClickListener { viewModel.onStatusClick() }
            binding.tvAllResume?.setOnClickListener { viewModel.resumeAllPausedTasks() }
            binding.ivCancel?.setOnClickListener { viewModel.cancelDownload() }
        }
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

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.activeTask.observe(this) { task ->
                updateEmptyViewVisibility()
            }
        } else {
            // For Portrait mode
            viewModel.downloadTasks.observe(this) { tasks ->
                downloadAdapter.submitList(tasks)
                updateEmptyViewVisibility()
            }
        }
    }

    private fun updateEmptyViewVisibility() {
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val isDownloadListEmpty = viewModel.downloadTasks.value.isNullOrEmpty()
        val isRecentListEmpty = viewModel.recentInstalled.value.isNullOrEmpty()

        if (isPortrait) {
            binding.layoutEmpty?.isVisible = isDownloadListEmpty && isRecentListEmpty
        } else {
            val isLandscapeTaskEmpty = viewModel.activeTask.value == null
            binding.layoutEmpty?.isVisible = isLandscapeTaskEmpty && isRecentListEmpty
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadQueueActivity::class.java)
            context.startActivity(intent)
        }
    }
}
