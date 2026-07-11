import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = readFileSync(fileURLToPath(new URL('./RecommendView.vue', import.meta.url)), 'utf-8')

describe('RecommendView conversation layout', () => {
  it('uses a single conversation-first input page instead of the old form/result split screen', () => {
    expect(source).toContain('RecommendationMessageList')
    expect(source).toContain('RecommendationComposer')
    expect(source).toContain('RecommendationConditionSummary')
    expect(source).toContain('getActiveConversation')
    expect(source).toContain('重新开始')
    expect(source).not.toContain('primary-recipe')
    expect(source).not.toContain('preview-panel')
  })
})
