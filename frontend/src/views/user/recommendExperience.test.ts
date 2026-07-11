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
})
