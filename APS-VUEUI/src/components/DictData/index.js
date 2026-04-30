import Vue from 'vue'
import store from '@/store'
import DataDict from '@/utils/dict'
import { getDicts as getDicts } from '@/api/system/dict/data'
import { getBaseData, baseDataRequest } from '@/api/system/dict/baseData'

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

function install() {
  Vue.use(DataDict, {
    metas: {
      '*': {
        labelField: 'dictLabel',
        valueField: 'dictValue',
        async request(dictMeta) {
          const data = await store.dispatch('dict/getDicts')
          // const storeDict = searchDictByKey(store.getters.dict, dictMeta.type)
          const storeDict = searchDictByKey(data, dictMeta.type, store.getters.language)
          if (storeDict) {
            return new Promise(resolve => { resolve(storeDict) })
          } else {
            return new Promise(async (resolve, reject) => {
              let requestFn
              if (baseDataRequest[dictMeta.type]) {
                requestFn = getBaseData(dictMeta.type)
              } else {
                requestFn = getDicts(dictMeta.type)
              }
              requestFn.then(res => {
                store.dispatch('dict/setDict', {key: dictMeta.type, value: res})
                resolve(res)
              }).catch(error => {
                reject(error)
              })
            })
          }
        }
      }
    }
  })
}

export default {
  install
}
