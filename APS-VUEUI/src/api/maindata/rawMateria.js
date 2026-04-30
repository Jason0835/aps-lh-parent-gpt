import request from '@/utils/request'
export function listRawMaterialInfo(query) {
  return request({
    url: '/maindata/rawMaterialOutboundRecord/list',
    method: 'post',
    data: query
  })
}
export function mesCatch(query) {
  return request({
    url: '/maindata/rawMaterialOutboundRecord/mesCatch',
    method: 'post',
    data: query
  })
}