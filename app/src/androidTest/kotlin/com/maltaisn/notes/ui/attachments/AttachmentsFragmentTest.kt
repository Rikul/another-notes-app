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

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.maltaisn.notes.NavGraphMainDirections
import com.maltaisn.notes.R
import com.maltaisn.notes.model.NotesDatabase
import com.maltaisn.notes.model.entity.Attachment
import com.maltaisn.notes.model.entity.Note
import com.maltaisn.notes.navigateSafe
import com.maltaisn.notes.testNote
import com.maltaisn.notes.ui.edit.EditFragmentDirections
import com.maltaisn.notes.ui.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.test.assertEquals
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.core.content.FileProvider
import com.maltaisn.notes.BuildConfig

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class AttachmentsFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var database: NotesDatabase

    private lateinit var context: Context
    private var noteId: Long = 0

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Create a test note for each test
        activityScenarioRule.scenario.onActivity { activity ->
            runBlocking {
                val note = testNote(
                    title = "Test Note",
                    content = "Content"
                )
                noteId = database.notesDao().insert(note)
            }
        }
    }

    @After
    fun cleanup() {
        try {
            Intents.release()
        } catch (e: IllegalStateException) {
            // Intents was not initialized, nothing to release
        }
        
        // Clean up database
        runBlocking {
            database.clearAllTables()
        }
    }

    private fun navigateToAttachments() {
        activityScenarioRule.scenario.onActivity { activity ->
            // Navigate to edit screen first, then to attachments
            activity.navController.navigateSafe(NavGraphMainDirections.actionEditNote(noteId))
            
            // Small delay to ensure navigation completes
            Thread.sleep(300)
            
            activity.navController.navigateSafe(EditFragmentDirections.actionEditToAttachments(noteId))
        }

        // Wait for fragment to load
        Thread.sleep(500)
    }

    private fun navigateToDashboard() {
        activityScenarioRule.scenario.onActivity { activity ->
            // Navigate to home/dashboard by popping back stack to start destination
            activity.navController.popBackStack(R.id.fragment_home, false)
        }

        // Wait for fragment to load
        Thread.sleep(500)
    }

    @Test
    fun attachmentsScreenDisplaysCorrectly() {
        navigateToAttachments()
        
        // Check toolbar title
        onView(withText(R.string.action_attachments))
            .check(matches(isDisplayed()))

        // Check FAB is displayed
        onView(withId(R.id.btn_add_attachment))
            .check(matches(isDisplayed()))

    }

    @Test
    fun attachmentDisplayedInList() {
        val date = Date()
        val expectedDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
        
        runBlocking {
            // Add an attachment directly to database
            val base64Data = Base64.encodeToString("Test content".toByteArray(), Base64.NO_WRAP)
            val attachment = Attachment(
                noteId = noteId,
                filename = "test_document.pdf",
                mimeType = "application/pdf",
                dateAdded = date,
                data = base64Data
            )
            database.attachmentsDao().insert(attachment)

            val binaryData = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, // PNG header
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D,
                0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte()
            )

            val pngBase64Data = Base64.encodeToString(binaryData, Base64.NO_WRAP)

            val pngAttachment = Attachment(
                noteId = noteId,
                filename = "image.png",
                mimeType = "image/png",
                dateAdded = date,
                data = pngBase64Data
            )

            database.attachmentsDao().insert(pngAttachment)
        }

        navigateToAttachments()

        onView(withText("test_document.pdf"))
            .check(matches(isDisplayed()))

        onView(withText("image.png"))
            .check(matches(isDisplayed()))

        // verify two dates are displayed.
        onView(withId(R.id.recycler_view))
            .check(matches(allOf(
                atPosition(0, hasDescendant(withText(expectedDate))),
                atPosition(1, hasDescendant(withText(expectedDate)))
            )))

        navigateToDashboard()

        navigateToAttachments()
        
        onView(withText("test_document.pdf"))
            .check(matches(isDisplayed()))

        onView(withText("image.png"))
            .check(matches(isDisplayed()))

    }

    @Test
    fun addAttachmentButtonOpensFilePicker() {
        navigateToAttachments()
        
        Intents.init()

        // Create a real file so ContentResolver can read it via FileProvider
        val tempFile = File(context.cacheDir, "picked_file.txt")
        FileOutputStream(tempFile).use {
            it.write("File content".toByteArray())
        }

        try {
            // Mock file picker intent using FileProvider to get a valid content:// URI
            // Use BuildConfig.APPLICATION_ID which should be com.maltaisn.notes.debug
            val authority = "${BuildConfig.APPLICATION_ID}.provider"
            val testUri = FileProvider.getUriForFile(context, authority, tempFile)
            
            val resultData = Intent()
            resultData.data = testUri
            resultData.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

            // Click FAB
            onView(withId(R.id.btn_add_attachment))
                .perform(click())

            // Wait a moment for intent handling and database insertion
            Thread.sleep(1000)

            // Verify the attachment is displayed in the list
            onView(withText("picked_file.txt"))
                .check(matches(isDisplayed()))

        } finally {
            tempFile.delete()
            Intents.release()
        }
    }

    @Test
    fun clickingAttachmentOpensFile() {
        var attachmentId: Long = 0
        val filename = "test_open.png"
        runBlocking {
            val base64Data = Base64.encodeToString("PNG content".toByteArray(), Base64.NO_WRAP)
            val attachment = Attachment(
                noteId = noteId,
                filename = filename,
                mimeType = "image/png",
                dateAdded = Date(),
                data = base64Data
            )
            attachmentId = database.attachmentsDao().insert(attachment)
        }

        navigateToAttachments()
        
        Intents.init()

        // Construct expected URI
        val expectedFilename = "temp_${attachmentId}_$filename"
        val expectedFile = File(context.cacheDir, expectedFilename)
        val authority = "${BuildConfig.APPLICATION_ID}.provider"
        val expectedUri = FileProvider.getUriForFile(context, authority, expectedFile)

        // Mock the result so we don't actually try to open a viewer
        intending(hasAction(Intent.ACTION_VIEW)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Click the attachment
        onView(withText(filename)).perform(click())
        
        Thread.sleep(500)

        // Verify intent
        Intents.intended(allOf(
            hasAction(Intent.ACTION_VIEW),
            androidx.test.espresso.intent.matcher.IntentMatchers.hasData(expectedUri),
            androidx.test.espresso.intent.matcher.IntentMatchers.hasType("image/png"),
            androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ))

        Intents.release()
    }

    @Test
    fun deleteButtonShowsConfirmationDialog() {
        runBlocking {
            // Add an attachment
            val base64Data = Base64.encodeToString("Test content".toByteArray(), Base64.NO_WRAP)
            val attachment = Attachment(
                noteId = noteId,
                filename = "test.txt",
                mimeType = "text/plain",
                dateAdded = Date(),
                data = base64Data
            )
            database.attachmentsDao().insert(attachment)

            // Navigate to attachments screen
        }
        navigateToAttachments()

        // Click delete button
        onView(withId(R.id.btn_delete))
            .perform(click())

        // Wait for dialog
        Thread.sleep(300)

        // Check confirmation dialog is displayed
        onView(withText(R.string.delete_attachment_message))
            .check(matches(isDisplayed()))

        onView(withText(R.string.action_yes))
            .check(matches(isDisplayed()))

        onView(withText(R.string.action_cancel))
            .check(matches(isDisplayed()))
    }

    @Test
    fun confirmDeleteRemovesAttachment() {
        runBlocking {
            // Add an attachment
        val base64Data = Base64.encodeToString("Test content".toByteArray(), Base64.NO_WRAP)
        val attachment = Attachment(
            noteId = noteId,
            filename = "to_delete.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )
        database.attachmentsDao().insert(attachment)

        // Navigate to attachments screen
        }
        navigateToAttachments()

        // Verify attachment is displayed
        onView(withText("to_delete.txt"))
            .check(matches(isDisplayed()))

        // Click delete button
        onView(withId(R.id.btn_delete))
            .perform(click())

        Thread.sleep(300)

        // Confirm deletion
        onView(withText(R.string.action_yes))
            .perform(click())

        Thread.sleep(500)

        runBlocking {
            // Verify attachment is removed from database
            val attachments = database.attachmentsDao().getByNoteId(noteId).first()
            assertEquals(0, attachments.size)
        }
    }

    @Test
    fun cancelDeleteKeepsAttachment() {
        runBlocking {
            // Add an attachment
        val base64Data = Base64.encodeToString("Test content".toByteArray(), Base64.NO_WRAP)
        val attachment = Attachment(
            noteId = noteId,
            filename = "keep_me.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )
        database.attachmentsDao().insert(attachment)

        // Navigate to attachments screen
        }
        navigateToAttachments()

        // Click delete button
        onView(withId(R.id.btn_delete))
            .perform(click())

        Thread.sleep(300)

        // Cancel deletion
        onView(withText(R.string.action_cancel))
            .perform(click())

        Thread.sleep(300)

        // Verify attachment still exists
        onView(withText("keep_me.txt"))
            .check(matches(isDisplayed()))

        runBlocking {
            val attachments = database.attachmentsDao().getByNoteId(noteId).first()
            assertEquals(1, attachments.size)
        }
    }

    @Test
    fun fileSizeExceededShowsError() {
        navigateToAttachments()
        
        Intents.init()

        // Create a temporary file larger than 5MB
        val tempFile = File(context.cacheDir, "large_test_file.bin")
        try {
            FileOutputStream(tempFile).use { fos ->
                // Write 6MB of data
                val chunk = ByteArray(1024 * 1024) // 1MB
                repeat(6) {
                    fos.write(chunk)
                }
            }

            val testUri = Uri.fromFile(tempFile)
            val resultData = Intent()
            resultData.data = testUri

            intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

            // Click FAB
            onView(withId(R.id.btn_add_attachment))
                .perform(click())

            Thread.sleep(1000)

            // Check error message is displayed
            onView(withText(R.string.attachment_max_size_error))
                .check(matches(isDisplayed()))

        } finally {
            // Clean up
            tempFile.delete()
            Intents.release()
        }
    }


    private fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}
