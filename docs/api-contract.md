# 膳哉 API 对接文档

本文档面向前端开发对接，描述当前后端已经实现的 REST API。

## 1. 通用约定

### 1.1 基础信息

- Base URL: `http://localhost:8081`
- API 前缀: `/api`
- Content-Type: `application/json`
- 时间字段格式: ISO-8601 字符串，例如 `2026-07-07T22:54:50`

### 1.2 鉴权

除注册、登录外，接口都需要请求头：

```http
Authorization: Bearer <token>
```

维护端接口 `/api/admin/**` 需要用户角色为 `MAINTAINER`。

### 1.3 统一响应

成功响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

失败响应：

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

### 1.4 枚举

饮食目标 `dietGoal`:

- `FAT_LOSS`: 减脂控热量
- `BALANCED`: 日常均衡
- `MUSCLE_GAIN`: 健身增肌

用户角色 `role`:

- `USER`
- `MAINTAINER`

## 2. 认证接口

### 2.1 注册

`POST /api/auth/register`

请求：

```json
{
  "username": "user2",
  "password": "123456",
  "nickname": "轻食用户"
}
```

响应 `data`:

```json
{
  "token": "jwt-token",
  "userId": 2,
  "username": "user2",
  "nickname": "轻食用户",
  "role": "USER"
}
```

### 2.2 登录

`POST /api/auth/login`

请求：

```json
{
  "username": "user1",
  "password": "123456"
}
```

响应 `data`:

```json
{
  "token": "jwt-token",
  "userId": 1,
  "username": "user1",
  "nickname": "健康用户",
  "role": "USER"
}
```

### 2.3 当前用户

`GET /api/auth/me`

响应 `data`:

```json
{
  "userId": 1,
  "username": "user1",
  "nickname": "健康用户",
  "role": "USER"
}
```

## 3. 健康档案接口

### 3.1 获取健康档案

`GET /api/profile`

响应 `data`:

```json
{
  "id": 1,
  "userId": 1,
  "gender": "女",
  "age": 21,
  "heightCm": 165.00,
  "weightKg": 58.00,
  "bmi": 21.30,
  "dietGoal": "FAT_LOSS",
  "tastePreferences": ["清淡", "低脂"],
  "avoidIngredients": ["花生"],
  "allergyIngredients": [],
  "cookingTimePreference": 30,
  "dailyCalorieTarget": 1800,
  "updatedAt": "2026-07-07T22:54:50"
}
```

### 3.2 保存健康档案

`PUT /api/profile`

请求：

```json
{
  "gender": "女",
  "age": 21,
  "heightCm": 165.00,
  "weightKg": 58.00,
  "dietGoal": "FAT_LOSS",
  "tastePreferences": ["清淡", "低脂"],
  "avoidIngredients": ["花生"],
  "allergyIngredients": [],
  "cookingTimePreference": 30
}
```

响应 `data`: 同 `GET /api/profile`。

### 3.3 健康档案摘要

`GET /api/profile/summary`

响应 `data`:

```json
{
  "hasProfile": true,
  "dietGoal": "FAT_LOSS",
  "bmi": 21.30,
  "bmiStatus": "正常",
  "dailyCalorieTarget": 1800,
  "cookingTimePreference": 30
}
```

## 4. 菜谱接口

### 4.1 菜谱列表

`GET /api/recipes`

查询参数：

- `keyword`: 可选，按名称或描述搜索
- `dietGoal`: 可选，`FAT_LOSS` / `BALANCED` / `MUSCLE_GAIN`
- `tag`: 可选，按口味或健康标签筛选

示例：`GET /api/recipes?dietGoal=FAT_LOSS&tag=低脂`

响应 `data`:

```json
[
  {
    "id": 1,
    "name": "鸡胸肉西兰花轻食碗",
    "description": "高蛋白低脂轻食",
    "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
    "cookingTime": 25,
    "difficulty": "EASY",
    "servings": 1,
    "calories": 420,
    "protein": 35.00,
    "fat": 9.00,
    "carbs": 45.00,
    "tasteTags": ["清淡"],
    "healthTags": ["低脂", "高蛋白"],
    "targetGoals": ["FAT_LOSS", "MUSCLE_GAIN"],
    "status": 1
  }
]
```

