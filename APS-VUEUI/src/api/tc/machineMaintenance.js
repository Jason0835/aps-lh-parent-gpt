import request from '@/utils/request'

export function listTcMachineMaintenance(query) {
  return request({
    url: '/tc/tcMachineMaintenance/list',
    method: 'post',
    data: query
  })
}
export function saveTcMachineMaintenance(data) {
  return request({
    url: '/tc/tcMachineMaintenance/save',
    method: 'post',
    data: data
  })
}
export function removeTcMachineMaintenance(query) {
  return request({
    url: '/tc/tcMachineMaintenance/remove',
    method: 'post',
    data: query
  })
}
export function getTcMachineMaintenance(id) {
  return request({
    url: '/tc/tcMachineMaintenance/' + id,
    method: 'get'
  })
}
