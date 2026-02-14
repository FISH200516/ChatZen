package com.fishai.chatzen.data.repository

import com.fishai.chatzen.data.api.RetrofitClient
import com.fishai.chatzen.data.api.TavilyRequest
import com.fishai.chatzen.data.api.ByteDanceRequest
import com.fishai.chatzen.data.model.WebSearchProvider
import com.fishai.chatzen.data.model.WebSearchResult
import kotlinx.coroutines.flow.first

import com.fishai.chatzen.data.api.BaiduMessage
import com.fishai.chatzen.data.api.BaiduSearchRequest

class WebSearchRepository(private val settingsRepository: SettingsRepository) {

    suspend fun search(query: String): List<WebSearchResult> {
        val provider = settingsRepository.getWebSearchProvider().first()
        val apiKey = settingsRepository.getWebSearchApiKey(provider).first()

        if (provider != WebSearchProvider.NONE && apiKey.isNullOrBlank()) {
             // For some providers/modes we might not need a key, but generally yes.
             return emptyList() 
        }

        return when (provider) {
            WebSearchProvider.TAVILY -> searchTavily(apiKey!!, query)
            WebSearchProvider.BING -> searchBing(apiKey!!, query)
            WebSearchProvider.BYTEDANCE -> searchByteDance(apiKey!!, query)
            WebSearchProvider.BAIDU -> searchBaidu(apiKey!!, query)
            WebSearchProvider.NONE -> emptyList()
        }
    }

    private suspend fun searchTavily(apiKey: String, query: String): List<WebSearchResult> {
        return try {
            val response = RetrofitClient.tavilyApi.search(
                TavilyRequest(api_key = apiKey, query = query)
            )
            response.results.map {
                WebSearchResult(it.title, it.url, it.content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun searchBing(apiKey: String, query: String): List<WebSearchResult> {
        return try {
            val response = RetrofitClient.bingApi.search(apiKey, query)
            response.webPages?.value?.map {
                WebSearchResult(it.name, it.url, it.snippet)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun searchByteDance(apiKey: String, query: String): List<WebSearchResult> {
         return try {
            // Placeholder implementation
            val response = RetrofitClient.byteDanceApi.search("Bearer $apiKey", ByteDanceRequest(query))
            response.data.map {
                WebSearchResult(it.title, it.url, it.summary)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private suspend fun searchBaidu(apiKey: String, query: String): List<WebSearchResult> {
        return try {
            val response = RetrofitClient.baiduApi.search(
                apiKey = "Bearer $apiKey",
                request = BaiduSearchRequest(
                    messages = listOf(BaiduMessage(role = "user", content = query))
                )
            )
            response.references.map {
                WebSearchResult(
                    title = it.title ?: "",
                    url = it.url ?: "",
                    snippet = it.content ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