### 4.2 菜谱详情

`GET /api/recipes/{id}`

响应 `data`:

```json
{
  "id": 1,
  "name": "鸡胸肉西兰花轻食碗",
  "description": "高蛋白低脂轻食",
  "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
  "cookingTime": 25,
  "difficulty": "EASY",
  "servings": 1,
  "calories": 420,
  "protein": 35.00,
  "fat": 9.00,
  "carbs": 45.00,
  "tasteTags": ["清淡"],
  "healthTags": ["低脂", "高蛋白"],
  "targetGoals": ["FAT_LOSS", "MUSCLE_GAIN"],
  "steps": ["准备食材", "煎熟鸡胸肉", "搭配蔬菜装盘"],
  "ingredients": [
    {
      "ingredientId": 1,
      "name": "鸡胸肉",
      "category": "肉蛋奶",
      "quantity": 200.00,
      "unit": "g",
      "core": true
    }
  ],
  "status": 1,
  "createdBy": 2,
  "createdAt": "2026-07-07T22:54:50",
  "updatedAt": "2026-07-07T22:54:50"
}
```

## 5. 推荐接口

### 5.1 生成推荐

`POST /api/recommendations`

请求：

```json
{
  "availableIngredients": ["鸡胸肉", "西兰花", "鸡蛋"],
  "excludedIngredients": ["花生"],
  "dietGoal": "FAT_LOSS",
  "cookingTime": 30,
  "servings": 1
}
```

响应 `data`:

```json
{
  "historyId": 5,
  "aiSummary": "本次优先推荐鸡胸肉西兰花轻食碗，结合鸡胸肉、西兰花，更适合减脂控热量场景。",
  "aiHealthTip": "建议控制额外油脂摄入，保留高蛋白食材，并搭配蔬菜增强饱腹感。",
  "aiShoppingTip": "生成购物清单时会自动排除你已有的鸡胸肉、西兰花，只补充菜谱中缺少的食材。",
  "aiGenerated": true,
  "recipes": [
    {
      "id": 1,
      "name": "鸡胸肉西兰花轻食碗",
      "score": 92,
      "reason": "匹配鸡胸肉和西兰花，蛋白质充足，适合减脂。",
      "calories": 420,
      "protein": 35.00,
      "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg"
    }
  ]
}
```

### 5.2 推荐历史列表

`GET /api/recommendations/history`

响应 `data`:

```json
[
  {
    "id": 5,
    "inputIngredients": ["鸡胸肉", "西兰花"],
    "excludedIngredients": ["花生"],
    "dietGoal": "FAT_LOSS",
    "cookingTime": 30,
    "servings": 1,
    "resultRecipeIds": [1, 3, 6],
    "aiSummary": "本次优先推荐鸡胸肉西兰花轻食碗，结合鸡胸肉、西兰花，更适合减脂控热量场景。",
    "aiHealthTip": "建议控制额外油脂摄入，保留高蛋白食材，并搭配蔬菜增强饱腹感。",
    "aiShoppingTip": "生成购物清单时会自动排除你已有的鸡胸肉、西兰花，只补充菜谱中缺少的食材。",
    "aiGenerated": true,
    "createdAt": "2026-07-07T22:54:50"
  }
]
```

### 5.3 推荐历史详情

`GET /api/recommendations/history/{id}`

响应 `data`:

