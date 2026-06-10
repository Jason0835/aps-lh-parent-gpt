import request from '@/utils/request'

export function listTmMachineInfo(query) {
  return request({
    url: '/tm/tmMachineInfo/list',
    method: 'post',
    data: query
  })
}
export function saveTmMachineInfo(data) {
  return request({
    url: '/tm/tmMachineInfo/save',
    method: 'post',
    data: data
  })
}
export function removeTmMachineInfo(query) {
  return request({
    url: '/tm/tmMachineInfo/remove',
    method: 'post',
    data: query
  })
}
export function getTmMachineInfo(id) {
  return request({
    url: '/tm/tmMachineInfo/' + id,
    method: 'get'
  })
}
