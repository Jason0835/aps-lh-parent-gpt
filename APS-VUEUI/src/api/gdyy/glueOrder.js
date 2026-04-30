import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'gdyy/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'gdyy/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'gdyy/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'gdyy/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
