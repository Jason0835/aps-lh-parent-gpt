import request from '@/utils/request'

export function listTmShiftConfig(query) {
  return request({
    url: '/tm/tmShiftConfig/list',
    method: 'post',
    data: query
  })
}
export function saveTmShiftConfig(data) {
  return request({
    url: '/tm/tmShiftConfig/save',
    method: 'post',
    data: data
  })
}
export function removeTmShiftConfig(query) {
  return request({
    url: '/tm/tmShiftConfig/remove',
    method: 'post',
    data: query
  })
}
export function getTmShiftConfig(id) {
  return request({
    url: '/tm/tmShiftConfig/' + id,
    method: 'get'
  })
}
