package com.fishai.chatzen.data.model

import kotlinx.serialization.Serializable

import com.fishai.chatzen.R

@Serializable
enum class ModelType(val displayNameResId: Int) {
    CHAT(R.string.model_type_chat),
    REASONING(R.string.model_type_reasoning),
    CODE(R.string.model_type_code)
}

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: Provider,
    val created: Long = 0, // Timestamp in seconds (Unix time)
    val type: ModelType = inferModelType(id),
    val customProviderId: String? = null, // Only for Provider.CUSTOM
    val overrideVisionSupport: Boolean? = null, // Allow user to override vision support
    val enabled: Boolean = false, // Default to disabled
    val isReasoningDetected: Boolean = false // If true, model has outputted reasoning content in the past
) {
    companion object {
        fun inferModelType(id: String): ModelType {
            val lowerId = id.lowercase()
            return when {
                lowerId.contains("reasoner") || 
                lowerId.contains("reasoning") || 
                lowerId.contains("thinking") ||
                lowerId.contains("chain of thought") || 
                lowerId.contains("cot") ||
                lowerId.contains("deepthink") ||
                lowerId.contains("logic") ||
                lowerId.contains("k1") || 
                lowerId.contains("r1") || 
                lowerId.contains("qwq") || 
                lowerId.contains("o1-") || 
                lowerId.contains("o3-") ||
                lowerId.startsWith("o1") || 
                lowerId.startsWith("o3") -> ModelType.REASONING
                lowerId.contains("coder") || 
                lowerId.contains("code-") || // avoid matching generic words if possible, but code- is safer
                lowerId.contains("codellama") -> ModelType.CODE
                else -> ModelType.CHAT
            }
        }
    }

    val supportsDeepThinking: Boolean
        get() = type == ModelType.REASONING || 
                id.contains("mimo", ignoreCase = true) || 
                id.contains("pro", ignoreCase = true) ||
                provider == Provider.MINIMAX ||
                provider == Provider.ZHIPU ||
                isReasoningDetected

    val supportsVision: Boolean
        get() {
            if (overrideVisionSupport == true) return true

            val lowerId = id.lowercase()
            return lowerId.contains("vision") ||
                    lowerId.contains("gpt-4o") ||
                    lowerId.contains("claude-3") ||
                    lowerId.contains("gemini") ||
                    lowerId.contains("glm-4v")
        }

    val manufacturer: String
        get() {
            return when (provider) {
                Provider.OPENAI -> "OpenAI"
                Provider.CLAUDE -> "Anthropic"
                Provider.GEMINI -> "Google"
                Provider.DEEPSEEK -> "DeepSeek"
                Provider.MIMO -> "Xiaomi"
                Provider.MINIMAX -> "MiniMax"
                Provider.ZHIPU -> "Zhipu AI"
                Provider.ALIYUN -> "Alibaba Cloud"
                Provider.MOONSHOT -> "Moonshot AI"
                Provider.GROK -> "xAI"
                Provider.SILICONFLOW, Provider.VOLCENGINE, Provider.CUSTOM, Provider.NVIDIA -> {
                    val lowerId = id.lowercase()
                    when {
                        lowerId.contains("deepseek") -> "DeepSeek"
                        lowerId.contains("qwen") -> "Qwen"
                        lowerId.contains("yi-") || lowerId.contains("yi/") -> "01.AI"
                        lowerId.contains("glm") -> "Zhipu AI"
                        lowerId.contains("llama") -> "Meta"
                        lowerId.contains("doubao") -> "Doubao"
                        lowerId.contains("gemma") -> "Google"
                        lowerId.contains("mistral") -> "Mistral"
                        lowerId.contains("internlm") -> "InternLM"
                        lowerId.contains("minimax") -> "MiniMax"
                        lowerId.contains("moonshot") -> "Moonshot"
                        lowerId.contains("baichuan") -> "Baichuan"
                        lowerId.contains("step") -> "StepFun"
                        lowerId.contains("phi") || lowerId.contains("microsoft") -> "Microsoft"
                        // NVIDIA specific parsing (often nvidia/...)
                        lowerId.startsWith("nvidia") -> "NVIDIA" 
                        lowerId.contains("grok") -> "xAI"
                        else -> {
                            // Try to extract vendor from ID (e.g. "Vendor/Model" or "Pro/Vendor/Model")
                            val parts = id.split("/")
                            if (parts.size > 1) {
                                val vendorIndex = if (parts[0].equals("Pro", ignoreCase = true)) 1 else 0
                                if (vendorIndex < parts.size) {
                                    val vendor = parts[vendorIndex]
                                    // Normalize common vendor names
                                    when(vendor.lowercase()) {
                                        "meta" -> "Meta"
                                        "google" -> "Google"
                                        "microsoft" -> "Microsoft"
                                        "mistralai", "mistral" -> "Mistral"
                                        "nvidia" -> "NVIDIA"
                                        else -> vendor.replaceFirstChar { 
                                            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                                        }
                                    }
                                } else {
                                    if (provider == Provider.CUSTOM) "Custom" else "Other"
                                }
                            } else {
                                if (provider == Provider.CUSTOM) "Custom" else "Other"
                            }
                        }
                    }
                }
            }
        }

    val simpleName: String
        get() {
            // Remove manufacturer prefix if present to avoid redundancy in UI
            // E.g. "Qwen/Qwen2.5-72B" -> "Qwen2.5-72B"
            // "deepseek-ai/DeepSeek-R1" -> "DeepSeek-R1"
            // "nvidia/llama-3.1-nemotron-70b-instruct" -> "llama-3.1-nemotron-70b-instruct"
            
            val parts = id.split("/")
            if (parts.size > 1) {
                // Return the last part which is usually the model name
                return parts.last()
            }
            return id
        }
}

