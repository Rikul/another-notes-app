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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.notes.BuildConfig
import com.maltaisn.notes.R
import com.maltaisn.notes.databinding.FragmentAttachmentsBinding
import com.maltaisn.notes.model.entity.Attachment
import com.maltaisn.notes.ui.common.ConfirmDialog
import com.maltaisn.notes.ui.observeEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttachmentsFragment : Fragment(), ConfirmDialog.Callback {

    private val viewModel: AttachmentsViewModel by viewModels()
    
    private var _binding: FragmentAttachmentsBinding? = null
    private val binding get() = _binding!!

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.addAttachment(uri)
        }
    }
    
    private var attachmentToDelete: Attachment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttachmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
             findNavController().popBackStack()
        }
        
        binding.btnAddAttachment.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        val adapter = AttachmentAdapter(
            onDelete = { attachment ->
                attachmentToDelete = attachment
                ConfirmDialog.newInstance(
                    message = R.string.delete_attachment_message,
                    btnPositive = R.string.action_yes,
                    btnNegative = R.string.action_cancel
                ).show(childFragmentManager, DELETE_DIALOG_TAG)
            },
            onOpen = { attachment ->
                viewModel.openAttachment(attachment)
            }
        )
        binding.recyclerView.adapter = adapter
        
        viewModel.attachments.observe(viewLifecycleOwner) { attachments ->
            adapter.submitList(attachments)
            binding.emptyView.isVisible = attachments.isEmpty()
        }
        
        viewModel.messageEvent.observeEvent(viewLifecycleOwner) { messageResId ->
            Snackbar.make(view, messageResId, Snackbar.LENGTH_SHORT).show()
        }
        
        viewModel.openAttachmentEvent.observeEvent(viewLifecycleOwner) { attachment ->
            val file = viewModel.getAttachmentFile(attachment)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", file)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, attachment.mimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Snackbar.make(view, R.string.attachment_no_app_error, Snackbar.LENGTH_SHORT).show()
                }
            } else {
                 Snackbar.make(view, R.string.attachment_file_not_found, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDialogPositiveButtonClicked(tag: String?) {
        if (tag == DELETE_DIALOG_TAG) {
            attachmentToDelete?.let {
                viewModel.deleteAttachment(it)
            }
            attachmentToDelete = null
        }
    }
    
    override fun onDialogNegativeButtonClicked(tag: String?) {
        if (tag == DELETE_DIALOG_TAG) {
            attachmentToDelete = null
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val DELETE_DIALOG_TAG = "delete_attachment_dialog"
    }
}
