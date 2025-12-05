package com.example.storechat.ui.download

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.storechat.R
import com.example.storechat.databinding.FragmentDownloadQueueBinding
import com.example.storechat.ui.detail.AppDetailActivity
import com.example.storechat.ui.home.DownloadListAdapter

class DownloadQueueFragment : Fragment() {

    private var _binding: FragmentDownloadQueueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloadQueueViewModel by viewModels()

    private lateinit var recentAdapter: DownloadRecentAdapter
    private var downloadAdapter: DownloadListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadQueueBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // Setup for recent installs (only exists in portrait layout), so use a safe call
        binding.recyclerRecent?.let { recyclerView ->
            recentAdapter = DownloadRecentAdapter { app ->
                context?.let { AppDetailActivity.start(it, app) }
            }
            recyclerView.adapter = recentAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        // Setup for download list (only in landscape layout)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            downloadAdapter = DownloadListAdapter { task ->
                viewModel.onStatusClick(task)
            }
            // Set the adapter for the data binding variable in the XML
            binding.downloadAdapter = downloadAdapter
            binding.recyclerDownloads?.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener {
            activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.closeDrawer(GravityCompat.END)
        }
        // These views only exist in the portrait layout, so use safe calls.
        binding.tvStatus?.setOnClickListener { viewModel.onStatusClick() }
        binding.ivCancelDownload?.setOnClickListener { viewModel.cancelDownload() }
    }

    private fun observeViewModel() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.onToastMessageShown()
            }
        }

        // Observe recent installs only if the view exists (portrait)
        binding.recyclerRecent?.let {
            viewModel.recentInstalled.observe(viewLifecycleOwner) { list ->
                if (::recentAdapter.isInitialized) {
                    recentAdapter.submitList(list)
                }
            }
        }

        // Observe download tasks only in landscape mode
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.downloadTasks.observe(viewLifecycleOwner) { tasks ->
                downloadAdapter?.submitList(tasks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
