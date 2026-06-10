import request, { downloadLink } from '@/utils/request'

export function listStorageLaneLimit(query) {
  return request({ url: '/cd90/cd90StorageLaneLimit/list', method: 'post', data: query })
}
export function getStorageLaneLimit(id) {
  return request({ url: `/cd90/cd90StorageLaneLimit/getInfo/${id}`, method: 'get' })
}
export function addStorageLaneLimit(data) {
  return request({ url: '/cd90/cd90StorageLaneLimit/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateStorageLaneLimit(data) {
  return request({ url: '/cd90/cd90StorageLaneLimit/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delStorageLaneLimit(data) {
  return request({ url: '/cd90/cd90StorageLaneLimit/remove', method: 'post', data })
}
export function exportStorageLaneLimit(query) {
  return downloadLink('/cd90/cd90StorageLaneLimit/export', query)
}
