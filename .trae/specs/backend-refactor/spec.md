# 后端重构规范

## Why
VerifyMC 后端代码经过多次迭代开发，存在架构不清晰、代码重复、部分逻辑分散等问题。需要进行全面梳理与重构，以提升代码质量、可维护性和可扩展性，同时修复可能存在的逻辑错误。

## What Changes
- 重构 WebServer 类，拆分过长的路由处理逻辑
- 统一 API 响应格式和错误处理机制
- 优化数据访问层 (DAO) 的代码结构
- 改进服务层的依赖注入和职责划分
- 增强代码的可测试性
- 修复潜在的逻辑错误和边界条件处理

## Impact
- Affected specs: 用户注册流程、审核流程、管理后台
- Affected code: 
  - `plugin/src/main/java/team/kitemc/verifymc/web/` - Web 层
  - `plugin/src/main/java/team/kitemc/verifymc/service/` - 服务层
  - `plugin/src/main/java/team/kitemc/verifymc/db/` - 数据访问层
  - `plugin/src/main/java/team/kitemc/verifymc/registration/` - 注册流程

---

## 现有功能全面梳理

### 1. 项目结构概览

```
plugin/src/main/java/team/kitemc/verifymc/
├── VerifyMC.java                 # 主插件类 (877行)
├── Metrics.java                  # bStats 指标收集
├── ResourceManager.java          # 资源管理器 (504行)
├── db/                           # 数据访问层
│   ├── UserDao.java              # 用户数据接口 (184行)
│   ├── FileUserDao.java          # JSON 文件存储 (608行)
│   ├── MysqlUserDao.java         # MySQL 存储 (727行)
│   ├── AuditDao.java             # 审计日志接口
│   ├── AuditRecord.java          # 审计记录实体
│   ├── FileAuditDao.java         # JSON 审计存储 (106行)
│   └── MysqlAuditDao.java        # MySQL 审计存储 (63行)
├── mail/
│   └── MailService.java          # 邮件服务 (308行)
├── registration/
│   ├── RegistrationOutcome.java  # 注册结果枚举
│   ├── RegistrationOutcomeResolver.java  # 注册结果解析器
│   └── RegistrationOutcomeMessageKeyMapper.java  # 消息映射
├── service/
│   ├── AuthmeService.java        # AuthMe 集成 (420行)
│   ├── CaptchaService.java       # 图形验证码 (303行)
│   ├── DiscordService.java       # Discord OAuth (624行)
│   ├── EssayScoringService.java  # 评分服务接口 (104行)
│   ├── OpenAICompatibleScoringProvider.java  # LLM 评分 (305行)
│   ├── QuestionnaireApplicationService.java  # 问卷应用服务
│   ├── QuestionnaireService.java # 问卷服务 (488行)
│   ├── RegistrationApplicationService.java   # 注册应用服务 (50行)
│   ├── ReviewApplicationService.java         # 审核应用服务 (15行)
│   ├── VerifyCodeService.java    # 验证码服务 (175行)
│   └── VersionCheckService.java  # 版本检查 (274行)
└── web/
    ├── AdminUserOperationHandler.java  # 管理操作处理器 (60行)
    ├── ApiResponseFactory.java         # API 响应工厂 (24行)
    ├── QuestionnaireHandler.java       # 问卷处理器 (委托模式)
    ├── RegistrationHandler.java        # 注册处理器 (委托模式)
    ├── RegistrationProcessingHandler.java  # 注册处理逻辑 (364行)
    ├── RegistrationRequest.java        # 注册请求实体 (33行)
    ├── RegistrationValidationResult.java  # 验证结果 (18行)
    ├── ReviewHandler.java              # 审核处理器 (委托模式)
    ├── ReviewWebSocketServer.java      # WebSocket 服务器 (81行)
    ├── UserAdminHandler.java           # 用户管理处理器 (委托模式)
    ├── WebAuthHelper.java              # 认证助手 (72行)
    ├── WebResponseHelper.java          # 响应助手 (32行)
    ├── WebServer.java                  # Web 服务器 (1862行)
    └── StaticHandler (内部类)          # 静态资源处理
```

### 2. 核心模块详细分析

#### 2.1 主插件类 (VerifyMC.java)

**职责**:
- 插件生命周期管理 (onEnable/onDisable)
- 配置加载与验证
- 服务初始化与依赖注入
- 白名单同步机制
- 玩家登录拦截
- 命令处理 (/vmc)

