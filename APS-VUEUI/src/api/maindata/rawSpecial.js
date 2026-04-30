import request from '@/utils/request'
export function listSpecialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialStock/list',
    method: 'post',
    data: query
  })
}
export function saveSpecialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialStock/save',
    method: 'post',
    data: query
  })
}
export function removeSpecialInfo(query) {
  return request({
    url: '/maindata/rawSpecialMaterialStock/remove',
    method: 'post',
    data: query
  })
}