import request from '@/utils/request'

//
export function listLineSideStock(query) {
  return request({
    url: '/cd90/lineSideStock/list',
    method: 'post',
    data: query
  })
}
export function syncLineSideStock(query) {
  return request({
    url: '/cd90/lineSideStock/syncStock',
    method: 'post',
    data: query
  })
}
