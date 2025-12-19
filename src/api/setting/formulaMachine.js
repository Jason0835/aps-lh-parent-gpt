import request from '@/utils/request'


export function listFormulaMachine(query) {
  return request({
    url: '/setting/formulaMachine/list',
    method: 'post',
    data: query
  })
}
export function removeFormulaMachine(query) {
  return request({
    url: '/setting/formulaMachine/remove',
    method: 'post',
    data: query
  })
}
export function saveFormulaMachine(query) {
  return request({
    url: '/setting/formulaMachine/save',
    method: 'post',
    data: query
  })
}

// =
export function getFormulaMachineList(query) {
  return request({
    url: '/setting/formulaMachine/getFormulaMachineList',
    method: 'post',
    data: query
  })
}
export function getRecipeMachineList(query) {
  return request({
    url: '/setting/formulaMachine/getRecipeMachineList',
    method: 'post',
    data: query
  })
}
