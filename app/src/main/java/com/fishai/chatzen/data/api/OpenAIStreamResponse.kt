package com.fishai.chatzen.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OpenAIStreamResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice> = emptyList(),
    val usage: OpenAIUsage? = null
)
