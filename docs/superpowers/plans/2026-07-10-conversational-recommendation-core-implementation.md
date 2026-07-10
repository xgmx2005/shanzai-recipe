# 对话式推荐核心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有表单式推荐改为可恢复、可纠错的后端受控对话，并在确认后跳转独立推荐结果页。

**Architecture:** Spring Boot 后端保存会话、消息和结构化上下文，由纯状态机决定下一阶段；DeepSeek 解释器只提取字段，失败时回退本地词典解释器。Vue 前端只渲染后端状态，确认后复用现有 `RecommendationService` 与历史接口恢复结果。

**Tech Stack:** Java 17、Spring Boot 3.3、MyBatis-Plus、MySQL 8、Jackson、JUnit 5/Mockito、Vue 3、TypeScript、Naive UI、Vitest。

---

## 文件结构

### 后端新增

- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationStage.java`：五阶段枚举。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationStatus.java`：会话生命周期枚举。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationContext.java`：结构化条件与字段来源。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationAnswerAnalysis.java`：单轮提取结果。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationFlow.java`：纯状态迁移和无效回答升级规则。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationAnswerInterpreter.java`：自然语言解释边界。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/DictionaryConversationAnswerInterpreter.java`：无 AI 时的本地兜底。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/HttpConversationAnswerInterpreter.java`：DeepSeek JSON 提取。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationEntity.java`：会话表实体。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMessageEntity.java`：消息表实体。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMapper.java`：会话 Mapper。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMessageMapper.java`：消息 Mapper。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationService.java`：创建、恢复、发送、修改和确认用例。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationController.java`：`/api/recommendation-conversations` 接口。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationResponse.java`：统一会话响应。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/StartConversationRequest.java`：是否替换未完成会话。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationMessageRequest.java`：消息请求。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationContextPatchRequest.java`：确认卡修改请求。
- `backend/src/main/resources/db/migrations/2026-07-10-add-recommendation-conversation.sql`：数据库迁移。

### 前端新增

- `frontend/src/components/recommendation/RecommendationMessageList.vue`：消息列表。
- `frontend/src/components/recommendation/RecommendationComposer.vue`：输入、发送和快捷回答。
- `frontend/src/components/recommendation/RecommendationConditionSummary.vue`：条件确认卡。
- `frontend/src/views/user/RecommendationResultView.vue`：独立推荐结果页。
- `frontend/src/utils/recommendationConversation.ts`：会话阶段展示和结果路由工具。

### 主要修改

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/data.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendedRecipeResponse.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationService.java`
- `frontend/src/api/recommendation.ts`
- `frontend/src/types.ts`
- `frontend/src/router/index.ts`
- `frontend/src/views/user/RecommendView.vue`

## Task 1: 会话数据库结构

**Files:**
- Create: `backend/src/main/resources/db/migrations/2026-07-10-add-recommendation-conversation.sql`
- Modify: `backend/src/main/resources/db/schema.sql`
- Modify: `backend/src/main/resources/db/data.sql`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationConversationSchemaTest.java`

- [ ] **Step 1: 写失败的结构测试**

```java
class RecommendationConversationSchemaTest {
    private String schema;

    @BeforeEach
    void loadSchema() throws IOException {
        schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));
    }

    @Test
    void schemaContainsConversationAndIdempotentMessageTables() {
        assertTrue(schema.contains("recommendation_conversation ("));
        assertTrue(schema.contains("recommendation_conversation_message ("));
        assertTrue(schema.contains("UNIQUE KEY uk_conversation_client_message"));
        assertTrue(schema.contains("context_json JSON NOT NULL"));
        assertTrue(schema.contains("conversation_context_json JSON"));
        assertTrue(schema.contains("result_detail_json JSON"));
    }
}
```

- [ ] **Step 2: 运行测试并确认因表不存在而失败**

Run: `mvn -B -ntp -Dtest=RecommendationConversationSchemaTest test`

Expected: FAIL，提示 `CREATE TABLE recommendation_conversation` 断言失败。

- [ ] **Step 3: 增加正式表结构和迁移**

