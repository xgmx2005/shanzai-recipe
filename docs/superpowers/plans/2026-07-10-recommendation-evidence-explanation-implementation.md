# 推荐联网证据讲解 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为已经由知识库选出的真实菜谱生成带有效来源的个性化好处讲解，并把讲解与来源快照保存到推荐历史。

**Architecture:** DeepSeek 只发起搜索意图和基于证据生成结构化讲解；Spring 后端通过 Tavily Search API 执行搜索、白名单过滤和证据 ID 校验。任一联网步骤失败时保留真实菜谱结果并回退知识库说明，历史页读取保存快照而不重复联网。

**Tech Stack:** Java 17、Spring Boot RestClient、Jackson、MyBatis-Plus、MySQL JSON、DeepSeek Tool Calls/JSON Output、Tavily Search API、JUnit 5、MockRestServiceServer、Vue 3、TypeScript、Vitest。

---

## 执行前提

先完整执行 `2026-07-10-conversational-recommendation-core-implementation.md` 并通过其全量回归；本计划依赖其中新增的 `RecommendationResultView.vue`、结构化结果快照和安全空结果。

## 文件结构

### 后端新增

- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/HealthEvidenceSearchClient.java`：搜索边界。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/TavilyHealthEvidenceSearchClient.java`：Tavily HTTP 适配器。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/DisabledHealthEvidenceSearchClient.java`：无 Key 降级。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSource.java`：证据快照。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSourcePolicy.java`：协议、域名和字段过滤。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceContext.java`：用户目标与菜谱事实。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceAnalysis.java`：讲解与状态。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSearchToolExecutor.java`：DeepSeek Tool Call 的受控执行边界。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceExplanationClient.java`：DeepSeek 讲解边界。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/HttpEvidenceExplanationClient.java`：DeepSeek 证据讲解。
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceService.java`：搜索、验证和降级编排。
- `backend/src/main/resources/db/migrations/2026-07-10-add-recommendation-evidence.sql`：历史字段迁移。

### 前端新增

- `frontend/src/components/recommendation/RecommendationEvidencePanel.vue`：动态讲解与可展开来源。

### 主要修改

- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationResponse.java`
- `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/schema.sql`
- `frontend/src/types.ts`
- `frontend/src/views/user/RecommendationResultView.vue`

## Task 1: 推荐历史证据字段

**Files:**
- Create: `backend/src/main/resources/db/migrations/2026-07-10-add-recommendation-evidence.sql`
- Modify: `backend/src/main/resources/db/schema.sql`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationEvidenceSchemaTest.java`

- [ ] **Step 1: 写失败的字段测试**

```java
@Test
void recommendationHistoryStoresEvidenceSnapshotAndStatus() throws IOException {
    String schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));
    assertTrue(schema.contains("ai_benefit_analysis JSON"));
    assertTrue(schema.contains("ai_evidence_sources JSON"));
    assertTrue(schema.contains("ai_evidence_status VARCHAR(20)"));
    assertTrue(schema.contains("ai_evidence_fallback_reason VARCHAR(255)"));
}
```

- [ ] **Step 2: 运行并确认字段不存在**

Run: `mvn -B -ntp -Dtest=RecommendationEvidenceSchemaTest test`

Expected: FAIL，首个 JSON 字段断言失败。

- [ ] **Step 3: 添加迁移与实体字段**

```sql
ALTER TABLE recommendation_history
    ADD COLUMN ai_benefit_analysis JSON NULL AFTER ai_generated,
    ADD COLUMN ai_evidence_sources JSON NULL AFTER ai_benefit_analysis,
    ADD COLUMN ai_evidence_status VARCHAR(20) NOT NULL DEFAULT 'FALLBACK' AFTER ai_evidence_sources,
    ADD COLUMN ai_evidence_fallback_reason VARCHAR(255) NULL AFTER ai_evidence_status;
