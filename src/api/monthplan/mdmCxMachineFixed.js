import request from '@/utils/request'

// =
export function listCxMachineFixed(query) {
  return request({
    url: '/monthplan/mdmCxMachineFixed/list',
    method: 'post',
    data: query
  })
}
export function editCxMachineFixed(query) {
  return request({
    url: '/monthplan/mdmCxMachineFixed/save',
    method: 'post',
    data: query
  })
}
export function removeCxMachineFixed(query) {
  return request({
    url: '/monthplan/mdmCxMachineFixed/remove',
    method: 'post',
    data: query
  })
}