```sql
CREATE TABLE IF NOT EXISTS recommendation_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stage VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    context_json JSON NOT NULL,
    invalid_answer_count INT NOT NULL DEFAULT 0,
    recommendation_history_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recommendation_conversation_user_status (user_id, status),
    CONSTRAINT fk_recommendation_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_recommendation_conversation_history FOREIGN KEY (recommendation_history_id)
        REFERENCES recommendation_history (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recommendation_conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    client_message_id VARCHAR(64),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    extracted_data_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_client_message (conversation_id, client_message_id),
    KEY idx_conversation_message_order (conversation_id, id),
    CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES recommendation_conversation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_conversation_context = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_history'
      AND COLUMN_NAME = 'conversation_context_json'
);
SET @context_sql = IF(
    @has_conversation_context = 0,
    'ALTER TABLE recommendation_history ADD COLUMN conversation_context_json JSON NULL AFTER excluded_ingredients',
    'SELECT 1'
);
PREPARE context_stmt FROM @context_sql;
EXECUTE context_stmt;
DEALLOCATE PREPARE context_stmt;

SET @has_result_detail = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_history'
      AND COLUMN_NAME = 'result_detail_json'
);
SET @result_sql = IF(
    @has_result_detail = 0,
    'ALTER TABLE recommendation_history ADD COLUMN result_detail_json JSON NULL AFTER result_recipe_ids',
    'SELECT 1'
);
PREPARE result_stmt FROM @result_sql;
EXECUTE result_stmt;
DEALLOCATE PREPARE result_stmt;
```

在全量 `schema.sql` 的 `recommendation_history` 定义中直接加入 `conversation_context_json JSON` 和 `result_detail_json JSON`。删除顺序先删除消息表，再删除会话表；`data.sql` 的清理顺序同样先清消息再清会话。增量迁移使用上面的动态 `ALTER TABLE`，两张会话表使用 `CREATE TABLE IF NOT EXISTS`，不包含 `DROP TABLE`。

- [ ] **Step 4: 运行结构测试**

Run: `mvn -B -ntp -Dtest=RecommendationConversationSchemaTest test`

Expected: PASS，1 test，0 failures。

- [ ] **Step 5: 提交数据库结构**

```bash
git add backend/src/main/resources/db backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationConversationSchemaTest.java
git commit -m "feat: 添加推荐对话数据表"
```

## Task 2: 纯状态机与无效回答控制

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationStage.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationStatus.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationContext.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationAnswerAnalysis.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationFlow.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationFlowTest.java`

- [ ] **Step 1: 写状态迁移失败测试**

```java
class ConversationFlowTest {
    private final ConversationFlow flow = new ConversationFlow();

    @Test
    void advancesPastFieldsAlreadyAnsweredInOneMessage() {
        RecommendationConversationContext context = RecommendationConversationContext.empty();
        ConversationAnswerAnalysis analysis = new ConversationAnswerAnalysis(
            true,
            "清淡控热量",
            "FAT_LOSS",
            List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
            List.of(),
            List.of(),
            30,
            1,
            List.of(),
            List.of(),
            new BigDecimal("0.96")
        );

        ConversationTransition transition = flow.apply(
            ConversationStage.INTENT,
            ConversationStatus.ACTIVE,
            context,
            0,
            analysis
        );

        assertEquals(ConversationStage.CONFIRM, transition.stage());
        assertEquals(ConversationStatus.READY_TO_CONFIRM, transition.status());
        assertEquals(0, transition.invalidAnswerCount());
    }

    @Test
    void invalidAnswersDoNotAdvanceAndEscalateGuidance() {
        ConversationTransition first = flow.apply(
            ConversationStage.INGREDIENTS,
            ConversationStatus.ACTIVE,
            RecommendationConversationContext.empty(),
            0,
            ConversationAnswerAnalysis.invalid()
        );
        ConversationTransition second = flow.apply(
            first.stage(), first.status(), first.context(), first.invalidAnswerCount(),
            ConversationAnswerAnalysis.invalid()
        );
        ConversationTransition third = flow.apply(
            second.stage(), second.status(), second.context(), second.invalidAnswerCount(),
            ConversationAnswerAnalysis.invalid()
        );

        assertEquals(ConversationStage.INGREDIENTS, second.stage());
        assertEquals(2, second.invalidAnswerCount());
        assertEquals(GuidanceMode.QUICK_OPTIONS, second.guidanceMode());
        assertEquals(3, third.invalidAnswerCount());
        assertEquals(GuidanceMode.RESTART_OPTION, third.guidanceMode());
    }
}
```

- [ ] **Step 2: 运行测试并确认类型尚不存在**

Run: `mvn -B -ntp -Dtest=ConversationFlowTest test`

Expected: FAIL，编译器报告 `ConversationFlow` 等类型不存在。

- [ ] **Step 3: 实现明确的领域类型**

```java
public enum ConversationStage {
    INTENT, INGREDIENTS, RESTRICTIONS, CONTEXT, CONFIRM
}

