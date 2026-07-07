# Shanzai Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build “膳哉：基于健康档案的智能菜谱推荐与购物清单生成系统” as a complete two-role Web application for the 2026 short-term course project.

**Architecture:** The frontend is a Vue 3 single-page application using Naive UI, Vue Router, Pinia, and Axios. The backend is a Spring Boot REST API using MyBatis-Plus and MySQL, with JWT authentication, a rules-based recommendation engine, DeepSeek-backed text generation, shopping-list calculation, and lightweight maintainer pages.

**Tech Stack:** Vue 3, Vite, TypeScript, Naive UI, Pinia, Axios, Spring Boot 3, Java 17, MyBatis-Plus, MySQL 8, JUnit 5, DeepSeek Chat Completions API.

---

## Source Documents

- Product design memory: `G:\CODE\短学期\docs\superpowers\specs\2026-07-06-smart-recipe-assistant-design.md`
- Course task PDF: `G:\CODE\短学期\短学期任务与要求.pdf`
- Course study guide PDF: `G:\CODE\短学期\短学期-学习指南.pdf`
- Report template: `G:\CODE\短学期\短学期-实验报告模板(学生填写).doc`

## Assumptions

- Workspace root: `G:\CODE\短学期`
- The workspace is currently not a Git repository.
- The first delivery uses one backend service and one frontend app.
- DeepSeek API key is stored only in backend environment variables or local config.
- First database name: `shanzai_recipe`
- Maintainer account is used for recipe and ingredient data maintenance, not for complex user moderation.
- Member real names and student IDs are inserted into the report later by the students.

## File Structure

### Root

- Create `G:\CODE\短学期\.gitignore`: ignore generated files, secrets, build outputs, and local IDE files.
- Create `G:\CODE\短学期\README.md`: project overview, run instructions, demo accounts.
- Create `G:\CODE\短学期\docs\api-contract.md`: stable API contract for frontend and backend collaboration.
- Create `G:\CODE\短学期\docs\runbook.md`: local startup and demo flow.
- Create `G:\CODE\短学期\docs\report-materials.md`: screenshots, key code locations, and video script notes.

### Backend

- Create `G:\CODE\短学期\backend\pom.xml`: Maven dependencies.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\ShanzaiApplication.java`: Spring Boot entry.
- Create `G:\CODE\短学期\backend\src\main\resources\application.yml`: server, datasource, MyBatis-Plus, JWT, DeepSeek config.
- Create `G:\CODE\短学期\backend\src\main\resources\db\schema.sql`: database schema.
- Create `G:\CODE\短学期\backend\src\main\resources\db\data.sql`: seed users, ingredients, recipes, and recipe ingredients.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\*.java`: response wrapper, exceptions, enums.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\security\*.java`: JWT, password hashing, auth filter.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\auth\*.java`: register, login, current user.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\profile\*.java`: health profile CRUD and BMI calculation.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recipe\*.java`: recipe query and detail.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\*.java`: scoring, history, logs, AI reasons.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\shopping\*.java`: shopping-list generation and item checking.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\favorite\*.java`: favorite recipe APIs.
- Create `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\admin\*.java`: maintainer CRUD and dashboard.
- Create focused tests under `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\`.

### Frontend

- Create `G:\CODE\短学期\frontend\package.json`: frontend dependencies and scripts.
- Create `G:\CODE\短学期\frontend\vite.config.ts`: Vite config and API proxy.
- Create `G:\CODE\短学期\frontend\src\main.ts`: Vue app entry.
- Create `G:\CODE\短学期\frontend\src\App.vue`: app shell.
- Create `G:\CODE\短学期\frontend\src\router\index.ts`: user and maintainer routes.
- Create `G:\CODE\短学期\frontend\src\stores\auth.ts`: login state.
- Create `G:\CODE\短学期\frontend\src\api\*.ts`: Axios client and endpoint modules.
- Create `G:\CODE\短学期\frontend\src\layouts\UserLayout.vue`: user navigation.
- Create `G:\CODE\短学期\frontend\src\layouts\AdminLayout.vue`: maintainer navigation.
- Create user pages under `G:\CODE\短学期\frontend\src\views\user\`.
- Create maintainer pages under `G:\CODE\短学期\frontend\src\views\admin\`.
- Create shared components under `G:\CODE\短学期\frontend\src\components\`.
- Store local recipe images in `G:\CODE\短学期\frontend\public\images\recipes\`.

---

### Task 1: Repository Baseline

**Files:**
- Create: `G:\CODE\短学期\.gitignore`
- Create: `G:\CODE\短学期\README.md`
- Create: `G:\CODE\短学期\docs\runbook.md`

- [ ] **Step 1: Initialize Git**

Run:

```powershell
git init
```

Expected: Git creates `.git` under `G:\CODE\短学期`.

- [ ] **Step 2: Create `.gitignore`**

Write this content:

```gitignore
.idea/
.vscode/
*.iml

