import request from '@/utils/request'

// =
export function listCodeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/list',
    method: 'post',
    data: query
  })
}
export function editCodeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/save',
    method: 'post',
    data: query
  })
}
export function removeCodeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/remove',
    method: 'post',
    data: query
  })
}
export function checkRollCodeUnique(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/checkRollCodeUnique',
    method: 'post',
    data: query
  })
}


