import request from '@/utils/request'

export function listTmLossSetting(query) {
  return request({
    url: '/tm/tmLossSetting/list',
    method: 'post',
    data: query
  })
}
export function saveTmLossSetting(data) {
  return request({
    url: '/tm/tmLossSetting/save',
    method: 'post',
    data: data
  })
}
export function removeTmLossSetting(query) {
  return request({
    url: '/tm/tmLossSetting/remove',
    method: 'post',
    data: query
  })
}
export function getTmLossSetting(id) {
  return request({
    url: '/tm/tmLossSetting/' + id,
    method: 'get'
  })
}
