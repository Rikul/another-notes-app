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

import androidx.room.*
import com.maltaisn.notes.model.entity.Attachment
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentsDao {

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY dateAdded DESC")
    fun getByNoteId(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun getByNoteIdSync(noteId: Long): List<Attachment>

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAllByNoteId(noteId: Long)
    
    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): Attachment?
}
