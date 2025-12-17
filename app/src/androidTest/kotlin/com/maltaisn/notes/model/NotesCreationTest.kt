/*
 * Copyright 2025 Nicolas Maltais
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
import com.maltaisn.notes.testNote
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class NotesCreationTest {

    private lateinit var database: NotesDatabase
    private lateinit var notesDao: NotesDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java).build()
        notesDao = database.notesDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createNoteAndVerifyInDao() = runBlocking {
        // Create a test note
        val note = testNote(title = "Test Note", content = "Test content for the note")

        // Insert the note into the database
        val noteId = notesDao.insert(note)

        // Verify the note exists in the database
        val retrievedNote = notesDao.getById(noteId)

        // Assert the note was successfully inserted and retrieved
        assertNotNull(retrievedNote, "Note should exist in database after insertion")
        assertEquals(note.copy(id = noteId), retrievedNote, "Retrieved note should match inserted note")
    }
}
