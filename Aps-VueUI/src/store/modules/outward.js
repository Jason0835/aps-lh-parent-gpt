const state = {
  data: new Array()
}
const mutations = {
  SET_SELECT_DATA: (state, data) => {
    state.data = data;
  },
  REMOVE_SELECT_DATA: (state, key) => {
    state.data = [];
  },
}

const actions = {
  // 设置字典
  setData({ commit }, data) {
    commit('SET_SELECT_DATA', data)
  },
  // 删除字典
  removeData({ commit }) {
    commit('REMOVE_SELECT_DATA')
  },

}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

