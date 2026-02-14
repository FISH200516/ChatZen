# ChatZen

ChatZen is a modern, feature-rich AI chat application for Android, built with Jetpack Compose. It aggregates multiple AI providers into a single, unified interface, offering a seamless chat experience with support for advanced features like Markdown rendering, LaTeX math, code highlighting, and vision capabilities.

ChatZen 是一款基于 Jetpack Compose 构建的现代化、功能丰富的 Android AI 聊天应用。它将多个 AI 提供商聚合到一个统一的界面中，提供无缝的聊天体验，并支持 Markdown 渲染、LaTeX 数学公式、代码高亮和视觉功能等高级特性。

---

## 🇬🇧 English Introduction

### ✨ Key Features

- **Multi-Provider Support**: Seamlessly switch between major AI providers including OpenAI, DeepSeek, Google Gemini, Anthropic Claude, Zhipu AI (GLM-4), Alibaba Cloud (Qwen), Moonshot (Kimi), and more.
- **Rich Text Rendering**: 
  - Full **Markdown** support.
  - **LaTeX** math equation rendering (perfect for academic and scientific queries).
  - **Syntax Highlighting** for code blocks.
- **Vision Support**: Upload and analyze images with supported vision models (e.g., GPT-4o, Claude 3.5 Sonnet, Gemini 1.5 Pro, Qwen-VL).
- **Streamed Responses**: Experience real-time text generation with typing effects.
- **Usage Statistics**: Track your token consumption and request counts with interactive daily/weekly/monthly charts.
- **Customization**:
  - **Dynamic Theming**: Material 3 design with support for light/dark modes and dynamic colors.
  - **Custom Providers**: Add any OpenAI-compatible API endpoint.
  - **Model Management**: Enable or disable specific models to suit your needs.
- **Privacy First**: API keys and chat history are stored locally on your device.

### 🤖 Supported Providers

ChatZen integrates with a wide range of official and third-party API providers:

- **Global Leaders**: OpenAI (GPT-4o), Anthropic (Claude 3.5), Google (Gemini 1.5), xAI (Grok).
- **Leading Chinese Models**: 
  - **DeepSeek** (V3, R1)
  - **Zhipu AI** (GLM-4 Plus/Air/Flash)
  - **Alibaba Cloud** (Qwen/Tongyi Qianwen)
  - **Moonshot AI** (Kimi)
  - **MiniMax**
  - **01.AI** (Yi Series via SiliconFlow)
  - **ByteDance** (Doubao via Volcengine)
- **Aggregators**: SiliconFlow.
- **Custom**: Support for any OpenAI-compatible API (e.g., local LLMs via Ollama).

### 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Async Processing**: Kotlin Coroutines & Flow
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room) & DataStore
- **Key Libraries**:
  - `coil-compose`: Image loading
  - `vico`: specific charting library for Jetpack Compose
  - `multiplatform-markdown-renderer`: Markdown rendering
  - `jlatexmath-android`: LaTeX rendering

### 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/FISH200516/ChatZen.git
    ```
2.  **Open in Android Studio**:
    - Ensure you have the latest version of Android Studio (Koala or later recommended).
    - Sync Gradle project.
3.  **Build and Run**:
    - Select your device/emulator and click "Run".
    - Minimum SDK: Android 8.0 (API 26).
4.  **Configure API Keys**:
    - Go to **Settings** -> **Service Providers**.
    - Enter your API keys for the services you wish to use.

---

## 🇨🇳 中文介绍

### ✨ 核心功能

- **多模型聚合**: 无缝切换主流 AI 服务商，包括 OpenAI、DeepSeek、Google Gemini、Claude、智谱 AI (GLM-4)、通义千问 (Qwen)、月之暗面 (Kimi) 等。
- **富文本渲染**:
  - 完整的 **Markdown** 支持。
  - **LaTeX** 数学公式渲染（非常适合学术和科学问答）。
  - 代码块 **语法高亮**。
- **视觉模型支持**: 支持上传图片并使用视觉模型（如 GPT-4o, Claude 3.5, Gemini, Qwen-VL）进行分析。
- **流式响应**: 体验打字机效果的实时文本生成。
- **用量统计**: 通过交互式的日/周/月图表追踪您的 Token 消耗和请求次数。
- **个性化定制**:
  - **动态主题**: Material 3 设计，支持深色/浅色模式及动态取色。
  - **自定义服务商**: 支持添加任何兼容 OpenAI 格式的 API 接口。
  - **模型管理**: 根据需要启用或禁用特定模型。
- **隐私优先**: API 密钥和聊天记录仅存储在您的本地设备上。

### 🤖 支持的服务商

ChatZen 集成了广泛的官方和第三方 API 服务：

- **国际领跑者**: OpenAI (GPT-4o), Anthropic (Claude 3.5), Google (Gemini 1.5), xAI (Grok).
- **国内领先模型**:
  - **DeepSeek** (深度求索 V3, R1)
  - **智谱 AI** (GLM-4 Plus/Air/Flash)
  - **阿里云百炼** (通义千问 Qwen)
  - **月之暗面** (Kimi)
  - **MiniMax** (海螺)
  - **零一万物** (Yi 系列，通过 SiliconFlow)
  - **火山引擎** (豆包 Doubao)
- **聚合平台**: SiliconFlow (硅基流动).
- **自定义**: 支持任何 OpenAI 兼容的 API (例如通过 Ollama 部署的本地大模型)。

### 🛠️ 技术栈

- **开发语言**: [Kotlin](https://kotlinlang.org/)
- **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **架构模式**: MVVM (Model-View-ViewModel)
- **网络请求**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **异步处理**: Kotlin Coroutines & Flow
- **本地存储**: [Room Database](https://developer.android.com/training/data-storage/room) & DataStore
- **关键库**:
  - `coil-compose`: 图片加载
  - `vico`: 适用于 Compose 的图表库
  - `multiplatform-markdown-renderer`: Markdown 渲染
  - `jlatexmath-android`: LaTeX 公式渲染

### 🚀 快速开始

1.  **克隆项目**:
    ```bash
    git clone https://github.com/yourusername/ChatZen.git
    ```
2.  **在 Android Studio 中打开**:
    - 确保使用较新版本的 Android Studio。
    - 同步 Gradle 项目。
3.  **编译运行**:
    - 选择您的设备或模拟器并点击 "Run"。
    - 最低支持版本: Android 8.0 (API 26)。
4.  **配置 API Key**:
    - 进入 **设置 (Settings)** -> **服务提供商 (Service Providers)**。
    - 输入您想要使用的服务的 API Key。

---

Developed with ❤️ by FishAI Team.
