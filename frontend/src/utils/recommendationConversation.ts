export function recommendationResultRoute(historyId: number) {
  return {
    path: '/user/recommend/result',
    query: {
      historyId: String(historyId),
    },
  }
}
