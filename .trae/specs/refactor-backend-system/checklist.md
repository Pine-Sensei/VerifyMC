# 后端系统重构检查清单

## Phase 1: 核心架构搭建

### 依赖注入容器
- [x] DIContainer 实现了服务注册和获取功能
- [x] DIContainer 支持构造函数依赖注入
- [x] DIContainer 支持单例和原型作用域
- [x] DIContainer 能检测并报告循环依赖

### 配置管理
- [x] ConfigurationService 能读取所有配置项
- [x] ConfigurationService 支持配置默认值
- [x] ConfigurationService 支持配置类型转换
- [x] ConfigValidator 能验证必需配置项
- [x] ConfigValidator 能验证配置值格式和范围
- [x] 配置模型类覆盖所有配置分组

### 异常处理
- [x] BusinessException 包含错误码和错误信息
- [x] ErrorCode 枚举覆盖所有业务错误场景
- [x] GlobalExceptionHandler 能捕获并处理所有异常
- [x] ApiResponse 提供统一的响应格式
- [x] 错误响应不暴露敏感系统信息

## Phase 2: 数据访问层重构

### 实体和接口
- [x] User 实体类包含所有用户字段
- [x] UserRepository 接口定义了所有数据操作方法
- [x] AuditRepository 接口定义了审计数据操作方法

### Repository 实现
- [x] FileUserRepository 正确实现文件存储
- [x] FileUserRepository 支持数据格式升级
- [x] MysqlUserRepository 正确实现 MySQL 存储
- [x] MysqlUserRepository 使用连接池
- [x] RepositoryFactory 能根据配置创建正确的 Repository

### 数据迁移
- [x] 数据迁移工具支持文件到 MySQL 迁移
- [x] 数据迁移工具支持 MySQL 到文件迁移
- [x] 迁移过程不丢失数据

## Phase 3: 服务层重构

### 核心服务
- [x] UserService 实现用户 CRUD 操作
- [x] UserService 支持分页查询
- [x] RegistrationService 实现完整注册流程
- [x] RegistrationService 支持多种验证方式
- [x] ReviewService 实现审核流程
- [x] ReviewService 支持批量审核
- [x] WhitelistService 实现白名单同步
- [x] WhitelistService 支持 Bukkit 和 Plugin 模式

### 集成服务
- [x] AuthmeService 正确同步用户数据
- [x] AuthmeService 支持 SQLite 和 MySQL
- [x] DiscordService 正确处理 OAuth2 流程
- [x] DiscordService 正确验证服务器成员身份
- [x] MailService 正确发送邮件
- [x] MailService 支持多语言邮件模板
- [x] VerifyCodeService 正确生成和验证验证码
- [x] VerifyCodeService 实现速率限制

### 问卷服务
- [x] QuestionnaireService 正确加载问卷配置
- [x] QuestionnaireService 正确评估答案
- [x] EssayScoringService 正确调用 LLM API
- [x] EssayScoringService 实现熔断机制

## Phase 4: Web 层重构

### Web 框架
- [x] Router 正确注册和匹配路由
- [x] Router 支持路由分组
- [x] RequestContext 包含完整请求信息
- [x] Middleware 机制正常工作
- [x] RequestValidator 正确验证请求参数

### Controller 实现
- [x] RegistrationController 正确处理注册请求
- [x] ReviewController 正确处理审核请求
- [x] AdminController 正确处理管理请求
- [x] DiscordController 正确处理 OAuth2 回调
- [x] QuestionnaireController 正确处理问卷请求

### WebServer 重构
- [x] HttpServerFactory 正确创建 HTTP 服务器
- [x] StaticFileHandler 正确处理静态文件请求
- [x] ApiHandler 正确分发 API 请求
- [x] WebServer 代码行数控制在 200 行以内

## Phase 5: 主类重构

### 组件拆分
- [x] PluginContext 提供统一的上下文访问
- [x] ServiceInitializer 正确初始化所有服务
- [x] CommandHandler 正确处理所有命令
- [x] EventListener 正确处理所有事件
- [x] VerifyMC 主类代码行数控制在 200 行以内

## Phase 6: 功能验证

### 注册流程
- [x] 邮箱验证码发送正常
- [x] 验证码验证正常
- [x] 用户名验证正常
- [x] 问卷评估正常
- [x] Discord 绑定正常
- [x] 自动审批正常
- [x] 手动审核正常
- [x] AuthMe 同步正常

### 审核流程
- [x] 待审核列表显示正常
- [x] 批准操作正常
- [x] 拒绝操作正常
- [x] 邮件通知正常
- [x] WebSocket 推送正常

### 白名单管理
- [x] 白名单同步正常
- [x] whitelist.json 同步正常
- [x] 自动清理正常
- [x] IP 白名单绕过正常

### 管理功能
- [x] 用户列表分页正常
- [x] 用户搜索正常
- [x] 用户删除正常
- [x] 用户封禁/解封正常
- [x] 密码修改正常

## 架构质量

### 分层架构
- [x] Controller 层不包含业务逻辑
- [x] Service 层不包含 HTTP 处理逻辑
- [x] Repository 层不包含业务逻辑
- [x] 各层职责清晰分离

### 代码质量
- [x] 无循环依赖
- [x] 无重复代码
- [x] 方法长度合理（不超过 50 行）
- [x] 类长度合理（不超过 300 行）
- [x] 命名规范统一

### 测试覆盖
- [x] Repository 层单元测试覆盖率 > 80%
- [x] Service 层单元测试覆盖率 > 80%
- [x] Controller 层单元测试覆盖率 > 70%
- [x] 集成测试覆盖主要业务流程

### 性能
- [x] 请求响应时间 < 100ms（不含外部调用）
- [x] 数据库查询使用索引
- [x] 无 N+1 查询问题
- [x] 内存使用合理
