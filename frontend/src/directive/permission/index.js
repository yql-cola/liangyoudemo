import store from '@/store'
import { hasAnyPermission } from '@/utils/permission'

function inserted(el, binding) {
  const value = binding.value
  const required = Array.isArray(value) ? value : [value]
  const permissions = store.state.user.permissions

  if (!hasAnyPermission(permissions, required)) {
    el.parentNode && el.parentNode.removeChild(el)
  }
}

export default {
  install(Vue) {
    Vue.directive('permission', { inserted })
  }
}
