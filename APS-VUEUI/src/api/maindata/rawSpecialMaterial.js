import request from '@/utils/request'
export function listSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRecord/list',
    method: 'post',
    data: query
  })
}
export function saveSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRecord/save',
    method: 'post',
    data: query
  })
}
export function removeSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRecord/remove',
    method: 'post',
    data: query
  })
}