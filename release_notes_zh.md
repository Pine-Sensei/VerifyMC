[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.2.9) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.2.9 更新日志

## 注册与问卷流程稳定化

- 恢复基岩版/Java 平台选择器的显示并修复相关显示问题
- 修复基岩版用户名正则校验逻辑
- 解决注册下一步流程卡住的问题
- 恢复集成式问卷步骤流程
- 确保自动审批通过时正确显示白名单确认
- 开启问卷后自动强制要求通过（移除 `auto_approve_on_pass` 和 `require_pass_before_register`，统一使用 `register.auto_approve`）

## LLM 与评分改进

- 将 `OpenAICompatibleScoringProvider` 改为具体类，移除空壳子类 `DeepSeekScoringProvider` 和 `GoogleScoringProvider`
- 新增 `llm.enabled` 开关，关闭后 text 题自动转人工审核
- 强制要求 `llm.api_base` 使用 HTTPS，防止 API key 泄露
- 移除全局 `llm.max_score`，text 题应在题目级别定义 `max_score`
- 同步配置帮助文档与 LLM 配置默认值

## 管理面板重构

- 将审核和用户管理合并到 `AdminPanelNew`
- 通过 `sessionService` 集中管理 admin session 和认证重定向
- 新增拒绝理由弹窗
- 问卷得分和评审摘要列根据配置动态显示
- 拒绝操作现在会移除白名单并注销 Authme 账户

## 清理

- 移除废弃组件：`AdminReviewPanel`、`AllUsersPanel`、`NotificationBar`、`NotificationContainer`、`NotificationDemo`
- 移除插件启动时的 `ResourceUpdater` 初始化
- 合并 JSON POST 请求逻辑和通知辅助方法 (`notifyResult`)
