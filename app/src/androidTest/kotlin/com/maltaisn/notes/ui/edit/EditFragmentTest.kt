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

package com.maltaisn.notes.ui.edit

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.maltaisn.notes.NavGraphMainDirections
import com.maltaisn.notes.R
import com.maltaisn.notes.getNoteColorResource
import com.maltaisn.notes.model.NotesDatabase
import com.maltaisn.notes.model.entity.Note
import com.maltaisn.notes.navigateSafe
import com.maltaisn.notes.ui.main.MainActivity
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class EditFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var database: NotesDatabase

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        activityScenarioRule.scenario.onActivity { activity ->
            context = activity.applicationContext
            activity.navController.navigateSafe(NavGraphMainDirections.actionEditNote(Note.NO_ID))
        }
    }

    @Test
    fun colorIconOpensSelectionDialog() {
        onView(withId(R.id.fragment_edit_layout))
            .perform(click())
        onView(withId(R.id.fragment_edit_layout))
            .perform(closeSoftKeyboard())

        // Click on the color icon
        clickColorAction()

        // Check if the color selection dialog is displayed
        // We check for one of the color buttons, e.g., the default color button
        onView(withId(R.id.color_btn_0))
            .check(matches(isDisplayed()))
    }

    @Test
    fun colorSelectionDialogShowsAllOptions() {
        // Open the color selection dialog
        clickColorAction()

        // Verify that all 6 color options are displayed
        onView(withId(R.id.color_btn_0)).check(matches(isDisplayed()))
        onView(withId(R.id.color_btn_1)).check(matches(isDisplayed()))
        onView(withId(R.id.color_btn_2)).check(matches(isDisplayed()))
        onView(withId(R.id.color_btn_3)).check(matches(isDisplayed()))
        onView(withId(R.id.color_btn_4)).check(matches(isDisplayed()))
        onView(withId(R.id.color_btn_5)).check(matches(isDisplayed()))
    }

    @Test
    fun selectingColorUpdatesEditorBackground() {
        // Open the color selection dialog
        clickColorAction()

        // Select color 1
        onView(withId(R.id.color_btn_1))
            .perform(click())

        // Verify that the editor background color is updated
        val expectedColor = ContextCompat.getColor(context, getNoteColorResource(1))
        onView(withId(R.id.fragment_edit_layout))
            .check(matches(withBackgroundColor(expectedColor)))
        onView(withId(R.id.content_container))
            .check(matches(withBackgroundColor(expectedColor)))
    }

    @Test
    fun selectingColorAndSavingUpdatesNoteInList() = runBlocking {
        // Wait for RecyclerView to be ready
        Thread.sleep(200)

        // Scroll to the title item (position 0) and click it to focus
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                click()
            ))

        // Type text into the focused title field
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                typeText("Color Test Note")
            ))

        // Close soft keyboard
        onView(withId(R.id.fragment_edit_layout))
            .perform(closeSoftKeyboard())

        // Open the color selection dialog
        clickColorAction()

        // Select color 2
        onView(withId(R.id.color_btn_2))
            .perform(click())

        // Navigate back to save and go to list
        pressBack()

        // Wait for the note to be saved to the database
        Thread.sleep(500)

        // Verify the note was saved with the correct color in the database
        val notesDao = database.notesDao()
        val notes = notesDao.getAll()

        // Find the note with our title
        val savedNote = notes.find { it.note.title == "Color Test Note" }
        assertNotNull(savedNote, "Note with title 'Color Test Note' should exist in database")
        assertEquals(2, savedNote.note.color , "Note should have color 2")
    }

    @Test
    fun verifyLightModeColors() {
        // Force light theme
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        //activityScenarioRule.scenario.recreate()

        // Open the color selection dialog
        clickColorAction()

        // Verify that Color 1 is #FFF9E6 (Light mode color)
        val expectedColor = Color.parseColor("#FFF9E6")
        onView(withId(R.id.color_btn_1))
            .check(matches(withBackgroundTint(expectedColor)))
    }

    @Test
    fun verifyDarkModeColors() {
        // Force dark theme
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Open the color selection dialog
        clickColorAction()

        // Verify that Color 1 is #2D2D30 (Dark mode color)
        val expectedColor = Color.parseColor("#2D2D30")
        onView(withId(R.id.color_btn_1))
            .check(matches(withBackgroundTint(expectedColor)))
    }

    @Test
    fun userCancelsColorSelection() {
        // Get initial background color (should be default)
        val initialColor = ContextCompat.getColor(context, getNoteColorResource(0))
        onView(withId(R.id.fragment_edit_layout))
            .check(matches(withBackgroundColor(initialColor)))

        // Open the color selection dialog
        clickColorAction()

        // Press back to cancel/dismiss the dialog
        pressBack()

        // Verify that the note's color on the editor screen remains unchanged
        onView(withId(R.id.fragment_edit_layout))
            .check(matches(withBackgroundColor(initialColor)))
    }

    private fun clickColorAction() {
        // Close soft keyboard to ensure toolbar is visible
        onView(withId(R.id.fragment_edit_layout))
            .perform(closeSoftKeyboard())

        Thread.sleep(200)

        onView(withContentDescription(R.string.action_color))
            .perform(click())

        // delay for .5 seconds to allow the dialog to open
        Thread.sleep(500)
    }

    private fun withBackgroundColor(expectedColor: Int) = object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with background color: $expectedColor")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item.background is ColorDrawable) {
                return (item.background as ColorDrawable).color == expectedColor
            }
            if (item is MaterialCardView) {
                return item.cardBackgroundColor.defaultColor == expectedColor
            }
            return false
        }
    }

    private fun withBackgroundTint(expectedColor: Int) = object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with background tint: $expectedColor")
        }

        override fun matchesSafely(item: View): Boolean {
            return item.backgroundTintList?.defaultColor == expectedColor
        }
    }

}
