import { describe, expect, it } from 'vitest'
import { recommendationResultRoute } from './recommendationConversation'

describe('recommendationResultRoute', () => {
  it('opens the recommendation result page with a persisted history id', () => {
    expect(recommendationResultRoute(42)).toEqual({
      path: '/user/recommend/result',
      query: {
        historyId: '42',
      },
    })
  })
})
