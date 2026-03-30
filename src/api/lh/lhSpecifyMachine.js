import request,{ downloadLink } from '@/utils/request'


export function listLhSpecifyMachine(query) {
  return request({
    url: '/lh/lhSpecifyMachine/list',
    method: 'post',
    data: query
  })
}
export function removeLhSpecifyMachine(query) {
  return request({
    url: '/lh/lhSpecifyMachine/remove',
    method: 'post',
    data: query
  })
}
export function editLhSpecifyMachine(query) {
  return request({
    url: '/lh/lhSpecifyMachine/save',
    method: 'post',
    data: query
  })
}
export function getLhMachineList(query) {
  return request({
    url: '/lh/lhSpecifyMachine/getMachineList',
    method: 'post',
    params: query
  })
}
