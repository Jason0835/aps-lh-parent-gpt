import request from '@/utils/request'

// =
export function listCodeColor(query) {
  return request({
    url: '/tc/sidewallCodeColor/list',
    method: 'post',
    data: query
  })
}
export function editCodeColor(query) {
  return request({
    url: '/tc/sidewallCodeColor/edit',
    method: 'post',
    data: query
  })
}
export function removeCodeColor(query) {
  return request({
    url: '/tc/sidewallCodeColor/remove',
    method: 'post',
    data: query
  })
}


