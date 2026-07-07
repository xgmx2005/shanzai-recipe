# 膳哉前端界面设计交接文档

本文档给前端开发同学使用，目标是让页面实现有统一视觉方向、明确路由结构和可落地的组件拆分。配套设计图见：[docs/ui/shanzai-ui-design-board.svg](ui/shanzai-ui-design-board.svg)。

## 1. 设计定位

膳哉不是普通后台系统，也不是纯聊天式 AI 工具。前端应呈现为“健康饮食决策工具”：用户能快速完成“填档案 -> 输食材 -> 得推荐 -> 看菜谱 -> 生成购物清单”的闭环；维护员能高效维护菜谱和食材数据。

视觉关键词：

- 清爽健康：浅米白背景、草本绿主色、番茄红强调色、少量深青蓝用于数据和管理端。
- 产品感：用户端要有真实菜谱图片、营养标签和清晰行动入口。
- 工具感：维护端避免花哨，优先表格密度、筛选、编辑抽屉和统计摘要。
- 不做营销页：登录后直接进入可操作页面，首页服务于推荐流程。

## 2. 设计图说明

设计总览图包含 7 个关键页面：

1. 登录/注册页
2. 用户首页
3. 健康档案页
4. 智能推荐输入页
5. 推荐结果页
6. 菜谱详情 + 购物清单生成
7. 维护端看板 + 菜谱管理

打开方式：

- 直接用浏览器打开 `docs/ui/shanzai-ui-design-board.svg`
- 或在 Markdown 预览里查看本文档中的相对链接

## 3. 路由与页面结构

| 路由 | 页面 | 角色 | 主要目标 |
|---|---|---|---|
| `/login` | 登录/注册页 | 公开 | 账号登录，按角色跳转 |
| `/user/home` | 用户首页 | USER | 展示档案摘要、快捷入口、最近推荐 |
| `/user/profile` | 健康档案 | USER | 维护身高体重、目标、偏好、忌口 |
| `/user/recommend` | 智能推荐 | USER | 输入已有食材、排除食材、烹饪条件 |
| `/user/recommend/result` | 推荐结果 | USER | 展示 AI 摘要、菜谱卡片、评分和营养 |
| `/user/recipes/:id` | 菜谱详情 | USER | 查看步骤、营养、收藏、生成购物清单 |
| `/user/shopping-lists` | 购物清单 | USER | 分组查看和勾选待买食材 |
| `/user/favorites` | 收藏菜谱 | USER | 查看收藏并取消收藏 |
| `/user/history` | 推荐历史 | USER | 回看输入条件和推荐结果 |
| `/admin/dashboard` | 维护端看板 | MAINTAINER | 数据统计、热门菜谱、目标分布 |
| `/admin/recipes` | 菜谱管理 | MAINTAINER | 搜索、新增、编辑、下架菜谱 |
| `/admin/ingredients` | 食材管理 | MAINTAINER | 搜索、分类筛选、维护营养数据 |

## 4. 布局规范

### 4.1 用户端布局

用户端使用顶部导航 + 内容区布局：

- 顶栏左侧：品牌「膳哉」
- 顶栏中部：首页、健康档案、智能推荐、购物清单、收藏、历史
- 顶栏右侧：当前用户、退出按钮
- 页面内容最大宽度：`1180px`
- 页面内不使用大面积嵌套卡片。页面区块直接铺在内容区，卡片只用于菜谱、统计项、表单分组。

### 4.2 维护端布局

维护端使用左侧菜单 + 顶部工具条：

- 左侧宽度：`220px`
- 菜单项：统计看板、菜谱管理、食材管理
- 内容区优先表格和筛选栏，少用装饰
- 新增/编辑使用右侧抽屉，不跳页

### 4.3 移动端适配

第一版优先桌面演示，但页面不能在手机宽度乱版：

- `<= 768px` 时用户端导航折叠菜单或底部导航
- 表单从双列改为单列
- 菜谱卡片从三列改为一列
- 维护端表格横向滚动，抽屉宽度改为 `100vw`

## 5. 视觉系统

### 5.1 色彩变量

建议在 `frontend/src/styles/theme.css` 中定义：

