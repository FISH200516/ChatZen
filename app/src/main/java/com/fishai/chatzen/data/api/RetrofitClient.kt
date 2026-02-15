package com.fishai.chatzen.data.api

import com.fishai.chatzen.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()

    private object NetworkLogConfig {
        @Volatile var enabled: Boolean = BuildConfig.DEBUG
        @Volatile var level: HttpLoggingInterceptor.Level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
        val redacted = setOf("Authorization", "Cookie", "Set-Cookie", "X-Api-Key", "Api-Key")
    }

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (NetworkLogConfig.enabled) NetworkLogConfig.level else HttpLoggingInterceptor.Level.NONE
            NetworkLogConfig.redacted.forEach { redactHeader(it) }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    private val okHttpClient by lazy { createOkHttpClient() }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // Singletons for standard providers
    val openAIApi: OpenAIApi by lazy { createRetrofit("https://api.openai.com/v1/").create(OpenAIApi::class.java) }
    val claudeApi: ClaudeApi by lazy { createRetrofit("https://api.anthropic.com/v1/").create(ClaudeApi::class.java) }
    val geminiApi: GeminiApi by lazy { createRetrofit("https://generativelanguage.googleapis.com/v1beta/").create(GeminiApi::class.java) }

    // Web Search Providers
    val tavilyApi: TavilyApi by lazy { createRetrofit("https://api.tavily.com/").create(TavilyApi::class.java) }
    val bingApi: BingApi by lazy { createRetrofit("https://api.bing.microsoft.com/v7.0/").create(BingApi::class.java) }
    
    // Placeholders for ByteDance and Baidu - endpoints are illustrative
    val byteDanceApi: ByteDanceApi by lazy { createRetrofit("https://api.search.bytedance.com/").create(ByteDanceApi::class.java) }
    val baiduApi: BaiduApi by lazy { createRetrofit("https://qianfan.baidubce.com/").create(BaiduApi::class.java) }
}
