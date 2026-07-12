import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('FavoritesView', () => {
  it('presents favorites as a refined personal recipe library', () => {
    const source = readFileSync(fileURLToPath(new URL('./FavoritesView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('recipe-library-hero')
    expect(source).toContain('library-overview')
    expect(source).toContain('library-toolbar')
    expect(source).toContain('favorite-card-grid')
    expect(source).toContain('library-recipe-card')
    expect(source).toContain('select-marker')
    expect(source).toContain('empty-library')
    expect(source).toContain('width: 26px')
    expect(source).toContain('height: 26px')
    expect(source).toContain('aspect-ratio: 16 / 10')
    expect(source).toContain('right: 12px')
    expect(source).toContain('bottom: 14px')
    expect(source).toContain('grid-template-columns: repeat(2, minmax(0, 1fr))')
    expect(source).not.toContain('summary-strip')
    expect(source).not.toContain('favorite-row-card')
    expect(source).not.toContain('latestFavoriteTime')
    expect(source).not.toContain('<span>最近收藏</span>')
  })

  it('uses a compact heart action with confirmation before removing a favorite', () => {
    const source = readFileSync(fileURLToPath(new URL('./FavoritesView.vue', import.meta.url)), 'utf8')

    expect(source).toContain('favorite-heart-button')
    expect(source).toContain('requestRemoveFavorite')
    expect(source).toContain('removeFavoriteTarget')
    expect(source).toContain('confirmRemoveFavorite')
    expect(source).toContain('确认取消收藏')
    expect(source).not.toContain('Trash2')
  })
})
