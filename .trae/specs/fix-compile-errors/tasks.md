# Tasks

## 错误分类与修复任务

### Task 1: 修复 HikariCP 依赖问题
**问题**: `com.zaxxer.hikari` 包不存在
**文件**: 
- `ConnectionPoolConfig.java`
- `MysqlUserRepository.java`

**修复方案**:
- [ ] SubTask 1.1: 检查 pom.xml 是否包含 HikariCP 依赖
- [ ] SubTask 1.2: 如果缺失，添加 HikariCP 依赖到 pom.xml
- [ ] SubTask 1.3: 确保导入语句正确

### Task 2: 修复 ApiResponse 构造函数访问权限
**问题**: `ApiResponse(boolean,String,String,Object)` 是 private 访问控制
**文件**: `GlobalExceptionHandler.java`

**修复方案**:
- [ ] SubTask 2.1: 将 ApiResponse 构造函数改为 public
- [ ] SubTask 2.2: 或添加公共工厂方法

### Task 3: 修复 Middleware 接口实现
**问题**: AuthMiddleware 未正确实现 Middleware 接口的 `intercept(RequestContext)` 方法
**文件**: `AuthMiddleware.java`

**修复方案**:
- [ ] SubTask 3.1: 检查 Middleware 接口定义的方法签名
- [ ] SubTask 3.2: 修改 AuthMiddleware 实现正确的方法
- [ ] SubTask 3.3: 添加缺失的 getRawExchange() 方法到 RequestContext

### Task 4: 修复 RequestContext 缺失的方法
**问题**: RequestContext 缺少多个方法
**缺失方法**:
- `getBody()` - 获取请求体
- `sendJson(JSONObject)` - 发送 JSON 响应
- `sendUnauthorized()` - 发送 401 响应
- `sendMethodNotAllowed()` - 发送 405 响应
- `sendNotFound(String)` - 发送 404 响应
- `getQueryParam(String, String)` - 带默认值的查询参数
- `getQueryParamAsInt(String, int)` - 获取整数查询参数
- `getLanguage()` - 获取语言

**文件**: `RequestContext.java`

**修复方案**:
- [ ] SubTask 4.1: 添加 getBody() 方法
- [ ] SubTask 4.2: 添加 sendJson(JSONObject) 方法
- [ ] SubTask 4.3: 添加 sendUnauthorized() 方法
- [ ] SubTask 4.4: 添加 sendMethodNotAllowed() 方法
- [ ] SubTask 4.5: 添加 sendNotFound(String) 方法
- [ ] SubTask 4.6: 添加 getQueryParam(String, String) 重载方法
- [ ] SubTask 4.7: 添加 getQueryParamAsInt(String, int) 方法
- [ ] SubTask 4.8: 添加 getLanguage() 方法

### Task 5: 修复 User 实体缺失字段
**问题**: User 实体缺少 `getReason()` 方法
**文件**: `User.java`

**修复方案**:
- [ ] SubTask 5.1: 添加 reason 字段到 User 实体
- [ ] SubTask 5.2: 添加 getReason() 方法

### Task 6: 修复 AdminController 接口实现
**问题**: AdminController 未正确实现 RouteHandler 接口
**文件**: `AdminController.java`

**修复方案**:
- [ ] SubTask 6.1: 检查 RouteHandler 接口定义
- [ ] SubTask 6.2: 确保 AdminController 实现正确的 handle(RequestContext) 方法

### Task 7: 修复 VersionCheckService 缺失方法
**问题**: VersionCheckService 缺少 `checkForUpdates()` 方法和 `VersionCheckResult` 内部类
**文件**: `VersionCheckService.java` 或 `AdminController.java`

**修复方案**:
- [ ] SubTask 7.1: 检查 VersionCheckService 是否存在
- [ ] SubTask 7.2: 添加缺失的方法和类

### Task 8: 修复 DataMigrationTool 连接获取问题
**问题**: `getConnection()` 方法不存在
**文件**: `DataMigrationTool.java`

**修复方案**:
- [ ] SubTask 8.1: 确保 MysqlUserRepository 有 getConnection() 方法
- [ ] SubTask 8.2: 或修改 DataMigrationTool 使用其他方式获取连接

# Task Dependencies

- [Task 3] depends on [Task 4] (AuthMiddleware 需要 RequestContext 的方法)
- [Task 6] depends on [Task 4] (AdminController 需要 RequestContext 的方法)
- [Task 8] depends on [Task 1] (需要 HikariCP 正常工作)

# Parallelizable Tasks

以下任务可以并行执行：
- Task 1, Task 2, Task 5, Task 7 (独立修复)
- Task 4 (RequestContext 方法添加)
