import request from '@/utils/request'

export function listCurlRoll(query) {
  return request({
    url: 'cd90/cd90CurlLength/list',
    method: 'post',
    data: query
  })
}
export function removeCurlRoll(query) {
  return request({
    url: 'cd90/cd90CurlLength/remove',
    method: 'post',
    data: query
  })
}
export function saveCurlRoll(query) {
  return request({
    url: 'cd90/cd90CurlLength/save',
    method: 'post',
    data: query
  })
}
export function checkCurlRollCodeUnique(query) {
  return request({
    url: 'cd90/cd90CurlLength/checkCurlRollCodeUnique',
    method: 'post',
    data: query
  })
}
