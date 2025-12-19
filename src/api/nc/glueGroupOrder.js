import request from '@/utils/request'

export function listGlueGroupOrder(query) {
  return request({
    url: 'nc/glueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueGroupOrder(query) {
  return request({
    url: 'nc/glueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueGroupOrder(query) {
  return request({
    url: 'nc/glueGroupOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueGroupCodeUnique(query) {
  return request({
    url: 'nc/glueGroupOrder/checkGlueGroupCodeUnique',
    method: 'post',
    data: query
  })
}