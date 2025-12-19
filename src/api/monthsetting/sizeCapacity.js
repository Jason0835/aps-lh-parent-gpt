import request, { downloadLink } from '@/utils/request'

// =
export function listSizeCapacity(query) {
  return request({
    url: '/monthsetting/sizeCapacity/list',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function saveSizeCapacity(query) {
  return request({
    url: '/monthsetting/sizeCapacity/save',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function removeSizeCapacity(query) {
  return request({
    url: '/monthsetting/sizeCapacity/remove',
    method: 'post',
    data: query
  })
}
export function getDemandInfo(query) {
  return request({
    url: '/monthsetting/sizeCapacity/getDemandInfo',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function getInfo(query) {
  return request({
    url: '/monthsetting/sizeCapacity/getInfo',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}