public enum ConversationStatus {
    ACTIVE, READY_TO_CONFIRM, COMPLETED, CANCELLED
}

public enum GuidanceMode {
    NORMAL, EXAMPLE, QUICK_OPTIONS, RESTART_OPTION
}

public record AvailableIngredientInput(
    String name,
    BigDecimal quantity,
    String unit,
    boolean quantityKnown
) {
}

public record ConversationAnswerAnalysis(
    boolean relevant,
    String intentText,
    String dietGoal,
    List<AvailableIngredientInput> availableIngredients,
    List<String> excludedIngredients,
    List<String> allergyIngredients,
    Integer cookingTime,
    Integer servings,
    List<String> unknownTerms,
    List<String> conflicts,
    BigDecimal confidence
) {
    public static ConversationAnswerAnalysis invalid() {
        return new ConversationAnswerAnalysis(
            false, null, null, List.of(), List.of(), List.of(), null, null,
            List.of(), List.of(), BigDecimal.ZERO
        );
    }
}
```

`RecommendationConversationContext` 使用不可变 record，提供 `empty()` 与 `merge(ConversationAnswerAnalysis)`。`ConversationFlow.apply` 先处理无效回答，再调用 `firstMissingStage(context)`；当无缺失字段且无冲突、未知词时返回 `CONFIRM/READY_TO_CONFIRM`。无效次数 1、2、3 依次映射 `EXAMPLE`、`QUICK_OPTIONS`、`RESTART_OPTION`。

- [ ] **Step 4: 运行状态机测试**

Run: `mvn -B -ntp -Dtest=ConversationFlowTest test`

Expected: PASS，2 tests，0 failures；第二次无效回答给快捷选项，第三次给重新开始入口。

- [ ] **Step 5: 提交状态机**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationFlowTest.java
git commit -m "feat: 添加推荐对话状态机"
```

## Task 3: 自然语言解释器与本地降级

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationAnswerInterpreter.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/DictionaryConversationAnswerInterpreter.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/HttpConversationAnswerInterpreter.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/DictionaryConversationAnswerInterpreterTest.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/HttpConversationAnswerInterpreterTest.java`

- [ ] **Step 1: 写词典解释器失败测试**

```java
class DictionaryConversationAnswerInterpreterTest {
    private final DictionaryConversationAnswerInterpreter interpreter =
        new DictionaryConversationAnswerInterpreter();

    @Test
    void normalizesFoodAliasesAndExtractsContextNumbers() {
        ConversationAnswerAnalysis result = interpreter.interpret(
            ConversationStage.INGREDIENTS,
            "有300克鸡胸肉、两个鸡蛋和西蓝花，一个人吃，半小时以内",
            RecommendationConversationContext.empty()
        );

        assertTrue(result.relevant());
        assertEquals(List.of("鸡胸肉", "鸡蛋", "西兰花"),
            result.availableIngredients().stream().map(AvailableIngredientInput::name).toList());
        assertEquals(1, result.servings());
        assertEquals(30, result.cookingTime());
    }

