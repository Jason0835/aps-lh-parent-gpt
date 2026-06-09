import request from '@/utils/request'

export function listTmCurlRoll(query) {
  return request({
    url: '/tm/tmCurlRoll/list',
    method: 'post',
    data: query
  })
}
export function saveTmCurlRoll(data) {
  return request({
    url: '/tm/tmCurlRoll/save',
    method: 'post',
    data: data
  })
}
export function removeTmCurlRoll(query) {
  return request({
    url: '/tm/tmCurlRoll/remove',
    method: 'post',
    data: query
  })
}
export function getTmCurlRoll(id) {
  return request({
    url: '/tm/tmCurlRoll/' + id,
    method: 'get'
  })
}