```

在 `schema.sql` 的建表语句中加入相同字段；实体中使用 `String aiBenefitAnalysis`、`String aiEvidenceSources`、`String aiEvidenceStatus`、`String aiEvidenceFallbackReason` 及完整访问器，交给 Jackson 负责 JSON 序列化。

- [ ] **Step 4: 运行测试并提交**

Run: `mvn -B -ntp -Dtest=RecommendationEvidenceSchemaTest test`

Expected: PASS。

```bash
git add backend/src/main/resources/db backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryEntity.java backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationEvidenceSchemaTest.java
git commit -m "feat: 扩展推荐讲解证据字段"
```

## Task 2: Tavily 搜索客户端

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/HealthEvidenceSearchClient.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/TavilyHealthEvidenceSearchClient.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/DisabledHealthEvidenceSearchClient.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSource.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/TavilyHealthEvidenceSearchClientTest.java`

- [ ] **Step 1: 写 HTTP 搜索失败测试**

使用 `MockRestServiceServer` 返回：

```json
{
  "results": [
    {
      "title": "Protein and healthy eating",
      "url": "https://www.hsph.harvard.edu/nutritionsource/what-should-you-eat/protein/",
      "content": "Protein is found throughout the body and is part of many foods.",
      "score": 0.91
    }
  ]
}
```

断言请求为 `POST https://api.tavily.com/search`，Bearer 头来自配置，body 包含 `search_depth: basic`、`include_answer: false`、`include_raw_content: false`、`max_results: 5` 与 `include_domains`。

- [ ] **Step 2: 运行并确认客户端不存在**

Run: `mvn -B -ntp -Dtest=TavilyHealthEvidenceSearchClientTest test`

Expected: FAIL，目标类型不存在。

- [ ] **Step 3: 定义搜索边界**

```java
public interface HealthEvidenceSearchClient {
    List<EvidenceSource> search(String query, List<String> allowedDomains);
}

public record EvidenceSource(
    String id,
    String title,
    String organization,
    String url,
    String publishedDate,
    String accessedAt,
    String snippet
) {
}
```

Tavily 客户端按结果顺序生成 `source-1` 至 `source-5`，`organization` 从主机名映射；发布日期不可获得时保持 `null`。API Key 为空时由 Spring 条件配置注入 `DisabledHealthEvidenceSearchClient` 并返回空列表。

- [ ] **Step 4: 增加配置**

```yaml
app:
  evidence:
    tavily:
      base-url: https://api.tavily.com
      api-key: ${TAVILY_API_KEY:}
      connect-timeout: 2s
      read-timeout: 5s
    allowed-domains:
      - nhc.gov.cn
      - chinanutri.cn
      - who.int
      - nih.gov
      - hsph.harvard.edu
      - usda.gov
```

- [ ] **Step 5: 运行搜索客户端测试并提交**

Run: `mvn -B -ntp -Dtest=TavilyHealthEvidenceSearchClientTest test`

Expected: PASS，合法响应和空 Key 降级通过。

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/TavilyHealthEvidenceSearchClientTest.java backend/src/main/resources/application.yml
git commit -m "feat: 接入健康资料搜索客户端"
```

## Task 3: 来源策略与证据去重

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSourcePolicy.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSourcePolicyTest.java`

- [ ] **Step 1: 写安全过滤失败测试**

```java
@Test
void keepsHttpsAllowedSourcesAndDeduplicatesUrls() {
    EvidenceSourcePolicy policy = new EvidenceSourcePolicy(List.of("nih.gov", "who.int"));
    List<EvidenceSource> filtered = policy.filter(List.of(
        source("https://www.nih.gov/a", "NIH", "有效片段"),
        source("https://www.nih.gov/a", "NIH duplicate", "重复片段"),
        source("http://www.who.int/b", "WHO insecure", "片段"),
        source("https://shop.example.com/c", "Shop", "营销内容")
    ));
    assertEquals(1, filtered.size());
    assertEquals("https://www.nih.gov/a", filtered.get(0).url());
}
```

- [ ] **Step 2: 运行并确认策略不存在**

Run: `mvn -B -ntp -Dtest=EvidenceSourcePolicyTest test`

Expected: FAIL。

- [ ] **Step 3: 实现过滤规则**

`filter` 仅保留 HTTPS、主机等于允许域或其子域、标题和片段非空的结果；按规范化 URL 去重；重新编号为连续 `source-1`、`source-2`；最多保留 8 条。禁止通过字符串 `endsWith("nih.gov")` 接受 `fakenih.gov`，必须使用 `host.equals(domain) || host.endsWith("." + domain)`。

