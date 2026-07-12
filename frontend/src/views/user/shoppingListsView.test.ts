import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('ShoppingListsView', () => {
  it('organizes shopping items by category with clear progress and bulk actions', () => {
    const source = readFileSync(fileURLToPath(new URL('./ShoppingListsView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('remainingItemCount')
    expect(source).toContain('groupedItems')
    expect(source).toContain('markAllItems')
    expect(source).toContain('全部买好了')
    expect(source).toContain('全部标记未买')
    expect(source).toContain('category-section')
    expect(source).toContain('未买')
    expect(source).toContain('已买')
  })

  it('keeps hide-purchased as a normal list control without an extra purchase mode', () => {
    const source = readFileSync(fileURLToPath(new URL('./ShoppingListsView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('hidePurchasedItems')
    expect(source).toContain('visibleGroupedItems')
    expect(source).toContain('隐藏已买')
    expect(source).toContain('list-toolbar')
    expect(source).not.toContain('purchaseMode')
    expect(source).not.toContain('采购模式')
    expect(source).not.toContain('退出采购')
    expect(source).not.toContain('purchase-status')
  })

  it('keeps the purchase view restrained and consistent with the project palette', () => {
    const source = readFileSync(fileURLToPath(new URL('./ShoppingListsView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('list-toolbar')
    expect(source).toContain('progressColor')
    expect(source).toContain('progressRailColor')
    expect(source).toContain('var(--sz-green-dark)')
    expect(source).toContain('var(--sz-mint)')
    expect(source).toContain('var(--sz-surface)')
    expect(source).not.toContain('summary-row')
    expect(source).not.toContain('done-card')
  })

  it('keeps the opening layout stable without rewriting the route during initial auto selection', () => {
    const source = readFileSync(fileURLToPath(new URL('./ShoppingListsView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('openList(nextListId, { syncRoute: requestedId !== undefined })')
    expect(source).toContain('options.syncRoute !== false')
    expect(source).toContain('list-loading-shell')
    expect(source).toContain('min-height: min(620px, calc(100vh - 220px))')
    expect(source).toContain('align-self: stretch')
  })
})
