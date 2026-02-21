# 统一登录页面与权限框架重构 Spec

## Why
当前系统将管理员和玩家分离到不同页面，需要重构为统一的页面框架，根据用户权限动态显示/隐藏功能模块，提供更好的用户体验和更清晰的权限管理。

## What Changes
- 登录后所有用户进入相同的统一页面
- 设计包含菜单和功能模块的界面框架
- 根据用户权限动态控制界面元素显示/隐藏
- 后端对所有管理接口实施严格权限校验
- 实现管理操作审计日志功能

## Impact
- Affected specs: 登录认证流程、权限管理、前端路由
- Affected code:
  - `frontend/glassx/src/components/LoginForm.vue` - 登录跳转逻辑
  - `frontend/glassx/src/pages/AdminPanel.vue` - 重构为统一面板
  - `frontend/glassx/src/pages/UserStatus.vue` - 合并到统一面板
  - `frontend/glassx/src/router.ts` - 路由重构
  - `frontend/glassx/src/services/api.ts` - API 调用
  - `frontend/glassx/src/services/session.ts` - 会话管理
  - `plugin/src/main/java/team/kitemc/verifymc/web/handler/LoginHandler.java` - 登录响应
  - `plugin/src/main/java/team/kitemc/verifymc/web/handler/*.java` - 权限校验
  - `frontend/glassx/src/locales/*.json` - 国际化

## ADDED Requirements

### Requirement: 统一页面框架
系统应提供统一的页面框架，所有角色登录后进入相同 URL 页面。

#### Scenario: 用户登录后进入统一面板
- **WHEN** 任意用户登录成功
- **THEN** 系统跳转到统一面板页面 `/dashboard`
- **AND** 页面显示用户权限可见的功能模块

### Requirement: 权限动态判断
菜单和功能模块的显示/隐藏需基于实时权限判断。

#### Scenario: 玩家查看界面
- **WHEN** 玩家登录后查看界面
- **THEN** 显示玩家区菜单（个人资料、下载中心）
- **AND** 隐藏管理区菜单（服务器状态、玩家管理、审计日志）

#### Scenario: 管理员查看界面
- **WHEN** 管理员登录后查看界面
- **THEN** 显示所有菜单（玩家区 + 管理区）
- **AND** 管理区菜单可见并可操作

### Requirement: 功能区域划分

#### 玩家区（所有用户可见）
1. **个人资料模块**
   - 查看和编辑个人信息
   - 修改密码
   
2. **下载中心模块**
   - 下载客户端资源
   - 查看下载说明

#### 管理区（仅管理员可见）
1. **服务器状态模块**
   - 查看服务器运行状态
   - 查看在线玩家

2. **玩家管理模块**
   - 审核待注册玩家
   - 管理现有玩家（封禁、解封、删除）
   - 修改玩家信息

3. **审计日志模块**
   - 查看管理操作日志
   - 按时间、操作类型筛选

### Requirement: 后端权限校验
后端必须对每个管理接口实施严格的权限校验。

#### Scenario: 非管理员访问管理接口
- **WHEN** 非管理员用户尝试调用管理接口
- **THEN** 系统返回 403 Forbidden 错误
- **AND** 记录安全审计日志

#### Scenario: 管理员访问管理接口
- **WHEN** 管理员用户调用管理接口
- **THEN** 系统验证权限通过
- **AND** 执行操作
- **AND** 记录操作审计日志

### Requirement: 审计日志功能
实现管理操作的审计日志功能，记录所有管理操作。

#### Scenario: 记录管理操作
- **WHEN** 管理员执行任何管理操作
- **THEN** 系统记录审计日志
- **AND** 日志包含：操作时间、操作者、操作类型、目标对象、操作结果

## MODIFIED Requirements

### Requirement: 登录 API 响应
登录 API 响应应包含用户角色信息。

**修改后：**
```json
{
  "success": true,
  "message": "登录成功",
  "token": "xxx",
  "username": "player1",
  "isAdmin": false
}
```

### Requirement: 前端登录成功跳转逻辑
登录成功后统一跳转到 `/dashboard` 页面。

**修改后：** 所有用户登录成功后跳转到 `/dashboard`

### Requirement: 路由结构
简化路由结构，移除分离的 `/admin` 和 `/status` 页面。

**修改后：**
- `/dashboard` - 统一面板页面（需登录）
- `/login` - 登录页面
- `/register` - 注册页面
- `/` - 首页

## REMOVED Requirements

### Requirement: 分离的管理员和玩家页面
**Reason**: 统一使用 `/dashboard` 页面，根据权限动态显示功能
**Migration**: 
- `/admin` 路由重定向到 `/dashboard`
- `/status` 路由重定向到 `/dashboard`