**关键流程**:
```
onEnable()
├── saveDefaultConfig()
├── 初始化 ResourceManager
├── 加载 i18n 资源
├── 初始化服务
│   ├── VerifyCodeService
│   ├── MailService
│   ├── AuthmeService
│   ├── VersionCheckService
│   ├── CaptchaService
│   ├── QuestionnaireService
│   └── DiscordService
├── 初始化数据存储
│   ├── MySQL: MysqlUserDao + MysqlAuditDao
│   └── File: FileUserDao + FileAuditDao
├── 数据迁移 (如需要)
├── AuthMe 同步
├── 启动 WebSocket 服务器
├── 启动 Web 服务器
├── 白名单同步
├── 注册事件监听器
├── 启动定时任务
│   ├── AuthMe 同步任务
│   └── whitelist.json 监控
└── 启动版本检查
```

**问题**:
- 类过长 (877行)，职责过多
- 服务初始化分散在多处
- 缺少统一的服务生命周期管理

#### 2.2 Web 服务器 (WebServer.java)

**职责**:
- HTTP 服务器管理
- API 路由定义
- 静态资源服务
- 请求验证与响应格式化

**API 端点列表**:

| 端点 | 方法 | 认证 | 功能 |
|------|------|------|------|
| `/` | GET | 否 | 静态资源 |
| `/api/ping` | GET | 否 | 健康检查 |
| `/api/config` | GET | 否 | 获取前端配置 |
| `/api/check-whitelist` | GET | 否 | 检查白名单状态 |
| `/api/discord/auth` | POST | 否 | Discord OAuth URL |
| `/api/discord/callback` | GET | 否 | Discord OAuth 回调 |
| `/api/discord/status` | GET | 否 | Discord 绑定状态 |
| `/api/reload-config` | POST | 是 | 重载配置 |
| `/api/captcha` | GET | 否 | 获取验证码 |
| `/api/questionnaire` | GET | 否 | 获取问卷 |
| `/api/submit-questionnaire` | POST | 否 | 提交问卷 |
| `/api/send_code` | POST | 否 | 发送验证码 |
| `/api/register` | POST | 否 | 用户注册 |
| `/api/admin-login` | POST | 否 | 管理员登录 |
| `/api/admin-verify` | POST | 是 | 验证 Token |
| `/api/pending-list` | GET | 是 | 获取待审核列表 |
| `/api/review` | POST | 是 | 审核操作 |
| `/api/all-users` | GET | 是 | 获取所有用户 |
| `/api/users-paginated` | GET | 是 | 分页用户列表 |
| `/api/delete-user` | POST | 是 | 删除用户 |
| `/api/ban-user` | POST | 是 | 封禁用户 |
| `/api/unban-user` | POST | 是 | 解封用户 |
| `/api/change-password` | POST | 是 | 修改密码 |
| `/api/user-status` | GET | 否 | 用户状态查询 |
| `/api/version-check` | GET | 是 | 版本检查 |

**问题**:
- 类过长 (1862行)，包含路由定义、业务逻辑、工具方法
- 路由处理使用 Lambda 内联，难以维护
- 部分端点直接操作 DAO，违反分层原则
- 缺少统一的请求验证和错误处理

#### 2.3 数据访问层 (DAO)

**UserDao 接口方法**:
```java
// 用户注册
boolean registerUser(String uuid, String username, String email, String status)
boolean registerUser(String uuid, String username, String email, String status, String password)
boolean registerUser(..., Integer questionnaireScore, Boolean questionnairePassed, 
                      String questionnaireReviewSummary, Long questionnaireScoredAt)

// 用户更新
boolean updateUserStatus(String uuidOrName, String status)
boolean updateUserPassword(String uuidOrName, String password)
boolean updateUserDiscordId(String uuidOrName, String discordId)

// 用户查询
Map<String, Object> getUserByUuid(String uuid)
Map<String, Object> getUserByUsername(String username)
Map<String, Object> getUserByDiscordId(String discordId)
List<Map<String, Object>> getAllUsers()
List<Map<String, Object>> getPendingUsers()
List<Map<String, Object>> getUsersWithPagination(int page, int pageSize)
List<Map<String, Object>> getUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery)
List<Map<String, Object>> getApprovedUsersWithPagination(int page, int pageSize)
List<Map<String, Object>> getApprovedUsersWithPaginationAndSearch(int page, int pageSize, String searchQuery)

// 用户删除
boolean deleteUser(String uuid)

// 统计
int countUsersByEmail(String email)
int getTotalUserCount()
int getTotalUserCountWithSearch(String searchQuery)
int getApprovedUserCount()
int getApprovedUserCountWithSearch(String searchQuery)

// Discord
boolean isDiscordIdLinked(String discordId)

// 持久化
void save()
```

**问题**:
- 方法过多，部分方法可以合并
- 返回 `Map<String, Object>` 而非实体类，类型不安全
- FileUserDao 和 MysqlUserDao 有大量重复代码
- 缺少事务支持

