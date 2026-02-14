[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.5.1) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.5.1 更新日志

## 🐛 Bug 修复

- 密码正则表达式默认值与 AuthMe 对齐（从 `^[a-zA-Z0-9_]{3,16}$` 改为 `^[!-~]{5,30}$`），修复包含特殊字符或超过 16 位的密码注册失败问题
- 修复手动审批时将哈希密码传给 AuthMe 的问题（改为跳过，管理员通过 `/api/change-password` 设置密码）
- 修复 MysqlUserDao 未对密码哈希的问题（现与 FileUserDao 一致）
- 修复 `/vmc reload` 导致其他插件类加载器关闭的问题，改为安全的配置热重载（不再 disable/enable 插件）

## � 安全改进

- Token 生成改用 `SecureRandom` 替代 `Math.random()`
- 管理员密码比较改用常量时间方法，防止时序攻击
- 邮件通知线程设为守护线程，防止阻止服务器关闭

## 🌐 国际化与体验优化

- 硬编码的"Username already exists"消息改为 i18n 支持
- 新增 `/vmc reload` 成功/失败/主题变更提示消息（中英文）
- 问卷服务（QuestionnaireService）支持通过 `/vmc reload` 热重载配置
