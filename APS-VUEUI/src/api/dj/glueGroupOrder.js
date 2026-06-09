import request from '@/utils/request'

export function listGlueGroupOrder(query) {
  return request({
    url: 'dj/glueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueGroupOrder(query) {
  return request({
    url: 'dj/glueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueGroupOrder(query) {
  return request({
    url: 'dj/glueGroupOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueGroupCodeUnique(query) {
  return request({
    url: 'dj/glueGroupOrder/checkGlueGroupCodeUnique',
    method: 'post',
    data: query
  })
}