#### 2.4 服务层

**服务依赖关系**:
```
VerifyMC (主插件)
├── VerifyCodeService (独立)
├── MailService (依赖 VerifyMC)
├── AuthmeService (依赖 UserDao)
├── VersionCheckService (独立)
├── CaptchaService (独立)
├── QuestionnaireService (依赖 EssayScoringService)
│   └── OpenAICompatibleScoringProvider
├── DiscordService (依赖 UserDao)
├── RegistrationApplicationService (独立)
├── QuestionnaireApplicationService (独立)
└── ReviewApplicationService (独立)
```

**问题**:
- 服务间依赖通过 setter 注入，不统一
- 部分服务直接依赖 Plugin 类
- 缺少服务接口，难以测试

### 3. 业务流程详细分析

#### 3.1 用户注册流程

```
前端 → POST /api/register
│
├── RegistrationHandler.handle()
│   └── RegistrationProcessingHandler.handle()
│       │
│       ├── 1. 基本输入验证
│       │   ├── 密码验证 (如 AuthMe 启用)
│       │   ├── 邮箱别名检查
│       │   ├── 邮箱域名白名单检查
│       │   ├── 用户名存在检查
│       │   ├── 用户名格式验证
│       │   ├── 用户名大小写冲突检查
│       │   ├── 邮箱注册数量限制
│       │   ├── 邮箱格式验证
│       │   ├── UUID 格式验证
│       │   └── 用户名非空验证
│       │
│       ├── 2. 问卷验证 (如启用)
│       │   ├── 检查问卷令牌
│       │   ├── 验证令牌有效期
│       │   ├── 验证答案一致性
│       │   └── 检查是否通过
│       │
│       ├── 3. 验证方式检查
│       │   ├── 图形验证码验证 (如启用)
│       │   └── 邮箱验证码验证
│       │
│       ├── 4. Discord 绑定检查 (如启用)
│       │
│       └── 5. 执行注册
│           ├── 解析注册决策
│           ├── 持久化用户数据
│           ├── 白名单同步 (如自动审批)
│           └── AuthMe 同步 (如启用)
│
└── 返回响应
```

**注册结果类型**:
```java
enum RegistrationOutcome {
    FAILED,                                    // 注册失败
    SUCCESS_PENDING,                          // 成功，待审核
    SUCCESS_WHITELISTED,                      // 成功，已加入白名单
    QUESTIONNAIRE_PENDING_REVIEW,             // 问卷通过，待审核
    QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW  // 评分异常，待审核
}
```

#### 3.2 审核流程

```
管理员 → POST /api/review
│
├── 验证认证 Token
├── 解析请求参数 (uuid, action, reason)
├── 验证 UUID 格式
├── 验证 action (approve/reject)
├── 获取用户信息
├── 更新用户状态
├── 同步白名单
│   ├── approve: whitelist add
│   └── reject: whitelist remove
├── AuthMe 同步 (如启用)
├── 发送邮件通知 (异步)
├── WebSocket 广播
└── 返回响应
```

#### 3.3 问卷提交流程

```
前端 → POST /api/submit-questionnaire
│
├── 速率限制检查
│   ├── IP 限制 (默认 20次/5分钟)
│   ├── UUID 限制 (默认 8次/5分钟)
│   └── 邮箱限制 (默认 6次/5分钟)
│
├── 解析答案
├── 验证答案格式
├── 评分
│   ├── 选择题: 本地评分
│   └── 文本题: LLM 评分
│       ├── 检查熔断器状态
│       ├── 获取并发许可
│       ├── 调用 LLM API
│       ├── 解析评分结果
│       └── 处理失败重试
│
├── 生成问卷令牌
├── 存储问卷结果
└── 返回响应
```

### 4. 已识别的问题

#### 4.1 架构问题

| 问题 | 位置 | 严重程度 | 描述 |
|------|------|----------|------|
| WebServer 类过长 | WebServer.java | 高 | 1862行，包含路由、业务逻辑、工具方法 |
| 职责不清晰 | WebServer.java | 高 | Web 层直接操作 DAO、发送邮件 |
| 缺少统一异常处理 | 全局 | 中 | 每个端点独立处理异常 |
| 重复代码 | FileUserDao/MysqlUserDao | 中 | 多处重复的用户查询、状态检查逻辑 |
| 类型不安全 | DAO 层 | 中 | 返回 Map 而非实体类 |
| 服务依赖混乱 | 服务层 | 中 | setter 注入、构造函数注入混用 |

#### 4.2 潜在逻辑错误

