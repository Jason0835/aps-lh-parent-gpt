import request from '@/utils/request'

// =
export function listShiftLimit(query) {
  return request({
    url: '/cx/shiftLimit/list',
    method: 'post',
    data: query
  })
}
export function editShiftLimit(query) {
  return request({
    url: '/cx/shiftLimit/edit',
    method: 'post',
    data: query
  })
}
export function removeShiftLimit(query) {
  return request({
    url: '/cx/shiftLimit/remove',
    method: 'post',
    data: query
  })
}


