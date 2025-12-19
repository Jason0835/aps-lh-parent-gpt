import request,{ downloadLink } from '@/utils/request'

/**
 * 成型机台列表
 * @param {Object} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: 'xwyy/machine/list',
    method: 'post',
    data: query
  })
}
/**
 * 编辑成型机台
 * @param {Object} query
 * @returns
 */
export function editMachine(query) {
  return request({
    url: 'xwyy/machine/edit',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: 'xwyy/machine/checkMachineCodeUnique',
    method: 'post',
    data: query
  })
}
export function exportData(query) {
  return downloadLink("/xwyy/machine/export", query);
}
