import request from '@/utils/request'
export function listSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRatio/list',
    method: 'post',
    data: query
  })
}
export function saveSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRatio/save',
    method: 'post',
    data: query
  })
}
export function removeSpecialMaterialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialRatio/remove',
    method: 'post',
    data: query
  })
}