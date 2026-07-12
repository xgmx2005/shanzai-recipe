import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const recommendSource = readFileSync(fileURLToPath(new URL('./RecommendView.vue', import.meta.url)), 'utf-8')
const summarySource = readFileSync(
  fileURLToPath(new URL('../../components/recommendation/RecommendationConditionSummary.vue', import.meta.url)),
  'utf-8',
)
const messageListSource = readFileSync(
  fileURLToPath(new URL('../../components/recommendation/RecommendationMessageList.vue', import.meta.url)),
  'utf-8',
)
const composerSource = readFileSync(
  fileURLToPath(new URL('../../components/recommendation/RecommendationComposer.vue', import.meta.url)),
  'utf-8',
)
const layoutSource = readFileSync(fileURLToPath(new URL('../../layouts/UserLayout.vue', import.meta.url)), 'utf-8')
const cookingOverlayStyle = layoutSource.slice(
  layoutSource.indexOf('.route-cooking-overlay {'),
  layoutSource.indexOf('.route-cooking-scene {'),
)

describe('recommendation conversation experience', () => {
  it('makes the agent prompt and composer the visual priority', () => {
    expect(recommendSource).toContain('agent-prompt')
    expect(recommendSource).toContain('conversation-workspace')
    expect(recommendSource).toContain('conversation-main')
    expect(recommendSource).toContain('condition-dock')
    expect(recommendSource).toContain('RecommendationComposer')
    expect(recommendSource.indexOf('RecommendationComposer')).toBeLessThan(
      recommendSource.indexOf('RecommendationConditionSummary'),
    )
    expect(summarySource).toContain('compact-summary')
    expect(summarySource).toContain('position: sticky')
  })

  it('uses the account avatar text for user messages', () => {
    expect(messageListSource).toContain('userAvatarText')
    expect(messageListSource).not.toContain('<UserRound')
  })

  it('renders agent responses with optimistic sending, thinking, and streamed reveal states', () => {
    expect(recommendSource).toContain('pendingUserMessage')
    expect(recommendSource).toContain('agentThinking')
    expect(recommendSource).toContain('streamAssistantContent')
    expect(recommendSource).toContain('displayMessages')
    expect(messageListSource).toContain('streamingMessageId')
    expect(messageListSource).toContain('is-streaming')
  })

  it('treats quick options as removable chips inside the composer instead of sending immediately', () => {
    expect(composerSource).toContain('selectedOptions')
    expect(composerSource).toContain('toggleOption')
    expect(composerSource).toContain('removeOption')
    expect(composerSource).toContain('selected-option-chips')
    expect(composerSource).toContain('combinedContent')
    expect(composerSource).not.toContain('submit(option)')
  })

  it('lets users manually edit recognized recommendation conditions from the summary card', () => {
    expect(summarySource).toContain('editing')
    expect(summarySource).toContain('draft')
    expect(summarySource).toContain('saveConditions')
    expect(summarySource).toContain('cancelEdit')
    expect(summarySource).toContain('condition-editor')
    expect(recommendSource).toContain('patchConversationContext')
    expect(recommendSource).toContain('saveConditions')
  })

  it('renders a safe diet goal label and an agent-side generate action when conditions are ready', () => {
    expect(summarySource).toContain('goalLabel')
    expect(summarySource).not.toContain("goalLabels[context.dietGoal]")
    expect(messageListSource).toContain('showGenerateAction')
    expect(messageListSource).toContain('generate-action')
    expect(messageListSource).toContain('去生成')
    expect(recommendSource).toContain('canGenerateRecommendation')
    expect(recommendSource).toContain('/4 条件已明确')
    expect(recommendSource).not.toContain('context.availableIngredients.length > 0')
    expect(summarySource).toContain('未指定已有食材，将按目标和时间推荐')
  })

  it('uses a branded health-profile reminder instead of the default blue info alert', () => {
    expect(recommendSource).toContain('profile-reminder-alert')
    expect(recommendSource).toContain('profile-reminder-icon')
    expect(recommendSource).toContain('profile-reminder-action')
    expect(recommendSource).toContain('健康档案未完善')
    expect(recommendSource).not.toContain('type="info"')
  })

  it('uses one full-screen generation transition before navigating to recommendation results', () => {
    expect(recommendSource).toContain('generationConditionSummary')
    expect(recommendSource).toContain('useRecommendationTransitionStore')
    expect(recommendSource).toContain('routeTransition.start(generationConditionSummary.value)')
    expect(recommendSource).toContain('routeTransition.markServing()')
    expect(recommendSource).toContain('routeTransition.finish()')
    expect(recommendSource).not.toContain('generation-overlay')
    expect(recommendSource).not.toContain('generation-card')
    expect(recommendSource).toContain('await sleep(420)')
    expect(recommendSource.indexOf('routeTransition.start(generationConditionSummary.value)')).toBeLessThan(
      recommendSource.indexOf('confirmConversation(conversation.value.id)'),
    )
    expect(recommendSource.indexOf('await sleep(420)')).toBeLessThan(
      recommendSource.indexOf('recommendationResultRoute(result.historyId)'),
    )
  })

  it('keeps a full-screen cooking transition alive across the recommendation result route change', () => {
    expect(layoutSource).toContain('useRecommendationTransitionStore')
    expect(layoutSource).toContain('routeTransition.markServing()')
    expect(layoutSource).toContain('routeTransition.finish()')
    expect(layoutSource).toContain('route-cooking-overlay')
    expect(layoutSource).toContain('route-cooking-scene')
    expect(layoutSource).toContain('cooking-bench')
    expect(layoutSource).toContain('route-cooking-progress')
    expect(layoutSource).toContain('cooking-progress')
    expect(layoutSource).toContain('正在为你规划这一餐')
    expect(layoutSource).toContain('理解饮食目标')
    expect(layoutSource).toContain('匹配知识库菜谱')
    expect(layoutSource).toContain('生成推荐理由')
    expect(layoutSource).toContain('generating-step')
    expect(layoutSource).toContain('cooking-step-light')
    expect(layoutSource).toContain('正在打开推荐结果')
    expect(cookingOverlayStyle).not.toContain('backdrop-filter')
  })
})
