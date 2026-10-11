const state = {
  sidebarOpened: true
}

const mutations = {
  TOGGLE_SIDEBAR(state) {
    state.sidebarOpened = !state.sidebarOpened
  }
}

export default {
  namespaced: true,
  state,
  mutations
}
