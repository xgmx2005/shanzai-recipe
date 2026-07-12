import { defineStore } from 'pinia'

type RecommendationTransitionPhase = 'preparing' | 'serving'

interface RecommendationTransitionState {
  active: boolean
  phase: RecommendationTransitionPhase
  summary: string
}

export const useRecommendationTransitionStore = defineStore('recommendationTransition', {
  state: (): RecommendationTransitionState => ({
    active: false,
    phase: 'preparing',
    summary: '',
  }),
  actions: {
    start(summary: string) {
      this.active = true
      this.phase = 'preparing'
      this.summary = summary
    },
    markServing() {
      if (!this.active) return
      this.phase = 'serving'
    },
    finish() {
      this.active = false
      this.phase = 'preparing'
      this.summary = ''
    },
  },
})
