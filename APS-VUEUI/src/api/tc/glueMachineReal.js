import request from '@/utils/request'

export function listGlueMachineReal(query) {
  return request({
    url: '/tc/tcGlueMachineReal/list',
    method: 'post',
    data: query
  })
}
export function saveGlueMachineReal(data) {
  return request({
    url: '/tc/tcGlueMachineReal/save',
    method: 'post',
    data: data
  })
}
export function removeGlueMachineReal(query) {
  return request({
    url: '/tc/tcGlueMachineReal/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/tc/tcGlueMachineReal/' + id,
    method: 'get'
  })
}
