import request from '@/utils/request'

export function listTmMouthPlate(query) {
  return request({ url: '/tm/tmMouthPlate/list', method: 'post', data: query })
}
export function saveTmMouthPlate(data) {
  return request({ url: '/tm/tmMouthPlate/save', method: 'post', data: data })
}
export function removeTmMouthPlate(query) {
  return request({ url: '/tm/tmMouthPlate/remove', method: 'post', data: query })
}
export function getTmMouthPlate(id) {
  return request({ url: '/tm/tmMouthPlate/' + id, method: 'get' })
}
