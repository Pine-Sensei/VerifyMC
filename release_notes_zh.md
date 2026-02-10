[English](https://github.com/KiteMC/VerifyMC/releases/tag/v1.2.7) | 简体中文 | [官方文档](https://kitemc.com/docs/verifymc/)

# VerifyMC v1.2.7 更新日志

## 🐛 Bug 修复

### 问卷功能无法显示

- 修复了即使 `config.yml` 和 `questionnaire.yml` 中均启用问卷功能，注册页面仍不显示问卷表单的问题
- 将 `QuestionnaireForm` 组件集成到注册流程中，用户需先完成问卷再进行注册
- 在 `ConfigResponse` 类型定义中补充了 `questionnaire` 字段，与后端 API 返回数据对齐
