package com.example.paging3pagecache

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.paging3pagecache.databinding.ItemStatusBinding
import com.example.paging3pagecache.extensions.parseAsMastodonHtml
import com.example.paging3pagecache.mastodon.Status

val STATUS_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
    override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean = oldItem == newItem
}

class StatusAdapter : PagingDataAdapter<Status, StatusViewHolder>(STATUS_COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class StatusViewHolder(view: View, private val binding: ItemStatusBinding) : RecyclerView.ViewHolder(view) {
    fun bind(status: Status?) {
        if (status != null) {
            binding.statusId.text = status.id
            binding.content.text = status.content.parseAsMastodonHtml()
        } else {
            binding.statusId.text = ""
            binding.content.text = ""
        }
    }

    companion object {
        fun create(parent: ViewGroup): StatusViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemStatusBinding.inflate(inflater, parent, false)
            return StatusViewHolder(binding.root, binding)
        }
    }
}
