import request from '@/utils/request'

// =
export function listDimensionLimit(query) {
  return request({
    url: '/cx/dimensionLimit/list',
    method: 'post',
    data: query
  })
}
export function editDimensionLimit(query) {
  return request({
    url: '/cx/dimensionLimit/edit',
    method: 'post',
    data: query
  })
}
export function removeDimensionLimit(query) {
  return request({
    url: '/cx/dimensionLimit/remove',
    method: 'post',
    data: query
  })
}