```json
{
  "id": 5,
  "inputIngredients": ["鸡胸肉", "西兰花"],
  "excludedIngredients": ["花生"],
  "dietGoal": "FAT_LOSS",
  "cookingTime": 30,
  "servings": 1,
  "resultRecipeIds": [1, 3, 6],
  "aiSummary": "本次优先推荐鸡胸肉西兰花轻食碗，结合鸡胸肉、西兰花，更适合减脂控热量场景。",
  "aiHealthTip": "建议控制额外油脂摄入，保留高蛋白食材，并搭配蔬菜增强饱腹感。",
  "aiShoppingTip": "生成购物清单时会自动排除你已有的鸡胸肉、西兰花，只补充菜谱中缺少的食材。",
  "aiGenerated": true,
  "recipes": [
    {
      "id": 1,
      "name": "鸡胸肉西兰花轻食碗",
      "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
      "calories": 420,
      "protein": 35.00
    }
  ],
  "createdAt": "2026-07-07T22:54:50"
}
```

## 6. 购物清单接口

### 6.1 创建购物清单

`POST /api/shopping-lists`

请求：

```json
{
  "recipeIds": [1, 3],
  "availableIngredients": ["鸡胸肉"],
  "title": "本周轻食采购"
}
```

响应 `data`:

```json
{
  "id": 10,
  "title": "本周轻食采购",
  "sourceRecipeIds": [1, 3],
  "status": "ACTIVE",
  "items": [
    {
      "id": 20,
      "ingredientId": 9,
      "ingredientName": "西兰花",
      "category": "蔬菜",
      "quantity": 150.00,
      "unit": "g",
      "checked": false
    }
  ],
  "createdAt": "2026-07-07T22:54:50",
  "updatedAt": "2026-07-07T22:54:50"
}
```

说明：

- `availableIngredients` 中已有的食材不会进入采购清单。
- 多个菜谱中同名且单位相同的食材会合并数量。

### 6.2 购物清单列表

`GET /api/shopping-lists`

响应 `data`:

```json
[
  {
    "id": 10,
    "title": "本周轻食采购",
    "sourceRecipeIds": [1, 3],
    "status": "ACTIVE",
    "itemCount": 4,
    "checkedCount": 1,
    "createdAt": "2026-07-07T22:54:50"
  }
]
```

### 6.3 购物清单详情

`GET /api/shopping-lists/{id}`

响应 `data`: 同创建购物清单响应。

### 6.4 勾选购物清单项

`PATCH /api/shopping-lists/{listId}/items/{itemId}`

请求：

```json
{
  "checked": true
}
```

响应 `data`:

```json
{
  "id": 20,
  "ingredientId": 9,
  "ingredientName": "西兰花",
  "category": "蔬菜",
  "quantity": 150.00,
  "unit": "g",
  "checked": true
}
```

### 6.5 删除购物清单

`DELETE /api/shopping-lists/{id}`

响应 `data`: `null`

## 7. 收藏接口

### 7.1 收藏菜谱

`POST /api/recipes/{id}/favorite`

响应 `data`:

```json
{
  "favoriteId": 8,
  "recipeId": 1,
  "recipeName": "鸡胸肉西兰花轻食碗",
  "description": "高蛋白低脂轻食",
  "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
  "calories": 420,
  "protein": 35.00,
  "createdAt": "2026-07-07T22:54:50"
}
```

说明：重复收藏同一个菜谱会直接返回已有收藏，不会重复插入。

### 7.2 取消收藏

`DELETE /api/recipes/{id}/favorite`

响应 `data`: `null`

### 7.3 收藏列表

`GET /api/favorites`

响应 `data`:

```json
[
  {
    "favoriteId": 8,
    "recipeId": 1,
    "recipeName": "鸡胸肉西兰花轻食碗",
    "description": "高蛋白低脂轻食",
    "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
    "calories": 420,
    "protein": 35.00,
    "createdAt": "2026-07-07T22:54:50"
  }
]
```

## 8. 维护端接口

维护端接口需要 `MAINTAINER` token。

### 8.1 维护端 Dashboard

`GET /api/admin/dashboard`

响应 `data`:

```json
{
  "userCount": 12,
  "recipeCount": 18,
  "ingredientCount": 36,
  "recommendationCount": 27
}
```

### 8.2 热门菜谱统计

`GET /api/admin/stats/popular-recipes`

查询参数：

- `limit`: 可选，默认 `10`

