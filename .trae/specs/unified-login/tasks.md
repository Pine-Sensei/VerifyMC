# Tasks

- [x] Task 1: 后端登录接口返回用户角色信息
  - [x] SubTask 1.1: 在 LoginHandler.java 中判断用户是否为管理员（通过 OpsManager.isOp）
  - [x] SubTask 1.2: 在登录成功响应中添加 `isAdmin` 字段
  - [x] SubTask 1.3: 确保 `/api/login` 端点正确返回角色信息

- [x] Task 2: 后端管理接口权限校验增强
  - [x] SubTask 2.1: 创建权限校验工具类或中间件
  - [x] SubTask 2.2: 为所有 `/api/admin/*` 接口添加权限校验
  - [x] SubTask 2.3: 非管理员访问时返回 403 Forbidden
  - [x] SubTask 2.4: 记录权限校验失败的审计日志

- [x] Task 3: 审计日志功能实现（已存在，无需修改）
  - [x] SubTask 3.1: 设计审计日志数据结构（操作时间、操作者、操作类型、目标对象、操作结果）
  - [x] SubTask 3.2: 创建审计日志存储表或文件
  - [x] SubTask 3.3: 在管理操作中集成审计日志记录
  - [x] SubTask 3.4: 创建审计日志查询接口 `/api/admin/audits`

- [x] Task 4: 前端会话管理重构
  - [x] SubTask 4.1: 更新 session.ts 存储 `isAdmin` 信息
  - [x] SubTask 4.2: 提供获取当前用户权限的方法

- [x] Task 5: 前端统一面板页面设计
  - [x] SubTask 5.1: 创建 Dashboard.vue 统一面板页面
  - [x] SubTask 5.2: 设计侧边栏菜单组件（根据权限动态显示）
  - [x] SubTask 5.3: 实现玩家区模块：个人资料
  - [x] SubTask 5.4: 实现玩家区模块：下载中心
  - [x] SubTask 5.5: 实现管理区模块：服务器状态
  - [x] SubTask 5.6: 实现管理区模块：玩家管理（迁移现有功能）
  - [x] SubTask 5.7: 实现管理区模块：审计日志

- [x] Task 6: 前端路由重构
  - [x] SubTask 6.1: 添加 `/dashboard` 路由
  - [x] SubTask 6.2: 设置 `/admin` 重定向到 `/dashboard`
  - [x] SubTask 6.3: 设置 `/status` 重定向到 `/dashboard`
  - [x] SubTask 6.4: 更新登录成功跳转逻辑

- [x] Task 7: 前端 API 服务更新
  - [x] SubTask 7.1: 更新 api.ts 中的登录响应类型
  - [x] SubTask 7.2: 添加审计日志查询 API
  - [x] SubTask 7.3: 添加下载中心相关 API（如需要）

- [x] Task 8: 国际化文案更新
  - [x] SubTask 8.1: 添加 Dashboard 相关翻译
  - [x] SubTask 8.2: 添加各功能模块的翻译
  - [x] SubTask 8.3: 更新登录成功提示文案

- [x] Task 9: 测试验证
  - [x] SubTask 9.1: 测试玩家登录后界面显示
  - [x] SubTask 9.2: 测试管理员登录后界面显示
  - [x] SubTask 9.3: 测试后端权限校验
  - [x] SubTask 9.4: 测试审计日志记录

# Task Dependencies
- Task 2 依赖 Task 1（权限校验需要识别管理员）
- Task 3 可与 Task 1-2 并行
- Task 4 依赖 Task 1（前端需要后端返回的 isAdmin）
- Task 5 依赖 Task 4（面板需要权限信息）
- Task 6 依赖 Task 5（路由指向新页面）
- Task 7 可与 Task 5 并行
- Task 8 可与 Task 5 并行
- Task 9 依赖所有其他任务完成
