import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'dj/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'dj/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'dj/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'dj/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
