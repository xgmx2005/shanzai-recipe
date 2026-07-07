# 数据库设计说明

## 设计目标

数据库围绕“建档案、输食材、推荐菜谱、看详情、生成购物清单、收藏和记录历史”这一闭环设计。第一版保留 10 张核心表，避免过度拆分，保证两人小组可以按时完成。

## 核心表

| 表名 | 作用 |
|---|---|
| `user` | 存储普通用户和数据维护员账号 |
| `user_profile` | 存储身高、体重、BMI、饮食目标、偏好和忌口 |
| `ingredient` | 存储食材分类、单位、营养数据和别名 |
| `recipe` | 存储菜谱基础信息、图片、标签、营养摘要和步骤 |
| `recipe_ingredient` | 存储菜谱与食材的用量关系 |
| `favorite` | 存储用户收藏菜谱 |
| `recommendation_history` | 存储一次推荐请求和推荐结果 |
| `shopping_list` | 存储购物清单主表 |
| `shopping_list_item` | 存储购物清单明细 |
| `recommendation_log` | 存储推荐分数和统计日志 |

## 关键设计

- `user.role` 使用 `USER` 和 `MAINTAINER` 区分普通用户和数据维护员。
- `recipe.target_goals` 第一版使用逗号分隔字符串，降低课程项目实现复杂度。
- `ingredient.aliases` 用于处理“番茄/西红柿”这类输入差异。
- `recipe_ingredient.is_core` 标记核心食材，推荐算法优先计算核心食材匹配度。
- `shopping_list_item.ingredient_name` 冗余保存食材名，避免食材库修改影响历史清单展示。
- `recommendation_log` 用于维护端统计热门菜谱和饮食目标分布。

## 初始数据

`data.sql` 包含：

- 普通用户 `user1 / 123456`
- 数据维护员 `maintainer / 123456`
- 36 个第一批核心食材
- 18 道演示菜谱
- 每道菜至少 3 个食材关联

密码使用 Spring Security BCrypt 哈希，默认明文密码仅用于本地演示。
