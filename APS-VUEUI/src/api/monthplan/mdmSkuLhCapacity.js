import request from '@/utils/request'
export function listCapacity(query) {
  return request({
    url: '/monthplan/mdmSkuLhCapacity/list',
    method: 'post',
    data: query
  })
}
export function removeCapacity(query) {
  return request({
    url: '/monthplan/mdmSkuLhCapacity/remove',
    method: 'post',
    data: query
  })
}
export function saveCapacity(query) {
  return request({
    url: '/monthplan/mdmSkuLhCapacity/save',
    method: 'post',
    data: query
  })
}
export function getCapacity(query) {
  return request({
    url: '/monthplan/mdmSkuLhCapacity/getClassCapacity',
    method: 'post',
    data: query
  })
}