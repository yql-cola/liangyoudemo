import { normalizeApiResponse } from '@/utils/request'

describe('request response normalization', () => {
  test('returns data when backend code is zero', () => {
    expect(normalizeApiResponse({ code: 0, message: 'success', data: { id: 1 } })).toEqual({ id: 1 })
  })

  test('throws backend message when code is non-zero', () => {
    expect(() => normalizeApiResponse({ code: 400, message: 'validation-error', data: null }))
      .toThrow('validation-error')
  })
})
