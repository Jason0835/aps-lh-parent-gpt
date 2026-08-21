import request, { downloadLink } from '@/utils/request'

/**
 * 查询缠绕盘-机台关系列表
 * @param {Object} query 查询条件
 * @returns
 */
export function listDiscMachine(query) {
  return request({
    url: '/gsq/discMachine/list',
    method: 'post',
    data: query
  })
}

/**
 * 获取缠绕盘-机台关系详细信息
 * @param {Number} id 主键ID
 * @returns
 */
export function getDiscMachineInfo(id) {
  return request({
    url: '/gsq/discMachine/getInfo/' + id,
    method: 'get'
  })
}

/**
 * 保存缠绕盘-机台关系（id为空新增，id不为空修改）
 * @param {Object} query 实体
 * @returns
 */
export function saveDiscMachine(query) {
  return request({
    url: '/gsq/discMachine/save',
    method: 'post',
    data: query
  })
}

/**
 * 删除缠绕盘-机台关系（逻辑删除）
 * @param {String|Array} ids 主键ID集合（逗号分隔字符串）
 * @returns
 */
export function removeDiscMachine(ids) {
  return request({
    url: '/gsq/discMachine/remove',
    method: 'post',
    params: { ids }
  })
}

/**
 * 校验缠绕盘+机台组合唯一性
 * @param {Object} query 实体
 * @returns
 */
export function checkDiscMachineUnique(query) {
  return request({
    url: '/gsq/discMachine/checkUnique',
    method: 'post',
    data: query
  })
}

/**
 * 导出缠绕盘-机台关系
 * @param {Object} query 查询条件
 * @returns
 */
export function exportDiscMachine(query) {
  return downloadLink('/gsq/discMachine/export', query)
}
