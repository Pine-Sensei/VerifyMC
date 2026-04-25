# ✅ VerifyMC

[English](README.md) | 简体中文 | [📚 官方文档](https://kitemc.com/zh/docs/verifymc/) | [🚀 更新日志](release_notes_zh.md)

---

## 🚀 项目简介

**VerifyMC** 是一款面向 Minecraft 服务器的网页优先白名单与注册系统。它为玩家提供了更清晰的申请流程，并为服务器管理人员提供了实用的管理面板，用于审核、用户管理、审计记录和日常运维。

它专为那些希望比手动编辑白名单文件更有条理、同时仍保持足够简单以适用于小型私服的社区而设计。

---

## 📝 主要功能

1. 🖥️ **网页注册门户**：玩家通过浏览器注册，无需盲入服务器或发送手动申请。
2. 🔐 **灵活的验证流程**：组合使用邮箱验证、自托管验证码、可选的 Discord 绑定和可选的问卷系统。
3. ✅ **自动或手动审核**：运行轻量级私服流程，或要求管理员审核每份申请。
4. 🎛️ **管理面板**：在一个地方审核待处理用户、管理账户、查看审计日志和实时服务器状态。
5. 👤 **玩家自助区域**：已登录玩家可以在同一网页界面访问个人资料、修改密码、更新邮箱和下载服务器文件。
6. 📦 **下载中心**：直接从管理面板发布整合包、资源包或客户端文件。
7. 🔐 **AuthMe 集成**：当 AuthMe 是你技术栈的一部分时，支持密码收集、同步和管理员密码修改。
8. 🌉 **基岩版支持**：支持 Geyser/Floodgate 风格的基岩版前缀，适用于混合平台社区。
9. 🔗 **代理支持**：可选的 BungeeCord/Velocity 插件可在玩家进入网络之前检查白名单状态。
10. 💾 **灵活存储**：将用户数据存储在本地文件或 MySQL 中，审计记录也可存储在文件或 MySQL 中。
11. 🌍 **多语言支持**：网页界面、插件消息和邮件模板支持中英文，并支持自定义 i18n 文件。
12. 🤖 **问卷评分**：通过任何 OpenAI 兼容的 LLM 端点（DeepSeek、Google 等）对文本答案进行评分，内置并发和熔断器控制。
13. 🎨 **现代 GlassX 界面**：简洁的默认前端，包含公告、登录、注册、玩家区域和管理区域。
14. 🌐 **外部前端托管**：从 Pages/CDN/Nginx 提供前端服务，同时将插件 API 保留在 Minecraft 服务器上。
15. 🛠️ **游戏内管理命令**：使用 `/vmc` 命令在游戏内快速审核和管理。
16. 🛡️ **安全加固**：包括速率限制、审计追踪、哈希密码和更安全的配置验证。
17. 📱 **短信验证**：可选的腾讯云或阿里云短信验证，支持手机号注册流程。
18. ✉️ **用户通知邮件**：当申请被审核通过或拒绝时，自动通过邮件通知玩家。
19. 🔗 **Discord 解绑**：玩家可以解绑自己的 Discord 账号；管理员可以解绑其他用户的 Discord。
20. 🔔 **版本检查**：自动检查最新版本，管理面板中显示更新通知。
21. 🔒 **内置 SSL**：通过 keystore 配置可选的 HTTPS/WSS 支持。
22. 📧 **邮箱域名白名单**：限制特定邮箱域名注册，可选别名检测。

---

## 🖼️ 界面概览

- **首页**：面向新玩家的品牌展示和公告入口。
- **注册页**：用户名、邮箱/短信/验证码、可选密码、可选问卷和可选 Discord 流程。
- **玩家面板**：个人资料区域、修改密码、更新邮箱和下载中心。
- **管理面板**：待审核列表、用户管理、审计日志、服务器状态（TPS、内存、在线玩家）和版本更新通知。
- **截图**：最新截图维护在官方文档中，以保持仓库轻量。

---

## ⚙️ 配置项说明

- `config.yml`：认证方式（邮箱、验证码）、白名单模式、SMTP、SSL、主题、存储、Discord、下载、基岩版设置、邮箱域名白名单、用户通知、前端托管等。
- `questionnaire.yml`：问题列表、答案类型（`single_choice`、`multiple_choice`、`text`）、通过分数和文本评分规则。
- `plugin-proxy/config.yml`：后端 URL、注册 URL、踢出消息（支持颜色代码）、缓存、超时、语言、自动更新和备份设置（使用代理插件时）。

---

## 📦 安装与配置

1. 下载最新版 `VerifyMC.jar`，放入服务器 `plugins` 目录。
2. 启动服务器一次以生成 `config.yml`、语言文件、邮件模板和 `questionnaire.yml`。
3. 根据你的注册流程编辑 `config.yml`。如果不使用 AuthMe，请设置 `authme.enabled: false`。
4. 重启服务器并访问 `http://你的服务器IP:8080`。
5. 从网页注册第一个账号，然后在 `config.yml` 中选择管理员认证模式。
6. 如果 `admin_auth.mode: op`，给自己 OP 权限以使用管理面板和游戏内管理操作。
7. 如果 `admin_auth.mode: permission`，授予 `verifymc.admin` 或你需要的特定 `verifymc.admin.*` 权限节点。对于离线用户的网页管理员权限检查，需要安装 Vault 和兼容 Vault 的权限插件。

### ✅ 建议最低环境

- Java 17+
- Bukkit/Spigot/Paper/Folia 1.20+
- 生产环境强烈建议使用公网 HTTPS 域名
- 仅当启用 `email` 验证方式时需要 SMTP 邮箱账号
- 仅当选择 `storage: mysql` 时需要 MySQL

### ⚡ 5 分钟快速开始

1. 在 `config.yml` 中设置 `auth_methods: [captcha]`（最快启动方式）。其他选项：`[email]`，或组合如 `[email, captcha]`。
2. 设置 `whitelist_mode: plugin` 和 `web_register_url: https://your-domain.com/`。
3. 如果不使用 AuthMe，设置 `authme.enabled: false`。
4. 启动或重启服务器。
5. 访问 `http://你的服务器IP:8080`，注册账号，然后给该账号 OP 权限以访问管理面板。
6. 可选：小型私服可开启 `register.auto_approve: true`。

### 🌍 常见部署模式

- **单服务器**：主插件自行提供前端服务。这是最简单的设置，适用于大多数服务器。
- **独立前端源**：将 `frontend/glassx/dist` 托管在另一个域名或 CDN 上，同时插件继续提供 API 和 WebSocket 端点。
- **代理网络**：将主插件保留在后端 Paper/Folia 服务器上，并在 Waterfall 或 Velocity 上安装可选的代理插件以实现网络级管控。

### 🌍 在其他源托管前端

默认情况下，VerifyMC 自行提供构建的前端服务。如果你想将静态 `glassx` 文件托管在 Cloudflare Pages、GitHub Pages、CDN 或 Nginx 上，同时将 API 保留在插件服务器上：

1. 在 `config.yml` 中设置 `frontend.serve_static: false`。
2. 将 `frontend.allowed_origins` 设置为前端源列表，例如 `['https://your-pages-domain.pages.dev']`。
3. 如需匹配单层子域名，可使用 `https://*.example.com` 这种写法；所有匹配都会忽略端口。
4. 在构建 `frontend/glassx` 之前，将 `VITE_BOOTSTRAP_API_BASE` 设置为插件 HTTP 基础 URL，例如 `https://mc.example.com:8080`。
5. 不要在 `VITE_BOOTSTRAP_API_BASE` 后追加 `/api`；前端会自动派生 API 和 WebSocket URL。
6. 构建 `frontend/glassx` 并将生成的 `dist/` 文件部署到你的静态主机。

在此模式下，页面和资源从你的静态主机加载，而 API 和 WebSocket 流量仍然发送到 VerifyMC 插件服务器。

### 🔗 使用代理插件

如果你想在玩家进入代理网络之前进行白名单管控：

1. 保持主 VerifyMC 插件在后端 Bukkit/Paper/Folia 服务器上运行。
2. 在 Waterfall 或 Velocity 上安装代理插件。
3. 在 `plugin-proxy/config.yml` 中，将 `backend_url` 设置为后端 VerifyMC 网页地址，例如 `http://backend-host:8080`。
4. 不要在 `backend_url` 后追加 `/api`；代理插件会自行添加 API 路径。
5. 将 `register_url` 设置为未通过审核的玩家应打开的公开页面。

### 🛠️ 常用 `/vmc` 命令

- `/vmc reload`：重载插件配置和语言缓存。
- `/vmc approve <用户名>`：审核通过待处理用户。
- `/vmc reject <用户名> [原因]`：拒绝用户，可附带原因。
- `/vmc ban <用户名> [原因]`：封禁用户并将其移出白名单。
- `/vmc unban <用户名>`：解封已封禁用户。
- `/vmc delete <用户名>`：删除用户记录。
- `/vmc list [all|pending|approved|rejected|banned]`：按状态列出用户。
- `/vmc info <用户名>`：查看单个用户详情。
- `/vmc version`：显示当前插件版本。

### 🔐 权限节点

- `verifymc.use`：访问基础 `/vmc` 命令和 `/vmc version`。
- `verifymc.admin`：授予以下所有管理员子命令权限。
- `verifymc.admin.reload`：允许 `/vmc reload`。
- `verifymc.admin.approve`：允许 `/vmc approve`。
- `verifymc.admin.reject`：允许 `/vmc reject`。
- `verifymc.admin.delete`：允许 `/vmc delete`。
- `verifymc.admin.ban`：允许 `/vmc ban`。
- `verifymc.admin.unban`：允许 `/vmc unban`。
- `verifymc.admin.list`：允许 `/vmc list`。
- `verifymc.admin.info`：允许 `/vmc info`。
- `verifymc.admin.audit`：允许在网页管理面板查看管理员审计日志。
- `verifymc.admin.sync`：允许从网页管理面板触发管理员同步操作。
- `verifymc.admin.password`：允许从网页管理面板修改用户密码。
- `verifymc.admin.unlink`：允许从网页管理面板解绑其他用户的 Discord 账号。

### ⚙️ 管理员认证模式

- `admin_auth.mode: op`：`/vmc` 管理操作和网页管理员访问均由 OP 状态决定。
- `admin_auth.mode: permission`：`/vmc` 管理操作和网页管理员访问均由权限节点决定。
- 在 `permission` 模式下，命令检查直接使用 Bukkit 权限。
- 在 `permission` 模式下，离线用户的网页检查需要 Vault 加兼容 Vault 的权限插件。在线用户仍可通过 Bukkit 直接解析。

### 🧪 从源码构建

大多数服务器管理员可以直接使用发布文件。仅在你想要自定义或自行打包项目时才需要从源码构建。

构建前端：

```bash
cd frontend/glassx
npm ci
npm run build
```

可选的前端检查：

```bash
cd frontend/glassx
npm run type-check
npm run test
```

构建主插件：

```bash
cd plugin
mvn clean package
```

构建代理插件：

```bash
cd plugin-proxy
mvn clean package
```

构建产物：

- 主插件 jar：`plugin/target/verifymc-1.7.8.jar`
- 代理插件 jar：`plugin-proxy/target/verifymc-proxy-1.7.8.jar`
- 前端包：`frontend/glassx/dist/`

---

## 💬 官方社区

- **QQ 群**: 1041540576 ([点击加入](https://qm.qq.com/q/F7zuhZ7Mze))
- **Discord**: [https://discord.gg/TCn9v88V](https://discord.gg/TCn9v88V)

---

> ❤️ 如果你喜欢本项目，欢迎 Star、分享与反馈！
