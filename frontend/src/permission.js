import router from './router'
import store from './store'
import { getToken } from '@/utils/auth'
import { hasPermission } from '@/utils/permission'

const whiteList = ['/login', '/401', '/404']

function routeAllowed(route) {
  const required = route.meta && route.meta.permission
  return hasPermission(store.state.user.permissions, required)
}

router.beforeEach(async(to, from, next) => {
  const token = getToken()

  if (!token) {
    if (whiteList.includes(to.path)) {
      next()
      return
    }
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  if (!store.state.user.id) {
    try {
      await store.dispatch('user/getInfo')
    } catch (error) {
      await store.dispatch('user/logout')
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }
  }

  if (!routeAllowed(to)) {
    next('/401')
    return
  }

  next()
})
