const state = {
  shipId: "",
  data: new Array()
}
const mutations = {
  SET_SHIP_ID: (state, shipId) => {
    state.shipId = shipId;
  },
  REMOVE_SHIP_ID: (state, key) => {
    state.shipId = "";
  },
  SET_SELECT_DATA: (state, data) => {
    state.data = data;
  },
  REMOVE_SELECT_DATA: (state, key) => {
    state.data = [];
  },
}

const actions = {
  // 设置选中出运单号
  setShipId({ commit }, shipId) {
    commit('SET_SHIP_ID', shipId)
  },
  // 删除选中出运单号
  removeShipId({ commit }) {
    commit('REMOVE_SHIP_ID')
  },

  // 设置选中商品
  setData({ commit }, data) {
    commit('SET_SELECT_DATA', data)
  },
  // 删除选中商品
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

