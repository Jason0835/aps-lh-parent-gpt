// store/modules/globalList.js
import {
  selectSkuStructure,

} from "@/api/monthplan/skuStructure";
const state = {
  structureList: [],

}

const mutations = {
  SET_LIST(state, list) {
    state.structureList = list
  },

}

const actions = {
  async fetchGlobalList({ commit }) {
    try {

      const res = await selectSkuStructure({
        pageSize: 1000,
        pageNum: 1,

      });
      let list=[]
      for (let i = 0; i < res.rows.length; i++) {
        let obj={
          label:res.rows[i].structureName,
          value:res.rows[i].structureName
        }
        list.push(obj)

      }

      commit('SET_LIST', list)
      return list
    } catch (error) {
      // commit('SET_ERROR', error.message)
      throw error
    } finally {
      // commit('SET_LOADING', false)
    }
  },


}

const getters = {
  structureList: state => state.structureList,

}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}