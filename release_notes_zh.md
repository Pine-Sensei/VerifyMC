[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.2.2) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.2.2 更新日志

## 多语言支持架构重构
- 完全重构国际化系统，支持无限语言扩展
- 在 config.yml 中可设置任意语言代码
- 自动为新语言创建配置文件(基于英文模板)

## 命令提示优化
- `/vmc reload` 命令现在会显示灰色提示信息，说明该命令仅能重载部分配置
- 重载相关消息(启动中、成功、失败)改为英文硬编码，提升跨语言一致性
- 主题切换提示改为英文硬编码，确保所有用户都能理解关键操作提示