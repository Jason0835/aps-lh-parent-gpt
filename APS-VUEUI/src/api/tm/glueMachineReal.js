import request from '@/utils/request'

export function listGlueMachineReal(query) {
  return request({
    url: '/tm/tmGlueMachineReal/list',
    method: 'post',
    data: query
  })
}
export function saveGlueMachineReal(data) {
  return request({
    url: '/tm/tmGlueMachineReal/save',
    method: 'post',
    data: data
  })
}
export function removeGlueMachineReal(query) {
  return request({
    url: '/tm/tmGlueMachineReal/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/tm/tmGlueMachineReal/' + id,
    method: 'get'
  })
}
