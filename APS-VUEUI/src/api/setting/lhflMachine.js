import request, { downloadLink } from '@/utils/request'


export function listLhflMachine(query) {
  return request({
    url: '/setting/lhflMachine/list',
    method: 'post',
    data: query
  })
}
export function removeLhflMachine(query) {
  return request({
    url: '/setting/lhflMachine/remove',
    method: 'post',
    data: query
  })
}
export function saveLhflMachine(query) {
  return request({
    url: '/setting/lhflMachine/save',
    method: 'post',
    data: query
  })
}
export function exportData(query) {
  return downloadLink('/setting/machineGlueDecompose/export', query);
}
