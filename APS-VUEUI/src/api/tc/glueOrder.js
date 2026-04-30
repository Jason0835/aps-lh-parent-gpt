import request from '@/utils/request'

export function listGlueOrder(query) {
  return request({
    url: 'tc/glueOrder/list',
    method: 'post',
    data: query
  })
}
export function removeGlueOrder(query) {
  return request({
    url: 'tc/glueOrder/remove',
    method: 'post',
    data: query
  })
}
export function saveGlueOrder(query) {
  return request({
    url: 'tc/glueOrder/save',
    method: 'post',
    data: query
  })
}
export function checkGlueCodeUnique(query) {
  return request({
    url: 'tc/glueOrder/checkGlueCodeUnique',
    method: 'post',
    data: query
  })
}
