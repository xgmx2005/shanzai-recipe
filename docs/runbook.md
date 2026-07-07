# 膳哉运行手册

## 1. 数据库

创建数据库：

```sql
CREATE DATABASE shanzai_recipe DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入：

```powershell
mysql -u root -p shanzai_recipe < backend/src/main/resources/db/schema.sql
mysql -u root -p shanzai_recipe < backend/src/main/resources/db/data.sql
```

如果 MySQL 命令行因为项目路径包含中文而无法 `SOURCE` SQL 文件，可以使用 IDEA 的 Database 面板连接 `shanzai_recipe` 后，分别右键运行：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/data.sql
```

也可以先把两个 SQL 文件复制到纯英文路径后再执行 `SOURCE`。

## 2. 后端

设置环境变量：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

启动：

```powershell
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

如果本机 MySQL 密码不是 `root`，启动前设置环境变量：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的 MySQL 密码"
```

在 IDEA 中可以进入 Run Configuration，在 Environment variables 中添加：

```text
DB_USERNAME=root;DB_PASSWORD=你的 MySQL 密码
```

## 3. 前端

安装依赖并启动：

```powershell
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

## 4. 演示流程

1. 使用 `user1 / 123456` 登录。
2. 进入健康档案页，选择减脂控热量、日常健康或健身增肌。
3. 输入已有食材，生成推荐。
4. 查看推荐理由、营养数据和菜谱详情。
5. 生成购物清单并勾选食材。
6. 使用 `maintainer / 123456` 登录维护端，展示菜谱、食材和统计。

