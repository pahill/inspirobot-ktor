package com.pamelaahill

import java.io.ByteArrayInputStream
import java.io.File

interface InspirationDb {
    suspend fun createInspiration(userId: Long, fileExtension: String, inputStream: ByteArrayInputStream): Inspiration?
    suspend fun updateInspiration(id: Long, tagTitles: List<String>): Inspiration?
    suspend fun findInspirationById(id: Long): Inspiration?
    suspend fun findInspirationImageById(id: Long): File?
    suspend fun findInspirationsByUserId(userId: Long): List<Inspiration>
    suspend fun findInspirationsByTag(tagId: Long): List<Inspiration>
    suspend fun findTagsByTitle(title: String): List<Tag>
    suspend fun findTags(): List<Tag>
}