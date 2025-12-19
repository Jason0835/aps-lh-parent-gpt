import request from '@/utils/request'

// =
export function listAccessoriesMachine(query) {
  return request({
    url: '/setting/accessoriesMachine/list',
    method: 'post',
    data: query
  })
}
export function removeAccessoriesMachine(query) {
  return request({
    url: '/setting/accessoriesMachine/remove',
    method: 'post',
    data: query
  })
}
export function saveAccessoriesMachine(query) {
  return request({
    url: '/setting/accessoriesMachine/save',
    method: 'post',
    data: query
  })
}
export function getRecipeMachineList(query) {
  return request({
    url: '/setting/accessoriesMachine/getRecipeMachineList',
    method: 'post',
    data: query
  })
}
