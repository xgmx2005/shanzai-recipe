import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const layoutSource = readFileSync(fileURLToPath(new URL('../../layouts/AdminLayout.vue', import.meta.url)), 'utf-8')
const dashboardSource = readFileSync(fileURLToPath(new URL('./DashboardView.vue', import.meta.url)), 'utf-8')

describe('admin knowledge workbench', () => {
  it('uses a focused admin shell without dead navigation entries', () => {
    expect(layoutSource).toContain('knowledge-admin-shell')
    expect(layoutSource).toContain('知识库维护中心')
    expect(layoutSource).toContain('数据维护工作台')
    expect(layoutSource).not.toContain('系统设置')
    expect(layoutSource).not.toContain('/admin/settings')
  })

  it('presents dashboard data as an operational maintenance overview', () => {
    expect(dashboardSource).toContain('maintenance-dashboard')
    expect(dashboardSource).toContain('maintenance-hero')
    expect(dashboardSource).toContain('maintenance-advice')
    expect(dashboardSource).toContain('知识库健康度')
    expect(dashboardSource).toContain('维护建议')
  })
})
