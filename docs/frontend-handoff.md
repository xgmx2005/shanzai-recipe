# 给前端同学的对接说明

请从 `main` 拉最新代码，然后新建分支：

```powershell
git pull origin main
git checkout -b feature/frontend-ui
```

先看这两个文件：

- `docs/frontend-ui-design.md`
- `docs/ui/shanzai-modern-light-imagegen-board.png`

第一批先做：

1. Vue 前端脚手架和主题变量。
2. `UserLayout` 和 `AdminLayout`。
3. 登录页。
4. 用户首页。
5. 健康档案页。
6. 智能推荐页。

第一批可以全部使用 mock 数据，不用等后端接口齐全。

视觉要求：

- 整体按“现代轻健康”做。
- 背景用奶油白，不要冷白。
- 主按钮用鲜蔬绿胶囊按钮。
- 用户端必须有真实或拟真的菜谱图片。
- 维护端保持深叶绿侧栏和高密度表格。
- 不要改成普通后台模板风格。

提交信息请使用中文，例如：

```text
feat: 搭建前端基础布局和主题变量
feat: 实现登录页和用户首页
feat: 实现健康档案和智能推荐页面
```

第一批完成后先发页面截图，对照设计稿确认风格，再继续做推荐结果、菜谱详情、购物清单、收藏历史和维护端页面。
