import request from '@/utils/request'

export function listTcMouthPlate(query) {
  return request({ url: '/tc/tcMouthPlate/list', method: 'post', data: query })
}
export function saveTcMouthPlate(data) {
  return request({ url: '/tc/tcMouthPlate/save', method: 'post', data: data })
}
export function removeTcMouthPlate(query) {
  return request({ url: '/tc/tcMouthPlate/remove', method: 'post', data: query })
}
export function getTcMouthPlate(id) {
  return request({ url: '/tc/tcMouthPlate/' + id, method: 'get' })
}
