# Tasks

## Phase 1: 核心架构搭建

- [x] Task 1: 创建核心架构基础类
  - [x] SubTask 1.1: 创建 DIContainer 依赖注入容器
  - [x] SubTask 1.2: 创建 Component 注解和 Service 注解
  - [x] SubTask 1.3: 创建 Lifecycle 接口和生命周期管理

- [x] Task 2: 创建统一配置管理服务
  - [x] SubTask 2.1: 创建 ConfigurationService 接口和实现
  - [x] SubTask 2.2: 创建 ConfigValidator 配置验证器
  - [x] SubTask 2.3: 创建配置模型类（DatabaseConfig, SmtpConfig, AuthMeConfig 等）

- [x] Task 3: 创建统一异常处理机制
  - [x] SubTask 3.1: 创建 BusinessException 业务异常基类
  - [x] SubTask 3.2: 创建 ErrorCode 错误码枚举
  - [x] SubTask 3.3: 创建 GlobalExceptionHandler 全局异常处理器
  - [x] SubTask 3.4: 创建 ApiResponse 统一响应类

## Phase 2: 数据访问层重构

- [x] Task 4: 重构 Repository 层
  - [x] SubTask 4.1: 创建 User 实体类
  - [x] SubTask 4.2: 创建 UserRepository 接口
  - [x] SubTask 4.3: 重构 FileUserRepository 实现
  - [x] SubTask 4.4: 重构 MysqlUserRepository 实现
  - [x] SubTask 4.5: 创建 AuditRepository 接口和实现

- [x] Task 5: 优化数据访问层
  - [x] SubTask 5.1: 创建 RepositoryFactory 工厂类
  - [x] SubTask 5.2: 实现数据迁移工具
  - [x] SubTask 5.3: 添加数据访问层单元测试

## Phase 3: 服务层重构

- [x] Task 6: 重构核心服务
  - [x] SubTask 6.1: 重构 UserService 用户服务
  - [x] SubTask 6.2: 重构 RegistrationService 注册服务
  - [x] SubTask 6.3: 重构 ReviewService 审核服务
  - [x] SubTask 6.4: 重构 WhitelistService 白名单服务

- [x] Task 7: 重构集成服务
  - [x] SubTask 7.1: 重构 AuthmeService AuthMe 集成服务
  - [x] SubTask 7.2: 重构 DiscordService Discord 集成服务
  - [x] SubTask 7.3: 重构 MailService 邮件服务
  - [x] SubTask 7.4: 重构 VerifyCodeService 验证码服务

- [x] Task 8: 重构问卷服务
  - [x] SubTask 8.1: 重构 QuestionnaireService 问卷服务
  - [x] SubTask 8.2: 重构 EssayScoringService 评分服务
  - [x] SubTask 8.3: 创建 QuestionnaireRepository 问卷配置仓库

## Phase 4: Web 层重构

- [x] Task 9: 创建 Web 框架基础
  - [x] SubTask 9.1: 创建 Router 路由器
  - [x] SubTask 9.2: 创建 RequestContext 请求上下文
  - [x] SubTask 9.3: 创建 Middleware 中间件机制
  - [x] SubTask 9.4: 创建 RequestValidator 请求验证器

- [x] Task 10: 创建 API Controller
  - [x] SubTask 10.1: 创建 RegistrationController 注册接口控制器
  - [x] SubTask 10.2: 创建 ReviewController 审核接口控制器
  - [x] SubTask 10.3: 创建 AdminController 管理接口控制器
  - [x] SubTask 10.4: 创建 DiscordController Discord 接口控制器
  - [x] SubTask 10.5: 创建 QuestionnaireController 问卷接口控制器

- [x] Task 11: 重构 WebServer
  - [x] SubTask 11.1: 创建 HttpServerFactory 服务器工厂
  - [x] SubTask 11.2: 创建 StaticFileHandler 静态文件处理器
  - [x] SubTask 11.3: 创建 ApiHandler API 处理器
  - [x] SubTask 11.4: 重构 WebServer 为路由分发器

## Phase 5: 主类重构

- [x] Task 12: 重构 VerifyMC 主类
  - [x] SubTask 12.1: 创建 PluginContext 插件上下文
  - [x] SubTask 12.2: 创建 ServiceInitializer 服务初始化器
  - [x] SubTask 12.3: 创建 CommandHandler 命令处理器
  - [x] SubTask 12.4: 创建 EventListener 事件监听器
  - [x] SubTask 12.5: 重构 VerifyMC 主类为生命周期管理器

## Phase 6: 测试与验证

- [x] Task 13: 编写单元测试
  - [x] SubTask 13.1: 编写 Repository 层单元测试
  - [x] SubTask 13.2: 编写 Service 层单元测试
  - [x] SubTask 13.3: 编写 Controller 层单元测试

- [x] Task 14: 集成测试
  - [x] SubTask 14.1: 编写注册流程集成测试
  - [x] SubTask 14.2: 编写审核流程集成测试
  - [x] SubTask 14.3: 编写白名单同步集成测试

- [x] Task 15: 系统审查
  - [x] SubTask 15.1: 代码审查 - 检查代码规范
  - [x] SubTask 15.2: 架构审查 - 验证分层架构
  - [x] SubTask 15.3: 性能测试 - 验证性能优化
  - [x] SubTask 15.4: 功能验证 - 确保功能与原系统一致

# Task Dependencies

- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 1, Task 2]
- [Task 5] depends on [Task 4]
- [Task 6] depends on [Task 4, Task 3]
- [Task 7] depends on [Task 6]
- [Task 8] depends on [Task 6]
- [Task 9] depends on [Task 1, Task 3]
- [Task 10] depends on [Task 6, Task 7, Task 8, Task 9]
- [Task 11] depends on [Task 9, Task 10]
- [Task 12] depends on [Task 6, Task 7, Task 8, Task 11]
- [Task 13] depends on [Task 4, Task 6, Task 10]
- [Task 14] depends on [Task 13]
- [Task 15] depends on [Task 14]

# Parallelizable Tasks

以下任务可以并行执行：
- Task 1, Task 2, Task 3 (Phase 1 核心架构)
- Task 6, Task 7, Task 8 (Phase 3 服务层重构，不同服务独立)
- Task 13.1, Task 13.2, Task 13.3 (Phase 6 单元测试)
- Task 15.1, Task 15.2, Task 15.3, Task 15.4 (Phase 6 系统审查)
