# ✅ VerifyMC

[简体中文](README_zh.md) | English | [📚 Official Documentation](https://kitemc.com/docs/VerifyMC/) | [🚀 Release Notes](release_notes_zh.md)

---

## 🚀 Introduction

**VerifyMC** is a web-first whitelist and registration system for Minecraft servers. It gives players a cleaner application flow and gives server staff a practical dashboard for review, user management, audit records, and day-to-day operations.

It is designed for communities that want something more structured than editing whitelist files by hand, while still remaining simple enough for small private servers.

---

## 📝 Key Features

1. 🖥️ **Web Registration Portal**: Players register from a browser instead of joining blind or sending manual applications.
2. 🔐 **Flexible Verification Flow**: Combine email verification, self-hosted CAPTCHA, optional Discord linking, and optional questionnaires.
3. ✅ **Auto or Manual Approval**: Run a lightweight private-server flow or require staff review for every application.
4. 🎛️ **Admin Dashboard**: Review pending users, manage accounts, inspect audit logs, and check live server status in one place.
5. 👤 **Player Self-Service Area**: Logged-in players can access their profile, change their password, update their email, and download server files from the same web UI.
6. 📦 **Download Center**: Publish modpacks, resource packs, or client files directly from the dashboard.
7. 🔐 **AuthMe Integration**: Support password collection, synchronization, and admin password changes when AuthMe is part of your stack.
8. 🌉 **Bedrock Support**: Works with Geyser/Floodgate style Bedrock prefixes for mixed-platform communities.
9. 🔗 **Proxy Support**: Optional BungeeCord/Velocity plugin can check whitelist status before players enter your network.
10. 💾 **Flexible Storage**: Store users in local files or MySQL, with audit records available in file or MySQL storage as well.
11. 🌍 **Multi-language Support**: Web UI, plugin messages, and mail templates support English and Chinese, with custom i18n files supported.
12. 🤖 **Questionnaire Scoring**: Score text answers through any OpenAI-compatible LLM endpoint (DeepSeek, Google, etc.), with concurrency and circuit-breaker controls.
13. 🎨 **Modern GlassX UI**: Clean default frontend with announcements, login, registration, player area, and admin area.
14. 🌐 **External Frontend Hosting**: Serve the frontend from Pages/CDN/Nginx while keeping the plugin API on your Minecraft server.
15. 🛠️ **In-Game Admin Commands**: Use `/vmc` commands for quick review and moderation tasks in-game.
16. 🛡️ **Security Hardening**: Includes rate limits, audit trails, hashed passwords, and safer config validation.
17. 📱 **SMS Verification**: Optional SMS verification via Tencent Cloud or Alibaba Cloud for phone-based registration flows.
18. ✉️ **User Notification Emails**: Automatically notify players via email when their application is approved or rejected.
19. 🔗 **Discord Unlink**: Players can unlink their Discord account; admins can unlink Discord for other users.
20. 🔔 **Version Check**: Automatic version checking with admin dashboard notifications when updates are available.
21. 🔒 **Built-in SSL**: Optional HTTPS/WSS support via keystore configuration.
22. 📧 **Email Domain Whitelist**: Restrict registration to specific email domains, with optional alias detection.

---

## 🖼️ Interface Overview

- **Home page**: branding and announcement entry point for new players.
- **Register page**: username, email/CAPTCHA, optional password, optional questionnaire, and optional Discord flow.
- **Player dashboard**: profile area, password change, email update, and download center.
- **Admin dashboard**: pending reviews, user management, audit log, server status (TPS, memory, online players), and version update notifications.
- **Screenshots**: the latest screenshots are maintained in the official documentation so the repository can stay lightweight.

---

## ⚙️ What You Configure

- `config.yml`: authentication methods (email, CAPTCHA), whitelist mode, SMTP, SSL, theme, storage, Discord, downloads, Bedrock settings, email domain whitelist, user notifications, frontend hosting, and more.
- `questionnaire.yml`: question list, answer types (`single_choice`, `multiple_choice`, `text`), pass score, and text scoring rules.
- `plugin-proxy/config.yml`: backend URL, registration URL, kick message (with color codes), cache, timeout, language, auto-update, and backup settings when using the proxy plugin.

---

## 📦 Installation & Configuration

1. Download the latest `VerifyMC.jar` release and place it in your server `plugins` directory.
2. Start the server once to generate `config.yml`, language files, email templates, and `questionnaire.yml`.
3. Edit `config.yml` based on your registration flow. If you do not use AuthMe, set `authme.enabled: false`.
4. Restart the server and open `http://your-server-ip:8080`.
5. Register the first account from the web page, then choose an admin auth mode in `config.yml`.
6. If `admin_auth.mode: op`, grant yourself OP to use both the admin dashboard and in-game admin actions.
7. If `admin_auth.mode: permission`, grant `verifymc.admin` or the specific `verifymc.admin.*` nodes you need. For web admin permission checks on offline users, install Vault and a Vault-compatible permissions plugin.

### ✅ Recommended Minimum Environment

- Java 17+
- Bukkit/Spigot/Paper/Folia 1.20+
- A public HTTPS domain is recommended for production use
- An SMTP account is only required when `email` verification is enabled
- MySQL is only required when you choose `storage: mysql`

### ⚡ 5-Minute Quick Start

1. Set `auth_methods: [captcha]` in `config.yml` for the fastest setup. Other options: `[email]` or a combination like `[email, captcha]`.
2. Set `whitelist_mode: plugin` and `web_register_url: https://your-domain.com/`.
3. If you do not use AuthMe, set `authme.enabled: false`.
4. Start or restart the server.
5. Open `http://your-server-ip:8080`, register an account, and grant that account OP to access the admin dashboard.
6. Optional: enable `register.auto_approve: true` for a small private community.

### 🌍 Common Deployment Modes

- **Single server**: the main plugin serves the frontend itself. This is the easiest setup and works for most servers.
- **Separate frontend origin**: host `frontend/glassx/dist` on another domain or CDN while the plugin keeps serving the API and WebSocket endpoints.
- **Proxy network**: keep the main plugin on your backend Paper/Folia server and install the optional proxy plugin on Waterfall or Velocity for network-level enforcement.

### 🌍 Hosting the Frontend on Another Origin

By default, VerifyMC serves the built frontend itself. If you want to host the static `glassx` files on Cloudflare Pages, GitHub Pages, a CDN, or Nginx while keeping the API on the plugin server:

1. Set `frontend.serve_static: false` in `config.yml`.
2. Set `frontend.allowed_origins` to a list of frontend origins, for example `['https://your-pages-domain.pages.dev']`.
3. To match a single subdomain level, you can use `https://*.example.com`; all matching ignores ports.
4. Before building `frontend/glassx`, set `VITE_BOOTSTRAP_API_BASE` to the plugin HTTP base URL, for example `https://mc.example.com:8080`.
5. Do not append `/api` to `VITE_BOOTSTRAP_API_BASE`; the frontend derives API and WebSocket URLs automatically.
6. Build `frontend/glassx` and deploy the generated `dist/` files to your static host.

In this mode, pages and assets are loaded from your static host, while API and WebSocket traffic still goes to the VerifyMC plugin server.

### 🔗 Using the Proxy Plugin

If you want whitelist enforcement before players enter a proxy network:

1. Keep the main VerifyMC plugin running on your backend Bukkit/Paper/Folia server.
2. Install the proxy plugin on Waterfall or Velocity.
3. In `plugin-proxy/config.yml`, set `backend_url` to the backend VerifyMC web address such as `http://backend-host:8080`.
4. Do not append `/api` to `backend_url`; the proxy plugin adds the API path itself.
5. Set `register_url` to the public page players should open when they are not approved yet.

### 🛠️ Common `/vmc` Commands

- `/vmc reload`: reload plugin configuration and language cache.
- `/vmc approve <username>`: approve a pending user.
- `/vmc reject <username> [reason]`: reject a user with an optional reason.
- `/vmc ban <username> [reason]`: ban a user and remove them from whitelist access.
- `/vmc unban <username>`: restore a banned user.
- `/vmc delete <username>`: remove a user record.
- `/vmc list [all|pending|approved|rejected|banned]`: list users by status.
- `/vmc info <username>`: inspect a single user.
- `/vmc version`: show the current plugin version.

### 🔐 Permission Nodes

- `verifymc.use`: access the base `/vmc` command and `/vmc version`.
- `verifymc.admin`: grants every admin subcommand below.
- `verifymc.admin.reload`: allow `/vmc reload`.
- `verifymc.admin.approve`: allow `/vmc approve`.
- `verifymc.admin.reject`: allow `/vmc reject`.
- `verifymc.admin.delete`: allow `/vmc delete`.
- `verifymc.admin.ban`: allow `/vmc ban`.
- `verifymc.admin.unban`: allow `/vmc unban`.
- `verifymc.admin.list`: allow `/vmc list`.
- `verifymc.admin.info`: allow `/vmc info`.
- `verifymc.admin.audit`: allow viewing admin audit logs in the web dashboard.
- `verifymc.admin.sync`: allow triggering admin sync actions from the web dashboard.
- `verifymc.admin.password`: allow changing user passwords from the web dashboard.
- `verifymc.admin.unlink`: allow unlinking Discord accounts for other users from the web dashboard.

### ⚙️ Admin Auth Mode

- `admin_auth.mode: op`: both `/vmc` admin actions and web admin access are decided by operator status.
- `admin_auth.mode: permission`: both `/vmc` admin actions and web admin access are decided by permission nodes.
- In `permission` mode, command checks use Bukkit permissions directly.
- In `permission` mode, web checks for offline users require Vault plus a Vault-compatible permissions plugin. Online users can still be resolved directly through Bukkit.

### 🧪 Build from Source

Most server owners can use the release files directly. Build from source only if you want to customize or self-package the project.

Build the frontend:

```bash
cd frontend/glassx
npm ci
npm run build
```

Optional frontend checks:

```bash
cd frontend/glassx
npm run type-check
npm run test
```

Build the main plugin:

```bash
cd plugin
mvn clean package
```

Build the proxy plugin:

```bash
cd plugin-proxy
mvn clean package
```

Build outputs:

- Main plugin jar: `plugin/target/verifymc-1.7.8.jar`
- Proxy plugin jar: `plugin-proxy/target/verifymc-proxy-1.7.8.jar`
- Frontend bundle: `frontend/glassx/dist/`

---

## 💬 Official Community

- **QQ Group**: 1041540576 ([Join](https://qm.qq.com/q/F7zuhZ7Mze))
- **Discord**: [https://discord.gg/TCn9v88V](https://discord.gg/TCn9v88V)

---

> ❤️ If you like this project, please Star, share, and give us feedback!
