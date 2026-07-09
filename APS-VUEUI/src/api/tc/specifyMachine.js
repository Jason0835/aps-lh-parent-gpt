import request from '@/utils/request'

export function listTcSpecifyMachine(query) {
  return request({ url: '/tc/tcSpecifyMachine/list', method: 'post', data: query })
}
export function saveTcSpecifyMachine(data) {
  return request({ url: '/tc/tcSpecifyMachine/save', method: 'post', data: data })
}
export function removeTcSpecifyMachine(query) {
  return request({ url: '/tc/tcSpecifyMachine/remove', method: 'post', data: query })
}
export function getTcSpecifyMachine(id) {
  return request({ url: '/tc/tcSpecifyMachine/' + id, method: 'get' })
}