    @Test
    void rejectsSymbolsWithoutChangingContext() {
        ConversationAnswerAnalysis result = interpreter.interpret(
            ConversationStage.INGREDIENTS,
            "@@@？？？",
            RecommendationConversationContext.empty()
        );
        assertFalse(result.relevant());
    }
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `mvn -B -ntp -Dtest=DictionaryConversationAnswerInterpreterTest test`

Expected: FAIL，解释器类型不存在。

- [ ] **Step 3: 实现解释器接口和本地词典**

```java
public interface ConversationAnswerInterpreter {
    ConversationAnswerAnalysis interpret(
        ConversationStage stage,
        String content,
        RecommendationConversationContext context
    );
}
```

本地解释器必须包含：`西蓝花 -> 西兰花`、`番茄 -> 番茄`、`鸡胸 -> 鸡胸肉` 的别名表；中文数量 `一至十` 与阿拉伯数字解析；分钟、小时、克、千克、毫升、个的单位解析；纯符号和去空白后少于一个有效汉字的内容返回 `invalid()`。模糊词“肉”“菜”放入 `unknownTerms`，不加入食材列表。

- [ ] **Step 4: 写 DeepSeek JSON 解释测试**

使用 `MockRestServiceServer` 验证请求包含阶段、当前上下文和 JSON 输出约束；响应为合法 JSON 时解析 `availableIngredients`，响应非法或 API Key 为空时调用词典解释器并返回兜底结果。

- [ ] **Step 5: 实现 `HttpConversationAnswerInterpreter`**

构造参数沿用 `app.deepseek.*`。系统提示必须声明：只提取字段，不生成菜谱，不覆盖已确认字段，输出字段固定为 `relevant`、`intentText`、`dietGoal`、`availableIngredients`、`excludedIngredients`、`allergyIngredients`、`cookingTime`、`servings`、`unknownTerms`、`conflicts`、`confidence`。解析后再次用本地词典规范食材名称。

- [ ] **Step 6: 运行解释器测试**

Run: `mvn -B -ntp -Dtest='*ConversationAnswerInterpreterTest' test`

Expected: PASS，合法 AI 响应、无 Key 降级、乱码拒绝均通过。

- [ ] **Step 7: 提交解释器**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation backend/src/main/resources/application.yml
git commit -m "feat: 添加推荐对话语言理解与降级"
```

## Task 4: 会话持久化与恢复

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationEntity.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMessageEntity.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMapper.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationMessageMapper.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationResponse.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationService.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationServiceTest.java`

- [ ] **Step 1: 写创建、恢复和幂等失败测试**

测试必须覆盖：创建会话时载入档案但仍从 `INTENT` 提问；查询最近未完成会话；重新开始时先把旧 `ACTIVE/READY_TO_CONFIRM` 会话标为 `CANCELLED`；发送同一 `clientMessageId` 两次只插入一次用户消息；读取他人会话抛出 `BusinessException("推荐对话不存在")`；刷新恢复的消息按 ID 升序。

核心断言：

```java
assertEquals(ConversationStage.INGREDIENTS, response.stage());
assertEquals(2, response.messages().size());
verify(messageMapper, times(1)).insert(argThat(message -> "message-001".equals(message.getClientMessageId())));
RecommendationConversationEntity replacedConversation = activeConversation(11L, 7L);
when(conversationMapper.selectList(any())).thenReturn(List.of(replacedConversation));
service.startConversation(7L, true);
assertEquals("CANCELLED", replacedConversation.getStatus());
```

- [ ] **Step 2: 运行并确认服务不存在**

Run: `mvn -B -ntp -Dtest=RecommendationConversationServiceTest test`

Expected: FAIL，`RecommendationConversationService` 不存在。

- [ ] **Step 3: 实现实体、Mapper 和 JSON 持久化**

会话实体映射 `recommendation_conversation`；消息实体映射 `recommendation_conversation_message`。服务使用已有 `ObjectMapper` 将 `RecommendationConversationContext` 序列化到 `context_json`。`findActiveConversation(userId)` 返回最近更新的一条 `ACTIVE/READY_TO_CONFIRM` 会话或空；`startConversation(userId, true)` 先取消该用户未完成会话再创建新会话。所有写操作使用 `@Transactional`。

- [ ] **Step 4: 实现发送消息用例**

```java
@Transactional
public ConversationResponse sendMessage(Long userId, Long conversationId, ConversationMessageRequest request) {
    RecommendationConversationEntity conversation = findOwned(userId, conversationId);
    RecommendationConversationMessageEntity existing = findByClientMessageId(conversationId, request.clientMessageId());
    if (existing != null) {
        return getConversation(userId, conversationId);
    }
    saveUserMessage(conversationId, request.clientMessageId(), request.content());
    RecommendationConversationContext context = readContext(conversation.getContextJson());
    ConversationAnswerAnalysis analysis = interpreter.interpret(
        ConversationStage.valueOf(conversation.getStage()), request.content(), context
    );
    ConversationTransition transition = flow.apply(
        ConversationStage.valueOf(conversation.getStage()),
        ConversationStatus.valueOf(conversation.getStatus()),
        context,
        conversation.getInvalidAnswerCount(),
        analysis
    );
    updateConversation(conversation, transition);
    saveAssistantMessage(conversationId, replyFactory.reply(transition));
    return getConversation(userId, conversationId);
}
```

- [ ] **Step 5: 运行服务测试**

Run: `mvn -B -ntp -Dtest=RecommendationConversationServiceTest test`

Expected: PASS，创建、继续、重新开始、恢复、所有权和幂等测试通过。

- [ ] **Step 6: 提交持久化服务**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationServiceTest.java
git commit -m "feat: 实现推荐对话持久化与恢复"
```

## Task 5: 修改条件、确认推荐与 HTTP 接口

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationController.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/StartConversationRequest.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationMessageRequest.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/ConversationContextPatchRequest.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation/RecommendationConversationControllerTest.java`

- [ ] **Step 1: 写控制器失败测试**

使用 `@WebMvcTest` 和 Spring Security 测试以下路由：

```text
POST  /api/recommendation-conversations
GET   /api/recommendation-conversations/active
GET   /api/recommendation-conversations/{id}
POST  /api/recommendation-conversations/{id}/messages
PATCH /api/recommendation-conversations/{id}/context
POST  /api/recommendation-conversations/{id}/confirm
```

断言未登录返回 401；登录用户的 `JwtUser.userId()` 传入服务；`GET /active` 没有会话时返回 `data: null`；`POST` 的 `replaceActive=true` 传入服务；空消息和超过 1000 字符的消息返回 400；`clientMessageId` 超过 64 字符返回 400。

- [ ] **Step 2: 运行并确认路由不存在**

Run: `mvn -B -ntp -Dtest=RecommendationConversationControllerTest test`

Expected: FAIL，目标路由返回 404。

- [ ] **Step 3: 实现确认卡修改**

`patchContext` 只接受 `intentText`、`dietGoal`、食材、排除食材、过敏、时间和人数字段；更新后重新运行规范化与 `firstMissingStage`，存在冲突时保持 `ACTIVE`，完整时进入 `READY_TO_CONFIRM`。

`ConversationMessageRequest` 明确定义为：

```java
public record ConversationMessageRequest(
    @NotBlank @Size(max = 1000) String content,
    @NotBlank @Size(max = 64) String clientMessageId
) {}

public record StartConversationRequest(boolean replaceActive) {}
```

Controller 的创建接口接收可空 body，缺省按 `replaceActive=false`；`GET /active` 只返回当前用户最近一条未完成会话。第三次无效回答中的“重新开始”按钮调用 `POST` 并提交 `{"replaceActive":true}`。

服务日志只记录 `conversationId`、阶段、状态、耗时和成功/降级标记，不记录消息正文、完整健康档案或提取 JSON。

- [ ] **Step 4: 实现幂等确认**

```java
@Transactional
public RecommendationResponse confirm(Long userId, Long conversationId) {
    RecommendationConversationEntity conversation = findOwned(userId, conversationId);
    if (ConversationStatus.COMPLETED.name().equals(conversation.getStatus())) {
        return historyService.getRecommendationResponse(userId, conversation.getRecommendationHistoryId());
    }
    if (!ConversationStatus.READY_TO_CONFIRM.name().equals(conversation.getStatus())) {
        throw new BusinessException("推荐条件尚未确认完整");
    }
    RecommendationRequest request = requestFactory.from(readContext(conversation.getContextJson()));
    RecommendationResponse response = recommendationService.recommend(userId, request);
    historyService.attachConversationContext(
        userId,
        response.historyId(),
        readContext(conversation.getContextJson())
    );
    conversation.setStatus(ConversationStatus.COMPLETED.name());
    conversation.setRecommendationHistoryId(response.historyId());
    conversationMapper.updateById(conversation);
    return response;
}
```

向 `RecommendationHistoryService` 增加 `attachConversationContext` 和 `getRecommendationResponse`。前者将已校验的 `RecommendationConversationContext` 序列化到 `recommendation_history.conversation_context_json`，并再次校验历史属于当前用户；后者从历史详情重建现有响应，确保重复确认不创建第二条历史。`RecommendationHistoryDetailResponse` 增加可空 `conversationContext`，旧历史没有快照时返回 `null`，结果页据此获得带数量的已有食材和目标人数。

- [ ] **Step 5: 实现控制器并运行测试**

Run: `mvn -B -ntp -Dtest='RecommendationConversationControllerTest,RecommendationConversationServiceTest' test`

Expected: PASS，所有路由、权限、修改和重复确认测试通过。

- [ ] **Step 6: 提交接口**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation backend/src/test/java/com/shanzai/recipe/modules/recommendation/conversation
git commit -m "feat: 提供推荐对话确认接口"
```

## Task 6: 保存可恢复的推荐结果快照与安全空结果

**Files:**
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendedRecipeResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryRecipeResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationService.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationServiceTest.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryServiceTest.java`

- [ ] **Step 1: 写结果快照失败测试**

```java
@Test
void savesMatchedAndMissingIngredientsInHistorySnapshot() throws Exception {
    RecommendationResponse response = recommendationService.recommend(
        7L,
        new RecommendationRequest(
            List.of("鸡胸肉"), List.of(), DietGoal.FAT_LOSS, 30, 2
        )
    );

    ArgumentCaptor<RecommendationHistoryEntity> captor =
        ArgumentCaptor.forClass(RecommendationHistoryEntity.class);
    verify(historyMapper).insert(captor.capture());
    List<RecommendedRecipeResponse> snapshot = objectMapper.readValue(
        captor.getValue().getResultDetailJson(),
        new TypeReference<List<RecommendedRecipeResponse>>() {}
    );

    assertEquals(List.of("鸡胸肉"), snapshot.get(0).matchedIngredients());
    assertTrue(snapshot.get(0).missingIngredients().contains("西兰花"));
    assertEquals(response.recipes().get(0).score(), snapshot.get(0).score());
}
```

- [ ] **Step 2: 写安全空结果失败测试**

```java
@Test
void returnsEmptySafeResultWithoutCallingAiWhenAllRecipesAreBlocked() {
    RecommendationResponse response = recommendationService.recommend(
        7L,
        new RecommendationRequest(
            List.of("鸡胸肉"), List.of("鸡胸肉", "鸡蛋", "豆腐"), DietGoal.FAT_LOSS, 30, 1
        )
    );

    assertTrue(response.recipes().isEmpty());
    assertEquals("暂无符合过敏和忌口约束的安全推荐，请调整条件后重试。", response.aiSummary());
    assertFalse(response.aiGenerated());
    verifyNoInteractions(deepSeekClient);
}
```

- [ ] **Step 3: 运行并确认失败**

Run: `mvn -B -ntp -Dtest='RecommendationServiceTest,RecommendationHistoryServiceTest' test`

Expected: FAIL，推荐响应没有匹配/缺少食材，历史也没有结果快照。

- [ ] **Step 4: 扩展推荐菜谱响应**

```java
public record RecommendedRecipeResponse(
    Long id,
    String name,
    int score,
    String reason,
    Integer calories,
    BigDecimal protein,
    String imageUrl,
    List<String> matchedIngredients,
    List<String> missingIngredients
) {}
```

`toResponse` 使用已经规范化的 `RecipeCandidate.ingredients` 与 `requestModel.availableIngredients` 求交集和差集；顺序沿用菜谱食材顺序，两个列表都去重。不得把排除或过敏食材放进 `missingIngredients`，因为包含硬约束食材的候选在评分前已经被过滤。

- [ ] **Step 5: 保存和恢复 JSON 快照**

`saveHistory` 用项目 `ObjectMapper` 把最终 `List<RecommendedRecipeResponse>` 写入 `history.resultDetailJson`，写入失败抛出 `BusinessException("推荐结果保存失败")` 并回滚推荐事务。`RecommendationHistoryService` 优先解析该 JSON 构造历史菜谱响应；旧历史 `resultDetailJson` 为空时继续按 `resultRecipeIds` 加载基础菜谱，但 `matchedIngredients/missingIngredients` 返回空数组、`score` 返回 0、`reason` 返回旧 `aiSummary`，保证兼容。

- [ ] **Step 6: 固化无安全结果规则**

安全过滤后候选为空时，不调用 DeepSeek 和后续 Tavily 证据服务；保存空的 `resultRecipeIds` 与 `resultDetailJson=[]`，返回固定摘要“暂无符合过敏和忌口约束的安全推荐，请调整条件后重试。”。不删除排除食材、不降低过敏约束、不生成知识库外菜谱。

- [ ] **Step 7: 运行推荐与历史测试**

Run: `mvn -B -ntp -Dtest='RecommendationServiceTest,RecommendationHistoryServiceTest' test`

Expected: PASS，结果刷新可恢复且无安全候选时严格返回空列表。

- [ ] **Step 8: 提交结果快照**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation backend/src/test/java/com/shanzai/recipe/modules/recommendation
git commit -m "feat: 保存可恢复的推荐匹配结果"
```

## Task 7: 前端会话 API、类型和结果路由

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/api/recommendation.ts`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/utils/recommendationConversation.ts`
- Test: `frontend/src/utils/recommendationConversation.test.ts`

- [ ] **Step 1: 写结果路由失败测试**

```ts
import { describe, expect, it } from 'vitest'
import { recommendationResultRoute } from './recommendationConversation'

describe('recommendationResultRoute', () => {
  it('builds a refresh-safe history result route', () => {
    expect(recommendationResultRoute(17)).toEqual({
      path: '/user/recommend/result',
      query: { historyId: '17' },
    })
  })
})
```

- [ ] **Step 2: 运行并确认工具不存在**

Run: `npm run test:unit -- src/utils/recommendationConversation.test.ts --run`

Expected: FAIL，无法导入 `recommendationConversation`。

- [ ] **Step 3: 增加类型与 API**

```ts
export type ConversationStage = 'INTENT' | 'INGREDIENTS' | 'RESTRICTIONS' | 'CONTEXT' | 'CONFIRM'
export type ConversationStatus = 'ACTIVE' | 'READY_TO_CONFIRM' | 'COMPLETED' | 'CANCELLED'

export interface AvailableIngredientInput {
  name: string
  quantity?: number
  unit?: string
  quantityKnown: boolean
}

export interface RecommendationConversationContext {
  intentText?: string
  dietGoal?: DietGoal
  availableIngredients: AvailableIngredientInput[]
  excludedIngredients: string[]
  allergyIngredients: string[]
  cookingTime?: number
  servings?: number
  unknownTerms: string[]
  conflicts: string[]
}

export interface RecommendationConversation {
  id: number
  stage: ConversationStage
  status: ConversationStatus
  invalidAnswerCount: number
  context: RecommendationConversationContext
  messages: RecommendationConversationMessage[]
  showConfirmation: boolean
  quickOptions: string[]
}

export interface RecommendedRecipe {
  id: number
  name: string
  score: number
  reason: string
  calories: number
  protein: number
  imageUrl: string
  matchedIngredients: string[]
  missingIngredients: string[]
}
```

把 `RecommendationHistoryDetail.recipes` 改为 `RecommendedRecipe[]`，并增加 `conversationContext?: RecommendationConversationContext`。在 `recommendation.ts` 增加 `getActiveConversation`、`startConversation(replaceActive = false)`、`getConversation`、`sendConversationMessage`、`patchConversationContext`、`confirmConversation`。路由 `recommend/result` 改为懒加载 `RecommendationResultView.vue`。

- [ ] **Step 4: 实现路由工具并运行测试**

```ts
export function recommendationResultRoute(historyId: number) {
  return {
    path: '/user/recommend/result',
    query: { historyId: String(historyId) },
  }
}
```

Run: `npm run test:unit -- src/utils/recommendationConversation.test.ts --run`

Expected: PASS，1 test。

- [ ] **Step 5: 提交 API 与路由**

```bash
git add frontend/src/types.ts frontend/src/api/recommendation.ts frontend/src/router/index.ts frontend/src/utils/recommendationConversation.ts frontend/src/utils/recommendationConversation.test.ts
git commit -m "feat: 添加推荐对话前端接口与路由"
```

## Task 8: 单列对话输入页

**Files:**
- Create: `frontend/src/components/recommendation/RecommendationMessageList.vue`
- Create: `frontend/src/components/recommendation/RecommendationComposer.vue`
- Create: `frontend/src/components/recommendation/RecommendationConditionSummary.vue`
- Modify: `frontend/src/views/user/RecommendView.vue`
- Test: `frontend/src/views/user/recommendConversationView.test.ts`

- [ ] **Step 1: 写页面结构失败测试**

使用源码结构测试锁定关键边界：

```ts
it('keeps conversation input separate from recommendation results', () => {
  const source = readFileSync(new URL('./RecommendView.vue', import.meta.url), 'utf8')
  expect(source).toContain('RecommendationMessageList')
  expect(source).toContain('RecommendationComposer')
  expect(source).toContain('RecommendationConditionSummary')
  expect(source).toContain('getActiveConversation')
  expect(source).toContain('重新开始')
  expect(source).not.toContain('primary-recipe')
  expect(source).not.toContain('preview-panel')
})
```

- [ ] **Step 2: 运行并确认旧页面仍包含结果栏**

Run: `npm run test:unit -- src/views/user/recommendConversationView.test.ts --run`

Expected: FAIL，旧页面仍包含 `preview-panel` 或缺少新组件。

- [ ] **Step 3: 实现三个专职组件**

`RecommendationMessageList` 只接收 `messages`；`RecommendationComposer` 通过 `submit` 事件发送文本并显示 `quickOptions`；`RecommendationConditionSummary` 只在 `showConfirmation` 为真时展示四组条件，并发出 `edit`、`confirm` 事件。

- [ ] **Step 4: 重写 `RecommendView` 状态流**

页面挂载时优先恢复路由查询中的 `conversationId`。没有 ID 时先调用 `getActiveConversation`：存在未完成会话则显示紧凑选择区“继续上次 / 重新开始”，不自动合并；继续时写入旧 ID，重新开始时调用 `startConversation(true)`；没有旧会话时调用 `startConversation(false)`。第三次无效回答的重新开始入口也调用同一函数。获得会话后用 `router.replace` 写入查询参数。发送期间禁用输入；失败时保留草稿；确认成功后调用：

```ts
const result = await confirmConversation(conversation.value.id)
await router.push(recommendationResultRoute(result.historyId))
```

布局使用现有 `sz-page`、`sz-panel`、`sz-chip` 和主题变量，内容列最大宽度 820px。移动端保持单列，不增加侧栏和重动画。

- [ ] **Step 5: 运行页面测试与构建**

Run: `npm run test:unit -- src/views/user/recommendConversationView.test.ts --run`

Expected: PASS。

Run: `npm run build`

Expected: `vue-tsc -b` 与 `vite build` 均成功。

- [ ] **Step 6: 提交对话输入页**

```bash
git add frontend/src/components/recommendation frontend/src/views/user/RecommendView.vue frontend/src/views/user/recommendConversationView.test.ts
git commit -m "feat: 将智能推荐改为对话式输入"
```

## Task 9: 独立推荐结果页

**Files:**
- Create: `frontend/src/views/user/RecommendationResultView.vue`
- Create: `frontend/src/components/recommendation/RecipeRecommendationCard.vue`
- Modify: `frontend/src/views/user/RecommendationHistoryView.vue`
- Test: `frontend/src/views/user/recommendationResultView.test.ts`

- [ ] **Step 1: 写独立结果页失败测试**

```ts
it('loads result from historyId and links to detail and shopping list', () => {
  const source = readFileSync(new URL('./RecommendationResultView.vue', import.meta.url), 'utf8')
  expect(source).toContain('getRecommendationHistory')
  expect(source).toContain('historyId')
  expect(source).toContain('RecipeRecommendationCard')
  expect(source).toContain('shoppingListRoute')
  expect(source).toContain('暂无符合过敏和忌口约束的安全推荐')
})
```

- [ ] **Step 2: 运行并确认结果页不存在**

Run: `npm run test:unit -- src/views/user/recommendationResultView.test.ts --run`

Expected: FAIL，目标文件不存在。

- [ ] **Step 3: 实现刷新安全的结果页**

校验 `route.query.historyId` 是正整数；无效时显示明确空状态和“重新开始推荐”；有效时调用 `getRecommendationHistory`。页面只展示本次条件、AI 摘要、菜谱卡和后续操作，不渲染聊天输入。

`RecipeRecommendationCard` 接收历史菜谱、目标人数和已有食材，展示真实图片、营养、匹配原因、“已利用食材”和“仍需购买”，并提供详情、收藏与购物清单入口。列表为空时展示“暂无符合过敏和忌口约束的安全推荐”，只提供“修改条件”和“重新开始推荐”，不在前端删除硬约束或伪造兜底菜谱。第二份联网证据计划再扩展个性化好处与来源。

- [ ] **Step 4: 修正历史页跳转**

推荐历史中的“查看本次结果”跳转到 `recommendationResultRoute(history.id)`，而不是在历史列表原地混排详情。

- [ ] **Step 5: 运行测试和构建**

Run: `npm run test:unit -- src/views/user/recommendationResultView.test.ts --run`

Expected: PASS。

Run: `npm run build`

Expected: 成功，无 TypeScript 错误。

- [ ] **Step 6: 提交结果页**

```bash
git add frontend/src/views/user/RecommendationResultView.vue frontend/src/components/recommendation/RecipeRecommendationCard.vue frontend/src/views/user/RecommendationHistoryView.vue frontend/src/views/user/recommendationResultView.test.ts
git commit -m "feat: 添加独立推荐结果页"
```

## Task 10: 核心链路回归验证

**Files:**
- Modify only if verification reveals a regression.

- [ ] **Step 1: 运行后端全量测试**

Run: `mvn -B -ntp test`

Expected: 所有测试通过，0 failures，0 errors。

- [ ] **Step 2: 运行前端全量单测**

Run: `npm run test:unit -- --run`

Expected: 所有测试文件通过。

- [ ] **Step 3: 运行前端生产构建**

Run: `npm run build`

Expected: `vue-tsc` 与 Vite 构建成功。

- [ ] **Step 4: 手动联调主链路**

启动后端 8081 与前端开发服务器，验证：登录后创建对话；一次输入多个条件；第二次乱码出现快捷选项、第三次乱码出现重新开始；刷新恢复；修改确认卡；确认后 URL 含 `historyId`；结果页刷新后仍显示相同的匹配/缺少食材；全部菜谱被过敏或忌口阻断时显示安全空结果；查看详情与生成购物清单仍可用。

- [ ] **Step 5: 提交仅由联调发现的必要修复**

```bash
git add backend frontend
git commit -m "fix: 修复推荐对话联调问题"
```

若联调未产生修改，不创建空提交。