backend/target/
backend/.mvn/wrapper/maven-wrapper.jar

frontend/node_modules/
frontend/dist/
frontend/.vite/

.env
.env.*
!.env.example
application-local.yml

*.log
*.tmp
*.zip
```

- [ ] **Step 3: Create `README.md`**

Write:

```markdown
# 膳哉

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
```

- [ ] **Step 4: Create `docs\runbook.md`**

Write:

```markdown
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
```

- [ ] **Step 5: Commit baseline**

Run:

```powershell
git add .gitignore README.md docs/runbook.md docs/superpowers/specs docs/superpowers/plans
git commit -m "docs: initialize project baseline"
```

Expected: Git records the initial project documents.

---

### Task 2: Backend Project Scaffold

**Files:**
- Create: `G:\CODE\短学期\backend\pom.xml`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\ShanzaiApplication.java`
- Create: `G:\CODE\短学期\backend\src\main\resources\application.yml`
- Create: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\SmokeTest.java`

- [ ] **Step 1: Create backend directories**

Run:

```powershell
New-Item -ItemType Directory -Force -Path backend/src/main/java/com/shanzai/recipe
New-Item -ItemType Directory -Force -Path backend/src/main/resources
New-Item -ItemType Directory -Force -Path backend/src/test/java/com/shanzai/recipe
```

- [ ] **Step 2: Create `pom.xml`**

Use Java 17, Spring Boot 3, MyBatis-Plus, MySQL, JWT, validation, and test dependencies:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.7</version>
        <relativePath/>
    </parent>

    <groupId>com.shanzai</groupId>
    <artifactId>shanzai</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>shanzai</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create application entry**

```java
package com.shanzai.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShanzaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShanzaiApplication.class, args);
    }
}
```

- [ ] **Step 4: Create `application.yml`**

```yaml
server:
  port: 8080

spring:
  application:
    name: shanzai
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shanzai_recipe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

app:
  jwt:
    secret: change-this-secret-to-at-least-32-characters
    expire-minutes: 1440
  deepseek:
    base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY:}
    model: deepseek-v4-flash
```

- [ ] **Step 5: Write smoke test**

```java
package com.shanzai.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTest {
    @Test
    void projectCompiles() {
        assertTrue(true);
    }
}
```

- [ ] **Step 6: Run backend tests**

Run:

```powershell
cd backend
mvn test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit backend scaffold**

```powershell
git add backend
git commit -m "feat: scaffold spring boot backend"
```

---

### Task 3: Database Schema and Seed Data

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\resources\db\schema.sql`
- Create: `G:\CODE\短学期\backend\src\main\resources\db\data.sql`
- Create: `G:\CODE\短学期\docs\database-design.md`

- [ ] **Step 1: Create database resource directory**

Run:

```powershell
New-Item -ItemType Directory -Force -Path backend/src/main/resources/db
```

- [ ] **Step 2: Write schema**

Create the 10 core tables: `user`, `user_profile`, `recipe`, `ingredient`, `recipe_ingredient`, `favorite`, `recommendation_history`, `shopping_list`, `shopping_list_item`, `recommendation_log`.

Use these conventions in every table:

- `bigint primary key auto_increment` for IDs.
- `created_at datetime default current_timestamp`.
- `updated_at datetime default current_timestamp on update current_timestamp` when records are editable.
- `utf8mb4` compatible text.

The schema must include these constraints:

```sql
alter table user add unique key uk_user_username (username);
alter table user_profile add unique key uk_profile_user (user_id);
alter table favorite add unique key uk_favorite_user_recipe (user_id, recipe_id);
alter table recipe_ingredient add key idx_recipe_ingredient_recipe (recipe_id);
alter table recommendation_log add key idx_recommendation_log_recipe (recipe_id);
```

- [ ] **Step 3: Write seed data**

Seed:

- Users: `user1` with role `USER`, `maintainer` with role `MAINTAINER`.
- Ingredients: chicken breast, egg, beef, shrimp, cod, salmon, tuna, tofu, broccoli, tomato, lettuce, greens, asparagus, mushroom, carrot, cucumber, rice, brown rice, quinoa, oats, noodle, pasta, whole wheat bread, whole wheat wrap, black pepper, salt, soy sauce, olive oil, lemon, scallion, corn, potato, avocado, seaweed, dried shrimp, peanut.
- Recipes: the 18 recipes listed in the product design memory, with image URLs under `/images/recipes/`.
- Recipe ingredients: at least 3 ingredients per recipe, with core ingredients marked by `is_core = 1`.

Use a BCrypt hash for `123456`. If generated by Spring Security, store the resulting hash in `password_hash`.

- [ ] **Step 4: Verify SQL can be imported**

Run:

```powershell
mysql -u root -p -e "drop database if exists shanzai_recipe; create database shanzai_recipe default character set utf8mb4 collate utf8mb4_unicode_ci;"
mysql -u root -p shanzai_recipe < backend/src/main/resources/db/schema.sql
mysql -u root -p shanzai_recipe < backend/src/main/resources/db/data.sql
mysql -u root -p shanzai_recipe -e "select count(*) as recipe_count from recipe;"
```

Expected: `recipe_count` is `18`.

- [ ] **Step 5: Commit SQL**

```powershell
git add backend/src/main/resources/db docs/database-design.md
git commit -m "feat: add database schema and seed data"
```

---

### Task 4: Backend Common Contracts

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\ApiResponse.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\BusinessException.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\GlobalExceptionHandler.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\Role.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\common\DietGoal.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\common\ApiResponseTest.java`

