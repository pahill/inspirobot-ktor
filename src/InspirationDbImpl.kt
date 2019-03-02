package com.pamelaahill

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.*
import java.util.Date

private const val IMAGE_DIR = "images"

class InspirationDbImpl : InspirationDb {

    init {
        Database.connect(hikari())
        transaction {
            create(Inspirations)
            create(Tags)
            create(InspirationTags)
        }
    }

    override suspend fun createInspiration(
        userId: Long,
        fileExtension: String,
        inputStream: ByteArrayInputStream
    ): Inspiration? {
        return dbQuery {
            //Save the file
            val inspirationImage = File("$IMAGE_DIR/${UUID.randomUUID()}.$fileExtension")
            inspirationImage.writeBytes(inputStream.readBytes())

            //Insert the inspiration
            val id = Inspirations.insert {
                it[Inspirations.userId] = userId
                it[Inspirations.fileUrl] = inspirationImage.toURI().toURL().toExternalForm()
                it[Inspirations.generatedDate] = Date().time
            } get Inspirations.id

            id?.let { syncFindInspirationById(it) }
        }
    }

    override suspend fun updateInspiration(id: Long, tagTitles: List<String>): Inspiration? {
        return dbQuery {
            //Ensure all the tags are in the table
            tagTitles.map { tagTitle ->
                val containsTag = !Tags.select { Tags.title eq tagTitle }.empty()
                if (!containsTag) {
                    Tags.insert {
                        it[Tags.title] = tagTitle
                    }
                }
            }

            //Get the tags
            val tags: List<Tag> = tagTitles.flatMap { tagTitle ->
                Tags.select { Tags.title eq tagTitle }.mapNotNull {
                    mapTagsToTag(it)
                }
            }

            //Replace all the tags
            InspirationTags.deleteWhere { InspirationTags.inspirationId eq id }
            tags.forEach { tag ->
                    InspirationTags.insert {
                        it[InspirationTags.inspirationId] = id
                        it[InspirationTags.tagId] = tag.id
                    }
            }

            syncFindInspirationById(id)
        }
    }

    override suspend fun findInspirationsByUserId(userId: Long): List<Inspiration> {
        return dbQuery {
            Inspirations.select { Inspirations.userId eq userId }.mapNotNull {
                mapInspirationsToInspiration(it)
            }
        }
    }

    override suspend fun findInspirationImageById(id: Long): File? {
        return findInspirationById(id)?.fileUrl?.toURI()?.let {
            File(it)
        }
    }

    override suspend fun findInspirationById(id: Long): Inspiration? {
        return dbQuery {
            syncFindInspirationById(id)
        }
    }

    override suspend fun findInspirationsByTag(tagId: Long): List<Inspiration> {
        return dbQuery {
            (Inspirations innerJoin InspirationTags).select { InspirationTags.tagId eq tagId }
                .mapNotNull { mapInspirationsToInspiration(it) }
        }
    }

    override suspend fun findTagsByTitle(title: String): List<Tag> {
        return dbQuery {
            Tags.select { Tags.title like title }.mapNotNull { mapTagsToTag(it) }
        }
    }

    override suspend fun findTags(): List<Tag> {
        return dbQuery {
            Tags.selectAll().mapNotNull { mapTagsToTag(it) }
        }
    }

    private fun addTagsToInspiration(id: Long): List<Tag> {
        return (InspirationTags innerJoin Tags).select { InspirationTags.inspirationId eq id }.mapNotNull {
            mapTagsToTag(it)
        }
    }

    private fun syncFindInspirationById(id: Long): Inspiration? {
        return Inspirations.select { Inspirations.id eq id }.mapNotNull {
            mapInspirationsToInspiration(it)
        }.firstOrNull()
    }

    private fun mapInspirationsToInspiration(resultRow: ResultRow) = Inspiration(
        id = resultRow[Inspirations.id],
        userId = resultRow[Inspirations.userId],
        fileUrl = URL(resultRow[Inspirations.fileUrl]),
        tags = addTagsToInspiration(resultRow[Inspirations.id]),
        generatedDate = resultRow[Inspirations.generatedDate]
    )

    private fun mapTagsToTag(resultRow: ResultRow) = Tag(
        id = resultRow[Tags.id],
        title = resultRow[Tags.title]
    )

    private suspend fun <T> dbQuery(block: () -> T): T {
        return withContext(Dispatchers.IO)
        {
            transaction { block() }
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    object Inspirations : Table() {
        val id = long("id").primaryKey().autoIncrement()
        val userId = long("userId")
        val fileUrl = varchar("fileUrl", 1024)
        val generatedDate = long("generatedDate")
    }

    object Tags : Table() {
        val id = long("id").primaryKey().autoIncrement()
        val title = varchar("title", 1024)
    }

    object InspirationTags : Table() {
        val inspirationId = long("inspirationId") references Inspirations.id
        val tagId = long("tagId") references Tags.id
    }
}