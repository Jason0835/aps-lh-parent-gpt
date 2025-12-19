import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'xwyy/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'xwyy/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'xwyy/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'xwyy/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