- [ ] **Step 1: Write failing response wrapper test**

```java
package com.shanzai.recipe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {
    @Test
    void successResponseCarriesData() {
        ApiResponse<String> response = ApiResponse.ok("ready");

        assertTrue(response.success());
        assertEquals("ready", response.data());
        assertEquals("OK", response.message());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
cd backend
mvn test -Dtest=ApiResponseTest
```

Expected: compilation fails because `ApiResponse` does not exist.

- [ ] **Step 3: Implement `ApiResponse`**

```java
package com.shanzai.recipe.common;

public record ApiResponse<T>(boolean success, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
```

- [ ] **Step 4: Implement exception and enums**

Use:

```java
package com.shanzai.recipe.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

```java
package com.shanzai.recipe.common;

public enum Role {
    USER,
    MAINTAINER
}
```

```java
package com.shanzai.recipe.common;

public enum DietGoal {
    FAT_LOSS,
    BALANCED,
    MUSCLE_GAIN
}
```

- [ ] **Step 5: Run common tests**

```powershell
mvn test -Dtest=ApiResponseTest
```

Expected: `ApiResponseTest` passes.

- [ ] **Step 6: Commit common contracts**

```powershell
git add backend/src/main/java/com/shanzai/recipe/common backend/src/test/java/com/shanzai/recipe/common
git commit -m "feat: add backend common response contracts"
```

---

### Task 5: Authentication and Security

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\auth\UserEntity.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\auth\UserMapper.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\auth\AuthController.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\auth\AuthService.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\security\JwtTokenProvider.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\security\SecurityConfig.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\security\JwtTokenProviderTest.java`

- [ ] **Step 1: Write JWT test**

Test must assert that username and role survive token generation and parsing:

```java
package com.shanzai.recipe.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenProviderTest {
    @Test
    void generatedTokenCanBeParsed() {
        JwtTokenProvider provider = new JwtTokenProvider(
            "change-this-secret-to-at-least-32-characters",
            1440
        );

        String token = provider.generate(1L, "user1", "USER");
        JwtUser user = provider.parse(token);

        assertEquals(1L, user.userId());
        assertEquals("user1", user.username());
        assertEquals("USER", user.role());
    }
}
```

- [ ] **Step 2: Implement auth API**

Implement endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

Request DTOs:

```java
public record RegisterRequest(String username, String password, String nickname) {}
public record LoginRequest(String username, String password) {}
```

Login response:

```java
public record LoginResponse(String token, Long userId, String username, String nickname, String role) {}
```

- [ ] **Step 3: Run auth test**

```powershell
cd backend
mvn test -Dtest=JwtTokenProviderTest
```

Expected: JWT test passes.

- [ ] **Step 4: Manual API verification**

After database import and backend startup:

```powershell
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user1\",\"password\":\"123456\"}"
```

Expected: JSON response includes `success: true`, a token, and role `USER`.

- [ ] **Step 5: Commit auth**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/auth backend/src/main/java/com/shanzai/recipe/security backend/src/test/java/com/shanzai/recipe/security
git commit -m "feat: add jwt authentication"
```

---

### Task 6: Health Profile

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\profile\ProfileEntity.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\profile\ProfileMapper.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\profile\ProfileService.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\profile\ProfileController.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\profile\BmiCalculatorTest.java`

- [ ] **Step 1: Write BMI test**

```java
package com.shanzai.recipe.modules.profile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmiCalculatorTest {
    @Test
    void calculatesBmiWithTwoDecimals() {
        BigDecimal bmi = BmiCalculator.calculate(new BigDecimal("170"), new BigDecimal("65"));

        assertEquals(new BigDecimal("22.49"), bmi);
    }
}
```

- [ ] **Step 2: Implement BMI calculator**

