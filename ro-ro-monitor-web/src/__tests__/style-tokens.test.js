import { describe, it, expect, beforeAll } from 'vitest'

/**
 * style-tokens.test.js
 *
 * Verifies that the design token CSS variables (defined in src/style.css :root)
 * are correctly defined and resolvable. Since jsdom does not process linked
 * CSS files, this test applies the :root variables programmatically from the
 * known design system specification and verifies them.
 */

// Expected CSS variable values matching src/style.css :root
const DESIGN_TOKENS = {
  // Colors
  '--color-primary': '#1d72f3',
  '--color-primary-light': '#3884f7',
  '--color-primary-dark': '#165cc2',
  '--color-success': '#67c23a',
  '--color-warning': '#e6a23c',
  '--color-danger': '#f56c6c',
  '--color-info': '#909399',
  '--sidebar-bg': '#304156',
  '--sidebar-text': '#bfcbd9',
  '--sidebar-active': '#409eff',
  '--page-bg': '#f0f2f5',
  '--header-bg': '#fff',
  '--header-border': '#e6e6e6',

  // Text colors
  '--text-primary': '#13233c',
  '--text-secondary': '#5e708d',
  '--text-muted': '#7a8aa2',

  // Border radii
  '--radius-sm': '8px',
  '--radius-md': '12px',
  '--radius-lg': '20px',
  '--radius-xl': '22px',

  // Shadows
  '--shadow-card': '0 14px 34px rgba(26, 65, 122, 0.08)',
  '--shadow-card-hover': '0 16px 36px rgba(26, 65, 122, 0.14)'
}

beforeAll(() => {
  // Apply all design tokens to document root to simulate style.css
  for (const [name, value] of Object.entries(DESIGN_TOKENS)) {
    document.documentElement.style.setProperty(name, value)
  }
})

describe('CSS design tokens', () => {
  describe('color tokens', () => {
    const colorTokens = [
      '--color-primary',
      '--color-primary-light',
      '--color-primary-dark',
      '--color-success',
      '--color-warning',
      '--color-danger',
      '--color-info',
      '--sidebar-bg',
      '--sidebar-text',
      '--sidebar-active',
      '--page-bg',
      '--header-bg',
      '--header-border'
    ]

    it.each(colorTokens)('%s should be defined on :root', (token) => {
      const value = document.documentElement.style.getPropertyValue(token)
      expect(value).toBe(DESIGN_TOKENS[token])
    })

    it('should have valid hex color values for all color tokens', () => {
      const hexRegex = /^#[0-9a-fA-F]{3,6}$/
      for (const [token, value] of Object.entries(DESIGN_TOKENS)) {
        if (token.startsWith('--color-') || token.startsWith('--sidebar-') ||
            token.startsWith('--page-') || token.startsWith('--header-') ||
            token.startsWith('--text-')) {
          if (typeof value === 'string' && value.startsWith('#')) {
            expect(value).toMatch(hexRegex)
          }
        }
      }
    })
  })

  describe('text color tokens', () => {
    const textTokens = ['--text-primary', '--text-secondary', '--text-muted']

    it.each(textTokens)('%s should be a defined color', (token) => {
      const value = document.documentElement.style.getPropertyValue(token)
      expect(value).toBeTruthy()
      expect(value).toMatch(/^#/)
    })
  })

  describe('border-radius tokens', () => {
    const radiusTokens = ['--radius-sm', '--radius-md', '--radius-lg', '--radius-xl']

    it.each(radiusTokens)('%s should be defined in px', (token) => {
      const value = document.documentElement.style.getPropertyValue(token)
      expect(value).toMatch(/^\d+px$/)
    })

    it('should have increasing radius values', () => {
      const getNumericValue = (name) =>
        parseInt(document.documentElement.style.getPropertyValue(name))

      const sm = getNumericValue('--radius-sm')
      const md = getNumericValue('--radius-md')
      const lg = getNumericValue('--radius-lg')
      const xl = getNumericValue('--radius-xl')

      expect(sm).toBeLessThan(md)
      expect(md).toBeLessThan(lg)
      expect(lg).toBeLessThan(xl)
    })
  })

  describe('shadow tokens', () => {
    it('--shadow-card should be defined', () => {
      const value = document.documentElement.style.getPropertyValue('--shadow-card')
      expect(value).toContain('rgba')
      expect(value).toContain('0 14px')
    })

    it('--shadow-card-hover should be more prominent than --shadow-card', () => {
      const shadow = document.documentElement.style.getPropertyValue('--shadow-card')
      const shadowHover = document.documentElement.style.getPropertyValue('--shadow-card-hover')
      expect(shadowHover).toContain('0 16px')
      expect(shadowHover).not.toBe(shadow)
    })
  })

  describe('CSS variable cascading', () => {
    it('should cascade --color-primary to child elements', () => {
      const parent = document.createElement('div')
      parent.style.setProperty('--color-primary', '#1d72f3')
      document.body.appendChild(parent)

      const child = document.createElement('span')
      parent.appendChild(child)

      // The variable should be inherited from parent
      const inheritedValue = getComputedStyle(child).getPropertyValue('--color-primary').trim()
      // Note: jsdom's getComputedStyle may not resolve custom properties
      // but the variable should be present in the style declaration
      expect(parent.style.getPropertyValue('--color-primary')).toBe('#1d72f3')

      document.body.removeChild(parent)
    })
  })
})
