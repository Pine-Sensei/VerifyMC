[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.5.1) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.5.1 更新日志

## Architecture / 架构：注册流程重构

- 将注册处理逻辑提取到独立的 `RegistrationProcessingHandler`，建立清晰的校验流水线
- 引入 `RegistrationOutcome` 枚举和 `RegistrationOutcomeResolver`，实现确定性的注册结果判定
- 新增 `RegistrationApplicationService`、`QuestionnaireApplicationService`、`ReviewApplicationService` 业务编排服务
- 创建 `ApiResponseFactory`、`WebResponseHelper`、`WebAuthHelper` 工具类，减少代码重复
- 引入 `RegistrationRequest` 和 `RegistrationValidationResult` 记录类，实现类型安全的请求/响应处理
- 新增委托 Handler 类（`RegistrationHandler`、`QuestionnaireHandler`、`ReviewHandler`、`UserAdminHandler`）作为扩展点

## Security / 安全改进

- Token 生成改用 `SecureRandom` 替代 `Math.random()`，确保密码学安全
- Token 清理线程设为守护线程，支持正确的中断处理，防止阻塞服务器关闭
- 密码存储统一通过 `AuthmeService.encodePasswordForStorage()` 处理，移除 `MysqlUserDao` 中的重复哈希
- 启动时自动迁移明文密码

## Frontend / 前端改进

- 新增 `useAdminUsers` composable，支持分页、搜索和多层降级（含单元测试）
- 新增问卷相关类型接口（`Question`、`QuestionType`、`SubmitQuestionnaireResponse`）
- 注册表单支持平台字段和基岩版用户名自动规范化
- 注册流程支持问卷 `manual_review_required`
- 全局固定背景，所有页面统一视觉体验

## Bug Fixes / 缺陷修复

- 修复 `shouldAutoApprove` 现在正确尊重 `manualReviewRequired` 标志
- 修复 `RegistrationOutcomeResolver` 在问卷通过但仍需人工审核场景下的逻辑
- 移除 `WebServer` 中重复的 `buildQuestionnaireReviewSummary`
- `AuditDao`/`AuditRecord` 从 `Map<String,Object>` 迁移到类型安全的 record 类
- `MysqlUserDao` 新增 `updateUserEmail` 以支持 AuthMe 同步
