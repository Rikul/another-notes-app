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
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maltaisn.notes.dateFor
import com.maltaisn.notes.model.JsonManager.ImportResult
import com.maltaisn.notes.model.entity.Attachment
import com.maltaisn.notes.testNote
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class JsonManagerAttachmentTest {

    private lateinit var database: NotesDatabase
    private lateinit var notesDao: NotesDao
    private lateinit var labelsDao: LabelsDao
    private lateinit var attachmentsDao: AttachmentsDao
    private lateinit var attachmentsRepository: AttachmentsRepository

    private lateinit var prefsManager: PrefsManager
    private lateinit var jsonManager: JsonManager

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java).build()
        notesDao = database.notesDao()
        labelsDao = database.labelsDao()
        attachmentsDao = database.attachmentsDao()
        attachmentsRepository = DefaultAttachmentsRepository(attachmentsDao, context)
        prefsManager = mock {
            on { encryptedImportKeyDerivationSalt } doReturn "salt"
            on { encryptedExportKeyDerivationSalt } doReturn "salt"
        }
        jsonManager = DefaultJsonManager(notesDao, labelsDao, attachmentsDao, attachmentsRepository, Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }, mock(), prefsManager)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun testExportImportWithAttachments() = runBlocking {
        prefsManager.shouldEncryptExportedData = false

        // Insert note
        val note = testNote(id = 1, title = "attachment note", content = "content")
        notesDao.insert(note)

        // Insert attachment
        val attachment = Attachment(
            noteId = 1,
            filename = "img.png",
            mimeType = "image/png",
            dateAdded = dateFor("2020-01-01Z"),
            data = "base64data"
        )
        attachmentsDao.insert(attachment)

        // Export
        val jsonData = jsonManager.exportJsonData()

        // Verify export contains attachment data
        assertTrue(jsonData.contains("img.png"))
        assertTrue(jsonData.contains("base64data"))

        // Clear database
        notesDao.clear()

        // Import
        assertEquals(ImportResult.SUCCESS, jsonManager.importJsonData(jsonData))

        // Verify import
        val importedNotes = notesDao.getAll()
        assertEquals(1, importedNotes.size)
        val importedNote = importedNotes[0].note
        assertEquals(note.title, importedNote.title)

        val importedAttachments = attachmentsDao.getByNoteIdSync(importedNote.id)
        assertEquals(1, importedAttachments.size)
        val importedAttachment = importedAttachments[0]
        assertEquals(attachment.filename, importedAttachment.filename)
        assertEquals(attachment.mimeType, importedAttachment.mimeType)
        assertEquals(attachment.data, importedAttachment.data)
        assertEquals(attachment.dateAdded.time, importedAttachment.dateAdded.time)
    }
}
