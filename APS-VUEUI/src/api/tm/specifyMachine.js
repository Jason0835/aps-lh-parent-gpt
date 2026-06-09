import request from '@/utils/request'

export function listTmSpecifyMachine(query) {
  return request({ url: '/tm/tmSpecifyMachine/list', method: 'post', data: query })
}
export function saveTmSpecifyMachine(data) {
  return request({ url: '/tm/tmSpecifyMachine/save', method: 'post', data: data })
}
export function removeTmSpecifyMachine(query) {
  return request({ url: '/tm/tmSpecifyMachine/remove', method: 'post', data: query })
}
export function getTmSpecifyMachine(id) {
  return request({ url: '/tm/tmSpecifyMachine/' + id, method: 'get' })
}
