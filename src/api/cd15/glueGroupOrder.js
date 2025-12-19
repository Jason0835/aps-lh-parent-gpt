import request from '@/utils/request'

export function listGlueGroupOrder(query) {
  return request({
    url: 'cd15/glueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueGroupOrder(query) {
  return request({
    url: 'cd15/glueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueGroupOrder(query) {
  return request({
    url: 'cd15/glueGroupOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueGroupCodeUnique(query) {
  return request({
    url: 'cd15/glueGroupOrder/sacheckGlueGroupCodeUniqueve',
    method: 'post',
    data: query
  })
}