- [ ] **Step 4: 运行并提交**

Run: `mvn -B -ntp -Dtest=EvidenceSourcePolicyTest test`

Expected: PASS。

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSourcePolicy.java backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSourcePolicyTest.java
git commit -m "feat: 添加推荐证据来源过滤"
```

## Task 4: DeepSeek 证据讲解客户端

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceContext.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceAnalysis.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceSearchToolExecutor.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/EvidenceExplanationClient.java`
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/HttpEvidenceExplanationClient.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/HttpEvidenceExplanationClientTest.java`

- [ ] **Step 1: 写来源 ID 解析失败测试**

```java
@Test
void executesSearchToolCallAndParsesReferencedEvidence() {
    when(toolExecutor.search("高蛋白饮食 权威营养资料"))
        .thenReturn(List.of(source("source-1")));
    when(toolExecutor.sources()).thenReturn(List.of(source("source-1")));

    RecommendationEvidenceAnalysis analysis = client.generate(context(), toolExecutor);

    assertEquals("COMPLETED", analysis.status());
    assertEquals(List.of("source-1"), analysis.recipes().get(0).benefits().get(0).evidenceIds());
    verify(toolExecutor).search("高蛋白饮食 权威营养资料");
}

@Test
void rejectsHallucinatedEvidenceIds() {
    when(toolExecutor.search(anyString())).thenReturn(List.of(source("source-1")));
    when(toolExecutor.sources()).thenReturn(List.of(source("source-1")));
    RecommendationEvidenceAnalysis analysis = client.generate(context(), toolExecutor);
    assertEquals("FALLBACK", analysis.status());
    assertTrue(analysis.recipes().isEmpty());
}
```

第一个测试给 `MockRestServiceServer` 排队两个 DeepSeek 响应：首个响应包含函数名 `search_health_evidence`、`tool_call_id=call-1` 和参数 `{"query":"高蛋白饮食 权威营养资料"}`；第二个响应是引用 `source-1` 的结构化讲解。断言第二次请求 messages 中存在 `role=tool`、`tool_call_id=call-1` 和经过白名单过滤的来源 JSON。第二个测试让最终响应引用 `source-99`。

- [ ] **Step 2: 运行并确认客户端不存在**

Run: `mvn -B -ntp -Dtest=HttpEvidenceExplanationClientTest test`

Expected: FAIL。

- [ ] **Step 3: 定义结构化讲解**

```java
public record RecommendationEvidenceAnalysis(
    String status,
    String overview,
    List<RecipeExplanation> recipes,
    List<EvidenceSource> sources,
    String fallbackReason
) {
    public record RecipeExplanation(
        Long recipeId,
        String personalizedReason,
        List<BenefitExplanation> benefits
    ) {
    }

    public record BenefitExplanation(String text, List<String> evidenceIds) {
    }
}

public interface EvidenceSearchToolExecutor {
    List<EvidenceSource> search(String query);
    List<EvidenceSource> sources();
}

public interface EvidenceExplanationClient {
    RecommendationEvidenceAnalysis generate(
        RecommendationEvidenceContext context,
        EvidenceSearchToolExecutor toolExecutor
    );
}
```

- [ ] **Step 4: 实现 DeepSeek Tool Calls 循环与 JSON 验证**

第一次 `/chat/completions` 请求声明唯一函数工具：

```json
{
  "type": "function",
  "function": {
    "name": "search_health_evidence",
    "description": "检索支持已推荐菜谱营养讲解的权威健康资料",
    "parameters": {
      "type": "object",
      "properties": { "query": { "type": "string", "maxLength": 120 } },
      "required": ["query"],
      "additionalProperties": false
    }
  }
}
```

设置 `tool_choice: required`。只接受函数名完全等于 `search_health_evidence` 的调用，去重并最多执行 3 个 query；每个 query 必须 4 至 120 字符。执行 `toolExecutor.search(query)` 后，如果最终没有有效来源立即返回 `FALLBACK`；否则把来源数组作为对应 `tool_call_id` 的 `role=tool` 消息追加到原会话，再发第二次 `/chat/completions`，设置 `response_format: {"type":"json_object"}` 并要求最终结构化讲解。

系统提示明确：菜谱和营养由数据库确定；只依据 tool 消息中的证据；每个 benefit 至少一个 evidenceId；禁止医疗化结论；不得新增菜谱、热量或食材。解析后验证 recipeId 属于上下文、evidenceId 属于 `toolExecutor.sources()`、文本非空，并拒绝包含“治疗”“治愈”“降血糖”“预防疾病”“替代药物”的讲解。任一响应超时、工具参数非法、无有效来源或 JSON 非法时返回 `FALLBACK`，不得抛出到推荐主流程。

- [ ] **Step 5: 运行并提交**

Run: `mvn -B -ntp -Dtest=HttpEvidenceExplanationClientTest test`

Expected: PASS，Tool Call 往返、合法引用、伪造引用和超限工具调用均通过。

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/HttpEvidenceExplanationClientTest.java
git commit -m "feat: 添加带来源的AI推荐讲解"
```

