package com.pamelaahill

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import java.io.ByteArrayInputStream
import java.net.URL

private const val INSPIROBOT_URL = "http://inspirobot.me/api?generate=true"

class InspirationApiImpl : InspirationApi {

    @KtorExperimentalAPI
    override suspend fun getInspiration(): URL? {
        val httpClient = HttpClient(CIO)
        val url = httpClient.get<String?>(INSPIROBOT_URL)
        return if (url != null) {
            URL(url)
        } else {
            null
        }
    }

    @KtorExperimentalAPI
    override suspend fun getInspirationImage(imageURL: URL): ByteArrayInputStream {
        val httpClient = HttpClient(CIO)
        val file = httpClient.get<ByteArray>(imageURL)
        return file.inputStream()
    }
}