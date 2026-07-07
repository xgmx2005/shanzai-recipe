# 膳哉

Repository: `shanzai-recipe`

膳哉是一个基于健康档案的智能菜谱推荐与购物清单生成系统。

核心流程：

1. 用户注册登录。
2. 填写健康档案和饮食目标。
3. 输入已有食材、忌口和烹饪条件。
4. 系统通过规则评分推荐菜谱。
5. DeepSeek 生成推荐理由和健康提示。
6. 用户查看菜谱详情、收藏菜谱并生成购物清单。
7. 数据维护员维护菜谱库、食材营养库和统计看板。

技术栈：

- 前端：Vue 3 + Vite + TypeScript + Naive UI + Pinia + Axios
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL + JWT
- AI：DeepSeek Chat Completions API

默认演示账号：

- 普通用户：`user1` / `123456`
- 数据维护员：`maintainer` / `123456`

协作规范：

- Git 协作与提交信息规范见 [docs/git-workflow.md](docs/git-workflow.md)。
