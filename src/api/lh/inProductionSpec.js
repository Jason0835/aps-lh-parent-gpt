import request from '@/utils/request'

// =
export function listInProductionSpec(query) {
  return request({
    url: '/lh/inProductionSpec/list',
    method: 'post',
    data: query
  })
}
export function editInProductionSpec(query) {
  return request({
    url: '/lh/inProductionSpec/edit',
    method: 'post',
    data: query
  })
}
export function removeInProductionSpec(query) {
  return request({
    url: '/lh/inProductionSpec/remove',
    method: 'post',
    data: query
  })
}


