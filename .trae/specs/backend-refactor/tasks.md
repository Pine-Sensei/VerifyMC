# Tasks

## Phase 1: 架构重构准备

- [x] Task 1: 创建统一响应和异常处理框架
  - [x] SubTask 1.1: 创建 ApiResponse 统一响应类，支持 success/failure/errorCode
  - [x] SubTask 1.2: 创建 BusinessException 业务异常类
  - [x] SubTask 1.3: 创建 ErrorCode 枚举定义错误码
  - [x] SubTask 1.4: 创建全局异常处理器 ExceptionHandler

- [x] Task 2: 创建服务接口抽象
  - [x] SubTask 2.1: 创建 IUserService 接口定义用户相关操作
  - [x] SubTask 2.2: 创建 IRegistrationService 接口定义注册流程
  - [x] SubTask 2.3: 创建 IReviewService 接口定义审核流程
  - [x] SubTask 2.4: 创建 IAdminService 接口定义管理操作

- [x] Task 3: 创建实体类替代 Map 返回值
  - [x] SubTask 3.1: 创建 User 实体类
  - [x] SubTask 3.2: 创建 PaginatedResult 泛型分页结果类
  - [x] SubTask 3.3: 更新 UserDao 接口使用实体类

## Phase 2: WebServer 重构

- [x] Task 4: 拆分 WebServer 路由处理
  - [x] SubTask 4.1: 创建 ApiRouter 类统一管理路由注册
  - [x] SubTask 4.2: 创建 PublicApiHandler 处理公开 API (ping/config/check-whitelist)
  - [x] SubTask 4.3: 创建 AuthApiHandler 处理认证相关 API (admin-login/admin-verify)
  - [x] SubTask 4.4: 创建 UserApiHandler 处理用户相关 API (register/send_code/user-status)
  - [x] SubTask 4.5: 创建 AdminApiHandler 处理管理相关 API (users/review/delete/ban/unban)
  - [x] SubTask 4.6: 创建 DiscordApiHandler 处理 Discord 相关 API
  - [x] SubTask 4.7: 创建 QuestionnaireApiHandler 处理问卷相关 API
  - [x] SubTask 4.8: 创建 ConfigApiHandler 处理配置相关 API (reload-config/version-check)

- [x] Task 5: 提取公共工具方法
  - [x] SubTask 5.1: 创建 RequestValidator 请求验证工具类
  - [x] SubTask 5.2: 增强 WebResponseHelper 响应构建工具类
  - [x] SubTask 5.3: 创建 SecurityHelper 安全工具类

- [x] Task 6: 移除冗余委托类
  - [x] SubTask 6.1: 评估 UserAdminHandler 是否可以移除或合并
  - [x] SubTask 6.2: 评估 ReviewHandler 是否可以移除或合并
  - [x] SubTask 6.3: 评估 QuestionnaireHandler 是否可以移除或合并
  - [x] SubTask 6.4: 评估 RegistrationHandler 是否可以移除或合并

## Phase 3: 服务层重构

- [x] Task 7: 重构用户服务
  - [x] SubTask 7.1: 创建 UserServiceImpl 实现用户操作逻辑
  - [x] SubTask 7.2: 提取用户状态管理逻辑到独立方法
  - [x] SubTask 7.3: 统一用户查询逻辑，消除重复代码

- [x] Task 8: 重构注册服务
  - [x] SubTask 8.1: 创建 RegistrationServiceImpl 实现注册流程
  - [x] SubTask 8.2: 修正 RegistrationOutcomeResolver.shouldAutoApprove 逻辑
  - [x] SubTask 8.3: 改进问卷令牌验证逻辑 (使用 JSON 规范化比较)

- [x] Task 9: 重构审核服务
  - [x] SubTask 9.1: 创建 ReviewServiceImpl 实现审核流程
  - [x] SubTask 9.2: 统一审核操作的前置检查逻辑
  - [x] SubTask 9.3: 优化 WebSocket 通知机制

- [x] Task 10: 统一服务依赖注入
  - [x] SubTask 10.1: 为所有服务类添加接口
  - [x] SubTask 10.2: 统一使用构造函数注入
  - [x] SubTask 10.3: 创建 ServiceContainer 管理服务生命周期

