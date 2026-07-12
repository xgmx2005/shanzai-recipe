import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('RecommendationHistoryView', () => {
  it('presents recommendation records as a refined logbook with a reusable report panel', () => {
    const source = readFileSync(fileURLToPath(new URL('./RecommendationHistoryView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('history-record-book')
    expect(source).toContain('record-card')
    expect(source).toContain('recommendation-report')
    expect(source).toContain('report-actions')
    expect(source).toContain('report-section')
    expect(source).toContain('report-condition-grid')
    expect(source).toContain('report-recipe-grid')
    expect(source).toContain('empty-history')
    expect(source).not.toContain('stats-strip')
    expect(source).not.toContain('detail-metrics')
    expect(source).not.toContain('history-timeline')
    expect(source).not.toContain('timeline-item')
  })
})
