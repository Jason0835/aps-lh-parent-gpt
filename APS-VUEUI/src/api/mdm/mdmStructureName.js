import request from '@/utils/request'
export function listStructureName(query) {
  return request({
    url: '/mdm/mdmStructureName/list',
    method: 'post',
    data: query
  })
}
export function saveStructureName(query) {
  return request({
    url: '/mdm/mdmStructureName/save',
    method: 'post',
    data: query
  })
}
export function removeStructureName(query) {
  return request({
    url: '/mdm/mdmStructureName/remove',
    method: 'post',
    data: query
  })
}