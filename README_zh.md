# ✅ VerifyMC

[English](README.md) | 简体中文 | [📚 官方文档](https://kitemc.com/zh/docs/verifymc/) | [🚀 更新日志](release_notes_zh.md)

---

## 🚀 项目简介

**VerifyMC** 是一套面向 Minecraft 服务器的网页化白名单与注册系统。它让玩家通过网页完成申请流程，也让服主和管理员通过统一后台完成审核、用户管理、审计记录查看与日常运维。

它适合想摆脱手改白名单文件、希望把注册和审核流程做得更清晰的社区，同时也足够简单，适合小型私服快速上线使用。

---

## 📝 主要功能

1. 🖥️ **网页注册入口**：玩家通过浏览器完成白名单申请，不再依赖手工收集资料。
2. 🔐 **灵活验证流程**：可组合邮箱验证码、自托管图形验证码、可选 Discord 绑定、可选注册问卷。
3. ✅ **自动或手动审核**：既能适配小型私服的快速放行，也能支持大型社区逐条审核。
4. 🎛️ **管理后台**：集中处理待审核用户、用户管理、审计日志与实时服务器状态。
5. 👤 **玩家自助区域**：登录后的玩家可在同一套前端中查看资料与下载资源。
6. 📦 **下载中心**：可发布整合包、资源包或客户端文件，直接展示在网页后台中。
7. 🔐 **AuthMe 集成**：当你的服务器使用 AuthMe 时，可接入密码收集、同步与后台改密流程。
8. 🌉 **基岩版支持**：兼容 Geyser/Floodgate 风格的 Bedrock 前缀配置，适合混合平台社区。
9. 🔗 **代理服支持**：可选 BungeeCord/Velocity 代理插件，在玩家进入网络前完成白名单校验。
10. 💾 **灵活存储**：用户数据可使用本地文件或 MySQL，审计日志也支持文件或 MySQL 存储。
11. 🌍 **多语言支持**：网页界面、插件消息与邮件模板支持中英文，并允许自定义 i18n 文件。
12. 🤖 **问卷智能评分**：文本题可接入 DeepSeek 或 Google 兼容接口进行 LLM 自动评分，并带并发控制与熔断保护。
13. 🎨 **现代化 GlassX 界面**：默认前端包含公告、登录、注册、玩家区与管理区。
14. 🛠️ **游戏内管理命令**：支持通过 `/vmc` 在游戏内快速执行审核和管理操作。
15. 🛡️ **安全加固**：内置限流、审计轨迹、密码哈希和更严格的配置校验。

---

## 🖼️ 界面与使用流程

- **首页**：展示服务器名称与公告，是玩家访问入口。
- **注册页**：处理用户名、邮箱/验证码、可选密码、可选问卷和可选 Discord 绑定流程。
- **玩家后台**：提供个人资料与下载中心。
- **管理后台**：集中展示待审核列表、用户管理、审计日志与服务器状态。
- **截图说明**：最新界面截图统一维护在官方文档中，仓库本身尽量保持轻量。

---

## ⚙️ 你需要配置的内容

- `config.yml`：认证方式、白名单模式、SMTP、主题、存储、Discord、下载资源与基岩版设置。
- `questionnaire.yml`：问卷题目、题型（`single_choice`、`multiple_choice`、`text`）、通过分数与文本题评分规则。
- `plugin-proxy/config.yml`：当启用代理插件时，配置后端地址、注册地址、踢出提示、缓存与语言。

---

## 📦 安装与配置

1. 下载最新发布版 `VerifyMC.jar`，放入服务器的 `plugins` 目录。
2. 启动服务器一次，生成 `config.yml`、语言文件、邮件模板与 `questionnaire.yml`。
3. 按你的注册流程编辑 `config.yml`。如果你不用 AuthMe，请将 `authme.enabled: false`。
4. 重启服务器后，访问 `http://你的服务器IP:8080`。
5. 从网页注册第一个账号后，在 `config.yml` 里选择管理员鉴权模式。
6. 如果 `admin_auth.mode: op`，给该账号 OP，即可使用 Web 管理后台和游戏内管理操作。
7. 如果 `admin_auth.mode: permission`，授予 `verifymc.admin` 或所需的 `verifymc.admin.*` 子权限。要让 Web 后台对离线用户也能正确判权，还需要安装 Vault 和兼容 Vault 的权限插件。

### ✅ 建议最低环境

- Java 17+
- Bukkit/Spigot/Paper/Folia 1.20+
- 生产环境建议准备可公网访问的 HTTPS 域名
- 只有启用 `email` 验证时才需要 SMTP 邮箱
- 只有选择 `storage: mysql` 时才需要 MySQL

### ⚡ 5 分钟快速开始

1. 在 `config.yml` 中设置 `auth_methods: [captcha]`，这是最快的启动方式。
2. 设置 `whitelist_mode: plugin` 和 `web_register_url: https://your-domain.com/`。
3. 如果你不使用 AuthMe，将 `authme.enabled: false`。
4. 启动或重启服务器。
5. 打开 `http://你的服务器IP:8080`，注册一个账号，并给该账号 OP 以访问管理后台。
6. 如果是小型私服，可选开启 `register.auto_approve: true` 简化审核流程。

### 🌍 常见部署方式

- **单服部署**：主插件直接托管前端页面，这是最简单、最适合多数服务器的方案。
- **代理服网络部署**：主插件运行在后端 Paper/Folia 服，代理插件运行在 Waterfall 或 Velocity，用于网络级白名单拦截。


### 🔗 使用代理插件

如果你希望玩家在进入代理网络前就完成白名单校验：

1. 保持主 VerifyMC 插件运行在后端 Bukkit/Paper/Folia 服务器上。
2. 在 Waterfall 或 Velocity 上安装代理插件。
3. 在 `plugin-proxy/config.yml` 中把 `backend_url` 设置为后端 VerifyMC 的网页地址，例如 `http://backend-host:8080`。
4. `backend_url` 不要追加 `/api`，代理插件会自行拼接 API 路径。
5. 把 `register_url` 设置为玩家未通过审核时应访问的公开注册页面。

### 🛠️ 常用 `/vmc` 命令

- `/vmc reload`：重载插件配置和语言缓存。
- `/vmc approve <用户名>`：通过待审核用户。
- `/vmc reject <用户名> [原因]`：拒绝用户并可附带原因。
- `/vmc ban <用户名> [原因]`：封禁用户并移除其白名单访问权限。
- `/vmc unban <用户名>`：解除封禁。
- `/vmc delete <用户名>`：删除用户记录。
- `/vmc list [all|pending|approved|rejected|banned]`：按状态查看用户列表。
- `/vmc info <用户名>`：查看单个用户信息。
- `/vmc version`：查看当前插件版本。

### 🔐 权限节点

- `verifymc.use`：访问基础 `/vmc` 命令和 `/vmc version`。
- `verifymc.admin`：授予下方所有管理子命令。
- `verifymc.admin.reload`：允许 `/vmc reload`。
- `verifymc.admin.approve`：允许 `/vmc approve`。
- `verifymc.admin.reject`：允许 `/vmc reject`。
- `verifymc.admin.delete`：允许 `/vmc delete`。
- `verifymc.admin.ban`：允许 `/vmc ban`。
- `verifymc.admin.unban`：允许 `/vmc unban`。
- `verifymc.admin.list`：允许 `/vmc list`。
- `verifymc.admin.info`：允许 `/vmc info`。
- `verifymc.admin.audit`：允许在 Web 后台查看审计日志。
- `verifymc.admin.sync`：允许在 Web 后台执行同步操作。
- `verifymc.admin.password`：允许在 Web 后台修改用户密码。

### ⚙️ 管理员鉴权模式

- `admin_auth.mode: op`：`/vmc` 管理操作和 Web 管理后台都按 OP 身份判断。
- `admin_auth.mode: permission`：`/vmc` 管理操作和 Web 管理后台都按权限节点判断。
- 在 `permission` 模式下，命令直接使用 Bukkit 权限判断。
- 在 `permission` 模式下，Web 对离线用户的权限判断依赖 Vault 和兼容 Vault 的权限插件；在线用户仍可直接通过 Bukkit 判断。

### 🧪 从源码构建

多数服主直接使用发布版即可。只有在你需要自行打包或二次定制时，才需要从源码构建。

构建前端：

```bash
cd frontend/glassx
npm ci
npm run build
```

可选前端检查：

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

- 主插件 jar：`plugin/target/verifymc-1.7.1.jar`
- 代理插件 jar：`plugin-proxy/target/verifymc-proxy-1.7.1.jar`
- 前端静态文件：`frontend/glassx/dist/`

---

## 📊 Bstats

![Bstats](https://bstats.org/signatures/bukkit/verifymc.svg)

---

## 💬 官方社区

- **QQ 群**: 1041540576 ([点击加入](https://qm.qq.com/q/F7zuhZ7Mze))
- **Discord**: [https://discord.gg/TCn9v88V](https://discord.gg/TCn9v88V)

---

> ❤️ 如果你喜欢本项目，欢迎 Star、分享与反馈！
