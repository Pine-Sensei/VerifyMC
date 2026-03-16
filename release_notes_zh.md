[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.7.5) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.7.5 更新日志

## 版本管理优化

- 主插件、代理的 `plugin.yml`/`bungee.yml` 改为 `${project.version}` 注入，Velocity 插件通过生成的 `BuildConstants.VERSION` 获取版本，彻底移除硬编码字符串。
- GitHub Actions 构建前会运行同步脚本，确保生成的 jar、发布说明和 README 中的构建产物示例都与当前版本一致。

## 工具链稳定

- Maven 分别过滤 `plugin.yml`/`bungee.yml`，其他资源仍保持原样，避免在过滤过程里错改其他配置。
- `maven-resources-plugin` + `build-helper-maven-plugin` 在代理模块生成版本常量源，Velocity 项目编译时直接引用，免去手动复制。
- `mvn -q -DskipTests compile`（主插件和代理）已通过，确保新版本注入流程对构建链没有回退。
