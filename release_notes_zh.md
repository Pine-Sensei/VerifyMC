[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.7.1) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.7.1 更新日志

## 安全修复

- 新增登录 IP 速率限制（每 IP 每分钟最多 5 次尝试），防止暴力破解
- 所有 API 响应添加 `X-Content-Type-Options`、`X-Frame-Options` 安全响应头
- AuthMe 配置中的表名/列名新增 SQL 标识符白名单校验，防止注入
- Debug 日志中移除验证码明文和密码哈希等敏感信息

## 关键 Bug 修复

- 修复 `sendReviewResult` 参数映射错误：4 处调用中 language 被错误传入 reason 参数
- 修复 WebSocket `broadcast()` → `broadcastMessage()` 方法名不匹配（审批/拒绝通知无法正确推送）
- 修复 BungeeCord 代理踢出消息乱码字符（`搂` → `§`）
- 修复 Token 清理线程未启动，导致过期 Token 持续堆积在内存中
- 修复 `VerifyCodeService`、`CaptchaService`、`WebAuthHelper` 清理线程在插件禁用时未停止（线程泄漏）
- 修复注册验证顺序：空值检查现在优先于数据库查询执行（防止 NPE）
- 修复 `auth_methods` 配置为空列表时强制要求邮箱验证码的问题

## 数据库可靠性

- MySQL：新增 `getConnection()` 连接有效性检查和自动重连机制
- MySQL：注册操作从 SELECT+INSERT 两步改为原子性 `INSERT IGNORE`，防止并发重复注册
- MySQL：JDBC URL 添加 `autoReconnect=true` 参数
- 文件存储：每次操作的全量同步写盘改为 dirty-flag + 5 秒后台定时刷盘，大幅提升性能

## 健壮性改进

- `OpsManager.isOp()` 使用本地快照迭代，避免与 `loadOps()` 的线程竞争
- `ConfigManager.getWebPort()`/`getWsPort()` 配置值超出范围时回退到默认值
- `FileAuditDao.save()` 失败时记录警告日志，不再静默忽略异常
- Admin 操作的用户名校验放宽，支持 Bedrock 用户名（点号前缀、连字符、空格）
- BungeeCord 代理 `PreLoginEvent` 处理改为异步模式，避免阻塞 Netty 事件循环
- `getApprovedUserCount()` 添加 Javadoc 说明实际返回的是非 pending 用户数
