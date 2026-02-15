# Live Updates 实时更新通知集成 Spec

## Why
ChatZen应用在对话生成过程中，用户需要切换到其他应用时无法直观地看到AI回复的进度。通过集成Android 16 (API 36+) 的Live Updates功能，可以在通知栏实时显示对话生成的各个阶段，提升用户体验。

## What Changes
- 新增 `LiveUpdateNotificationManager` 管理器，负责创建和更新实时通知
- 定义对话生成的阶段状态枚举（初始化、思考中、生成中、完成）
- 在 `ChatViewModel` 中集成通知管理，在对话生成各阶段更新通知
- 添加 `POST_PROMOTED_NOTIFICATIONS` 权限声明
- 添加必要的依赖项（accompanist-permissions）
- 添加通知图标资源

## Impact
- Affected specs: ChatViewModel, AndroidManifest, build.gradle.kts
- Affected code: 
  - `ChatViewModel.kt` - 集成通知管理
  - `AndroidManifest.xml` - 添加权限
  - `app/build.gradle.kts` - 添加依赖
  - 新增 `LiveUpdateNotificationManager.kt`
  - 新增 drawable 资源文件

## ADDED Requirements

### Requirement: Live Updates 通知管理器
系统 SHALL 提供 `LiveUpdateNotificationManager` 用于管理实时更新通知。

#### Scenario: 初始化通知管理器
- **WHEN** 应用启动时
- **THEN** 通知管理器被初始化，创建通知渠道

#### Scenario: 开始对话生成
- **WHEN** 用户发送消息开始对话生成
- **THEN** 显示"正在思考"阶段的实时更新通知

#### Scenario: AI开始回复
- **WHEN** AI开始流式输出内容
- **THEN** 更新通知为"正在生成回复"阶段

#### Scenario: 对话完成
- **WHEN** AI完成回复
- **THEN** 更新通知为"已完成"阶段，显示简短摘要

### Requirement: 对话生成阶段状态
系统 SHALL 定义以下对话生成阶段：

| 阶段 | 状态 | 进度 | 说明 |
|------|------|------|------|
| THINKING | 思考中 | 25% | 正在处理用户输入 |
| GENERATING | 生成中 | 50% | AI正在生成回复 |
| STREAMING | 输出中 | 75% | 正在流式输出内容 |
| COMPLETED | 已完成 | 100% | 对话生成完成 |

### Requirement: 权限管理
系统 SHALL 请求和管理以下权限：
- `POST_NOTIFICATIONS` - 发布通知权限（运行时权限）
- `POST_PROMOTED_NOTIFICATIONS` - 发布推广通知权限（安装时权限）

#### Scenario: 权限检查
- **WHEN** 用户首次使用对话功能
- **THEN** 检查并请求通知权限

### Requirement: API 版本兼容
系统 SHALL 在 API 36+ 设备上启用 Live Updates 功能，在低版本设备上降级为普通通知。

#### Scenario: 高版本设备
- **WHEN** 设备运行 Android 16 (API 36+)
- **THEN** 启用完整的 Live Updates 功能

#### Scenario: 低版本设备
- **WHEN** 设备运行 Android 15 或更低版本
- **THEN** 使用普通进度通知作为降级方案

## MODIFIED Requirements

### Requirement: ChatViewModel 对话生成流程
ChatViewModel 的对话生成流程 SHALL 集成实时更新通知：

1. `sendMessage()` 方法开始时调用 `LiveUpdateNotificationManager.startGeneration()`
2. `streamResponse()` 流式响应期间调用 `LiveUpdateNotificationManager.updateProgress()`
3. 响应完成时调用 `LiveUpdateNotificationManager.completeGeneration()`
4. 取消生成时调用 `LiveUpdateNotificationManager.cancelGeneration()`
