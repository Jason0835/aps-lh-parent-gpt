import request from '@/utils/request'

// cxPersionTrainSetting
export function listCxPersionTrainSetting(query) {
  return request({
    url: '/cx/cxPersionTrainSetting/list',
    method: 'post',
    data: query
  })
}
export function saveCxPersionTrainSetting(query) {
  return request({
    url: '/cx/cxPersionTrainSetting/save',
    method: 'post',
    data: query
  })
}
export function saveCxPersionTrainSettingList(query) {
  return request({
    url: '/cx/cxPersionTrainSetting/saveList',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}



