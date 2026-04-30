import request from '@/utils/request'

//
export function listBigRoll(query) {
  return request({
    url: '/cd15/bigRoll/list',
    method: 'post',
    data: query
  })
}
export function editBigRoll(query) {
  return request({
    url: '/cd15/bigRoll/save',
    method: 'post',
    data: query
  })
}
export function removeBigRoll(query) {
  return request({
    url: '/cd15/bigRoll/remove',
    method: 'post',
    data: query
  })
}
export function checkBigRollCodeUnique(query) {
  return request({
    url: '/cd15/bigRoll/checkBigRollCodeUnique',
    method: 'post',
    data: query
  })
}

