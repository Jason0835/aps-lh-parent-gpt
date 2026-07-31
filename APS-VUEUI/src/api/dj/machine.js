import request, { downloadLink } from '@/utils/request'

/**
 * 成型机台列表
 * @param {Object} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: 'dj/machine/list',
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
    url: 'dj/machine/save',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: 'dj/machine/checkMachineCodeUnique',
    method: 'post',
    data: query
  })
}
/**
 * 删除成型机台
 * @param {Array|String} ids
 * @returns
 */
export function removeMachine(ids) {
  return request({
    url: 'dj/machine/remove',
    method: 'post',
    params: { ids: ids instanceof Array ? ids.join(',') : ids }
  })
}

export function exportData(query) {
  return downloadLink("/dj/machine/export", query);
}