```css
:root {
  --sz-bg: #f7f4ec;
  --sz-surface: #fffdf7;
  --sz-surface-muted: #efe7d7;
  --sz-ink: #1f2a24;
  --sz-text-muted: #66736a;
  --sz-green: #2f6f4e;
  --sz-green-soft: #dbe8d4;
  --sz-tomato: #d85b3a;
  --sz-blue: #376c8a;
  --sz-line: #ded6c7;
  --sz-warning: #c98a2c;
  --sz-radius: 8px;
}
```

使用原则：

- 主按钮用 `--sz-green`
- 危险或强调行动用 `--sz-tomato`
- 管理端图表和表格强调用 `--sz-blue`
- 卡片边框用浅线条，不使用厚重阴影

### 5.2 字体与排版

字体直接使用系统中文字体，避免额外下载依赖：

```css
font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", sans-serif;
```

字号建议：

| 用途 | 字号 | 字重 |
|---|---:|---:|
| 页面标题 | 28px | 700 |
| 区块标题 | 18px | 700 |
| 正文 | 14px | 400 |
| 辅助说明 | 12px | 400 |
| 数据数字 | 24px | 700 |

不要使用随视口缩放的字号。移动端通过布局换行解决，不靠 `vw` 缩小文字。

### 5.3 组件圆角与密度

- 卡片圆角：`8px`
- 按钮圆角：`8px`
- 输入框圆角：`8px`
- 表格行高：维护端 `44px` 左右
- 页面区块间距：`24px`
- 表单项间距：`16px`

## 6. 组件拆分建议

| 组件 | 路径建议 | 说明 |
|---|---|---|
| `UserLayout.vue` | `frontend/src/layouts/UserLayout.vue` | 用户端顶部导航和内容容器 |
| `AdminLayout.vue` | `frontend/src/layouts/AdminLayout.vue` | 维护端侧边栏和顶部工具条 |
| `RecipeCard.vue` | `frontend/src/components/RecipeCard.vue` | 推荐结果、收藏、首页复用 |
| `NutritionBar.vue` | `frontend/src/components/NutritionBar.vue` | 热量、蛋白质、脂肪、碳水展示 |
| `IngredientTagInput.vue` | `frontend/src/components/IngredientTagInput.vue` | 食材输入和忌口输入复用 |
| `GoalSegment.vue` | `frontend/src/components/GoalSegment.vue` | FAT_LOSS/BALANCED/MUSCLE_GAIN 选择 |
| `RecipeEditorDrawer.vue` | `frontend/src/components/RecipeEditorDrawer.vue` | 菜谱新增编辑抽屉 |
| `IngredientEditorDrawer.vue` | `frontend/src/components/IngredientEditorDrawer.vue` | 食材新增编辑抽屉 |

Naive UI 对应：

- 表单：`n-form`, `n-form-item`, `n-input-number`, `n-select`, `n-dynamic-tags`
- 目标切换：`n-radio-group` 或自定义分段按钮
- 标签：`n-tag`
- 表格：`n-data-table`
- 抽屉：`n-drawer`
- 反馈：`n-message`, `n-spin`, `n-empty`, `n-popconfirm`

## 7. 页面级设计

### 7.1 登录/注册页

目标：让用户明确这是健康菜谱产品，并能快速登录演示账号。

结构：

- 左侧品牌区：产品名、正式副标题、三步流程
- 右侧登录表单：账号、密码、登录按钮、注册切换
- 下方演示账号提示：`user1 / 123456`、`maintainer / 123456`

交互：

- 登录成功后调用 `/api/auth/me` 或读取登录返回角色
- `USER` 跳转 `/user/home`
- `MAINTAINER` 跳转 `/admin/dashboard`

### 7.2 用户首页

目标：告诉用户当前健康状态，并引导开始推荐。

内容：

- 欢迎语和当前饮食目标
- BMI、每日目标热量、偏好标签
- 快捷入口：完善档案、开始推荐、购物清单、收藏
- 最近推荐记录 2 到 3 条

空状态：

- 没有档案时展示“先完善健康档案”的主按钮
- 没有推荐历史时展示“输入食材生成第一份推荐”

### 7.3 健康档案页

字段：

- 性别、年龄、身高、体重
- 饮食目标：减脂控热量、日常健康、健身增肌
- 口味偏好：清淡、低脂、高蛋白、家常、快手等
- 忌口食材、过敏食材
- 期望烹饪时间

保存后：

