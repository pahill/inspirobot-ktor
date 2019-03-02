package com.pamelaahill

import java.net.URL

data class Inspiration(
    val id: Long,
    val userId: Long,
    val fileUrl: URL,
    val tags: List<Tag>,
    val generatedDate: Long
)

data class Tag(
    val id: Long,
    val title: String
)

