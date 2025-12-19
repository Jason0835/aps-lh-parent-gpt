import request from '@/utils/request'
export function listStructure(query) {
  return request({
    url: '/monthplan/mdmStructureLhRatio/list',
    method: 'post',
    data: query
  })
}
export function removeStructure(query) {
  return request({
    url: '/monthplan/mdmStructureLhRatio/remove',
    method: 'post',
    data: query
  })
}
export function saveStructure(query) {
  return request({
    url: '/monthplan/mdmStructureLhRatio/save',
    method: 'post',
    data: query
  })
}