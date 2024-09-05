
/* eslint-disable no-array-constructor */
import { getDicts as getDicts } from '@/api/system/dict/data'

const DICT_CACHE_TIME = 24 * 60 * 60 * 1000 // 本地字典缓存有效期（单位毫秒）

function searchDictByKey(dict, key, lang) {
  if (key == null && key === '') {
    return null
  }
  try {
    for (let i = 0; i < dict.length; i++) {
      if (dict[i].key === key && dict[i].lang === lang) {
        return dict[i].value
      }
    }
  } catch (e) {
    return null
  }
}

function setLocalhost(data) {
  try {
    localStorage.setItem('DICT_DATA', typeof data === "string" ? data : JSON.stringify(data))
  }catch (e) {
    console.log(e)
  }
}

function getLocalhost() {
  let data = new Array()
  const str = localStorage.getItem('DICT_DATA')
  if (str) {
    try {
      data = JSON.parse(str)
    }catch (e) {
      console.log(e)
    }
  }
  return data
}

const state = {
  dict: getLocalhost()
}
const mutations = {
  SET_DICT: (state, { key, value, lang }) => {
    // console.log(555, lang)
    if (key !== null && key !== '') {
      state.dict.push({
        key: key,
        value: value,
        lang: lang || localStorage.getItem('language'), // 记录当前字段语言
        createtime: new Date().getTime() // 用于判断本地字典表的有效期
      })
      setLocalhost(state.dict)
    }
  },
  REMOVE_DICT: (state, key) => {
    for (let i = 0; i < state.dict.length; i++) {
      if (state.dict[i].key === key) {
        state.dict.splice(i, 1)
        setLocalhost(state.dict)
        return true
      }
    }
  },
  CLEAN_DICT: (state) => {
    state.dict = new Array()
    setLocalhost('')
  }
}

const actions = {
  // 获取字典数据
  async getDict({ dispatch, rootState }, { key, lang }) {
    if (typeof arguments[1] === 'string') key = arguments[1]
    if (!key) return []
    if (!lang) lang = rootState.app.language
    const data = await dispatch('getDicts')
    const storeDict = searchDictByKey(data, key, lang)
    if (storeDict) {
      if (key && key.indexOf('BASE_DATA') !== -1) {
        return storeDict.rows || storeDict
      }
      if (storeDict.rows) return storeDict.rows;
      return storeDict
    } else {
      const res = await getDicts(key)
      dispatch('setDict', { key, value: res })
      if (key && key.indexOf('BASE_DATA') !== -1) {
        return res.rows || res
      }
      if (res.rows) return res.rows;
      return res
    }
  },
  // 获取字典的值
  async getDictValue({ dispatch, rootState }, { key, code, lang }) {
    if (!code) return {}
    if (!lang) lang = rootState.app.language
    const arr = await dispatch('getDict', { key, lang }) || []
    const res = arr.filter(item => item.dictCode + '' === code) || []
    return res.length > 0 ? res[0] : {}
  },
  //  获取有效字典数据
  getDicts({ state, dispatch, rootState }, lang) {
    if (!lang) lang = rootState.app.language
    dispatch('refreshDict')
    // console.log(999, state.dict)
    return state.dict.filter(item => item.createtime && item.lang === lang)
  },
  // 更新字典，去除逾期字典数据
  refreshDict({ state, dispatch }) {
    for (let i = 0; i < state.dict.length; i++) {
      const item = state.dict[i]
      if (item.createtime && new Date().getTime() - item.createtime > DICT_CACHE_TIME) {
        dispatch('dict/removeDict', item.key, { root: true })
      }
    }
  },
  // 设置字典
  setDict({ commit, rootState }, data) {
    if (!data.lang) data.lang = rootState.app.language
    commit('SET_DICT', data)
  },
  // 删除字典
  removeDict({ commit }, key) {
    commit('REMOVE_DICT', key)
  },
  // 清空字典
  cleanDict({ commit }) {
    commit('CLEAN_DICT')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

