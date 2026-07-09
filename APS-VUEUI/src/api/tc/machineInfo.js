import request from '@/utils/request'

export function listTcMachineInfo(query) {
  return request({
    url: '/tc/tcMachineInfo/list',
    method: 'post',
    data: query
  })
}
export function saveTcMachineInfo(data) {
  return request({
    url: '/tc/tcMachineInfo/save',
    method: 'post',
    data: data
  })
}
export function removeTcMachineInfo(query) {
  return request({
    url: '/tc/tcMachineInfo/remove',
    method: 'post',
    data: query
  })
}
export function getTcMachineInfo(id) {
  return request({
    url: '/tc/tcMachineInfo/' + id,
    method: 'get'
  })
}
