import request, {downloadLink} from '@/utils/request'

/**
 * 根据条件查询硫化机台信息
 * @param {*} query
 * @returns
 */
export function listMachine(query) {
  return request({
    url: '/lh/info/list',
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
    url: '/lh/info/save',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: '/lh/info/checkMachineCodeUnique',
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
    url: '/lh/info/remove',
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
  return downloadLink("/lh/info/export", params);
}

