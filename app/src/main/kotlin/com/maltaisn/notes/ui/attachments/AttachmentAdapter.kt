/*
 * Copyright 2026 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.notes.ui.attachments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maltaisn.notes.databinding.ItemAttachmentBinding
import com.maltaisn.notes.model.entity.Attachment
import java.text.SimpleDateFormat
import java.util.Locale

class AttachmentAdapter(
    private val onDelete: (Attachment) -> Unit,
    private val onOpen: (Attachment) -> Unit
) : ListAdapter<Attachment, AttachmentAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAttachmentBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onOpen(getItem(bindingAdapterPosition))
                }
            }
            binding.btnDelete.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDelete(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(attachment: Attachment) {
            binding.attachmentName.text = attachment.filename
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            binding.attachmentDate.text = dateFormat.format(attachment.dateAdded)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Attachment>() {
        override fun areItemsTheSame(oldItem: Attachment, newItem: Attachment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Attachment, newItem: Attachment) = oldItem == newItem
    }
}
