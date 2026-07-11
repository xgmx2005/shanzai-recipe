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
})
