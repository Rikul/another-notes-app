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

package com.maltaisn.notes.espresso

import android.view.View
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.Toolbar
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Matches a menu item action button (ActionMenuItemView) by its content description resource.
 * This is useful for clicking toolbar action buttons in Espresso tests.
 */
fun withMenuItemContentDescription(stringResourceId: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        private var expectedText: String? = null
        private var resourceName: String? = null

        override fun describeTo(description: Description) {
            description.appendText("with menu item content description from resource id: ")
            description.appendValue(stringResourceId)
            if (resourceName != null) {
                description.appendText("[")
                description.appendText(resourceName)
                description.appendText("]")
            }
            if (expectedText != null) {
                description.appendText(" value: ")
                description.appendText(expectedText)
            }
        }

        override fun matchesSafely(view: View): Boolean {
            if (expectedText == null) {
                try {
                    expectedText = view.resources.getString(stringResourceId)
                    resourceName = view.resources.getResourceEntryName(stringResourceId)
                } catch (e: Exception) {
                    return false
                }
            }

            // Check content description
            val contentDesc = view.contentDescription
            if (contentDesc != null && contentDesc.toString() == expectedText) {
                return true
            }

            // For ActionMenuItemView, also try to find it in a toolbar and match by menu item ID
            if (view is ActionMenuItemView) {
                val parent = view.parent
                if (parent is androidx.appcompat.widget.ActionMenuView) {
                    val toolbar = parent.parent
                    if (toolbar is Toolbar) {
                        val menu = toolbar.menu
                        for (i in 0 until menu.size()) {
                            val item = menu.getItem(i)
                            if (item.itemId == stringResourceId && item.isVisible) {
                                // Check if this is the right view by checking its position
                                // This is a heuristic match
                                return contentDesc == null || contentDesc.toString() == view.resources.getString(item.itemId)
                            }
                        }
                    }
                }
            }

            return false
        }
    }
}
