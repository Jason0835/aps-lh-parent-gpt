import request from '@/utils/request'

export function listTcDjSharedMachine(query) {
  return request({ url: '/tc/tcDjSharedMachine/list', method: 'post', data: query })
}
export function saveTcDjSharedMachine(data) {
  return request({ url: '/tc/tcDjSharedMachine/save', method: 'post', data: data })
}
export function removeTcDjSharedMachine(query) {
  return request({ url: '/tc/tcDjSharedMachine/remove', method: 'post', data: query })
}
export function getTcDjSharedMachine(id) {
  return request({ url: '/tc/tcDjSharedMachine/' + id, method: 'get' })
}
