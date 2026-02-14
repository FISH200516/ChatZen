package com.fishai.chatzen.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)
