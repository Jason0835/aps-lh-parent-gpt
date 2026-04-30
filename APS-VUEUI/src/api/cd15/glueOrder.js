import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'cd15/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'cd15/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'cd15/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'cd15/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
