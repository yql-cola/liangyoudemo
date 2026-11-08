import Cookies from 'js-cookie'
import { getToken, setToken, removeToken } from '@/utils/auth'

describe('tab login storage', () => {
  beforeEach(() => {
    sessionStorage.clear()
    Cookies.remove('Liangyou-Token')
  })

  test('migrates the old cookie once and removes it', () => {
    Cookies.set('Liangyou-Token', 'old-token')
    expect(getToken()).toBe('old-token')
    expect(sessionStorage.getItem('Liangyou-Token')).toBe('old-token')
    expect(Cookies.get('Liangyou-Token')).toBeUndefined()
  })

  test('prefers current tab token and clears old cookie', () => {
    sessionStorage.setItem('Liangyou-Token', 'tab-token')
    Cookies.set('Liangyou-Token', 'old-token')
    expect(getToken()).toBe('tab-token')
    expect(Cookies.get('Liangyou-Token')).toBeUndefined()
  })

  test('login and logout only use tab storage', () => {
    setToken('new-token')
    expect(getToken()).toBe('new-token')
    expect(Cookies.get('Liangyou-Token')).toBeUndefined()
    removeToken()
    expect(getToken()).toBeNull()
  })
})