## Task 5: 证据编排与推荐持久化

**Files:**
- Create: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationResponse.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryService.java`
- Modify: `backend/src/main/java/com/shanzai/recipe/modules/recommendation/RecommendationHistoryDetailResponse.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/evidence/RecommendationEvidenceServiceTest.java`
- Test: `backend/src/test/java/com/shanzai/recipe/modules/recommendation/RecommendationServiceTest.java`

- [ ] **Step 1: 写三次搜索上限和降级失败测试**

```java
@Test
void searchesAtMostThreeQueriesAndFallsBackWithoutSources() {
    when(explanationClient.generate(any(), any())).thenAnswer(invocation -> {
        EvidenceSearchToolExecutor executor = invocation.getArgument(1);
        executor.search("query-1 权威营养资料");
        executor.search("query-2 权威营养资料");
        executor.search("query-3 权威营养资料");
        executor.search("query-4 权威营养资料");
        return new RecommendationEvidenceAnalysis(
            "FALLBACK", "", List.of(), executor.sources(), "未获取到允许来源"
        );
    });
    RecommendationEvidenceService service = serviceWithEmptySearch();
    RecommendationEvidenceAnalysis analysis = service.generate(contextWithFiveRecipes());
    assertEquals("FALLBACK", analysis.status());
    assertEquals("未获取到允许来源", analysis.fallbackReason());
    verify(searchClient, atMost(3)).search(anyString(), anyList());
}
```

- [ ] **Step 2: 运行并确认服务不存在**

Run: `mvn -B -ntp -Dtest=RecommendationEvidenceServiceTest test`

Expected: FAIL。

- [ ] **Step 3: 实现受控工具执行器**

`generate` 只把前 3 道推荐菜谱的名称、每人营养和目标放入 `RecommendationEvidenceContext`，不包含用户姓名、体重、原始对话或完整健康档案。它创建一次 `BoundedEvidenceSearchToolExecutor` 并传给 `EvidenceExplanationClient.generate`。

执行器对 `search(query)` 做四层限制：最多 3 次；query 去空白后长度 4 至 120；重复 query 只复用结果；每次调用 `HealthEvidenceSearchClient.search(query, allowedDomains)` 后把累计结果交给 `EvidenceSourcePolicy.filter`，以首次出现顺序重新编号。`sources()` 返回不可变的最终快照。第 4 次调用直接返回空列表且不访问 Tavily。讲解客户端返回 `COMPLETED` 但 `sources()` 为空时，服务强制改为 `FALLBACK`。

- [ ] **Step 4: 集成推荐服务并保存快照**

在 `RecommendationService.recommend` 完成规则排序后调用一次 `evidenceService.generate`；推荐列表为空时跳过 DeepSeek 和 Tavily，保留核心计划定义的安全空结果。`saveHistory` 用 `ObjectMapper` 保存 analysis 和 sources；`RecommendationResponse`、历史详情响应增加 `aiBenefitAnalysis`、`aiEvidenceSources`、`aiEvidenceStatus`、`aiEvidenceFallbackReason`。

保留原 `aiSummary`、`aiHealthTip` 和 `aiShoppingTip` 以兼容首页和历史列表。联网讲解失败不得让 `recommend` 抛错。

- [ ] **Step 5: 运行推荐与证据测试**

Run: `mvn -B -ntp -Dtest='RecommendationEvidenceServiceTest,RecommendationServiceTest,RecommendationHistoryServiceTest' test`

Expected: PASS；推荐服务只执行一次证据编排；历史可以恢复同一快照。

- [ ] **Step 6: 提交编排**

```bash
git add backend/src/main/java/com/shanzai/recipe/modules/recommendation backend/src/test/java/com/shanzai/recipe/modules/recommendation
git commit -m "feat: 保存推荐联网讲解与来源快照"
```

## Task 6: 前端讲解与来源面板

**Files:**
- Modify: `frontend/src/types.ts`
- Create: `frontend/src/components/recommendation/RecommendationEvidencePanel.vue`
- Modify: `frontend/src/views/user/RecommendationResultView.vue`
- Test: `frontend/src/views/user/recommendationEvidencePanel.test.ts`

- [ ] **Step 1: 写失败的来源展示测试**

```ts
it('renders evidence status and source disclosure on the result page', () => {
  const source = readFileSync(new URL('./RecommendationResultView.vue', import.meta.url), 'utf8')
  expect(source).toContain('RecommendationEvidencePanel')
  expect(source).toContain('aiEvidenceStatus')
  expect(source).toContain('aiEvidenceSources')
})
```

- [ ] **Step 2: 运行并确认组件不存在**

Run: `npm run test:unit -- src/views/user/recommendationEvidencePanel.test.ts --run`

Expected: FAIL。

- [ ] **Step 3: 增加前端类型**

```ts
export interface RecommendationEvidenceSource {
  id: string
  title: string
  organization: string
  url: string
  publishedDate?: string
  accessedAt: string
  snippet: string
}

