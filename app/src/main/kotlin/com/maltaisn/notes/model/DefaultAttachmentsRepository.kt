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

package com.maltaisn.notes.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.maltaisn.notes.model.entity.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject

class DefaultAttachmentsRepository @Inject constructor(
    private val attachmentsDao: AttachmentsDao,
    @ApplicationContext private val context: Context
) : AttachmentsRepository {

    override fun getAttachmentsByNoteId(noteId: Long): Flow<List<Attachment>> {
        return attachmentsDao.getByNoteId(noteId)
    }

    override suspend fun addAttachment(noteId: Long, uri: Uri): Attachment {
        return withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            
            // Check size
            val pfd = contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Cannot access file")
            
            pfd.use {
                val size = it.statSize
                if (size > MAX_FILE_SIZE) {
                    throw FileSizeException()
                }
            }

            // Get metadata
            var filename = "attachment"
            var mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                         filename = cursor.getString(nameIndex)
                    }
                }
            }

            // Read file data and encode as base64
            val fileData = contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: throw IllegalArgumentException("Cannot read file")
            
            val base64Data = Base64.encodeToString(fileData, Base64.NO_WRAP)

            // Create and insert attachment with data
            val attachment = Attachment(
                noteId = noteId,
                filename = filename,
                mimeType = mimeType,
                dateAdded = Date(),
                data = base64Data
            )
            
            val id = attachmentsDao.insert(attachment)
            attachment.copy(id = id)
        }
    }

    override suspend fun deleteAttachment(attachment: Attachment) {
        withContext(Dispatchers.IO) {
            attachmentsDao.delete(attachment)
        }
    }

    override suspend fun deleteAllByNoteId(noteId: Long) {
        withContext(Dispatchers.IO) {
            attachmentsDao.deleteAllByNoteId(noteId)
        }
    }

    override fun getAttachmentFile(attachment: Attachment): File {
        // Decode base64 data and write to temporary file
        val fileData = Base64.decode(attachment.data, Base64.NO_WRAP)
        val tempFile = File(context.cacheDir, "temp_${attachment.id}_${attachment.filename}")
        tempFile.writeBytes(fileData)
        return tempFile
    }

    class FileSizeException : Exception("Maximum file size must be 5 MB")

    companion object {
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5 MB
    }
}
