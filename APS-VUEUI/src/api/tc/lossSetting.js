import request from '@/utils/request'

export function listTcLossSetting(query) {
  return request({
    url: '/tc/tcLossSetting/list',
    method: 'post',
    data: query
  })
}
export function saveTcLossSetting(data) {
  return request({
    url: '/tc/tcLossSetting/save',
    method: 'post',
    data: data
  })
}
export function removeTcLossSetting(query) {
  return request({
    url: '/tc/tcLossSetting/remove',
    method: 'post',
    data: query
  })
}
export function getTcLossSetting(id) {
  return request({
    url: '/tc/tcLossSetting/' + id,
    method: 'get'
  })
}