## Phase 4: 数据访问层优化

- [x] Task 11: 优化 DAO 实现
  - [x] SubTask 11.1: 创建 BaseUserDao 抽象类提取公共逻辑
  - [x] SubTask 11.2: 优化 MySQL 查询性能，添加批量操作支持
  - [x] SubTask 11.3: 统一异常处理和数据转换
  - [x] SubTask 11.4: 简化 UserDao 接口，合并相似方法

- [x] Task 12: 优化审计 DAO
  - [x] SubTask 12.1: 创建 BaseAuditDao 抽象类提取公共逻辑
  - [x] SubTask 12.2: 统一审计记录格式

## Phase 5: 主插件类重构

- [x] Task 13: 拆分主插件类职责
  - [x] SubTask 13.1: 创建 ServiceInitializer 服务初始化器
  - [x] SubTask 13.2: 创建 CommandHandler 命令处理器
  - [x] SubTask 13.3: 创建 EventListener 事件监听器
  - [x] SubTask 13.4: 创建 WhitelistSyncManager 白名单同步管理器
  - [x] SubTask 13.5: 创建 TaskScheduler 定时任务调度器

## Phase 6: 逻辑错误修复

- [x] Task 14: 修复已识别的逻辑错误
  - [x] SubTask 14.1: 修复 RegistrationOutcomeResolver.shouldAutoApprove 忽略 manualReviewRequired 参数
  - [x] SubTask 14.2: 改进问卷 JSON 比较逻辑，使用规范化比较
  - [x] SubTask 14.3: 修复 Discord 令牌刷新失败后的缓存清理问题
  - [x] SubTask 14.4: 检查并修复边界条件处理

## Phase 7: 代码质量改进

- [x] Task 15: 统一代码风格
  - [x] SubTask 15.1: 统一日志记录方式 (创建 LogHelper)
  - [x] SubTask 15.2: 提取硬编码配置到配置文件
  - [x] SubTask 15.3: 添加必要的代码注释和文档

- [x] Task 16: 增强可测试性
  - [x] SubTask 16.1: 为服务类添加接口，支持 Mock 测试
  - [x] SubTask 16.2: 创建测试工具类和 Mock 数据

## Phase 8: 验证与测试

- [x] Task 17: 功能验证
  - [x] SubTask 17.1: 验证用户注册流程完整性
  - [x] SubTask 17.2: 验证审核流程完整性
  - [x] SubTask 17.3: 验证管理后台功能完整性
  - [x] SubTask 17.4: 验证 Discord 集成功能
  - [x] SubTask 17.5: 验证问卷评分功能
  - [x] SubTask 17.6: 验证 AuthMe 集成功能
  - [x] SubTask 17.7: 验证代理插件功能

---

# Task Dependencies

```
Phase 1 (Task 1-3) 是所有后续任务的基础
  └── Task 1, 2, 3 可并行执行

Phase 2 (Task 4-6) 依赖 Phase 1
  ├── Task 4 依赖 Task 1, 3
  ├── Task 5 依赖 Task 1
  └── Task 6 可与 Task 4-5 并行执行

Phase 3 (Task 7-10) 依赖 Phase 1
  ├── Task 7, 8, 9 可并行执行，依赖 Task 2
  └── Task 10 依赖 Task 7-9 完成

Phase 4 (Task 11-12) 可与 Phase 2-3 并行执行
  └── Task 11, 12 可并行执行

Phase 5 (Task 13) 依赖 Phase 3-4
  └── Task 13 依赖 Task 10, 11 完成

Phase 6 (Task 14) 可在 Phase 3-5 执行过程中穿插进行

Phase 7 (Task 15-16) 依赖 Phase 2-5 完成

Phase 8 (Task 17) 依赖所有前置任务完成
```

# Parallel Execution Groups

**Group 1 (可并行)**: Task 1, Task 2, Task 3
**Group 2 (可并行)**: Task 4, Task 5, Task 6, Task 11, Task 12
**Group 3 (可并行)**: Task 7, Task 8, Task 9
**Group 4 (可并行)**: Task 14 (可在 Group 3 执行过程中穿插)
**Group 5 (可并行)**: Task 15, Task 16
