import request from '@/utils/request'

export function listTcShiftConfig(query) {
  return request({
    url: '/tc/tcShiftConfig/list',
    method: 'post',
    data: query
  })
}
export function saveTcShiftConfig(data) {
  return request({
    url: '/tc/tcShiftConfig/save',
    method: 'post',
    data: data
  })
}
export function removeTcShiftConfig(query) {
  return request({
    url: '/tc/tcShiftConfig/remove',
    method: 'post',
    data: query
  })
}
export function getTcShiftConfig(id) {
  return request({
    url: '/tc/tcShiftConfig/' + id,
    method: 'get'
  })
}
