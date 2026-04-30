import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'nc/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'nc/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'nc/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'nc/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
