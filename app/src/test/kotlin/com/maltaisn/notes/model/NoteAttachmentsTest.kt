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

import java.util.Base64
import com.maltaisn.notes.model.entity.Attachment
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NoteAttachmentsTest {

    @Test
    fun attachmentCreation() {
        val sampleData = "Test content".toByteArray()
        val base64Data = Base64.getEncoder().encodeToString(sampleData)

        val attachment = Attachment(
            id = 1,
            noteId = 100,
            filename = "document.pdf",
            mimeType = "application/pdf",
            dateAdded = Date(),
            data = base64Data
        )

        assertEquals(1, attachment.id)
        assertEquals(100, attachment.noteId)
        assertEquals("document.pdf", attachment.filename)
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals(base64Data, attachment.data)
    }

    @Test
    fun attachmentCopy() {
        val base64Data = Base64.getEncoder().encodeToString("test".toByteArray())
        val original = Attachment(
            id = 1,
            noteId = 100,
            filename = "original.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )

        val copied = original.copy(filename = "copied.txt")

        assertEquals(original.id, copied.id)
        assertEquals(original.noteId, copied.noteId)
        assertEquals("copied.txt", copied.filename)
        assertEquals(original.mimeType, copied.mimeType)
        assertEquals(original.data, copied.data)
    }

    @Test
    fun base64EncodingDecoding() {
        val originalText = "Hello, World! 你好世界 🌍"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)
        
        // Encode
        val base64Data = Base64.getEncoder().encodeToString(originalBytes)
        
        val attachment = Attachment(
            id = 1,
            noteId = 100,
            filename = "unicode.txt",
            mimeType = "text/plain",
            dateAdded = Date(),
            data = base64Data
        )

        // Decode and verify
        val decodedBytes = Base64.getDecoder().decode(attachment.data)
        val decodedText = String(decodedBytes, Charsets.UTF_8)
        
        assertEquals(originalText, decodedText)
    }

    @Test
    fun binaryDataEncoding() {
        // Simulate binary file data (like an image)
        val binaryData = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, // PNG header
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D,
            0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte()
        )

        val base64Data = Base64.getEncoder().encodeToString(binaryData)
        
        val attachment = Attachment(
            id = 1,
            noteId = 100,
            filename = "image.png",
            mimeType = "image/png",
            dateAdded = Date(),
            data = base64Data
        )

        // Decode and verify exact match
        val decoded = Base64.getDecoder().decode(attachment.data)
        assertEquals(binaryData.size, decoded.size)
        for (i in binaryData.indices) {
            assertEquals(binaryData[i], decoded[i], "Mismatch at index $i")
        }
    }

    @Test
    fun attachmentWithDifferentMimeTypes() {
        val base64Data = Base64.getEncoder().encodeToString("data".toByteArray())

        val textAttachment = Attachment(1, 100, "doc.txt", "text/plain", Date(), base64Data)
        val pdfAttachment = Attachment(2, 100, "doc.pdf", "application/pdf", Date(), base64Data)
        val imageAttachment = Attachment(3, 100, "img.jpg", "image/jpeg", Date(), base64Data)
        val videoAttachment = Attachment(4, 100, "vid.mp4", "video/mp4", Date(), base64Data)

        assertEquals("text/plain", textAttachment.mimeType)
        assertEquals("application/pdf", pdfAttachment.mimeType)
        assertEquals("image/jpeg", imageAttachment.mimeType)
        assertEquals("video/mp4", videoAttachment.mimeType)
    }

    @Test
    fun largeDataHandling() {
        // Simulate a ~1MB file
        val largeData = ByteArray(1024 * 1024) { it.toByte() }
        val base64Data = Base64.getEncoder().encodeToString(largeData)

        val attachment = Attachment(
            id = 1,
            noteId = 100,
            filename = "large_file.bin",
            mimeType = "application/octet-stream",
            dateAdded = Date(),
            data = base64Data
        )

        // Verify data can be decoded
        val decoded = Base64.getDecoder().decode(attachment.data)
        assertEquals(largeData.size, decoded.size)
    }

}
