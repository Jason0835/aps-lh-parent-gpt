import request from '@/utils/request'

/**
 * 根据条件查询硫化机台信息
 * @param {*} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: '/lh/machine/list',
    method: 'post',
    data: query
  })
}

/**
 * 修改硫化机台信息（id不为空）
 * @param {*} query
 * @returns
 */
export function editMachine(query) {
  return request({
    url: '/lh/machine/edit',
    method: 'post',
    data: query
  })
}

/**
 * 删除硫化机台信息（id不为空）
 * @param {*} query
 * @returns
 */
export function removeMachine(query) {
  return request({
    url: '/lh/machine/remove',
    method: 'post',
    data: query
  })
}

/**
 * 导出硫化机台信息
 * @param {*} params
 * @returns
 */
export function exportData(params) {
  return downloadLink("/lh/machine/export", params);
}

