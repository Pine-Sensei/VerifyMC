# 后端系统全面重构规范

## Why

当前 VerifyMC 后端系统存在严重的架构问题：主类 VerifyMC 承担过多职责（超过 870 行），WebServer 类过于臃肿（接近 1900 行），缺乏清晰的分层架构，服务间耦合度高，配置管理分散，异常处理不统一，导致系统难以扩展、维护成本高、测试困难。需要进行全面重构以提升系统的可扩展性、可维护性和性能。

## What Changes

- **BREAKING**: 重构整体架构为清晰的分层架构（Controller-Service-Repository）
- **BREAKING**: 引入依赖注入容器，实现服务解耦
- **BREAKING**: 统一配置管理，集中化配置读取和验证
- **BREAKING**: 统一异常处理机制，提供一致的错误响应
- **BREAKING**: 模块化 WebServer，拆分为独立的 API 路由处理器
- **BREAKING**: 引入统一的请求验证和响应构建机制
- **BREAKING**: 优化数据访问层，引入 Repository 模式
- **BREAKING**: 完善日志和监控机制

## Impact

- Affected specs: 用户注册流程、审核流程、白名单管理、AuthMe 集成、Discord 集成、问卷系统
- Affected code: 
  - `plugin/src/main/java/team/kitemc/verifymc/VerifyMC.java` - 主类重构
  - `plugin/src/main/java/team/kitemc/verifymc/web/WebServer.java` - 拆分重构
  - `plugin/src/main/java/team/kitemc/verifymc/service/*.java` - 服务层重构
  - `plugin/src/main/java/team/kitemc/verifymc/db/*.java` - 数据访问层优化
  - 新增多个核心架构类

## ADDED Requirements

### Requirement: 分层架构设计

系统 SHALL 采用清晰的分层架构，各层职责明确分离：

#### Scenario: 架构层次划分
- **WHEN** 系统启动时
- **THEN** 系统应按照以下层次结构组织：
  - **Controller 层**：处理 HTTP 请求，参数验证，响应构建
  - **Service 层**：业务逻辑处理，事务管理
  - **Repository 层**：数据访问，持久化操作
  - **Infrastructure 层**：配置管理，日志，工具类

### Requirement: 依赖注入容器

系统 SHALL 提供轻量级依赖注入容器，管理服务生命周期和依赖关系：

#### Scenario: 服务注册与获取
- **WHEN** 系统初始化时
- **THEN** 所有服务组件应自动注册到容器中
- **AND** 服务依赖应通过构造函数自动注入
- **AND** 支持单例和原型两种作用域

#### Scenario: 循环依赖检测
- **WHEN** 存在循环依赖时
- **THEN** 系统应在启动时抛出明确的异常信息

### Requirement: 统一配置管理

系统 SHALL 提供集中化的配置管理服务：

#### Scenario: 配置读取
- **WHEN** 服务需要读取配置时
- **THEN** 应通过 ConfigurationService 获取配置值
- **AND** 支持配置默认值
- **AND** 支持配置类型转换

#### Scenario: 配置验证
- **WHEN** 系统启动时
- **THEN** 应验证所有必需配置项是否存在
- **AND** 应验证配置值的格式和范围

#### Scenario: 配置热更新
- **WHEN** 执行 `/vmc reload` 命令时
- **THEN** 应重新加载配置并通知相关服务更新

### Requirement: 统一异常处理

系统 SHALL 提供统一的异常处理机制：

#### Scenario: 业务异常处理
- **WHEN** 业务逻辑抛出异常时
- **THEN** 应转换为统一的错误响应格式
- **AND** 应记录详细的错误日志
- **AND** 不应暴露敏感的系统信息

#### Scenario: 参数验证异常
- **WHEN** 请求参数验证失败时
- **THEN** 应返回明确的错误信息
- **AND** 应指出具体的错误字段和原因

### Requirement: WebServer 模块化

系统 SHALL 将 WebServer 拆分为独立的模块：

#### Scenario: 路由注册
- **WHEN** 系统启动时
- **THEN** 各 API 路由应通过路由注册器统一注册
- **AND** 支持路由分组和中间件

#### Scenario: 请求处理
- **WHEN** 收到 HTTP 请求时
- **THEN** 应经过统一的请求处理管道
- **AND** 支持请求日志记录
- **AND** 支持请求限流

### Requirement: Repository 模式优化

系统 SHALL 优化数据访问层设计：

#### Scenario: 数据访问接口
- **WHEN** 服务需要访问数据时
- **THEN** 应通过统一的 Repository 接口
- **AND** 支持多种存储后端（文件、MySQL、SQLite）
- **AND** 提供统一的数据模型

#### Scenario: 事务管理
- **WHEN** 执行多个数据操作时
- **THEN** 应支持事务管理
- **AND** 确保数据一致性

### Requirement: 主类重构

系统 SHALL 重构 VerifyMC 主类：

#### Scenario: 职责分离
- **WHEN** 系统启动时
- **THEN** 主类应仅负责插件生命周期管理
- **AND** 服务初始化应委托给 ServiceInitializer
- **AND** 命令处理应委托给 CommandHandler
- **AND** 事件监听应委托给 EventListener

#### Scenario: 代码行数限制
- **WHEN** 重构完成后
- **THEN** 主类代码行数应控制在 200 行以内

### Requirement: 日志和监控

系统 SHALL 完善日志和监控机制：

#### Scenario: 结构化日志
- **WHEN** 记录日志时
- **THEN** 应使用结构化日志格式
- **AND** 包含请求 ID、用户信息等上下文
- **AND** 支持日志级别动态调整

#### Scenario: 性能监控
- **WHEN** 处理请求时
- **THEN** 应记录请求处理时间
- **AND** 应记录关键操作的耗时

## MODIFIED Requirements

### Requirement: 用户注册流程

系统 SHALL 保持原有注册流程功能，但通过重构后的架构实现：

- 原有功能：邮箱验证、验证码验证、问卷评估、Discord 绑定、自动审批
- 改进：通过 Controller-Service-Repository 分层实现，代码更清晰
- 改进：统一异常处理，错误信息更友好
- 改进：请求验证逻辑复用，减少代码重复

### Requirement: 用户审核流程

系统 SHALL 保持原有审核流程功能，但优化实现：

- 原有功能：待审核列表、批准/拒绝操作、邮件通知、WebSocket 推送
- 改进：审核逻辑集中在 ReviewService 中
- 改进：支持批量审核操作
- 改进：审核日志记录更完整

### Requirement: 白名单管理

系统 SHALL 保持原有白名单功能，但优化实现：

- 原有功能：白名单同步、JSON 同步、自动清理
- 改进：白名单操作集中在 WhitelistService 中
- 改进：支持多种白名单模式的统一管理

## REMOVED Requirements

### Requirement: 分散的配置读取

**Reason**: 配置读取分散在各服务类中，难以维护和追踪
**Migration**: 所有配置读取迁移到 ConfigurationService

### Requirement: 内联的异常处理

**Reason**: 各处重复的 try-catch 代码块，错误响应格式不一致
**Migration**: 使用统一的异常处理机制和错误响应构建器

### Requirement: WebServer 中的业务逻辑

**Reason**: WebServer 类包含大量业务逻辑，违反单一职责原则
**Migration**: 业务逻辑迁移到对应的 Service 类，WebServer 仅负责路由分发
