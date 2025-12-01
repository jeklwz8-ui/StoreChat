package com.example.storechat.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.storechat.databinding.FragmentHomeBinding
import com.example.storechat.model.AppInfo
import com.example.storechat.ui.detail.AppDetailActivity
import com.example.storechat.ui.download.DownloadQueueActivity
import com.example.storechat.ui.search.SearchActivity
import com.example.storechat.ui.search.UpdateStatus
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var appListAdapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        appListAdapter = AppListAdapter(
            onItemClick = { app -> openDetail(app) },
            onActionClick = { app -> viewModel.handleAppAction(app) }
        )
        binding.recyclerAppList.adapter = appListAdapter

        binding.tabLayoutCategories.apply {
            addTab(newTab().setText(AppCategory.YANNUO.title))
            addTab(newTab().setText(AppCategory.ICBC.title))
            addTab(newTab().setText(AppCategory.CCB.title))

            // 强制标签左对齐
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_SCROLLABLE

        }

        binding.tabLayoutCategories.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    0 -> AppCategory.YANNUO
                    1 -> AppCategory.ICBC
                    2 -> AppCategory.CCB
                    else -> AppCategory.YANNUO
                }
                viewModel.selectCategory(category)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 将点击事件精确绑定到 ivSearch 图标上 (使用安全调用)
        binding.ivSearch?.setOnClickListener { SearchActivity.start(requireContext()) }

        // 为横屏的真实搜索框和按钮设置点击事件
        binding.btnSearch?.setOnClickListener { performSearch() }
        binding.etSearch?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.ivDownloadManager?.setOnClickListener { DownloadQueueActivity.start(requireContext()) }
        binding.tvVersion?.setOnClickListener { viewModel.checkAppUpdate() }
    }

    private fun performSearch() {
        val keyword = binding.etSearch?.text.toString()
        SearchActivity.start(requireContext(), keyword)
    }

    private fun observeViewModel() {
        viewModel.apps.observe(viewLifecycleOwner) { apps ->
            appListAdapter.submitList(apps)
        }

        viewModel.checkUpdateResult.observe(viewLifecycleOwner) { status ->
            when (status) {
                UpdateStatus.LATEST -> Toast.makeText(requireContext(), "当前已是最新版本", Toast.LENGTH_SHORT).show()
                UpdateStatus.NEW_VERSION -> showUpdateDialog()
                null -> {}
            }
            viewModel.clearUpdateResult()
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { packageName ->
            if (packageName != null) {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "无法打开应用", Toast.LENGTH_SHORT).show()
                }
                viewModel.onNavigationComplete()
            }
        }
    }

    private fun showUpdateDialog() {
        val currentVer = viewModel.appVersion.value ?: "V1.0.0"
        val latestVer = "V1.0.1"

        AlertDialog.Builder(requireContext())
            .setTitle("发现新版本")
            .setMessage("当前版本：$currentVer\n最新版本：$latestVer")
            .setNegativeButton("稍后") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("去更新") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "这里以后接入应用商店自更新逻辑", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun openDetail(app: AppInfo) {
        AppDetailActivity.start(requireContext(), app)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
