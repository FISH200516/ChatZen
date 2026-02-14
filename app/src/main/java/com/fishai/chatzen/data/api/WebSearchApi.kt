package com.fishai.chatzen.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// Tavily
interface TavilyApi {
    @POST("search")
    suspend fun search(
        @Body request: TavilyRequest
    ): TavilyResponse
}

@Serializable
data class TavilyRequest(
    val api_key: String,
    val query: String,
    val search_depth: String = "basic",
    val max_results: Int = 5,
    val include_answer: Boolean = false,
    val include_images: Boolean = false,
    val include_raw_content: Boolean = false
)

@Serializable
data class TavilyResponse(
    val results: List<TavilyResult> = emptyList()
)

@Serializable
data class TavilyResult(
    val title: String,
    val url: String,
    val content: String
)

// Bing
interface BingApi {
    @GET("search")
    suspend fun search(
        @Header("Ocp-Apim-Subscription-Key") apiKey: String,
        @Query("q") query: String,
        @Query("count") count: Int = 5
    ): BingResponse
}

@Serializable
data class BingResponse(
    val webPages: BingWebPages? = null
)

@Serializable
data class BingWebPages(
    val value: List<BingResult> = emptyList()
)

@Serializable
data class BingResult(
    val name: String,
    val url: String,
    val snippet: String
)

// ByteDance (Placeholder - Generic Structure)
interface ByteDanceApi {
    @POST("search") // Placeholder
    suspend fun search(
        @Header("Authorization") apiKey: String,
        @Body request: ByteDanceRequest
    ): ByteDanceResponse
}

@Serializable
data class ByteDanceRequest(
    val query: String,
    val limit: Int = 5
)

@Serializable
data class ByteDanceResponse(
    val data: List<ByteDanceResult> = emptyList()
)

@Serializable
data class ByteDanceResult(
    val title: String,
    val url: String,
    val summary: String
)

// Baidu
interface BaiduApi {
    @POST("v2/ai_search/web_search")
    suspend fun search(
        @Header("X-Appbuilder-Authorization") apiKey: String,
        @Body request: BaiduSearchRequest
    ): BaiduSearchResponse
}

@Serializable
data class BaiduSearchRequest(
    val messages: List<BaiduMessage>,
    val search_source: String = "baidu_search_v2",
    val resource_type_filter: List<BaiduResourceType> = listOf(BaiduResourceType("web", 10))
)

@Serializable
data class BaiduMessage(
    val role: String,
    val content: String
)

@Serializable
data class BaiduResourceType(
    val type: String,
    val top_k: Int
)

@Serializable
data class BaiduSearchResponse(
    val references: List<BaiduReference> = emptyList()
)

@Serializable
data class BaiduReference(
    val title: String? = null,
    val url: String? = null,
    val content: String? = null
)
