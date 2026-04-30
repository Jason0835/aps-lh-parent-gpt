import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'tq/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'tq/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'tq/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'tq/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