export interface RecommendationBenefit {
  text: string
  evidenceIds: string[]
}
```

历史详情增加结构化 `aiBenefitAnalysis`、来源数组、状态与降级原因。

- [ ] **Step 4: 实现面板**

`COMPLETED` 时显示总述、每道菜的个性化理由和好处；好处后的引用编号点击后定位来源。来源使用普通 `<a target="_blank" rel="noopener noreferrer">`。`FALLBACK` 时显示知识库固定说明和“本次未获取外部参考资料”，不渲染虚假来源区域。

布局使用全宽 `sz-panel`，来源默认折叠；不将 AI 面板放进菜谱卡内部。

- [ ] **Step 5: 运行测试与构建**

Run: `npm run test:unit -- src/views/user/recommendationEvidencePanel.test.ts --run`

Expected: PASS。

Run: `npm run build`

Expected: 成功。

- [ ] **Step 6: 提交前端面板**

```bash
git add frontend/src/types.ts frontend/src/components/recommendation/RecommendationEvidencePanel.vue frontend/src/views/user/RecommendationResultView.vue frontend/src/views/user/recommendationEvidencePanel.test.ts
git commit -m "feat: 展示推荐联网讲解与出处"
```

## Task 7: 联网讲解回归验证

**Files:**
- Modify only if verification reveals a regression.

- [ ] **Step 1: 运行后端全量测试**

Run: `mvn -B -ntp test`

Expected: 全部测试通过，0 failures，0 errors。

- [ ] **Step 2: 运行前端单测与构建**

Run: `npm run test:unit -- --run`

Expected: 全部通过。

Run: `npm run build`

Expected: 成功。

- [ ] **Step 3: 验证三种运行状态**

1. 同时配置 `DEEPSEEK_API_KEY` 与 `TAVILY_API_KEY`：结果页出现带来源讲解，日志记录成功但不记录 Key。
2. 只配置 DeepSeek：真实菜谱正常，证据状态为 `FALLBACK`。
3. 两个 Key 都不配置：对话和推荐仍可使用本地规则完成。

- [ ] **Step 4: 检查安全边界**

验证结果页没有 `javascript:` 链接；白名单外 URL 不保存；刷新结果页不产生新的 Tavily 请求；历史详情返回与首次生成相同的来源快照。

- [ ] **Step 5: 提交仅由联调发现的必要修复**

```bash
git add backend frontend
git commit -m "fix: 修复推荐证据链联调问题"
```

若没有代码变化，不创建空提交。
