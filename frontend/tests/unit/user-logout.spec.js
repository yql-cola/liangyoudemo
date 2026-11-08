import { logout } from '@/api/auth'
import user from '@/store/modules/user'
import { setToken, getToken } from '@/utils/auth'

jest.mock('@/api/auth', () => ({ login: jest.fn(), getCurrentUser: jest.fn(), logout: jest.fn() }))

test('failed server logout still clears this tab and lets navigation continue', async () => {
  setToken('token')
  logout.mockRejectedValueOnce(new Error('network'))
  const commit = jest.fn()

  await expect(user.actions.logout({ commit })).resolves.toBeUndefined()

  expect(getToken()).toBeNull()
  expect(commit).toHaveBeenCalledWith('RESET_USER')
})
