# Git 协作规范

## 分支约定

- `main`：稳定版本，只放已经确认可运行或可交付的内容。
- `feature/backend`：后端、数据库、AI 接口相关开发。
- `feature/frontend`：前端页面、交互、静态资源相关开发。

## 提交流程

1. 开发前先拉取最新代码。
2. 在自己的功能分支完成开发。
3. 每完成一个清晰的小功能提交一次。
4. 合并到 `main` 前确认项目可以运行，关键流程没有明显错误。

## 提交信息

提交信息统一使用中文，格式建议为：

```text
类型: 具体动作
```

常用类型：

- `feat`：新增功能。
- `fix`：修复问题。
- `docs`：文档修改。
- `style`：界面样式或格式调整。
- `refactor`：代码重构，不改变功能。
- `test`：测试或验证相关。
- `chore`：配置、依赖、工程化杂项。

示例：

```text
feat: 添加用户登录接口
feat: 实现健康档案页面
fix: 修复推荐结果为空时页面报错
docs: 更新数据库设计说明
chore: 配置前端开发代理
```

## 身份约定

本仓库提交身份使用 GitHub 账号对应的本地配置：

```powershell
git config --local user.name xgmx2005
git config --local user.email xgmx2005@users.noreply.github.com
```
