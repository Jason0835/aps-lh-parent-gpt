import request from '@/utils/request'

export function listTcMachineSpeed(query) {
  return request({
    url: '/tc/tcMachineSpeed/list',
    method: 'post',
    data: query
  })
}
export function saveTcMachineSpeed(data) {
  return request({
    url: '/tc/tcMachineSpeed/save',
    method: 'post',
    data: data
  })
}
export function removeTcMachineSpeed(query) {
  return request({
    url: '/tc/tcMachineSpeed/remove',
    method: 'post',
    data: query
  })
}
export function getTcMachineSpeed(id) {
  return request({
    url: '/tc/tcMachineSpeed/' + id,
    method: 'get'
  })
}
