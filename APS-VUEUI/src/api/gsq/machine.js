import request,{ downloadLink } from '@/utils/request'

/**
 * 成型机台列表
 * @param {Object} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: 'gsq/machine/list',
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
    url: 'gsq/machine/edit',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: 'gsq/machine/checkMachineCodeUnique',
    method: 'post',
    data: query
  })
}

/**
 * 获取所有启用的钢丝圈机台信息（status=0），供下拉框数据源使用
 * @returns
 */
export function listEnabledMachines() {
  return request({
    url: 'gsq/machine/listEnabledMachines',
    method: 'get'
  })
}

export function exportData(params) {
  return downloadLink("/gsq/machine/export", params);
}

