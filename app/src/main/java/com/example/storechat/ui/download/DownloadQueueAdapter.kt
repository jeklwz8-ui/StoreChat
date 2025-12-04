package com.example.storechat.ui.download

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.storechat.databinding.ItemDownloadTaskBinding
import com.example.storechat.model.DownloadTask

class DownloadQueueAdapter(private val viewModel: DownloadQueueViewModel) :
    ListAdapter<DownloadTask, DownloadQueueAdapter.ViewHolder>(DownloadTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, viewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    class ViewHolder(private val binding: ItemDownloadTaskBinding, private val viewModel: DownloadQueueViewModel) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: DownloadTask) {
            binding.task = task

            // 取消下载关闭弹窗任务
            binding.ivCancel.setOnClickListener { viewModel.cancelDownload(task) }


            binding.tvStatus.setOnClickListener { viewModel.onStatusClick(task) }
            binding.executePendingBindings()
        }
    }
}

class DownloadTaskDiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
    override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
        return oldItem == newItem
    }
}
