import request from '@/utils/request'

export function listTcGlueGroupOrder(query) {
  return request({
    url: '/tc/tcGlueGroupOrder/list',
    method: 'post',
    data: query
  })
}
export function saveTcGlueGroupOrder(data) {
  return request({
    url: '/tc/tcGlueGroupOrder/save',
    method: 'post',
    data: data
  })
}
export function removeTcGlueGroupOrder(query) {
  return request({
    url: '/tc/tcGlueGroupOrder/remove',
    method: 'post',
    data: query
  })
}
export function getTcGlueGroupOrder(id) {
  return request({
    url: '/tc/tcGlueGroupOrder/' + id,
    method: 'get'
  })
}
