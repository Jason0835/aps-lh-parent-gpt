import request from '@/utils/request'
export function listAreaCapaInfo(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/list',
    method: 'post',
    data: query
  })
}

export function saveAreaCapaInfo(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/save',
    method: 'post',
    data: query
  })
}
export function removeAreaCapaInfo(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/remove',
    method: 'post',
    data: query
  })
}
export function copyAreaCapaInfo(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/copy',
    method: 'post',
    data: query
  })
}
export function copyCheckAreaCapaInfo(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/checkBeforeCopy',
    method: 'post',
    data: query
  })
}
export function areaList(query) {
  return request({
    url: '/monthplan/dpArea/list',
    method: 'post',
    data: query
  })
}
export function getSumCapacityAllocation(query) {
  return request({
    url: '/monthplan/mdmAreaCapaAllocation/getSumCapacityAllocation',
    method: 'post',
    data: query
  })
}