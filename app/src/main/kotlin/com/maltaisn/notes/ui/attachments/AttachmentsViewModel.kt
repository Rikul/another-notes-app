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

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.maltaisn.notes.R
import com.maltaisn.notes.model.AttachmentsRepository
import com.maltaisn.notes.model.DefaultAttachmentsRepository
import com.maltaisn.notes.model.entity.Attachment
import com.maltaisn.notes.ui.Event
import com.maltaisn.notes.ui.send
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val attachmentsRepository: AttachmentsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: Long = savedStateHandle["note_id"] ?: error("Note ID not passed")

    val attachments = attachmentsRepository.getAttachmentsByNoteId(noteId).asLiveData()

    private val _messageEvent = MutableLiveData<Event<Int>>()
    val messageEvent: LiveData<Event<Int>>
        get() = _messageEvent
        
    private val _openAttachmentEvent = MutableLiveData<Event<Attachment>>()
    val openAttachmentEvent: LiveData<Event<Attachment>>
        get() = _openAttachmentEvent

    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                attachmentsRepository.addAttachment(noteId, uri)
            } catch (e: DefaultAttachmentsRepository.FileSizeException) {
                _messageEvent.send(R.string.attachment_max_size_error)
            } catch (e: Exception) {
                _messageEvent.send(R.string.attachment_error_adding)
            }
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            try {
                attachmentsRepository.deleteAttachment(attachment)
            } catch (e: Exception) {
                _messageEvent.send(R.string.attachment_error_deleting)
            }
        }
    }
    
    fun openAttachment(attachment: Attachment) {
        _openAttachmentEvent.send(attachment)
    }
    
    fun getAttachmentFile(attachment: Attachment): File {
        return attachmentsRepository.getAttachmentFile(attachment)
    }
}
