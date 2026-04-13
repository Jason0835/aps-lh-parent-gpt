import request from '@/utils/request'

// =
export function listMoldingParams(query) {
  return request({
    url: '/cx/cxKeyProduct/list',
    method: 'post',
    data: query
  })
}
export function editMoldingParams(query) {
  return request({
    url: '/cx/cxKeyProduct/save',
    method: 'post',
    data: query
  })
}
export function removeMoldingParams(query) {
  return request({
    url: '/cx/cxKeyProduct/remove',
    method: 'post',
    data: query
  })
}


