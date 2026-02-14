# 🛡️ VerifyMC

[简体中文](README_zh.md) | English | [📚 Official Documentation](https://kitemc.com/docs/VerifyMC/)

---

## 🚀 Introduction

**VerifyMC** is an ultra-lightweight, powerful whitelist management plugin for Minecraft servers. It supports web-based registration, auto/manual review, banning, theme switching, AuthMe integration, and high customizability, helping you secure and manage your server community with ease.

---

## 📝 Key Features

1. 🖥️ **Web Registration & Review**: Players can submit whitelist applications via a web page; admins can review, ban, and manage users online.
2. 🔒 **Auto/Manual Review**: Supports both automatic approval and manual admin review to fit different server needs.
3. 🚫 **Ban System**: Ban problematic players to keep your server safe.
4. 🎨 **GlassX Theme**: Beautiful glassmorphism design with smooth animations and modern UI.
5. 📨 **Email Verification & Domain Whitelist**: Integrated SMTP email verification, supports email domain whitelist and alias limit.
6. 🔐 **Self-hosted CAPTCHA**: Built-in graphical CAPTCHA (math/text) - no external services required.
7. 🎮 **Discord Integration**: OAuth2 Discord account linking with optional/required mode.
8. 📋 **Registration Questionnaire**: Customizable questionnaire system with multi-language support.
9. 📧 **User Notifications**: Automatic email notifications for whitelist approval/rejection.
10. 🌐 **Multi-language Support**: Both web UI and plugin messages support English and Chinese.
11. ⚙️ **Highly Customizable**: Set max accounts per email, player ID regex, whitelist bypass IPs, and more.
12. 🪶 **Lightweight**: Plugin jar is under 6MB, integrates multiple features, and runs efficiently.
13. 🔄 **Auto Update & Backup**: Config files auto-upgrade, with full backup before each update.
14. 🧩 **Flexible Whitelist Modes**: Supports Bukkit native whitelist sync, plugin self-management, and MySQL storage.
15. 💾 **MySQL & Data File Storage**: Easily switch between local file and MySQL storage; supports automatic migration.
16. 📝 **Audit Log Multi-Storage**: Audit logs can be stored in file or MySQL.
17. 🌍 **Custom Internationalization**: Auto-loads any messages_xx.properties file; users can add any language.
18. 🔐 **AuthMe Integration**: Seamless integration with AuthMe plugin for password management and auto-registration.
19. 🎮 **Bedrock Support**: Geyser/Floodgate player prefix support for cross-platform servers.
20. 🔗 **Proxy Support**: BungeeCord/Velocity proxy plugin for network-level whitelist enforcement.
21. 🤖 **LLM Essay Scoring**: AI-powered auto-scoring for text questionnaire answers via DeepSeek/Google, with circuit breaker and concurrency control.

---

## 🖼️ Screenshots (GlassX Theme)

### Home Page

![Home GlassX](docs/screenshot-home-glassx.png)

### Registration Page

![Registration GlassX](docs/screenshot-register-glassx.png)

### Admin Panel

![Admin GlassX](docs/screenshot-admin-glassx.png)

---

## 🛠️ Tech Stack

- Java (Bukkit/Spigot/Paper/Folia plugin)
- Frontend: Vue3 + Tailwind CSS (custom themes supported)
- WebSocket real-time communication
- Email service: SMTP

---

## 📦 Installation & Configuration

1. Download the latest `VerifyMC.jar` and place it in your server's `plugins` directory.
2. Start the server to auto-generate config files, then edit `config.yml` as needed (see full example below).
3. Restart the server and visit `http://your_server_ip:8080` to access the admin panel.

### ✅ Recommended Minimum Environment

- Java 17+
- Bukkit/Spigot/Paper/Folia 1.20+
- A public domain with HTTPS enabled (recommended for production)
- SMTP mailbox account (required when using `email` verification)

### ⚡ 5-Minute Quick Start

1. Set `auth_methods: [captcha]` in `config.yml` (fastest setup, no SMTP required).
2. Set `whitelist_mode: plugin` and `web_register_url: https://your-domain.com/`.
3. Configure `admin.password` to a strong password.
4. (Optional) Enable `register.auto_approve: true` for small private servers.
5. Restart the server and open `http://your_server_ip:8080`.

### 🧪 Build from Source

```bash
cd plugin
mvn clean package
```

Output jar: `plugin/target/verifymc-<version>.jar`

```yaml
# ----------------------------------------
# General Settings
# ----------------------------------------
# Global language setting. Affects plugin messages and web UI. (e.g., 'zh', 'en')
language: zh
# Enable detailed console logs for troubleshooting.
debug: false

# ----------------------------------------
# Web Server
# ----------------------------------------
# The port for the web interface.
web_port: 8080
# The server name displayed on the web interface.
web_server_prefix: '[ Name ]'

# ----------------------------------------
# Verification & Whitelist
# ----------------------------------------
# Supported authentication methods. Options: 'email' (email verification), 'captcha' (graphical captcha).
# You can use multiple methods, e.g.: [email, captcha]
#
# [IMPORTANT] Configuring the captcha: section below does NOT enable captcha!
# To enable captcha, you MUST add 'captcha' to this list:
#   Captcha only: auth_methods: [captcha]
#   Both email and captcha: auth_methods: [email, captcha]
auth_methods:
  - email
# Maximum number of game accounts that can be linked to a single email address.
max_accounts_per_email: 2
# Whitelist mode: 'bukkit' (syncs with server's whitelist.json) or 'plugin' (uses internal database).
whitelist_mode: plugin
# Registration URL displayed to non-whitelisted players when in 'plugin' mode.
web_register_url: https://domain.com/

# ----------------------------------------
# Registration
# ----------------------------------------
# If true, new user registrations are automatically approved. If false, they require manual admin approval.
register:
  auto_approve: false

# ----------------------------------------
# User & Security
# ----------------------------------------
# A regular expression to validate player names.
username_regex: "^[a-zA-Z0-9_-]{3,16}$"
# If false, usernames that only differ by case (e.g., "Player" and "player") are treated as the same.
username_case_sensitive: false
# A list of IP addresses that can join the server without being whitelisted.
whitelist_bypass_ips:
  - 127.0.0.1
# The password for accessing the admin panel on the web interface.
admin:
  password: your_custom_password

# ----------------------------------------
# User Notification
# ----------------------------------------
# Send email notification to users when admin reviews their application.
user_notification:
  # Whether to enable user notification feature
  enabled: true
  # Whether to send notification when application is approved
  on_approve: true
  # Whether to send notification when application is rejected
  on_reject: true

# ----------------------------------------
# Frontend (UI)
# ----------------------------------------
frontend:
  # The visual theme for the web interface. Options: 'glassx'.
  theme: glassx
  # URL for the logo. Can be a web link or a local file path (e.g., '/logo.png').
  logo_url: /logo.png
  # A message to display on the homepage.
  announcement: Welcome to [ Name ]!

# ----------------------------------------
# Email (SMTP)
# ----------------------------------------
smtp:
  host: smtp.qq.com
  port: 587
  username: your_email@qq.com
  password: your_email_password
  from: your_email@qq.com
  enable_ssl: true
# Subject (title) of the verification code email
email_subject: VerifyMC Verification Code

# ----------------------------------------
# Sync Settings (for bukkit mode)
# ----------------------------------------
# If true, automatically syncs changes from whitelist.json to the plugin's database.
whitelist_json_sync: true
# If true, automatically adds approved users to whitelist.json and removes banned/deleted users.
auto_sync_whitelist: true
# If 'bukkit' mode is disabled, this setting (if true) cleans players from whitelist.json.
auto_cleanup_whitelist: true

# ----------------------------------------
# Auto Update & Backup
# ----------------------------------------
# If true, automatically adds new settings to your config.yml on plugin updates.
auto_update_config: true
# If true, automatically updates the language files.
auto_update_i18n: true
# If true, automatically updates the email templates.
auto_update_email: true
# If true, automatically updates theme files.
auto_update_static: true
# If true, creates a full backup of the plugin data folder before any auto-updates.
backup_on_update: true

# ----------------------------------------
# Email Registration Restrictions
# ----------------------------------------
# Enable email domain whitelist
enable_email_domain_whitelist: true
# Enable email alias limit (e.g. forbid user+xxx@gmail.com)
enable_email_alias_limit: false
# Email domain whitelist. Leave empty to use default mainstream domains
email_domain_whitelist:
  - gmail.com
  - 163.com
  - 126.com
  - qq.com
  - outlook.com
  - hotmail.com
  - icloud.com
  - yahoo.com
  - foxmail.com

# ----------------------------------------
# Storage & Data Migration
# ----------------------------------------
storage:
  # Storage type, options: data (local file), mysql (external database)
  type: data
  # Whether to automatically migrate data from the original storage to the new storage when switching storage.type (e.g., data→mysql or mysql→data)
  auto_migrate_on_switch: false
  mysql:
    host: localhost
    port: 3306
    database: verifymc
    user: root
    password: yourpassword

# ----------------------------------------
# Authme Integration Configuration
# ----------------------------------------
authme:
  # Whether to enable Authme integration functionality
  enabled: true
  # Integration mode: command (console command) / database (direct DB operations)
  mode: command
  # Whether to require password input during web registration
  require_password: true
  # Password regex pattern
  password_regex: "^[a-zA-Z0-9_]{3,16}$"
  database:
    # DB type in database mode: mysql / sqlite
    type: sqlite
    table: authme
    # Stored password format when writing to AuthMe DB directly: sha256 / md5vb / plaintext
    password_format: sha256
    sync_interval_seconds: 30
    # AuthMe SHA256/MD5VB salt length (default 16)
    salt_length: 16
    mysql:
      host: 127.0.0.1
      port: 3306
      database: authme
      user: root
      password: yourpassword
    sqlite:
      path: plugins/AuthMe/authme.db
    columns:
      mySQLColumnId: id
      # Column for storing or checking players nickname
      mySQLColumnName: username
      # Column for storing or checking players RealName
      mySQLRealName: realname
      # Column for storing players passwords
      mySQLColumnPassword: password
      # Column for storing players passwords salts
      mySQLColumnSalt: ''
      # Column for storing players emails
      mySQLColumnEmail: email
      # Column for storing if a player is logged in or not
      mySQLColumnLogged: isLogged
      # Column for storing if a player has a valid session or not
      mySQLColumnHasSession: hasSession
      # Column for storing a player's TOTP key (for two-factor authentication)
      mySQLtotpKey: totp
      # Column for storing the player's last IP
      mySQLColumnIp: ip
      # Column for storing players lastlogins
      mySQLColumnLastLogin: lastlogin
      # Column storing the registration date
      mySQLColumnRegisterDate: regdate
      # Column for storing the IP address at the time of registration
      mySQLColumnRegisterIp: regip
      # Column for storing player LastLocation - X
      mySQLlastlocX: x
      # Column for storing player LastLocation - Y
      mySQLlastlocY: y
      # Column for storing player LastLocation - Z
      mySQLlastlocZ: z
      # Column for storing player LastLocation - World Name
      mySQLlastlocWorld: world
      # Column for storing player LastLocation - Yaw
      mySQLlastlocYaw: yaw
      # Column for storing player LastLocation - Pitch
      mySQLlastlocPitch: pitch
      # Column for storing players uuids (optional)
      mySQLPlayerUUID: ''

# ----------------------------------------
# Captcha Configuration
# ----------------------------------------
# Captcha can be used as an alternative or supplement to email verification
# Add 'captcha' to auth_methods to enable: auth_methods: [captcha]
# Or use both: auth_methods: [email, captcha]
captcha:
  # Captcha type: math (math expression) or text (random characters)
  type: math
  # Length of text captcha (ignored for math type)
  length: 4
  # Captcha expiration time in seconds
  expire_seconds: 300

# ----------------------------------------
# Bedrock Player Support
# ----------------------------------------
# For Geyser/Floodgate bedrock players
bedrock:
  # Whether to enable bedrock player support
  enabled: false
  # Prefix for bedrock players (commonly "." for Floodgate)
  prefix: "."
  # Regex for bedrock usernames
  username_regex: "^\\.[a-zA-Z0-9_\\s]{3,16}$"

# ----------------------------------------
# Questionnaire Configuration
# ----------------------------------------
# Detailed questions are configured in questionnaire.yml
questionnaire:
  # Whether to enable questionnaire feature
  enabled: false
  # Minimum score to pass
  pass_score: 60
  # When questionnaire is enabled, users must pass it before registration
  rate_limit:
    # Time window for rate limiting in milliseconds
    window_ms: 300000
    ip:
      # Maximum submissions allowed per IP within the time window
      max: 20
    uuid:
      # Maximum submissions allowed per Minecraft UUID within the time window
      max: 8
    email:
      # Maximum submissions allowed per email within the time window
      max: 6

# ----------------------------------------
# LLM Essay Scoring (for text questions)
# ----------------------------------------
llm:
  # Enable LLM auto scoring. When disabled, all text questions require manual review
  enabled: true
  # Scoring provider. Options: deepseek / google
  provider: deepseek
  # OpenAI-compatible API base URL, must use https
  api_base: https://api.deepseek.com/v1
  # API key
  api_key: ""
  # Model name
  model: deepseek-chat
  # Request timeout in milliseconds
  timeout: 10000
  # Retry count when parsing/network call fails
  retry: 1
  # Maximum concurrent LLM requests (protects API and server load)
  max_concurrency: 4
  # Max wait time to acquire a concurrency slot in milliseconds
  acquire_timeout: 1500
  # Base backoff delay for retries in milliseconds
  retry_backoff_base: 300
  # Maximum backoff delay for retries in milliseconds
  retry_backoff_max: 5000
  # Maximum input length sent to the model in characters
  input_max_length: 2000
  circuit_breaker:
    # Threshold to open circuit breaker (consecutive failures)
    failure_threshold: 5
    # How long the circuit remains open in milliseconds
    open_ms: 30000
  # Fixed system prompt (combined with question, answer and scoring rule)
  system_prompt: |
    You are a fair Minecraft whitelist questionnaire grader.
    Score strictly based on the question, candidate answer, and scoring rule.
    Return JSON only.
  # Scoring rule text appended into the user prompt
  scoring_rule: |
    Evaluate primarily:
    1) Relevance to the question
    2) Completeness and level of detail
    3) Understanding of server rules and community norms
  # Expected model output schema (JSON)
  score_format: '{"score": number, "reason": string, "confidence": number}'

# ----------------------------------------
# Discord Integration (OAuth2)
# ----------------------------------------
# Requires creating a Discord application at https://discord.com/developers/applications
discord:
  # Whether to enable Discord integration
  enabled: false
  # Discord application client ID
  client_id: ""
  # Discord application client secret
  client_secret: ""
  # OAuth2 redirect URI
  redirect_uri: "https://yourdomain.com/api/discord/callback"
  # Optional: require users to be in specific guild/server
  guild_id: ""
  # Whether Discord linking is required for registration
  required: false

```

---

## 💬 Official Community

- **QQ Group**: 1041540576 ([Join](https://qm.qq.com/q/F7zuhZ7Mze))
- **Discord**: [https://discord.gg/TCn9v88V](https://discord.gg/TCn9v88V)

---

> ❤️ If you like this project, please Star, share, and give us feedback!
