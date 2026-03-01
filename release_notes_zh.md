[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.6.5) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.6.5 更新日志

## 优化与修复

- **前端弹窗重构**：统一了所有模态框（确认、密码、拒绝、版本更新）使用共享的 `Dialog` 组件，消除了代码冗余。
- **用户管理重构**：将用户管理模块拆分为独立组件（用户列表、待审核列表），优化了代码结构与加载逻辑。
- **交互体验升级**：增加了全屏遮罩、ESC 键关闭支持、自动焦点管理（打开时自动聚焦、防止误操作）。
- **无障碍性改进**：添加了标准的 `aria-labelledby` 和 `role="dialog"` 属性。
- **视觉修复**：调整了弹窗遮罩层级（z-index 60），解决了被其他 UI 元素遮挡的问题。
- **依赖更新**：更新了部分内部依赖。

# VerifyMC v1.6.3 更新日志

# VerifyMC v1.6.0 更新日志

## 架构：模块化重构

- 引入 `PluginContext` 作为中心服务容器，替代 `VerifyMC` 中分散的字段和 `WebServer` 的 13 参数构造函数
- 将 `ConfigManager`、`I18nManager`、`OpsManager`、`ResourceManager` 提取到 `core/` 包，实现清晰的职责分离
- 将单体 `WebServer`（约 1800 行）拆分为 20+ 个单一职责的 `HttpHandler`，位于 `web/handler/` 包
- 新增 `ApiRouter` 集中管理路由注册
- 新增 `VmcCommandExecutor`，支持游戏内管理命令（`/vmc approve/reject/ban/unban/delete/list/info`）
- 新增 `PlayerLoginListener`，支持插件模式白名单拦截和基于状态的踢出消息
- 管理端点认证统一通过 `AdminAuthUtil` 工具类实现

## 标识符迁移：UUID → 用户名

- 移除 UUID 作为主要用户标识，用户名现为所有层的唯一键
- `UserDao`、`FileUserDao`、`MysqlUserDao` 全面改用基于用户名的查询和操作
- `UserDao` 接口新增 `getUserByEmail`、`getUserByUsernameExact` 方法
- `UserDao` 新增便捷默认方法：`banUser`、`unbanUser`、`getUsers`、`getTotalUsers`、`getUsersByStatus`、`updatePassword`

## 后台登录重构

- 移除静态 `admin.password` 配置，改为基于玩家数据的认证
- 后台登录现在验证已注册玩家的凭据，仅服务器 OP 可访问管理面板
- 登录支持用户名和邮箱查找，集成 AuthMe 密码验证

## 安全改进

- 新增 `PasswordUtil` 工具类，SHA-256 + 盐值哈希（兼容 AuthMe `$SHA$` 格式）
- 明文密码回退时输出日志警告，提醒迁移
- 管理端点认证统一通过 `AdminAuthUtil` 实现
- 查询参数添加 URL 解码，防止编码字符注入

## API 端点重构

- RESTful 风格 API 路径：`/api/admin/users`、`/api/admin/user/approve`、`/api/admin/user/reject` 等
- 问卷端点迁移至 `/api/questionnaire/config` 和 `/api/questionnaire/submit`
- 新增 `/api/admin/sync` 端点，支持 AuthMe 数据同步
- 新增 `/api/user/status` 端点，支持用户状态查询

## 前端改进

- 后台登录表单更新为用户名 + 密码字段
- API 服务层同步更新为新的 RESTful 端点路径
- 通知消息简化统一

## 依赖更新

- MySQL 驱动从 `mysql-connector-java` 更新为 `mysql-connector-j`
- 移除重复的 `jakarta.mail-api` 依赖
- Maven Shade Plugin 更新至 3.5.0，优化过滤规则并添加 `ServicesResourceTransformer`

## 缺陷修复

- 修复 AuthMe 同步在状态升级时跳过已封禁用户的问题
- 修复问卷结果中评分服务不可用状态检测
- 修复 `WebResponseHelper.readJson` 对非法 JSON 请求体的处理
