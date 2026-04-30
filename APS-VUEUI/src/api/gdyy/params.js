import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/gdyy/params/list',
    method: 'post',
    data: query
  })
}
export function editParams(query) {
  return request({
    url: '/gdyy/params/edit',
    method: 'post',
    data: query
  })
}
export function removeParams(query) {
  return request({
    url: '/gdyy/params/remove',
    method: 'post',
    data: query
  })
}


