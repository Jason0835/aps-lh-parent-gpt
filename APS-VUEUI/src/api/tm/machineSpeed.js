import request from '@/utils/request'

export function listTmMachineSpeed(query) {
  return request({
    url: '/tm/tmMachineSpeed/list',
    method: 'post',
    data: query
  })
}
export function saveTmMachineSpeed(data) {
  return request({
    url: '/tm/tmMachineSpeed/save',
    method: 'post',
    data: data
  })
}
export function removeTmMachineSpeed(query) {
  return request({
    url: '/tm/tmMachineSpeed/remove',
    method: 'post',
    data: query
  })
}
export function getTmMachineSpeed(id) {
  return request({
    url: '/tm/tmMachineSpeed/' + id,
    method: 'get'
  })
}
