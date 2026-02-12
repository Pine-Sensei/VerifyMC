[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.2.8) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.2.8 更新日志

## 新功能

### 多步注册流程与问卷强制审核 (PR #18)

- 实现了注册前强制性多步骤问卷工作流
- 新增文本题类型，支持基于 LLM 的问答评分 (DeepSeek / Google Gemini)
- 熔断器、重试、并发控制及低置信度自动转人工审核
- 问卷 token 生命周期管理，防止重放攻击
- 可配置 `require_pass_before_register`

### 基岩版支持

- 新增 Java/Bedrock 双模式平台选择器（玻璃风格切换按钮）
- 基岩版用户名自动规范化（可配置前缀）
- 基岩版玩家独立的用户名正则校验

### 代理端 API Key 鉴权

- 统一 `security.api_key` / `proxy.api_key` 用于代理端到后端接口鉴权
- 使用时间恒定比较防止时序攻击
- 代理端启动时若 API key 为空将输出警告

## Bug 修复

### i18n 语言偏好持久化修复

- 修复了 `i18n.ts` 中运算符优先级错误导致语言偏好始终解析为 `zh` 的问题

### 登录热路径性能优化

- 将 `onPlayerLogin` 中的 O(n) 全用户扫描替换为 DAO 层索引查找

### 代理端白名单响应解析修复

- 修复了 `ApiClient.java` 中 `success`（请求成功）与 `found`（玩家存在）的区分逻辑
- 向后兼容旧协议

### MySQL 索引检测逻辑修复

- 修复了 `SHOW INDEX` 检测逻辑静默跳过索引创建的问题

## 改进

- 管理员审核面板现在显示问卷得分和评审摘要
- 管理员登录后导航栏自动隐藏注册入口
- 日志中对敏感数据（邮箱、验证码）进行脱敏处理
- 清理线程改为守护线程并支持正确中断
