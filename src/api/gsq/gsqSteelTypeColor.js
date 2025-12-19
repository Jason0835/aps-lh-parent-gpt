import request from '@/utils/request'

// =
export function listSteelTypeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/list',
    method: 'post',
    data: query
  })
}
export function editSteelTypeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/edit',
    method: 'post',
    data: query
  })
}
export function removeSteelTypeColor(query) {
  return request({
    url: '/gsq/gsqSteelTypeColor/remove',
    method: 'post',
    data: query
  })
}


