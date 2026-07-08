import { http } from './http'
import type {
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
