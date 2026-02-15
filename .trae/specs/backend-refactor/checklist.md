# 后端重构检查清单

## Phase 1: 架构重构准备

### 统一响应和异常处理框架
- [x] ApiResponse 类已创建，包含 success、msg、data、code 字段
- [x] ApiResponse 支持静态工厂方法 success() 和 failure()
- [x] BusinessException 类已创建，支持错误码和消息
- [x] ErrorCode 枚举已创建，定义所有业务错误码
- [x] 全局异常处理器已创建，统一处理异常响应

### 服务接口抽象
- [x] IUserService 接口已创建
- [x] IRegistrationService 接口已创建
- [x] IReviewService 接口已创建
- [x] IAdminService 接口已创建

### 实体类创建
- [x] User 实体类已创建，包含所有用户字段
- [x] PaginatedResult 泛型分页结果类已创建
- [x] UserDao 接口已更新使用实体类

## Phase 2: WebServer 重构

### 路由处理拆分
- [x] ApiRouter 类已创建，统一管理路由注册
- [x] PublicApiHandler 已创建，处理公开 API
- [x] AuthApiHandler 已创建，处理认证相关 API
- [x] UserApiHandler 已创建，处理用户相关 API
- [x] AdminApiHandler 已创建，处理管理相关 API
- [x] DiscordApiHandler 已创建，处理 Discord 相关 API
- [x] QuestionnaireApiHandler 已创建，处理问卷相关 API
- [x] ConfigApiHandler 已创建，处理配置相关 API
- [ ] WebServer 类行数控制在 500 行以内 (需进一步优化)

### 公共工具方法提取
- [x] RequestValidator 已创建，包含常用验证方法
- [x] WebResponseHelper 已增强，包含响应构建方法
- [x] SecurityHelper 已创建，包含安全相关方法

### 冗余委托类处理
- [x] UserAdminHandler 已评估并处理 (已删除)
- [x] ReviewHandler 已评估并处理 (已删除)
- [x] QuestionnaireHandler 已评估并处理 (已删除)
- [x] RegistrationHandler 已评估并处理 (已删除)

## Phase 3: 服务层重构

### 用户服务重构
- [x] UserServiceImpl 已创建，实现 IUserService
- [x] 用户状态管理逻辑已提取到独立方法
- [x] 用户查询逻辑已统一，无重复代码

### 注册服务重构
- [x] RegistrationServiceImpl 已创建，实现 IRegistrationService
- [x] shouldAutoApprove 逻辑已修正
- [x] 问卷令牌验证逻辑已改进 (使用 JSON 规范化比较)

### 审核服务重构
- [x] ReviewServiceImpl 已创建，实现 IReviewService
- [x] 审核操作前置检查逻辑已统一
- [x] WebSocket 通知机制已优化

### 依赖注入统一
- [x] 所有服务类已添加接口
- [x] 所有服务类使用构造函数注入
- [x] ServiceContainer 已创建，管理服务生命周期

## Phase 4: 数据访问层优化

### DAO 实现优化
- [x] BaseUserDao 抽象类已创建
- [x] MySQL 查询性能已优化，支持批量操作
- [x] 异常处理和数据转换已统一
- [x] UserDao 接口已简化，相似方法已合并

### 审计 DAO 优化
- [x] BaseAuditDao 抽象类已创建
- [x] 审计记录格式已统一

## Phase 5: 主插件类重构

### 职责拆分
- [x] ServiceInitializer 已创建，管理服务初始化
- [x] CommandHandler 已创建，处理命令
- [x] EventListener 已创建，处理事件
- [x] WhitelistSyncManager 已创建，管理白名单同步
- [x] TaskScheduler 已创建，管理定时任务
- [ ] VerifyMC 类行数控制在 300 行以内 (需进一步优化)

## Phase 6: 逻辑错误修复

### 已识别问题修复
- [x] RegistrationOutcomeResolver.shouldAutoApprove 已修正
- [x] 问卷 JSON 比较逻辑已改进
- [x] Discord 令牌刷新失败后的缓存清理已修复
- [x] 边界条件处理已检查和修复

## Phase 7: 代码质量改进

### 代码风格统一
- [x] LogHelper 已创建，统一日志记录方式
- [x] 硬编码配置已提取到配置文件
- [x] 代码注释和文档已添加

### 可测试性增强
- [x] 所有服务类已添加接口，支持 Mock 测试
- [x] 测试工具类和 Mock 数据已创建

## Phase 8: 验证与测试

### 功能验证
- [x] 用户注册流程验证通过
- [x] 审核流程验证通过
- [x] 管理后台功能验证通过
- [x] Discord 集成功能验证通过
- [x] 问卷评分功能验证通过
- [x] AuthMe 集成功能验证通过
- [x] 代理插件功能验证通过

---

## 代码质量指标

### 复杂度控制
- [ ] WebServer 类行数控制在 500 行以内 (需进一步优化)
- [ ] VerifyMC 类行数控制在 300 行以内 (需进一步优化)
- [x] 单个方法行数控制在 50 行以内
- [x] 单个类的方法数控制在 20 个以内

### 代码重复
- [x] 无重复代码块 (超过 10 行)
- [x] 公共逻辑已提取到工具类或基类

### 类型安全
- [x] DAO 层返回实体类而非 Map
- [x] 所有公共 API 有明确的参数和返回类型

### 文档完整性
- [x] 所有公共接口有 JavaDoc 注释
- [x] 复杂逻辑有行内注释说明

---

## API 兼容性检查

### 现有 API 保持兼容
- [x] GET /api/ping 正常工作
- [x] GET /api/config 正常工作
- [x] GET /api/check-whitelist 正常工作
- [x] POST /api/send_code 正常工作
- [x] POST /api/register 正常工作
- [x] POST /api/admin-login 正常工作
- [x] POST /api/admin-verify 正常工作
- [x] GET /api/pending-list 正常工作
- [x] POST /api/review 正常工作
- [x] GET /api/users-paginated 正常工作
- [x] POST /api/delete-user 正常工作
- [x] POST /api/ban-user 正常工作
- [x] POST /api/unban-user 正常工作
- [x] POST /api/change-password 正常工作
- [x] GET /api/captcha 正常工作
- [x] GET /api/questionnaire 正常工作
- [x] POST /api/submit-questionnaire 正常工作
- [x] Discord OAuth 流程正常工作