@Serializable
data class CustomProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val chatEndpoint: String = "/chat/completions"
)

@Serializable
enum class Provider(val displayNameResId: Int, val baseUrl: String = "", val websiteUrl: String = "") {
    OPENAI(R.string.provider_openai, "https://api.openai.com/v1/", "https://platform.openai.com/api-keys"),
    CLAUDE(R.string.provider_claude, "https://api.anthropic.com/v1/", "https://console.anthropic.com/settings/keys"),
    GEMINI(R.string.provider_gemini, "https://generativelanguage.googleapis.com/v1beta/", "https://aistudio.google.com/app/apikey"),
    DEEPSEEK(R.string.provider_deepseek, "https://api.deepseek.com/", "https://platform.deepseek.com/api_keys"),
    SILICONFLOW(R.string.provider_siliconflow, "https://api.siliconflow.cn/v1/", "https://cloud.siliconflow.cn/account/ak"),
    VOLCENGINE(R.string.provider_volcengine, "https://ark.cn-beijing.volces.com/api/v3/", "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey"),
    MIMO(R.string.provider_mimo, "https://api.xiaomimimo.com/v1/", "https://platform.xiaomimimo.com/"),
    MINIMAX(R.string.provider_minimax, "https://api.minimaxi.com/v1/", "https://platform.minimaxi.com/"),
    ZHIPU(R.string.provider_zhipu, "https://open.bigmodel.cn/api/paas/v4/", "https://open.bigmodel.cn/usercenter/apikeys"),
    ALIYUN(R.string.provider_aliyun, "https://dashscope.aliyuncs.com/compatible-mode/v1/", "https://bailian.console.aliyun.com/"),
    MOONSHOT(R.string.provider_moonshot, "https://api.moonshot.cn/v1/", "https://platform.moonshot.cn/console/api-keys"),
    NVIDIA(R.string.provider_nvidia, "https://integrate.api.nvidia.com/v1/", "https://build.nvidia.com/"),
    GROK(R.string.provider_grok, "https://api.x.ai/v1/", "https://console.x.ai/"),
    CUSTOM(R.string.provider_custom, "", "")
}

