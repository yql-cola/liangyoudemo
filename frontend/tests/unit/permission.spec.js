import { hasPermission } from '@/utils/permission'

describe('permission helpers', () => {
  test('allows access when user has required permission', () => {
    expect(hasPermission(['system:user:create'], 'system:user:create')).toBe(true)
  })

  test('denies access when required permission is missing', () => {
    expect(hasPermission(['system:user:list'], 'system:user:create')).toBe(false)
  })

  test('allows super admin wildcard permission', () => {
    expect(hasPermission(['*:*:*'], 'inventory:stock:update')).toBe(true)
  })
})