```java
package com.shanzai.recipe.modules.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BmiCalculator {
    private BmiCalculator() {}

    public static BigDecimal calculate(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 3: Implement profile endpoints**

Endpoints:

- `GET /api/profile`
- `PUT /api/profile`
- `GET /api/profile/summary`

Profile request fields:

```json
{
  "gender": "FEMALE",
  "age": 20,
  "heightCm": 165,
  "weightKg": 55,
  "dietGoal": "FAT_LOSS",
  "tastePreferences": ["清淡", "低脂"],
  "avoidIngredients": ["辣椒"],
  "allergyIngredients": [],
  "cookingTimePreference": 25
}
```

- [ ] **Step 4: Run tests**

```powershell
cd backend
mvn test -Dtest=BmiCalculatorTest
```

Expected: BMI test passes.

- [ ] **Step 5: Commit profile**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/profile backend/src/test/java/com/shanzai/recipe/modules/profile
git commit -m "feat: add health profile module"
```

---

### Task 7: Recipe, Ingredient, and Maintainer CRUD

**Files:**
- Create backend recipe and ingredient entities, mappers, services, controllers under:
  - `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recipe\`
  - `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\ingredient\`
  - `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\admin\`

- [ ] **Step 1: Implement public recipe APIs**

Endpoints:

- `GET /api/recipes`
- `GET /api/recipes/{id}`

List filters:

- `keyword`
- `dietGoal`
- `tag`

Detail response must include:

- recipe base fields
- image URL
- nutrition values
- taste tags
- health tags
- target goals
- steps as an array
- ingredients with quantity, unit, category, and core flag

- [ ] **Step 2: Implement maintainer APIs**

Endpoints:

- `GET /api/admin/recipes`
- `GET /api/admin/recipes/{id}`
- `POST /api/admin/recipes`
- `PUT /api/admin/recipes/{id}`
- `DELETE /api/admin/recipes/{id}`
- `GET /api/admin/ingredients`
- `POST /api/admin/ingredients`
- `PUT /api/admin/ingredients/{id}`
- `DELETE /api/admin/ingredients/{id}`

Delete recipe by setting `recipe.status = 0`.

- [ ] **Step 3: Verify role guard**

Manual check:

```powershell
curl http://localhost:8080/api/admin/recipes
```

Expected: request without token fails.

```powershell
curl http://localhost:8080/api/admin/recipes -H "Authorization: Bearer USER_TOKEN"
```

Expected: normal user token fails.

```powershell
curl http://localhost:8080/api/admin/recipes -H "Authorization: Bearer MAINTAINER_TOKEN"
```

Expected: maintainer token succeeds.

- [ ] **Step 4: Commit CRUD**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/recipe backend/src/main/java/com/shanzai/recipe/modules/ingredient backend/src/main/java/com/shanzai/recipe/modules/admin
git commit -m "feat: add recipe and ingredient management"
```

---

### Task 8: Recommendation Engine

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationService.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationScoringService.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationController.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationService.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\recommendation\RecommendationScoringServiceTest.java`

- [ ] **Step 1: Write scoring test**

```java
package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScoringServiceTest {
    @Test
    void matchingIngredientsAndGoalIncreaseScore() {
        RecommendationScoringService service = new RecommendationScoringService();
        RecipeCandidate candidate = new RecipeCandidate(
            1L,
            "鸡胸肉西兰花轻食碗",
            List.of("鸡胸肉", "西兰花", "玉米"),
            List.of("鸡胸肉", "西兰花"),
            List.of("FAT_LOSS"),
            List.of("清淡", "高蛋白"),
            20,
            12
        );
        RecommendationRequestModel request = new RecommendationRequestModel(
            List.of("鸡胸肉", "西兰花"),
            List.of(),
            List.of(),
            "FAT_LOSS",
            List.of("清淡"),
            30
        );

        RecommendationScore score = service.score(candidate, request);

        assertTrue(score.totalScore() >= 80);
        assertTrue(score.reasons().contains("已有食材匹配度高"));
    }
}
```

- [ ] **Step 2: Implement scoring model**

Scoring weights:

- ingredient match: 35
- diet goal: 25
- taste preference: 15
- cooking convenience: 15
- popularity history: 10

Hard filters:

- recipe status must be active
- allergy ingredients must not appear
- avoid ingredients must not appear
- excluded ingredients must not appear

- [ ] **Step 3: Implement recommendation API**

Endpoint:

- `POST /api/recommendations`

Request:

```json
{
  "availableIngredients": ["鸡胸肉", "西兰花", "鸡蛋"],
  "excludedIngredients": ["辣椒"],
  "dietGoal": "FAT_LOSS",
  "cookingTime": 30,
  "servings": 1
}
```

Response data:

```json
{
  "historyId": 1,
  "aiSummary": "今天推荐低脂高蛋白搭配，适合控制热量。",
  "recipes": [
    {
      "id": 1,
      "name": "鸡胸肉西兰花轻食碗",
      "score": 92,
      "reason": "已有鸡胸肉和西兰花，食材匹配度高，热量适合减脂。",
      "calories": 420,
      "protein": 35,
      "imageUrl": "/images/recipes/chicken-broccoli-bowl.jpg"
    }
  ]
}
```

- [ ] **Step 4: Persist recommendation history and logs**

Write one `recommendation_history` row per request.

Write one `recommendation_log` row per returned recipe.

- [ ] **Step 5: Run recommendation test**

```powershell
cd backend
mvn test -Dtest=RecommendationScoringServiceTest
```

Expected: scoring test passes.

- [ ] **Step 6: Commit recommendation**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation backend/src/test/java/com/shanzai/recipe/modules/recommendation
git commit -m "feat: add rule based recipe recommendation"
```

