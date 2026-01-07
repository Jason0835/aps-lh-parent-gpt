import Vue from 'vue'
import Vuex from 'vuex'
import app from './modules/app'
import dict from './modules/dict'
import user from './modules/user'
import tagsView from './modules/tagsView'
import permission from './modules/permission'
import settings from './modules/settings'
import createPersistedState from 'vuex-persistedstate'

import molding from './modules/molding'
import curing from './modules/curing'
import mix from './modules/mix'

import tread from './modules/semifinished/tread'
import bead from './modules/semifinished/bead'
import beadRing from './modules/semifinished/beadRing'
import cut15 from './modules/semifinished/cut15'
import cut90 from './modules/semifinished/cut90'
import insideLiner from './modules/semifinished/insideLiner'
import sidewall from './modules/semifinished/sidewall'
import fiberPress from './modules/semifinished/fiberPress'
import steelPress from './modules/semifinished/steelPress'
import globalList from './modules/globalList'


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
    globalList,

    // 成型
    molding,
    //硫化
    curing,
    // 半部件
    tread,
    bead,
    beadRing,
    sidewall,
    insideLiner,
    cut15,
    cut90,
    fiberPress,
    steelPress,
    //密炼
    mix,
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
