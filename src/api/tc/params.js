import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/tc/params/list',
    method: 'post',
    data: query
  })
}
export function editParams(query) {
  return request({
    url: '/tc/params/edit',
    method: 'post',
    data: query
  })
}
export function removeParams(query) {
  return request({
    url: '/tc/params/remove',
    method: 'post',
    data: query
  })
}