响应 `data`:

```json
[
  {
    "recipeId": 1,
    "recipeName": "鸡胸肉西兰花轻食碗",
    "recommendationCount": 9
  }
]
```

### 8.3 饮食目标分布

`GET /api/admin/stats/diet-goals`

响应 `data`:

```json
[
  {
    "dietGoal": "FAT_LOSS",
    "count": 12
  },
  {
    "dietGoal": "BALANCED",
    "count": 8
  }
]
```

### 8.4 维护端菜谱列表

`GET /api/admin/recipes`

查询参数：

- `keyword`: 可选
- `dietGoal`: 可选
- `tag`: 可选
- `status`: 可选，`1` 上架，`0` 下架

响应 `data`: `RecipeSummaryResponse[]`，字段同 `GET /api/recipes`。

### 8.5 维护端菜谱详情

`GET /api/admin/recipes/{id}`

响应 `data`: `RecipeDetailResponse`，字段同 `GET /api/recipes/{id}`。

### 8.6 新增菜谱

`POST /api/admin/recipes`

请求：

```json
{
  "name": "鸡胸肉西兰花轻食碗",
  "description": "高蛋白低脂轻食",
  "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg",
  "cookingTime": 25,
  "difficulty": "EASY",
  "servings": 1,
  "calories": 420,
  "protein": 35.00,
  "fat": 9.00,
  "carbs": 45.00,
  "tasteTags": ["清淡"],
  "healthTags": ["低脂", "高蛋白"],
  "targetGoals": ["FAT_LOSS", "MUSCLE_GAIN"],
  "steps": ["准备食材", "煎熟鸡胸肉", "搭配蔬菜装盘"],
  "ingredients": [
    {
      "ingredientId": 1,
      "quantity": 200.00,
      "unit": "g",
      "core": true
    }
  ]
}
```

响应 `data`: `RecipeDetailResponse`。

### 8.7 修改菜谱

`PUT /api/admin/recipes/{id}`

请求体同新增菜谱。响应 `data`: `RecipeDetailResponse`。

### 8.8 下架菜谱

`DELETE /api/admin/recipes/{id}`

响应 `data`: `null`

### 8.9 食材列表

`GET /api/admin/ingredients`

查询参数：

- `keyword`: 可选
- `category`: 可选

响应 `data`:

```json
[
  {
    "id": 1,
    "name": "鸡胸肉",
    "category": "肉蛋奶",
    "unit": "g",
    "caloriesPer100g": 165,
    "proteinPer100g": 31.00,
    "fatPer100g": 3.60,
    "carbsPer100g": 0.00,
    "aliases": ["鸡肉"],
    "createdAt": "2026-07-07T22:54:50",
    "updatedAt": "2026-07-07T22:54:50"
  }
]
```

### 8.10 新增食材

`POST /api/admin/ingredients`

请求：

```json
{
  "name": "鸡胸肉",
  "category": "肉蛋奶",
  "unit": "g",
  "caloriesPer100g": 165,
  "proteinPer100g": 31.00,
  "fatPer100g": 3.60,
  "carbsPer100g": 0.00,
  "aliases": ["鸡肉"]
}
```

响应 `data`: `IngredientResponse`。

### 8.11 修改食材

`PUT /api/admin/ingredients/{id}`

请求体同新增食材。响应 `data`: `IngredientResponse`。

### 8.12 删除食材

`DELETE /api/admin/ingredients/{id}`

响应 `data`: `null`

说明：如果食材已经被菜谱使用，后端会返回业务错误。

## 9. 前端对接建议

- 登录成功后保存 `token`，后续请求统一附加 `Authorization`。
- 普通用户登录后默认进入 `/user/home`，维护员登录后默认进入 `/admin/dashboard`。
- 推荐结果页可保存 `historyId`，后续用于跳转历史详情。
- 菜谱详情页生成购物清单时，建议传入当前用户已有食材到 `availableIngredients`。
- 所有删除接口成功时 `data` 为 `null`，以前端提示操作成功即可。
