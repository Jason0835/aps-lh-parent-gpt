import request from '@/utils/request'

export function listTcCurlRoll(query) {
  return request({
    url: '/tc/tcCurlRoll/list',
    method: 'post',
    data: query
  })
}
export function saveTcCurlRoll(data) {
  return request({
    url: '/tc/tcCurlRoll/save',
    method: 'post',
    data: data
  })
}
export function removeTcCurlRoll(query) {
  return request({
    url: '/tc/tcCurlRoll/remove',
    method: 'post',
    data: query
  })
}
export function getTcCurlRoll(id) {
  return request({
    url: '/tc/tcCurlRoll/' + id,
    method: 'get'
  })
}