object ModelRegistry {
    val allModels = listOf(
        // MiniMax
        ModelInfo("MiniMax-M2.5", "MiniMax M2.5", Provider.MINIMAX, 1710000000, ModelType.CHAT),
        ModelInfo("MiniMax-M2.5-highspeed", "MiniMax M2.5 HighSpeed", Provider.MINIMAX, 1710000000, ModelType.CHAT),
        ModelInfo("MiniMax-M2.1", "MiniMax M2.1", Provider.MINIMAX, 1710000000, ModelType.CHAT),
        ModelInfo("MiniMax-M2.1-highspeed", "MiniMax M2.1 HighSpeed", Provider.MINIMAX, 1710000000, ModelType.CHAT),
        ModelInfo("MiniMax-M2", "MiniMax M2", Provider.MINIMAX, 1710000000, ModelType.CHAT),
        
        // Zhipu AI
        ModelInfo("glm-4-plus", "GLM-4 Plus", Provider.ZHIPU, 1705363200, ModelType.CHAT),
        ModelInfo("glm-4-air", "GLM-4 Air", Provider.ZHIPU, 1705363200, ModelType.CHAT),
        ModelInfo("glm-4-flash", "GLM-4 Flash", Provider.ZHIPU, 1705363200, ModelType.CHAT),
        ModelInfo("glm-4-long", "GLM-4 Long", Provider.ZHIPU, 1705363200, ModelType.CHAT),
        ModelInfo("glm-4v-plus", "GLM-4V Plus", Provider.ZHIPU, 1705363200, ModelType.CHAT),

        // Alibaba Cloud Bailian (Qwen)
        ModelInfo("qwen-max", "Qwen Max", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen-plus", "Qwen Plus", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen-turbo", "Qwen Turbo", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen-long", "Qwen Long", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen-vl-max", "Qwen VL Max", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen2.5-72b-instruct", "Qwen 2.5 72B", Provider.ALIYUN, 1726700000, ModelType.CHAT),
        ModelInfo("qwen2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B", Provider.ALIYUN, 1726700000, ModelType.CODE),

        // Moonshot (Kimi)
        ModelInfo("moonshot-v1-8k", "Moonshot V1 8k", Provider.MOONSHOT, 1700000000, ModelType.CHAT),
        ModelInfo("moonshot-v1-32k", "Moonshot V1 32k", Provider.MOONSHOT, 1700000000, ModelType.CHAT),
        ModelInfo("moonshot-v1-128k", "Moonshot V1 128k", Provider.MOONSHOT, 1700000000, ModelType.CHAT),

        // Xiaomi MiMo
        ModelInfo("mimo-v2-flash", "MiMo V2 Flash", Provider.MIMO, 1735689600, ModelType.CHAT),

        // OpenAI
        ModelInfo("gpt-4o", "GPT-4o", Provider.OPENAI, 1715600000, ModelType.CHAT),
        ModelInfo("gpt-4o-mini", "GPT-4o Mini", Provider.OPENAI, 1721300000, ModelType.CHAT),
        ModelInfo("o1-mini", "o1 Mini", Provider.OPENAI, 1726100000, ModelType.REASONING),
        ModelInfo("o3-mini", "o3 Mini", Provider.OPENAI, 1738300000, ModelType.REASONING),
        
        // Claude
        ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet (New)", Provider.CLAUDE, 1729600000, ModelType.CHAT),
        ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", Provider.CLAUDE, 1729600000, ModelType.CHAT),
        
        // Gemini
        ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", Provider.GEMINI, 1707950000, ModelType.CHAT),
        ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", Provider.GEMINI, 1715600000, ModelType.CHAT),
        
        // DeepSeek
        ModelInfo("deepseek-chat", "DeepSeek V3", Provider.DEEPSEEK, 1735170000, ModelType.CHAT),
        ModelInfo("deepseek-reasoner", "DeepSeek R1", Provider.DEEPSEEK, 1737330000, ModelType.REASONING),
        
        // SiliconFlow
        ModelInfo("deepseek-ai/DeepSeek-R1", "DeepSeek R1 (SF)", Provider.SILICONFLOW, 1737330000, ModelType.REASONING),
        ModelInfo("Qwen/Qwen2.5-72B-Instruct", "Qwen 2.5 72B", Provider.SILICONFLOW, 1726700000, ModelType.CHAT),
        ModelInfo("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder", Provider.SILICONFLOW, 1726700000, ModelType.CODE),
        
        // Volcengine
        ModelInfo("doubao-pro-32k", "Doubao Pro 32k", Provider.VOLCENGINE, 1715730000, ModelType.CHAT),
        ModelInfo("doubao-1.5-pro-32k", "Doubao 1.5 Pro", Provider.VOLCENGINE, 1727740800, ModelType.CHAT),
        ModelInfo("doubao-1.5-vision-pro-32k", "Doubao 1.5 Vision Pro", Provider.VOLCENGINE, 1727740800, ModelType.CHAT),

        // Grok (xAI)
        ModelInfo("grok-2-latest", "Grok 2", Provider.GROK, 1735689600, ModelType.CHAT),
        ModelInfo("grok-2-vision-latest", "Grok 2 Vision", Provider.GROK, 1735689600, ModelType.CHAT),
        ModelInfo("grok-beta", "Grok Beta", Provider.GROK, 1700000000, ModelType.CHAT)
    )
    
    fun getModelsByProvider(provider: Provider): List<ModelInfo> {
        return allModels.filter { it.provider == provider }
    }
}
