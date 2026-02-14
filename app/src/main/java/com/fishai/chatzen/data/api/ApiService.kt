package com.fishai.chatzen.data.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OpenAIApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
    
    @Streaming
    @POST("chat/completions")
    fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): Call<ResponseBody>
    
    @GET("models")
    suspend fun getModels(@Header("Authorization") authorization: String): OpenAIModelListResponse

    // For custom base URLs (DeepSeek, SiliconFlow, Volcengine)
    @POST
    suspend fun chatCompletionUrl(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse

    @Streaming
    @POST
    fun chatCompletionUrlStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): Call<ResponseBody>

    @GET
    suspend fun getModelsUrl(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): OpenAIModelListResponse

    @Streaming
    @POST
    fun responsesUrlStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: OpenAIResponsesRequest
    ): Call<ResponseBody>
}

interface ClaudeApi {
    @Headers("anthropic-version: 2023-06-01")
    @POST("messages")
    suspend fun chatCompletion(
        @Header("x-api-key") apiKey: String,
        @Body request: ClaudeChatRequest
    ): ClaudeChatResponse
}

interface GeminiApi {
    @POST
    suspend fun chatCompletion(
        @Url url: String, // Full URL including key: ...:generateContent?key=API_KEY
        @Body request: GeminiChatRequest
    ): GeminiChatResponse
}