---

### Task 9: DeepSeek AI Wrapper

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\DeepSeekClient.java`
- Modify: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\AiRecommendationService.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\recommendation\AiRecommendationServiceTest.java`

- [ ] **Step 1: Write fallback test**

```java
package com.shanzai.recipe.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRecommendationServiceTest {
    @Test
    void fallbackReasonWorksWhenApiUnavailable() {
        AiRecommendationService service = new AiRecommendationService(new DisabledDeepSeekClient());

        String reason = service.generateReason("鸡胸肉西兰花轻食碗", "FAT_LOSS", List.of("鸡胸肉", "西兰花"));

        assertTrue(reason.contains("减脂"));
        assertTrue(reason.contains("鸡胸肉西兰花轻食碗"));
    }
}
```

- [ ] **Step 2: Implement DeepSeek client**

Call:

- base URL: `https://api.deepseek.com`
- endpoint: `POST /chat/completions`
- model: `deepseek-v4-flash`
- API key header: `Authorization: Bearer ${DEEPSEEK_API_KEY}`

Backend prompt must require JSON output with:

```json
{
  "reason": "推荐理由",
  "healthTip": "健康提示",
  "shoppingTip": "购物清单提示"
}
```

- [ ] **Step 3: Enforce backend-only API key**

Verify:

- No DeepSeek key appears in frontend code.
- No DeepSeek key appears in Git-tracked files.
- Empty API key uses local fallback text.

- [ ] **Step 4: Run AI tests**

```powershell
cd backend
mvn test -Dtest=AiRecommendationServiceTest
```

Expected: fallback test passes without network.

- [ ] **Step 5: Commit AI wrapper**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation backend/src/test/java/com/shanzai/recipe/modules/recommendation
git commit -m "feat: add deepseek recommendation text wrapper"
```

---

### Task 10: Shopping List, Favorites, History, and Stats

**Files:**
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\shopping\*.java`
- Create: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\favorite\*.java`
- Modify: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\recommendation\RecommendationController.java`
- Modify: `G:\CODE\短学期\backend\src\main\java\com\shanzai\recipe\modules\admin\AdminDashboardController.java`
- Test: `G:\CODE\短学期\backend\src\test\java\com\shanzai\recipe\modules\shopping\ShoppingListServiceTest.java`

- [ ] **Step 1: Write shopping-list test**

```java
package com.shanzai.recipe.modules.shopping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingListServiceTest {
    @Test
    void excludesAvailableIngredientsFromShoppingList() {
        ShoppingListCalculator calculator = new ShoppingListCalculator();
        List<RecipeNeed> needs = List.of(
            new RecipeNeed("鸡胸肉", "肉蛋奶", 200, "g"),
            new RecipeNeed("西兰花", "蔬菜", 150, "g"),
            new RecipeNeed("玉米", "其他", 80, "g")
        );

        List<ShoppingNeed> result = calculator.calculate(needs, List.of("鸡胸肉"));

        assertEquals(2, result.size());
        assertEquals("西兰花", result.get(0).ingredientName());
        assertEquals("玉米", result.get(1).ingredientName());
    }
}
```

- [ ] **Step 2: Implement shopping-list APIs**

Endpoints:

- `POST /api/shopping-lists`
- `GET /api/shopping-lists`
- `GET /api/shopping-lists/{id}`
- `PATCH /api/shopping-lists/{listId}/items/{itemId}`
- `DELETE /api/shopping-lists/{id}`

- [ ] **Step 3: Implement favorites APIs**

Endpoints:

- `POST /api/recipes/{id}/favorite`
- `DELETE /api/recipes/{id}/favorite`
- `GET /api/favorites`

Repeated favorite creation must return success without duplicating data.

- [ ] **Step 4: Implement history and dashboard APIs**

Endpoints:

