import { login, logout, getCurrentUser } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

const state = {
  token: getToken(),
  id: null,
  username: '',
  realName: '',
  roles: [],
  permissions: []
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
  },
  SET_USER(state, user) {
    state.id = user.id
    state.username = user.username
    state.realName = user.realName
    state.roles = user.roles || []
    state.permissions = user.permissions || []
  },
  RESET_USER(state) {
    state.token = ''
    state.id = null
    state.username = ''
    state.realName = ''
    state.roles = []
    state.permissions = []
  }
}

const actions = {
  async login({ commit }, form) {
    const data = await login(form)
    setToken(data.token)
    commit('SET_TOKEN', data.token)
    commit('SET_USER', data.userInfo)
  },
  async getInfo({ commit }) {
    const data = await getCurrentUser()
    commit('SET_USER', data)
    return data
  },
  async logout({ commit }) {
    try {
      await logout()
    } finally {
      removeToken()
      commit('RESET_USER')
    }
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
