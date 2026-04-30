import request from '@/utils/request'

export function listCurlRoll(query) {
  return request({
    url: 'nc/curlRoll/list',
    method: 'post',
    data: query
  })
}
export function removeCurlRoll(query) {
  return request({
    url: 'nc/curlRoll/remove',
    method: 'post',
    data: query
  })
}
export function saveCurlRoll(query) {
  return request({
    url: 'nc/curlRoll/save',
    method: 'post',
    data: query
  })
}
export function checkCurlRollCodeUnique(query) {
  return request({
    url: 'nc/curlRoll/checkCurlRollCodeUnique',
    method: 'post',
    data: query
  })
}
