# 编译错误修复检查清单

## HikariCP 依赖
- [ ] pom.xml 包含 HikariCP 依赖
- [ ] ConnectionPoolConfig.java 编译通过
- [ ] MysqlUserRepository.java 编译通过

## ApiResponse 构造函数
- [ ] ApiResponse 构造函数可被外部访问
- [ ] GlobalExceptionHandler.java 编译通过

## Middleware 接口
- [ ] Middleware 接口定义了 intercept(RequestContext) 方法
- [ ] AuthMiddleware 正确实现 Middleware 接口
- [ ] AuthMiddleware.java 编译通过

## RequestContext 方法
- [ ] getBody() 方法存在
- [ ] sendJson(JSONObject) 方法存在
- [ ] sendUnauthorized() 方法存在
- [ ] sendMethodNotAllowed() 方法存在
- [ ] sendNotFound(String) 方法存在
- [ ] getQueryParam(String, String) 方法存在
- [ ] getQueryParamAsInt(String, int) 方法存在
- [ ] getLanguage() 方法存在
- [ ] getRawExchange() 方法存在
- [ ] RequestContext.java 编译通过

## User 实体
- [ ] User 实体包含 reason 字段
- [ ] getReason() 方法存在
- [ ] User.java 编译通过

## AdminController
- [ ] AdminController 正确实现 RouteHandler 接口
- [ ] AdminController.java 编译通过

## VersionCheckService
- [ ] VersionCheckService 存在或有替代方案
- [ ] checkForUpdates() 方法存在
- [ ] VersionCheckResult 类存在

## DataMigrationTool
- [ ] getConnection() 方法可访问
- [ ] DataMigrationTool.java 编译通过

## 最终验证
- [ ] 所有 Java 文件编译通过
- [ ] 无诊断错误
