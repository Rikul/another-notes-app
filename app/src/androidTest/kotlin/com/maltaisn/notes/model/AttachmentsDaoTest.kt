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
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maltaisn.notes.model.entity.Attachment
import com.maltaisn.notes.testNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AttachmentsDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var notesDao: NotesDao
    private lateinit var attachmentsDao: AttachmentsDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java).build()
        notesDao = database.notesDao()
        attachmentsDao = database.attachmentsDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndRetrieveAttachment() = runBlocking {
        // Create a note first
        val noteId = notesDao.insert(testNote(title = "Test Note"))

        // Create an attachment
        val sampleData = "Hello World".toByteArray()
        val base64Data = Base64.encodeToString(sampleData, Base64.NO_WRAP)
        
        val attachment = Attachment(
            noteId = noteId,
            filename = "test.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )

        // Insert attachment
        val attachmentId = attachmentsDao.insert(attachment)
        assertTrue(attachmentId > 0)

        // Retrieve attachment
        val retrieved = attachmentsDao.getById(attachmentId)
        assertNotNull(retrieved)
        assertEquals(attachment.filename, retrieved.filename)
        assertEquals(attachment.mimeType, retrieved.mimeType)
        assertEquals(attachment.data, retrieved.data)
    }

    @Test
    fun getAttachmentsByNoteId() = runBlocking {
        // Create notes
        val noteId1 = notesDao.insert(testNote(title = "Note 1"))
        val noteId2 = notesDao.insert(testNote(title = "Note 2"))

        val base64Data = Base64.encodeToString("test".toByteArray(), Base64.NO_WRAP)

        // Add attachments to note 1
        val attachment1 = Attachment(
            noteId = noteId1,
            filename = "file1.txt",
            mimeType = "text/plain",
            dateAdded = Date(1000),
            data = base64Data
        )
        val attachment2 = Attachment(
            noteId = noteId1,
            filename = "file2.jpg",
            mimeType = "image/jpeg",
            dateAdded = Date(2000),
            data = base64Data
        )

        attachmentsDao.insert(attachment1)
        attachmentsDao.insert(attachment2)

        // Add attachment to note 2
        val attachment3 = Attachment(
            noteId = noteId2,
            filename = "file3.pdf",
            mimeType = "application/pdf",
            dateAdded = Date(3000),
            data = base64Data
        )
        attachmentsDao.insert(attachment3)

        // Get attachments for note 1
        val note1Attachments = attachmentsDao.getByNoteId(noteId1).first()
        assertEquals(2, note1Attachments.size)
        // Should be ordered by dateAdded DESC
        assertEquals("file2.jpg", note1Attachments[0].filename)
        assertEquals("file1.txt", note1Attachments[1].filename)

        // Get attachments for note 2
        val note2Attachments = attachmentsDao.getByNoteId(noteId2).first()
        assertEquals(1, note2Attachments.size)
        assertEquals("file3.pdf", note2Attachments[0].filename)
    }

    @Test
    fun deleteAttachment() = runBlocking {
        val noteId = notesDao.insert(testNote(title = "Test Note"))
        val base64Data = Base64.encodeToString("test".toByteArray(), Base64.NO_WRAP)

        val attachment = Attachment(
            noteId = noteId,
            filename = "test.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )

        val attachmentId = attachmentsDao.insert(attachment)
        assertNotNull(attachmentsDao.getById(attachmentId))

        // Delete attachment
        attachmentsDao.delete(attachment.copy(id = attachmentId))
        assertNull(attachmentsDao.getById(attachmentId))
    }

    @Test
    fun cascadeDeleteWhenNoteDeleted() = runBlocking {
        val noteId = notesDao.insert(testNote(title = "Test Note"))
        val base64Data = Base64.encodeToString("test".toByteArray(), Base64.NO_WRAP)

        val attachment = Attachment(
            noteId = noteId,
            filename = "test.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )

        attachmentsDao.insert(attachment)

        // Verify attachment exists
        val attachments = attachmentsDao.getByNoteId(noteId).first()
        assertEquals(1, attachments.size)

        // Delete note
        notesDao.delete(testNote(id = noteId))

        // Verify attachments are cascade deleted
        val attachmentsAfterDelete = attachmentsDao.getByNoteId(noteId).first()
        assertEquals(0, attachmentsAfterDelete.size)
    }

    @Test
    fun deleteAllByNoteId() = runBlocking {
        val noteId = notesDao.insert(testNote(title = "Test Note"))
        val base64Data = Base64.encodeToString("test".toByteArray(), Base64.NO_WRAP)

        // Add multiple attachments
        attachmentsDao.insert(Attachment(noteId = noteId, filename = "file1.txt", mimeType = "text/plain", dateAdded = Date(), data = base64Data))
        attachmentsDao.insert(Attachment(noteId = noteId, filename = "file2.txt", mimeType = "text/plain", dateAdded = Date(), data = base64Data))
        attachmentsDao.insert(Attachment(noteId = noteId, filename = "file3.txt", mimeType = "text/plain", dateAdded = Date(), data = base64Data))

        // Verify attachments exist
        assertEquals(3, attachmentsDao.getByNoteId(noteId).first().size)

        // Delete all attachments for note
        attachmentsDao.deleteAllByNoteId(noteId)

        // Verify all deleted
        assertEquals(0, attachmentsDao.getByNoteId(noteId).first().size)
    }

    @Test
    fun base64DataIntegrity() = runBlocking {
        val noteId = notesDao.insert(testNote(title = "Test Note"))

        // Test with binary data
        val originalData = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte(), 0xFE.toByte())
        val base64Data = Base64.encodeToString(originalData, Base64.NO_WRAP)

        val attachment = Attachment(
            noteId = noteId,
            filename = "binary.dat",
            mimeType = "application/octet-stream",
            dateAdded = Date(),
            data = base64Data
        )

        val attachmentId = attachmentsDao.insert(attachment)
        val retrieved = attachmentsDao.getById(attachmentId)!!

        // Decode and verify data
        val decodedData = Base64.decode(retrieved.data, Base64.NO_WRAP)
        assertTrue(originalData.contentEquals(decodedData))
    }
}
