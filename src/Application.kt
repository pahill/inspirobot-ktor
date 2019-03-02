@file:JvmName("Application")

package com.pamelaahill

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::inspirobot).start(wait = true)
}

fun Application.inspirobot() {
    val db: InspirationDb = InspirationDbImpl()
    val api: InspirationApi = InspirationApiImpl()

    install(ContentNegotiation) {
        gson {
        }
    }

    routing {
        post("/users/{userId}/inspirations") {
            val userId = call.parameters["userId"]?.toLongOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val inspirationUrl = api.getInspiration()
            if (inspirationUrl == null) {
                call.respond(HttpStatusCode.InternalServerError)
                return@post
            }

            val inspiration = db.createInspiration(
                userId = userId,
                fileExtension = inspirationUrl.toExternalForm().substringAfterLast("."),
                inputStream = api.getInspirationImage(inspirationUrl)
            )
            if (inspiration == null) {
                call.respond(HttpStatusCode.InternalServerError)
                return@post
            }

            call.respond(HttpStatusCode.Created, inspiration)
        }
        put("/users/{userId}/inspirations/{inspirationId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
            val inspirationId = call.parameters["inspirationId"]?.toLongOrNull()
            val tags = call.receive<List<String>>()
            if (userId == null || inspirationId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            val inspiration = db.updateInspiration(inspirationId, tags)
            if (inspiration == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            call.respond(HttpStatusCode.OK, inspiration)
        }
        get("/users/{userId}/inspirations/{inspirationId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
            val inspirationId = call.parameters["inspirationId"]?.toLongOrNull()
            if (userId == null || inspirationId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val inspiration = db.findInspirationById(inspirationId)
            if (inspiration == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(inspiration)
        }
        get("/users/{userId}/inspirations/{inspirationId}/images") {
            val userId = call.parameters["userId"]?.toLongOrNull()
            val inspirationId = call.parameters["inspirationId"]?.toLongOrNull()
            if (userId == null || inspirationId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val image = db.findInspirationImageById(inspirationId)
            if (image == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respondFile(image)
        }
        get("/users/{userId}/inspirations") {
            val userId = call.parameters["userId"]?.toLongOrNull()
            val tagId = call.parameters["tagId"]?.toLongOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val inspirations = if (tagId == null) {
                db.findInspirationsByUserId(userId)
            } else {
                db.findInspirationsByTag(tagId)
            }
            call.respond(inspirations)
        }
        get("/tags") {
            val title = call.parameters["title"]

            val tags = if (title == null) {
                db.findTags()
            } else {
                db.findTagsByTitle(title)
            }
            call.respond(tags)
        }
    }
}