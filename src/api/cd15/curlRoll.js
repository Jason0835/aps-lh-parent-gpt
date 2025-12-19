import request from '@/utils/request'

export function listCurlRoll(query) {
  return request({
    url: 'cd15/cd15CurlLength/list',
    method: 'post',
    data: query
  })
}
export function removeCurlRoll(query) {
  return request({
    url: 'cd15/cd15CurlLength/remove',
    method: 'post',
    data: query
  })
}
export function saveCurlRoll(query) {
  return request({
    url: 'cd15/cd15CurlLength/save',
    method: 'post',
    data: query
  })
}
export function checkCurlRollCodeUnique(query) {
  return request({
    url: 'cd15/cd15CurlLength/checkCurlRollCodeUnique',
    method: 'post',
    data: query
  })
}
