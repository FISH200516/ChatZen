package com.fishai.chatzen.data.model

import com.fishai.chatzen.R

enum class WebSearchProvider(
    val displayNameResId: Int, 
    val apiKeyUrl: String = ""
) {
    BYTEDANCE(R.string.search_provider_bytedance, "https://console.volcengine.com/ark/region:ark+cn-beijing/search"),
    BAIDU(R.string.search_provider_baidu, "https://console.bce.baidu.com/ai_apaas/appKey"),
    BING(R.string.search_provider_bing, "https://portal.azure.com/"),
    TAVILY(R.string.search_provider_tavily, "https://app.tavily.com/"),
    NONE(R.string.search_provider_none)
}
