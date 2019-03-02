package com.pamelaahill

import java.io.ByteArrayInputStream
import java.net.URL

interface InspirationApi {
    suspend fun getInspiration(): URL?
    suspend fun getInspirationImage(imageURL: URL): ByteArrayInputStream
}