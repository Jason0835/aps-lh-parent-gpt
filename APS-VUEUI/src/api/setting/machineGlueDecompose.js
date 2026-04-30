import request, { downloadLink } from '@/utils/request'

// =
export function listMachineGlueDecompose(query) {
  return request({
    url: '/setting/machineGlueDecompose/list',
    method: 'post',
    data: query
  })
}
export function removeMachineGlueDecompose(query) {
  return request({
    url: '/setting/machineGlueDecompose/remove',
    method: 'post',
    data: query
  })
}
export function saveMachineGlueDecompose(query) {
  return request({
    url: '/setting/machineGlueDecompose/save',
    method: 'post',
    data: query
  })
}
export function checkGlueMachineGlueDecomposeUnique(query) {
  return request({
    url: '/setting/machineGlueDecompose/checkGlueMachineGlueDecomposeUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/machineGlueDecompose/checkComplete',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/machineGlueDecompose/export', query);
}
