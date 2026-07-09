# 膳哉图片与图标接入规范

## 1. 目标

本规范用于统一膳哉项目中的菜谱成品图与食材图标接入方式，保证：

- 演示时图片稳定可显示
- 前端页面风格统一
- 单人维护成本可控
- 后续替换素材时不需要改业务代码

本次短学期项目采用：

- 菜谱图：真实照片
- 食材图：SVG 图标
- AI 生图：不进入主流程

## 2. 总体策略

### 2.1 菜谱图片策略

采用混合模式：

- 核心展示菜谱：使用本地真实照片
- 次要菜谱或测试数据：允许保留外部 URL
- 图片缺失时：使用统一默认图兜底

### 2.2 食材图标策略

- 食材统一使用 SVG 图标
- 不建设食材照片库
- 未命中映射表时使用默认图标

## 3. 目录规范

### 3.1 菜谱图片目录

```text
frontend/public/images/recipes/
```

建议至少准备：

- `default-recipe.svg`
- `chicken-broccoli-bowl.jpg`
- `tomato-braised-beef.jpg`
- `shrimp-tofu-soup.jpg`
- `tofu-vegetable-stir-fry.jpg`
- `egg-spinach-salad.jpg`
- `salmon-quinoa-salad.jpg`
- `beef-broccoli-quinoa.jpg`
- `chicken-avocado-salad.jpg`
- `chicken-corn-bowl.jpg`
- `tomato-egg-tofu-soup.jpg`

### 3.2 食材图标目录

```text
frontend/public/images/ingredients/
```

建议至少准备：

- `ingredient-default.svg`
- `chicken-breast.svg`
- `beef.svg`
- `shrimp.svg`
- `egg.svg`
- `tofu.svg`
- `broccoli.svg`
- `tomato.svg`
- `spinach.svg`
- `bell-pepper.svg`
- `corn.svg`
- `avocado.svg`
- `quinoa.svg`
- `onion.svg`
- `carrot.svg`
- `mushroom.svg`
- `lettuce.svg`

## 4. 命名规范

### 4.1 菜谱图片命名

规则：

- 全小写
- 使用英文语义名
- 单词之间用 `-`
- 后缀统一 `.jpg`

示例：

- `chicken-broccoli-bowl.jpg`
- `tomato-braised-beef.jpg`
- `shrimp-tofu-soup.jpg`

### 4.2 食材图标命名

规则：

- 全小写
- 使用英文语义名
- 单词之间用 `-`
- 后缀统一 `.svg`

示例：

- `chicken-breast.svg`
- `bell-pepper.svg`
- `ingredient-default.svg`

## 5. 数据层规则

### 5.1 数据库字段

当前继续沿用 `recipe.image_url` 字段，不额外新增字段也可以完成本次交付。

推荐写法：

- 本地图片：`/images/recipes/chicken-broccoli-bowl.jpg`
- 外部图片：`https://...`

### 5.2 后端返回规则

后端直接返回 `imageUrl`，不额外拼装图片地址。

约定：

- 若 `imageUrl` 是以 `http://` 或 `https://` 开头，则前端直接使用
- 若 `imageUrl` 是以 `/images/recipes/` 开头，则前端按本地静态资源处理

## 6. 前端展示规则

### 6.1 菜谱图优先级

前端展示优先级统一为：

1. `imageUrl` 有值且能正常加载：直接显示
2. `imageUrl` 指向本地菜谱图：显示本地图片
3. 加载失败：回退到 `default-recipe.jpg`

### 6.2 食材图标优先级

前端展示优先级统一为：

1. 食材名命中映射表：显示对应 SVG 图标
2. 未命中：显示 `ingredient-default.svg`

### 6.3 不建议的做法

本次项目不建议：

- 运行时联网自动找图
- 运行时 AI 生图
- 一道菜动态抓取外部图片后直接展示

原因：

- 不稳定
- 依赖网络环境
- 可能出现失效链接
- 图片风格难统一
- 答辩风险高

## 7. 现有代码接入点

当前菜谱图主要通过以下位置解析：

- `frontend/src/api/http.ts`
- `frontend/src/components/RecipeCard.vue`
- `frontend/src/views/user/HomeView.vue`
- `frontend/src/views/user/RecommendView.vue`
- `frontend/src/views/user/RecipeDetailView.vue`
- `frontend/src/views/user/FavoritesView.vue`
- `frontend/src/views/user/RecommendationHistoryView.vue`

其中 `frontend/src/api/http.ts` 里的 `backendAssetUrl` 负责处理图片地址。

后续接入目标：

- `/images/recipes/*.jpg` 优先返回本地静态图片
- 图片失败时不再随机外链，而是统一回退到固定默认图

## 8. 食材图标映射建议

前端建议维护一份固定映射表，例如：

```ts
const ingredientIconMap: Record<string, string> = {
  鸡胸肉: '/images/ingredients/chicken-breast.svg',
  牛肉: '/images/ingredients/beef.svg',
  虾仁: '/images/ingredients/shrimp.svg',
  鸡蛋: '/images/ingredients/egg.svg',
  豆腐: '/images/ingredients/tofu.svg',
  西兰花: '/images/ingredients/broccoli.svg',
  番茄: '/images/ingredients/tomato.svg',
}
```

未命中时统一使用：

```ts
'/images/ingredients/ingredient-default.svg'
```

## 9. 优先落地顺序

单人开发时建议按以下顺序执行：

1. 先准备 `default-recipe.jpg` 和 `ingredient-default.svg`
2. 再补 8 到 12 道核心菜谱本地图
3. 再补 16 个核心食材 SVG 图标
4. 最后替换外部测试图和长尾数据

## 10. 本次交付最低标准

答辩前至少保证：

- 首页、推荐结果、详情页使用真实菜谱图
- 购物清单和详情页中的核心食材有统一图标
- 所有缺图场景都有默认图兜底
- 不出现图片加载失败的破图状态
