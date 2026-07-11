import { beforeEach, describe, expect, it, vi } from 'vitest'

import { confirmConversation, restartConversation, sendConversationMessage } from './recommendation'
import { http } from './http'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('recommendation conversation api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('restarts conversation with replaceActive flag', async () => {
    vi.mocked(http.post).mockResolvedValueOnce({ data: { id: 10 } })

    const response = await restartConversation()

    expect(http.post).toHaveBeenCalledWith('/recommendation-conversations', { replaceActive: true })
    expect(response).toEqual({ id: 10 })
  })

  it('sends message and confirms using conversation endpoints', async () => {
    vi.mocked(http.post)
      .mockResolvedValueOnce({ data: { id: 10, stage: 'INGREDIENTS' } })
      .mockResolvedValueOnce({ data: { historyId: 99 } })

    await sendConversationMessage(10, { content: '鸡胸肉 300g', clientMessageId: 'msg-1' })
    await confirmConversation(10)

    expect(http.post).toHaveBeenNthCalledWith(1, '/recommendation-conversations/10/messages', {
      content: '鸡胸肉 300g',
      clientMessageId: 'msg-1',
    })
    expect(http.post).toHaveBeenNthCalledWith(2, '/recommendation-conversations/10/confirm')
  })
})
