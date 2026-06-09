import request from '@/utils/request'

export function listTmGlueGroupOrder(query) {
  return request({
    url: '/tm/tmGlueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function saveTmGlueGroupOrder(data) {
  return request({
    url: '/tm/tmGlueGroupOrder/save',
    method: 'post',
    data: data
  })
}
export function removeTmGlueGroupOrder(query) {
  return request({
    url: '/tm/tmGlueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function getTmGlueGroupOrder(id) {
  return request({
    url: '/tm/tmGlueGroupOrder/' + id,
    method: 'get'
  })
}
