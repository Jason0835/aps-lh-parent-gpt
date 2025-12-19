import request from '@/utils/request'

// =
export function listShareMoldInfo(query) {
  return request({
    url: '/cx/shareMoldInfo/list',
    method: 'post',
    data: query
  })
}
export function editShareMoldInfo(query) {
  return request({
    url: '/cx/shareMoldInfo/edit',
    method: 'post',
    data: query
  })
}
export function removeShareMoldInfo(query) {
  return request({
    url: '/cx/shareMoldInfo/remove',
    method: 'post',
    data: query
  })
}


