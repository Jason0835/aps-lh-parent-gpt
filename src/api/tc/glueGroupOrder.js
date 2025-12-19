import request from '@/utils/request'

export function listGlueGroupOrder(query) {
  return request({
    url: 'tc/glueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueGroupOrder(query) {
  return request({
    url: 'tc/glueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueGroupOrder(query) {
  return request({
    url: 'tc/glueGroupOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueGroupCodeUnique(query) {
  return request({
    url: 'tc/glueGroupOrder/checkGlueGroupCodeUnique',
    method: 'post',
    data: query
  })
}