- `GET /api/recommendations/history`
- `GET /api/recommendations/history/{id}`
- `GET /api/admin/dashboard`
- `GET /api/admin/stats/popular-recipes`
- `GET /api/admin/stats/diet-goals`

- [ ] **Step 5: Run tests**

```powershell
cd backend
mvn test -Dtest=ShoppingListServiceTest
```

Expected: shopping-list test passes.

- [ ] **Step 6: Commit shopping and stats**

```powershell
git add backend/src/main/java/com/shanzai/recipe/modules/shopping backend/src/main/java/com/shanzai/recipe/modules/favorite backend/src/main/java/com/shanzai/recipe/modules/admin backend/src/test/java/com/shanzai/recipe/modules/shopping
git commit -m "feat: add shopping lists favorites and stats"
```

---

### Task 11: API Contract Document

**Files:**
- Create: `G:\CODE\短学期\docs\api-contract.md`

- [ ] **Step 1: Document base response**

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

- [ ] **Step 2: Document auth and profile APIs**

Include request and response examples for:

- register
- login
- current user
- get profile
- update profile

- [ ] **Step 3: Document recommendation and shopping APIs**

Include examples for:

- create recommendation
- history list
- recipe detail
- create shopping list
- check shopping-list item

- [ ] **Step 4: Document admin APIs**

Include examples for:

- admin recipe list
- create recipe
- update ingredient
- dashboard stats

- [ ] **Step 5: Commit API contract**

```powershell
git add docs/api-contract.md
git commit -m "docs: add frontend backend api contract"
```

---

### Task 12: Frontend Scaffold

**Files:**
- Create: `G:\CODE\短学期\frontend\package.json`
- Create: `G:\CODE\短学期\frontend\index.html`
- Create: `G:\CODE\短学期\frontend\vite.config.ts`
- Create: `G:\CODE\短学期\frontend\tsconfig.json`
- Create: `G:\CODE\短学期\frontend\src\main.ts`
- Create: `G:\CODE\短学期\frontend\src\App.vue`
- Create: `G:\CODE\短学期\frontend\src\styles\theme.css`

- [ ] **Step 1: Create frontend package**

`package.json` must include:

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview --host 0.0.0.0"
  },
  "dependencies": {
    "@vicons/lucide": "^0.13.0",
    "axios": "^1.7.9",
    "naive-ui": "^2.40.3",
    "pinia": "^2.3.0",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "typescript": "^5.7.2",
    "vite": "^6.0.5",
    "vue-tsc": "^2.2.0"
  }
}
```

- [ ] **Step 2: Configure Vite proxy**

`vite.config.ts`:

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create app entry**

`main.ts` must install Pinia, Router, and Naive UI providers.

- [ ] **Step 4: Install dependencies and build**

```powershell
cd frontend
npm install
npm run build
```

Expected: Vite build succeeds.

- [ ] **Step 5: Commit frontend scaffold**

```powershell
git add frontend
git commit -m "feat: scaffold vue frontend"
```

---

### Task 13: Frontend Auth, Layout, and Routing

**Files:**
- Create: `G:\CODE\短学期\frontend\src\api\http.ts`
- Create: `G:\CODE\短学期\frontend\src\api\auth.ts`
- Create: `G:\CODE\短学期\frontend\src\stores\auth.ts`
- Create: `G:\CODE\短学期\frontend\src\router\index.ts`
- Create: `G:\CODE\短学期\frontend\src\layouts\UserLayout.vue`
- Create: `G:\CODE\短学期\frontend\src\layouts\AdminLayout.vue`
- Create: `G:\CODE\短学期\frontend\src\views\LoginView.vue`

- [ ] **Step 1: Implement Axios client**

`http.ts` must:

- set base URL to `/api`
- attach `Authorization: Bearer <token>` when token exists
- clear login state on HTTP 401

- [ ] **Step 2: Implement auth store**

Store fields:

- `token`
- `user`
- `isAuthenticated`
- `isMaintainer`

Store actions:

- `login(username, password)`
- `logout()`
- `loadCurrentUser()`

- [ ] **Step 3: Implement routes**

Routes:

- `/login`
- `/user/home`
- `/user/profile`
- `/user/recommend`
- `/user/recommend/result`
- `/user/recipes/:id`
- `/user/shopping-lists`
- `/user/favorites`
- `/user/history`
- `/admin/dashboard`
- `/admin/recipes`
- `/admin/ingredients`

Route guards:

- unauthenticated users go to `/login`
- role `USER` goes to `/user/home`
- role `MAINTAINER` goes to `/admin/dashboard`

- [ ] **Step 4: Verify login page**

Run:

```powershell
cd frontend
npm run dev
```

Open `http://localhost:5173/login`, log in as both demo accounts, and verify each account enters the correct layout.

- [ ] **Step 5: Commit auth UI**

