import Vue from 'vue'
import Vuex from 'vuex'
import app from './modules/app'
import dict from './modules/dict'
import user from './modules/user'
import tagsView from './modules/tagsView'
import permission from './modules/permission'
import settings from './modules/settings'
import outward from './modules/outward'
import overseasWaybill from './modules/overseasWaybill'
import createPersistedState from 'vuex-persistedstate'

import getters from './getters'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: {
    app,
    dict,
    user,
    tagsView,
    permission,
    settings,
    outward,
    overseasWaybill
  },
  getters,
  plugins: [
    createPersistedState({
      // 持久化配置项
      key: 'vuex-persistence',
      paths: ['baseData']
    })
  ]
})

export default store
