# 编译错误修复规范

## Why

重构后的代码存在多处编译错误，主要包括：HikariCP 依赖缺失、API 不匹配、方法签名不一致等问题，需要统一修复以确保代码可编译运行。

## What Changes

- 修复 HikariCP 连接池依赖问题
- 修复 ApiResponse 构造函数访问权限
- 修复 Middleware 接口实现
- 修复 RequestContext 缺失的方法
- 修复 Controller 中的方法调用
- 修复 User 实体缺失的字段
- 修复 VersionCheckService 缺失的方法

## Impact

- Affected files:
  - `infrastructure/persistence/ConnectionPoolConfig.java`
  - `infrastructure/persistence/MysqlUserRepository.java`
  - `infrastructure/persistence/DataMigrationTool.java`
  - `infrastructure/exception/GlobalExceptionHandler.java`
  - `infrastructure/web/AuthMiddleware.java`
  - `infrastructure/web/RequestContext.java`
  - `infrastructure/web/controller/AdminController.java`
  - `domain/model/User.java`

## ADDED Requirements

### Requirement: HikariCP 依赖修复

系统 SHALL 正确配置 HikariCP 连接池依赖：

#### Scenario: MySQL 存储模式
- **WHEN** 使用 MySQL 存储时
- **THEN** 应正确加载 HikariCP 类
- **AND** 应正确创建连接池

### Requirement: ApiResponse 构造函数可访问

ApiResponse SHALL 提供公共构造函数或工厂方法：

#### Scenario: 异常处理
- **WHEN** GlobalExceptionHandler 创建错误响应时
- **THEN** 应能正常创建 ApiResponse 实例

### Requirement: Middleware 接口实现正确

AuthMiddleware SHALL 正确实现 Middleware 接口：

#### Scenario: 请求拦截
- **WHEN** 请求经过认证中间件时
- **THEN** 应调用正确的 intercept 方法

### Requirement: RequestContext 方法完整

RequestContext SHALL 提供所有 Controller 需要的方法：

#### Scenario: 请求处理
- **WHEN** Controller 处理请求时
- **THEN** 应能调用 getBody(), sendJson(), sendUnauthorized() 等方法

### Requirement: User 实体字段完整

User 实体 SHALL 包含所有必要字段：

#### Scenario: 用户信息展示
- **WHEN** 获取用户拒绝原因时
- **THEN** 应能通过 getReason() 方法获取

## MODIFIED Requirements

无

## REMOVED Requirements

无
