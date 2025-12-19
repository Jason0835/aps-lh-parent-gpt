import request from '@/utils/request'

// =
export function listMoldingParams(query) {
  return request({
    url: '/cx/params/list',
    method: 'post',
    data: query
  })
}
export function editMoldingParams(query) {
  return request({
    url: '/cx/params/edit',
    method: 'post',
    data: query
  })
}
export function removeMoldingParams(query) {
  return request({
    url: '/cx/params/remove',
    method: 'post',
    data: query
  })
}