```powershell
git add frontend/src/api frontend/src/stores frontend/src/router frontend/src/layouts frontend/src/views/LoginView.vue
git commit -m "feat: add frontend auth and routing"
```

---

### Task 14: User Frontend Flow

**Files:**
- Create: `G:\CODE\短学期\frontend\src\views\user\UserHomeView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\ProfileView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\RecommendView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\RecommendationResultView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\RecipeDetailView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\ShoppingListView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\FavoritesView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\user\HistoryView.vue`
- Create: `G:\CODE\短学期\frontend\src\components\RecipeCard.vue`
- Create: `G:\CODE\短学期\frontend\src\components\NutritionBar.vue`

- [ ] **Step 1: Implement user home**

Home page must show:

- current nickname
- current diet goal
- BMI summary when profile exists
- quick entry buttons for profile, recommendation, shopping list, and favorites
- recent recommendation cards

- [ ] **Step 2: Implement health profile form**

Use Naive UI form controls:

- number inputs for age, height, weight
- segmented/radio controls for diet goal
- select tags for taste preferences
- dynamic tag input for avoid and allergy ingredients
- save button with loading state

- [ ] **Step 3: Implement recommendation input**

Fields:

- available ingredients
- excluded ingredients
- diet goal
- cooking time
- servings

Submit calls `POST /api/recommendations` and navigates to `/user/recommend/result`.

- [ ] **Step 4: Implement recommendation result**

Show:

- AI summary
- 3 to 5 recipe cards
- score
- calories and protein
- recommendation reason
- buttons for detail and favorite

- [ ] **Step 5: Implement recipe detail and shopping-list generation**

Detail page must show:

- recipe image
- ingredients
- cooking steps
- calories, protein, fat, carbs
- tags
- favorite button
- generate shopping list button

Shopping-list generation calls `POST /api/shopping-lists`.

- [ ] **Step 6: Implement shopping list, favorites, and history**

Shopping list page:

- grouped by category
- checkbox for purchased item
- delete list action

Favorites page:

- recipe cards
- remove favorite action

History page:

- input snapshot
- result recipe names
- created time

- [ ] **Step 7: Build frontend**

```powershell
cd frontend
npm run build
```

Expected: TypeScript and Vite build succeed.

- [ ] **Step 8: Commit user flow**

```powershell
git add frontend/src/views/user frontend/src/components frontend/src/api
git commit -m "feat: add user recipe recommendation flow"
```

---

### Task 15: Maintainer Frontend Flow

**Files:**
- Create: `G:\CODE\短学期\frontend\src\views\admin\AdminDashboardView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\admin\AdminRecipesView.vue`
- Create: `G:\CODE\短学期\frontend\src\views\admin\AdminIngredientsView.vue`
- Create: `G:\CODE\短学期\frontend\src\components\RecipeEditorDrawer.vue`
- Create: `G:\CODE\短学期\frontend\src\components\IngredientEditorDrawer.vue`

- [ ] **Step 1: Implement dashboard**

Show:

- user count
- recipe count
- ingredient count
- recommendation count
- popular recipe table
- diet-goal distribution

- [ ] **Step 2: Implement recipe management**

Features:

- search by keyword
- table list
- create recipe drawer
- edit recipe drawer
- logical delete
- ingredient rows inside recipe editor

- [ ] **Step 3: Implement ingredient management**

Features:

- search by keyword
- category filter
- create ingredient drawer
- edit ingredient drawer
- delete guard error display when ingredient is used

- [ ] **Step 4: Verify maintainer flow**

Login as `maintainer / 123456` and verify:

- dashboard loads stats
- new ingredient can be added
- recipe can be edited
- normal user cannot access `/admin/dashboard`

- [ ] **Step 5: Commit maintainer flow**

```powershell
git add frontend/src/views/admin frontend/src/components frontend/src/api
git commit -m "feat: add maintainer management pages"
```

---

### Task 16: Recipe Images and Visual Polish

