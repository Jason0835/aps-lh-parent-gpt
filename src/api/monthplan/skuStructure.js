import request from '@/utils/request'
export function listSkuStructure(query) {
  return request({
    url: '/monthplan/mdmSkuStructureRef/list',
    method: 'post',
    data: query
  })
}
export function editSkuStructure(query) {
  return request({
    url: '/monthplan/mdmSkuStructureRef/save',
    method: 'post',
    data: query
  })
}
export function removeSkuStructure(query) {
  return request({
    url: '/monthplan/mdmSkuStructureRef/remove',
    method: 'post',
    data: query
  })
}