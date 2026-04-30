import request,{ downloadLink } from '@/utils/request'

/**
 * 成型机台列表
 * @param {Object} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: 'gdyy/machine/list',
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
    url: 'gdyy/machine/edit',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: 'gdyy/machine/checkMachineCodeUnique',
    method: 'post',
    data: query
  })
}

/**
 * 导出机台信息
 * @param {*} params
 * @returns
 */
export function exportData(params) {
  return downloadLink("/gdyy/machine/export", params);
}