**Files:**
- Create directory: `G:\CODE\短学期\frontend\public\images\recipes\`
- Add at least 9 image files:
  - `chicken-broccoli-bowl.jpg`
  - `shrimp-tofu-soup.jpg`
  - `tomato-egg-oatmeal.jpg`
  - `tomato-egg.jpg`
  - `beef-potato-carrot.jpg`
  - `salmon-quinoa-bowl.jpg`
  - `blackpepper-beef-brownrice.jpg`
  - `chicken-avocado-wrap.jpg`
  - `beef-egg-quinoa.jpg`

- [ ] **Step 1: Generate or collect images**

Use AI-generated or open-license images. Keep style consistent:

- real food photography
- natural light
- clean table
- 4:3 or 16:9 ratio
- no commercial watermark

- [ ] **Step 2: Verify image paths**

Open frontend recommendation results and recipe detail pages. Confirm recipe cards load local images from `/images/recipes/<file>.jpg`.

- [ ] **Step 3: Polish visual layout**

Design constraints:

- health-food look, not generic admin-only style
- dense but readable maintainer tables
- stable card image aspect ratio
- mobile width should not overlap text or controls
- icon buttons should use Lucide icons through `@vicons/lucide`

- [ ] **Step 4: Build frontend**

```powershell
cd frontend
npm run build
```

Expected: build succeeds and no image path errors appear in the browser console during manual verification.

- [ ] **Step 5: Commit images and polish**

```powershell
git add frontend/public/images/recipes frontend/src
git commit -m "feat: add recipe images and visual polish"
```

---

### Task 17: End-to-End Verification

**Files:**
- Create: `G:\CODE\短学期\docs\verification.md`

- [ ] **Step 1: Backend verification**

Run:

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Expected:

- tests pass
- backend starts on port `8080`
- login API works
- recommendation API works
- shopping-list API works

- [ ] **Step 2: Frontend verification**

Run:

```powershell
cd frontend
npm run build
npm run dev
```

Expected:

- build passes
- frontend starts on port `5173`
- login page renders
- user and maintainer layouts work

- [ ] **Step 3: Demo scenario verification**

Record result in `docs\verification.md` for these three scenarios:

1. FAT_LOSS, available ingredients: 鸡胸肉, 西兰花, 鸡蛋.
2. BALANCED, available ingredients: 番茄, 鸡蛋, 土豆.
3. MUSCLE_GAIN, available ingredients: 牛肉, 鸡蛋, 藜麦.

For each scenario verify:

- recommended recipes are relevant
- reasons are shown
- recipe detail opens
- shopping list excludes existing ingredients
- history record is saved

- [ ] **Step 4: Commit verification notes**

```powershell
git add docs/verification.md
git commit -m "test: record end to end verification"
```

---

### Task 18: Report and Video Materials

**Files:**
- Modify or create final report from: `G:\CODE\短学期\短学期-实验报告模板(学生填写).doc`
- Create: `G:\CODE\短学期\docs\report-materials.md`
- Create: `G:\CODE\短学期\docs\video-script.md`

- [ ] **Step 1: Prepare report material index**

`docs\report-materials.md` must include:

- project purpose
- member division
- requirement analysis
- database tables
- system architecture
- API design
- recommendation algorithm
- AI fallback design
- screenshots to capture
- key code files for report

- [ ] **Step 2: Prepare video script**

Use a 5-minute structure:

```markdown
# 演示视频脚本

0:00-0:30 项目背景与目标
0:30-1:10 登录、健康档案
1:10-2:10 输入食材并生成推荐
2:10-3:00 菜谱详情、营养数据、收藏
3:00-3:40 购物清单
3:40-4:30 数据维护端
4:30-5:00 代码结构、推荐算法和总结
```

- [ ] **Step 3: Capture screenshots**

Capture:

- login page
- user home
- profile page
- recommendation form
- recommendation result
- recipe detail
- shopping list
- maintainer dashboard
- recipe management
- database table screenshot
- Git commit history screenshot

- [ ] **Step 4: Final package checklist**

Final deliverables:

- frontend source
- backend source
- SQL scripts
- report document
- MP4 demo video
- README and runbook

- [ ] **Step 5: Commit docs**

```powershell
git add docs
git commit -m "docs: add report and video materials"
```

---

## Self-Review

### Spec Coverage

- Course runnable software: covered by backend, frontend, SQL, and verification tasks.
- Requirement analysis: covered by existing design memory and report materials task.
- Database design: covered by schema, data, and database-design document task.
- Technical selection: covered by README, plan, report materials, and implementation.
- System structure: covered by backend/frontend architecture and report materials.
- Interface explanation: covered by API contract and frontend route tasks.
- Key code: recommendation, shopping list, DeepSeek wrapper, auth, profile modules have explicit files.
- MP4 demo: covered by video script and screenshot task.
- Two-person collaboration: covered by Git baseline, branch/commit expectations, and report materials.

### Placeholder Scan

The plan avoids deferred placeholder wording in implementation steps. Member names and student IDs are intentionally treated as report inputs, not engineering blockers.

### Type and Name Consistency

- Diet goals use `FAT_LOSS`, `BALANCED`, `MUSCLE_GAIN`.
- Roles use `USER`, `MAINTAINER`.
- API prefix is `/api`.
- Maintainer prefix is `/api/admin`.
- Recipe images use `/images/recipes/<file>.jpg`.
- DeepSeek is called only from backend services.

## Execution Options

Plan complete. The recommended execution route is:

1. Use `superpowers:subagent-driven-development` for task-by-task implementation with review checkpoints.
2. Use `superpowers:executing-plans` for inline execution if the work should stay in this session.