| 问题 | 位置 | 严重程度 | 描述 |
|------|------|----------|------|
| shouldAutoApprove 忽略参数 | RegistrationOutcomeResolver.java | 高 | `manualReviewRequired` 参数被忽略 |
| JSON 比较误判 | RegistrationProcessingHandler.java | 中 | 使用 `similar()` 比较可能误判 |
| Discord 令牌缓存 | DiscordService.java | 低 | 刷新失败后未正确清理缓存 |
| 硬编码配置 | 多处 | 低 | 超时时间、重试次数硬编码 |

**shouldAutoApprove 问题详解**:
```java
// 当前实现
public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
    return registerAutoApprove;  // 忽略了 manualReviewRequired
}

// 正确实现应该是
public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
    return registerAutoApprove && !manualReviewRequired;
}
```

#### 4.3 代码质量问题

| 问题 | 位置 | 描述 |
|------|------|------|
| 日志不一致 | 全局 | 部分使用 debugLog，部分直接使用 Logger |
| 缺少接口抽象 | 服务层 | 服务类直接依赖具体实现 |
| Handler 委托模式冗余 | web/ | UserAdminHandler 等只是简单委托 |
| 配置硬编码 | 多处 | 魔法数字、字符串散落各处 |

### 5. 配置项分析

**config.yml 主要配置**:
- 基础配置: language, debug, web_port, web_server_prefix
- 认证配置: auth_methods, max_accounts_per_email
- 白名单配置: whitelist_mode, whitelist_json_sync, auto_sync_whitelist
- 管理配置: admin.password
- 通知配置: user_notification.*
- 前端配置: frontend.theme, frontend.logo_url, frontend.announcement
- SMTP 配置: smtp.*
- 存储配置: storage.type, storage.mysql.*
- AuthMe 配置: authme.*
- 验证码配置: captcha.*
- Bedrock 配置: bedrock.*
- 问卷配置: questionnaire.*, llm.*
- Discord 配置: discord.*

### 6. 代理插件分析

**plugin-proxy 模块**:
- VerifyMCProxy: BungeeCord 代理插件
- ApiClient: HTTP API 客户端，带缓存
- ProxyConfig: 代理配置
- ProxyVersionCheckService: 版本检查
- ProxyResourceUpdater: 资源更新

**代理插件职责**:
- 拦截玩家登录事件
- 调用后端 API 检查白名单状态
- 缓存白名单状态
- 踢出未注册玩家

---

## ADDED Requirements

### Requirement: 模块化架构
系统 SHALL 采用分层架构，明确划分 Web 层、服务层、数据访问层的职责。

#### Scenario: Web 层职责
- **WHEN** 处理 HTTP 请求时
- **THEN** Web 层仅负责请求解析、参数验证、响应格式化，不直接操作数据库

#### Scenario: 服务层职责
- **WHEN** 执行业务逻辑时
- **THEN** 服务层负责业务规则验证、数据操作、事件发布

### Requirement: 统一响应格式
系统 SHALL 提供统一的 API 响应格式。

#### Scenario: 成功响应
- **WHEN** API 调用成功
- **THEN** 返回 `{success: true, msg: "...", data: {...}}`

#### Scenario: 失败响应
- **WHEN** API 调用失败
- **THEN** 返回 `{success: false, msg: "...", code: "ERROR_CODE"}`

### Requirement: 统一异常处理
系统 SHALL 提供全局异常处理机制。

#### Scenario: 业务异常
- **WHEN** 抛出业务异常
- **THEN** 返回友好的错误消息，不暴露内部细节

#### Scenario: 系统异常
- **WHEN** 发生系统异常
- **THEN** 记录详细日志，返回通用错误消息

### Requirement: 依赖注入
系统 SHALL 支持依赖注入，便于测试和维护。

#### Scenario: 服务依赖
- **WHEN** 服务类需要其他服务
- **THEN** 通过构造函数注入，而非内部创建

---

## MODIFIED Requirements

### Requirement: RegistrationOutcomeResolver 逻辑修正
原有逻辑中 `shouldAutoApprove` 方法忽略 `manualReviewRequired` 参数，需修正。

**修正后逻辑**:
```java
public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
    return registerAutoApprove && !manualReviewRequired;
}
```

### Requirement: 问卷令牌验证改进
原有逻辑使用 `similar()` 方法比较 JSON，可能产生误判。

**修正后逻辑**: 使用 JSON 规范化后比较或逐字段验证。

### Requirement: 移除冗余 Handler 委托类
UserAdminHandler、ReviewHandler、QuestionnaireHandler、RegistrationHandler 等委托类可简化或移除。

---

## REMOVED Requirements

### Requirement: 无
本次重构不删除现有功能，仅优化代码结构。
