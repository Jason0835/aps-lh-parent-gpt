import request from '@/utils/request'

export function listTmGlueOrder(query) {
  return request({
    url: '/tm/tmGlueOrder/list',
    method: 'post',
    data: query
  })
}
export function saveTmGlueOrder(data) {
  return request({
    url: '/tm/tmGlueOrder/save',
    method: 'post',
    data: data
  })
}
export function removeTmGlueOrder(query) {
  return request({
    url: '/tm/tmGlueOrder/remove',
    method: 'post',
    data: query
  })
}
export function getTmGlueOrder(id) {
  return request({
    url: '/tm/tmGlueOrder/' + id,
    method: 'get'
  })
}
