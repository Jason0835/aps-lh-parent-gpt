import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'gsq/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'gsq/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'gsq/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'gsq/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
