import request from '@/utils/request'

export function listCurlRoll(query) {
  return request({
    url: 'dj/curlRoll/list',
    method: 'post',
    data: query
  })
}
export function removeCurlRoll(query) {
  return request({
    url: 'dj/curlRoll/remove',
    method: 'post',
    data: query
  })
}
export function saveCurlRoll(query) {
  return request({
    url: 'dj/curlRoll/save',
    method: 'post',
    data: query
  })
}
export function checkCurlRollCodeUnique(query) {
  return request({
    url: 'dj/curlRoll/checkUnique',
    method: 'post',
    data: query
  })
}
