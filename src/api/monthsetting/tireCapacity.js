import request, { downloadLink } from '@/utils/request'

// =
export function listTireCapacity(query) {
  return request({
    url: '/monthsetting/tireCapacity/list',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function saveTireCapacity(query) {
  return request({
    url: '/monthsetting/tireCapacity/save',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function removeTireCapacity(query) {
  return request({
    url: '/monthsetting/tireCapacity/remove',
    method: 'post',
    data: query
  })
}
export function getDemandInfo(query) {
  return request({
    url: '/monthsetting/tireCapacity/getDemandInfo',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}
export function getInfo(query) {
  return request({
    url: '/monthsetting/tireCapacity/getInfo',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: query
  })
}