- 显示 BMI 和状态：偏瘦、正常、超重、肥胖
- 显示系统估算每日热量目标

### 7.4 智能推荐页

目标：降低用户输入成本。

表单：

- 已有食材：标签输入，提供常用食材快捷选项
- 不想吃/排除食材：标签输入
- 饮食目标：默认读取健康档案，也允许本次覆盖
- 烹饪时间：15/25/40/60 分钟
- 人数：1 到 4 人

提交：

- 调用 `POST /api/recommendations`
- loading 文案使用“正在匹配菜谱和营养目标”
- 成功跳转 `/user/recommend/result`

### 7.5 推荐结果页

内容：

- 顶部 AI 摘要：推荐逻辑、健康提示、购物提示
- 推荐卡片：图片、名称、评分、热量、蛋白质、匹配食材、推荐理由
- 卡片操作：查看详情、收藏、生成购物清单

排序：

- 综合评分最高的菜谱置顶
- 清楚展示“已有食材命中”和“需要补买”

### 7.6 菜谱详情与购物清单

菜谱详情：

- 大图、名称、目标标签、烹饪时间
- 营养摘要
- 食材清单
- 步骤
- 收藏按钮和生成购物清单按钮

购物清单：

- 按分类分组：肉蛋奶、蔬菜、主食、调味品
- 已有食材不进入待买列表
- 支持勾选已购买

### 7.7 收藏和历史

收藏页：

- 复用 `RecipeCard`
- 支持取消收藏

历史页：

- 左侧显示输入快照：已有食材、排除食材、目标
- 右侧显示当次推荐结果
- 支持点击回到菜谱详情

### 7.8 维护端看板

内容：

- 用户数量、菜谱数量、食材数量、推荐次数
- 热门菜谱排行
- 饮食目标分布
- 最近维护提示

注意：

- 维护端不做复杂用户管理
- 统计图第一版可用简单条形图和环形比例，不必引入大型图表库

### 7.9 菜谱/食材管理

菜谱管理：

- 搜索：菜名、标签、适合目标
- 表格列：菜名、目标、时间、热量、状态、更新时间、操作
- 编辑抽屉：基础信息、营养数据、食材行、步骤、图片路径

食材管理：

- 搜索：名称、别名
- 分类筛选：肉蛋奶、蔬菜、主食、调味品、其他
- 表格列：名称、分类、单位、热量、蛋白质、脂肪、碳水、操作

## 8. API 对接顺序

前端可以先用 mock 数据完成页面，再逐步接真实接口。

建议顺序：

1. 登录和角色跳转：`POST /api/auth/login`, `GET /api/auth/me`
2. 健康档案：首页和档案页：`GET /api/profile`, `PUT /api/profile`, `GET /api/profile/summary`
3. 菜谱基础：`GET /api/recipes`, `GET /api/recipes/{id}`
4. 推荐：`POST /api/recommendations`, `GET /api/recommendations/history`
5. 收藏与购物清单：`GET /api/favorites`, `POST /api/shopping-lists`
6. 维护端：`/api/admin/**`

## 9. 状态处理

每个页面至少实现这些状态：

- loading：骨架屏或 `n-spin`
- empty：明确下一步行动，例如“去生成推荐”
- error：显示错误消息并保留用户输入
- success：保存成功、收藏成功、购物清单生成成功

HTTP 401：

- 清空 token
- 跳转 `/login`
- 提示“登录已过期，请重新登录”

HTTP 403：

- 显示“当前账号无权访问该页面”
- 普通用户不要进入维护端

## 10. 前端实现优先级

第一批必须完成：

1. 登录页和路由守卫
2. 用户首页
3. 健康档案页
4. 智能推荐输入页
5. 推荐结果页
6. 菜谱详情页
7. 购物清单页
8. 维护端看板、菜谱管理、食材管理

第二批补充：

- 收藏页
- 历史页
- 移动端细节
- 视觉 polish 和图片替换

## 11. 交付检查清单

前端组员每完成一个页面，应检查：

- 页面是否符合对应路由和角色权限
- 是否使用统一颜色变量和 8px 圆角
- 文字是否在 375px 手机宽度下不溢出
- 表单是否有 loading 和错误提示
- 空状态是否给出下一步按钮
- 菜谱卡片是否显示图片、热量、蛋白质和操作按钮
- 维护端表格是否有搜索、筛选和编辑入口

