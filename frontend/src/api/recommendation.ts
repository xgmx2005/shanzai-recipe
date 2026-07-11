import { http } from './http'
import type {
  ConversationContextPatchRequest,
  ConversationMessageRequest,
  ConversationResponse,
  RecommendationHistoryDetail,
  RecommendationHistorySummary,
  RecommendationRequest,
  RecommendationResponse,
} from '@/types'

export function createRecommendation(payload: RecommendationRequest) {
  return http.post<RecommendationResponse>('/recommendations', payload).then((res) => res.data)
}

export function listRecommendationHistory() {
  return http
    .get<RecommendationHistorySummary[]>('/recommendations/history')
    .then((res) => res.data)
}

export function getRecommendationHistory(id: number) {
  return http
    .get<RecommendationHistoryDetail>(`/recommendations/history/${id}`)
    .then((res) => res.data)
}

export function getActiveConversation() {
  return http
    .get<ConversationResponse | null>('/recommendation-conversations/active')
    .then((res) => res.data)
}

export function startConversation(replaceActive = false) {
  return http
    .post<ConversationResponse>('/recommendation-conversations', { replaceActive })
    .then((res) => res.data)
}

export function restartConversation() {
  return startConversation(true)
}

export function getConversation(id: number) {
  return http
    .get<ConversationResponse>(`/recommendation-conversations/${id}`)
    .then((res) => res.data)
}

export function sendConversationMessage(id: number, payload: ConversationMessageRequest) {
  return http
    .post<ConversationResponse>(`/recommendation-conversations/${id}/messages`, payload)
    .then((res) => res.data)
}

export function patchConversationContext(id: number, payload: ConversationContextPatchRequest) {
  return http
    .patch<ConversationResponse>(`/recommendation-conversations/${id}/context`, payload)
    .then((res) => res.data)
}

export function confirmConversation(id: number) {
  return http
    .post<RecommendationResponse>(`/recommendation-conversations/${id}/confirm`)
    .then((res) => res.data)
}
