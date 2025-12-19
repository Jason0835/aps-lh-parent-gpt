import request from '@/utils/request'

// =
export function listCloseOutRange(query) {
  return request({
    url: '/cx/closeOutRange/list',
    method: 'post',
    data: query
  })
}
export function editCloseOutRange(query) {
  return request({
    url: '/cx/closeOutRange/edit',
    method: 'post',
    data: query
  })
}
export function removeCloseOutRange(query) {
  return request({
    url: '/cx/closeOutRange/remove',
    method: 'post',
    data: query
  })
}


