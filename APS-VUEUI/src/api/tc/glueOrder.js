import request from '@/utils/request'

export function listTcGlueOrder(query) {
  return request({
    url: '/tc/tcGlueOrder/list',
    method: 'post',
    data: query
  })
}
export function saveTcGlueOrder(data) {
  return request({
    url: '/tc/tcGlueOrder/save',
    method: 'post',
    data: data
  })
}
export function removeTcGlueOrder(query) {
  return request({
    url: '/tc/tcGlueOrder/remove',
    method: 'post',
    data: query
  })
}
export function getTcGlueOrder(id) {
  return request({
    url: '/tc/tcGlueOrder/' + id,
    method: 'get'
  })
}
